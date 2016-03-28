package com.amchealth.mqtt_client_api;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.amchealth.test.UtilSsl;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;

public class SocketTest {

	private static final TimeUnit UNIT = TimeUnit.SECONDS;
	static final String URL = "tcp://localhost:3000";
	private Socket socket;

	@Before
	public void setUpTestSocket() {
		socket = createTestSocket();
	}

	private Socket createTestSocket() {
		return new Socket(URL, "clientId");//, UtilSsl.getSslContext());
	}

	@After
	public void tearDownTestSocket() {
		if (socket.isConnected())
			socket.disconnect();
	}

	@Test
	public void shouldBeDisconnectedAfterCreated() {
		assertTrue(socket != null);
		assertFalse(socket.isConnected());
	}

	@Test
	public void shouldBeAbleToConnect() {
		final CountDownLatch signal = new CountDownLatch(1);

		EventEmitter<String> connectEmitter = socket.getConnectEmitter();
		connectEmitter.on("socket::connected", new Event<String>() {
			@Override
			public void onEmit(String... data) {
				signal.countDown();
			}
		});

		socket.connect();

		try {
			signal.await(2, UNIT);
			assertTrue(socket.isConnected());
			assertEquals(0, signal.getCount());
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void shouldBeAbleToDisconnetAfterConnect() {
		final CountDownLatch signal = new CountDownLatch(2);

		EventEmitter<String> connectEmitter = socket.getConnectEmitter();
		connectEmitter.on("socket::connected", new Event<String>() {
			@Override
			public void onEmit(String... data) {
				signal.countDown();
			}
		});
		connectEmitter.on("socket::disconnected", new Event<String>() {
			@Override
			public void onEmit(String... data) {
				signal.countDown();
			}
		});

		socket.connect();
		socket.disconnect();

		try {
			signal.await(2, UNIT);
			assertFalse(socket.isConnected());
			assertEquals(0, signal.getCount());
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void shouldBeAbleToPublishAMessage() {
		final CountDownLatch signal = new CountDownLatch(1);

		EventEmitter<String> connectEmitter = socket.getConnectEmitter();
		connectEmitter.on("defaultTopicResponses", new Event<String>() {
			@Override
			public void onEmit(String... data) {
				signal.countDown();
			}
		});

		socket.connect();
		socket.subscribe("defaultTopicResponses");
		socket.publish("defaultTopic","test message");

		try {
			signal.await(2, UNIT);
			assertTrue(socket.isConnected());
			assertEquals(0, signal.getCount());
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void shouldHandleMultipleConnections() throws InterruptedException {
		final CountDownLatch connectState = new CountDownLatch(2);
		final CountDownLatch disconnectState1 = new CountDownLatch(1);
		final CountDownLatch disconnectState2 = new CountDownLatch(1);
		MqttWrapper socketClient = socket.socketClient;
		Socket socket2 = createTestSocket();
		EventEmitter<String> connectEmitter1 = socket.getConnectEmitter();
		EventEmitter<String> connectEmitter2 = socket2.getConnectEmitter();

		Event<String> cb = new Event<String>() {
			@Override
			public void onEmit(String... data) {
				connectState.countDown();
			}
		};
		Event<String> dcb1 = new Event<String>() {
			@Override
			public void onEmit(String... data) {
				disconnectState1.countDown();
			}
		};
		Event<String> dcb2 = new Event<String>() {
			@Override
			public void onEmit(String... data) {
				disconnectState2.countDown();
			}
		};
		connectEmitter1.once("socket::connected", cb);
		connectEmitter2.once("socket::connected", cb);
		connectEmitter1.once("socket::disconnected", dcb1);
		connectEmitter2.once("socket::disconnected", dcb2);

		socket.connect();
		socket2.connect();
		connectState.await(2, UNIT);
		assertEquals(0, connectState.getCount());
		assertEquals(2, socketClient.eventEmitters.size());

		socket.disconnect();

		disconnectState1.await(2, UNIT);
		assertEquals(0, disconnectState1.getCount());
		assertEquals(1, socketClient.eventEmitters.size());
		assertFalse(socket.isConnected());
		assertTrue(socket2.isConnected());

		socket2.disconnect();
		disconnectState2.await(2, UNIT);
		assertEquals(0, disconnectState2.getCount());
		assertEquals(0, socketClient.eventEmitters.size());
		assertFalse(socket2.isConnected());
		assertFalse(socket.isConnected());
	}
}
