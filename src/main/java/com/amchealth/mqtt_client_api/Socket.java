package com.amchealth.mqtt_client_api;

import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.scispike.callback.EventEmitter;

public class Socket {
	static final Map<String, MqttWrapper> globalSockets = new Hashtable<String, MqttWrapper>();

	private EventEmitter<String> emitter = null;
	private String baseURL;
	private String clientId;
	MqttWrapper socketClient;

	public Socket(String baseURL, String clientId) {
		this(baseURL,clientId,null);
	}

	public Socket(String baseURL, String clientId, SSLContext sslContext) {
		this.baseURL = baseURL;
		this.clientId = clientId; //TODO ??
		this.socketClient = getMQTTClient();
		this.socketClient.setSSLContext(sslContext);
		this.emitter = new EventEmitter<String>();
	}

	private synchronized MqttWrapper getMQTTClient() {
		MqttWrapper socket = globalSockets.get(baseURL);
		if (socket == null) {
			socket = new MqttWrapper(baseURL, clientId);
			globalSockets.put(baseURL, socket);
		}
		return socket;
	}

	public void connect() {
		socketClient.connect(emitter);
		socketClient.subscribe("defaultTopicResponses");// TODO topic
	}

	public boolean isConnected() {
		return socketClient != null && socketClient.isConnected();
	}

	public void send(String string) {
		MqttMessage message = new MqttMessage(string.getBytes());
		socketClient.publish("defaultTopic", message); // TODO topic
	}

	public EventEmitter<String> getConnectEmitter() {
		return emitter;
	}

	public void disconnect() {
		socketClient.disconnect(emitter);
		socketClient = null;
	}

}
