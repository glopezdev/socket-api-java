package com.amchealth.mqtt_client_api;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.scispike.callback.EventEmitter;

public class MqttWrapper {
	final Set<EventEmitter<String>> eventEmitters = new LinkedHashSet<EventEmitter<String>>();

	MqttClient socketClient;

	private SSLContext sslContext;

	MqttWrapper(String baseURL, String clientId) {
		try {
			socketClient = new MqttClient(baseURL, clientId);
		} catch (MqttException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		socketClient.setCallback(new MqttCallback() {
			public void messageArrived(String topic, MqttMessage message)
					throws Exception {
				String msg = new String(message.getPayload());
				emit(topic, msg);
			}

			public void deliveryComplete(IMqttDeliveryToken arg0) {
				System.out.println("message delivered ");
			}

			public void connectionLost(Throwable arg0) {
				arg0.printStackTrace();
				System.out.println("On connection lost " + arg0.getMessage());
				emit("socket::disconnected");
			}
		});
	}

	public void connect(EventEmitter<String> emitter) {
		if (!isConnected()) {
			MqttConnectOptions options = new MqttConnectOptions();
			if(sslContext != null)
				options.setSocketFactory(sslContext.getSocketFactory());
			try {
				socketClient.connect(options);
				eventEmitters.add(emitter);
				emitter.emit("socket::connected");
			} catch (MqttSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			eventEmitters.add(emitter);
			emitter.emit("socket::connected");
		}
	}

	
	public void subscribe(String topic) {
		try {
			socketClient.subscribe(topic);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isConnected() {
		return socketClient.isConnected();
	}

	public void publish(String topic, MqttMessage message) {
		try {
			socketClient.publish(topic, message);
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void disconnect(EventEmitter<String> eventEmitter) {

		eventEmitters.remove(eventEmitter);
		if (eventEmitters.size() == 0) {
			try {
				socketClient.disconnect();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			eventEmitters.remove(eventEmitter);
		}
		eventEmitter.emit("socket::disconnected");
		eventEmitter.removeAllListeners();

	}
	
	private void emit(String message, String... data) {
		for (EventEmitter<String> e : eventEmitters) {
			e.emit(message, data);
		}
	}

	public void setSSLContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}
	
}
