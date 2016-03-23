package com.amchealth.mqtt_client_api;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.scispike.callback.EventEmitter;

public class MqttWrapper {
	final Set<EventEmitter<String>> eventEmitters = new LinkedHashSet<EventEmitter<String>>();

	MqttClient socketClient;

	MqttWrapper(String baseURL, String clientId) {
		try {
			socketClient = new MqttClient(baseURL, clientId);
		} catch (MqttException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		socketClient.setCallback(new MqttCallback() {

			public void messageArrived(String arg0, MqttMessage arg1)
					throws Exception {
				String msg = new String(arg1.getPayload());
				System.out.println("message arrived " + arg0 + " - " + msg);
				emit("event", msg);
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
			try {
				socketClient.connect();
			} catch (MqttSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		eventEmitters.add(emitter);
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
}
