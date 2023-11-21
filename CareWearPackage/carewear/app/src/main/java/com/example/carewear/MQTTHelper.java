package com.example.carewear;

import android.content.Context;

import androidx.annotation.NonNull;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

class MQTTHelper {

    private final MqttAndroidClient client;

    public MQTTHelper(@NonNull Context context, @NonNull String serverUri) {
        String clientId = org.eclipse.paho.client.mqttv3.MqttClient.generateClientId();
        client = new MqttAndroidClient(context.getApplicationContext(), serverUri, clientId);
    }

    public void connect(@NonNull IMqttActionListener callback) {
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public boolean isConnected() {
        // Return the connection status of your MQTT client
        return client != null && client.isConnected();
    }

    public void disconnect(@NonNull IMqttActionListener callback) {
        try {
            IMqttToken token = client.disconnect();
            token.setActionCallback(callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishMessage(@NonNull String topic, @NonNull String msg, @NonNull IMqttActionListener callback) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            IMqttToken token = client.publish(topic, message);
            token.setActionCallback(callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void setCallback(@NonNull MqttCallback callback) {
        client.setCallback(callback);
    }
}