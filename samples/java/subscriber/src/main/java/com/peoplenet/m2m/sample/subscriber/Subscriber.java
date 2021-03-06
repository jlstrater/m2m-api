package com.peoplenet.m2m.sample.subscriber;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientFactory;
import net.sf.xenqtt.client.MqttClientListener;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;

/**
 * Sample synchronous MQTT subscriber using the <a href="http://xenqtt.sourceforge.net/documentation.html">XenQtt</a> library.
 */
public class Subscriber implements MqttClientListener {

	private static final Logger log = Logger.getLogger(Subscriber.class.getName());

	private MqttSettings mqttSettings;
	private MqttClientFactory factory;
	private MqttClient client;

	public static void main(String[] args) {

		Subscriber subscriber = new Subscriber();
		try {
			subscriber.init();
			subscriber.subscribe();
			subscriber.waitForDelay();
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Exception attempting to subscribe to MQTT topic.", t);
		} finally {
			subscriber.shutdown();
		}
	}

	private void waitForDelay() throws InterruptedException {
		// sleep for the number of millis specified in mqtt.properties
		if (mqttSettings.getWaitTimeMillis() == -1) {
			log.info("Waiting for messages until program termination.");
			Thread.sleep(Long.MAX_VALUE);
		} else {
			log.info("Waiting " + mqttSettings.getWaitTimeMillis() + " ms for incoming messages.");
			Thread.sleep(mqttSettings.getWaitTimeMillis());
		}
	}

	private void init() throws IOException {

		log.info("Initializing MQTT subscriber...");
		mqttSettings = new MqttSettings(getMqttProperties());
		factory = new MqttClientFactory(mqttSettings.getBrokerUri(), mqttSettings.getMessageHandlerThreadPoolSize(), true);
		client = factory.newSynchronousClient(this);
	}

	/**
	 * Create the MQTT client, connect and subscribe.
	 */
	private void subscribe() throws IOException {

		// create new mqtt client providing this as the MqttClientListener implementation
		MqttClient client = factory.newSynchronousClient(this);

		// username and password are not required per the MQTT spec, only specify if available
		ConnectReturnCode returnCode = mqttSettings.getUsername() != null ?
				client.connect(mqttSettings.getClientId(), mqttSettings.isCleanSession(), mqttSettings.getUsername(), mqttSettings.getPassword()) :
				client.connect(mqttSettings.getClientId(), mqttSettings.isCleanSession());

		if (returnCode != ConnectReturnCode.ACCEPTED) {
			throw new IOException("Unable to connect to MQTT broker. Reason:" + returnCode);
		}
		log.info("MQTT connection established.");

		Subscription subscription = new Subscription(mqttSettings.getTopic(), mqttSettings.getQos());
		client.subscribe(new Subscription[] { subscription });
		log.info("Successfully subscribed to " + subscription);
	}

	private Properties getMqttProperties() throws IOException {
		Properties props = new Properties();
		props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("mqtt.properties"));
		return props;
	}

	private void shutdown() {

		log.info("Subscriber shutting down...");
		if (client != null) {
			try {
				client.disconnect();
			} catch (Exception e) {
				// ignore
			}
		}
		if (factory != null) {
			try {
				factory.shutdown();
			} catch (Exception e) {
				// ignore
			}
		}
		log.info("Subscriber shutdown complete.");
	}

	/**
	 * Handle each message with this method.
	 */
	@Override public void publishReceived(MqttClient client, PublishMessage message) {

		log.info("Received: " + message.getPayloadString());
	}

	/**
	 * Called if the MQTT connection becomes disconnected. In most cases, XenQTT will attempt to re-establish the connection.
	 */
	@Override public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {

		log.info("MQTT connection disconnected. Reconnecting:" + reconnecting);
	}
}
