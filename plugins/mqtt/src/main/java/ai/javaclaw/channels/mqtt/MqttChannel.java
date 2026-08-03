package ai.javaclaw.channels.mqtt;

import ai.javaclaw.agent.Agent;
import ai.javaclaw.channels.Channel;
import ai.javaclaw.channels.ChannelMessageReceivedEvent;
import ai.javaclaw.channels.ChannelRegistry;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class MqttChannel implements Channel, MqttCallback, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttChannel.class);

    private final String serverUri;
    private final String clientId;
    private final String username;
    private final String password;
    private final String instructionTopic;
    private final String replyTopicPrefix;
    private final Agent agent;
    private final ChannelRegistry channelRegistry;
    private final MqttClient mqttClient;

    public MqttChannel(String serverUri,
                       String clientId,
                       String username,
                       String password,
                       String instructionTopic,
                       String replyTopicPrefix,
                       Agent agent,
                       ChannelRegistry channelRegistry) throws MqttException {
        this(serverUri, clientId, username, password, instructionTopic, replyTopicPrefix, agent, channelRegistry,
                new MqttClient(serverUri, clientId));
    }

    MqttChannel(String serverUri,
                String clientId,
                String username,
                String password,
                String instructionTopic,
                String replyTopicPrefix,
                Agent agent,
                ChannelRegistry channelRegistry,
                MqttClient mqttClient) throws MqttException {
        this.serverUri = serverUri;
        this.clientId = clientId;
        this.username = emptyToNull(username);
        this.password = emptyToNull(password);
        this.instructionTopic = instructionTopic;
        this.replyTopicPrefix = trimTrailingSlash(replyTopicPrefix);
        this.agent = agent;
        this.channelRegistry = channelRegistry;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(this);
        connectAndSubscribe();
        channelRegistry.registerChannel(this);
        LOGGER.info("Started MQTT integration");
    }

    @Override
    public void sendMessage(String message) {
        sendMessage(replyTopicPrefix, message);
    }

    public void sendMessage(String topic, String message) {
        try {
            mqttClient.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e) {
            throw new RuntimeException("Failed to publish MQTT message to topic '" + topic + "'", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        LOGGER.warn("MQTT connection lost for broker {}", serverUri, cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
        if (payload.isBlank()) {
            return;
        }

        String replyTopic = resolveReplyTopic(topic, payload);
        String prompt = extractPrompt(payload);
        String conversationId = getConversationId(replyTopic);

        channelRegistry.publishMessageReceivedEvent(new ChannelMessageReceivedEvent(getName(), prompt));
        String response = agent.respondTo(conversationId, prompt);
        sendMessage(replyTopic, response);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    @Override
    public void close() throws MqttException {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } finally {
            mqttClient.close();
            channelRegistry.unregisterChannel(this);
        }
    }

    private void connectAndSubscribe() throws MqttException {
        if (!mqttClient.isConnected()) {
            mqttClient.connect(connectOptions());
        }
        mqttClient.subscribe(instructionTopic);
    }

    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        if (username != null) {
            options.setUserName(username);
        }
        if (password != null) {
            options.setPassword(password.toCharArray());
        }
        return options;
    }

    private String resolveReplyTopic(String topic, String payload) {
        int separator = payload.indexOf('\n');
        if (separator <= 0) {
            return replyTopicPrefix;
        }

        String firstLine = payload.substring(0, separator).trim();
        String remainder = payload.substring(separator + 1).trim();
        if (remainder.isBlank()) {
            return replyTopicPrefix;
        }

        if (firstLine.startsWith("reply-topic:")) {
            String requestedTopic = firstLine.substring("reply-topic:".length()).trim();
            if (!requestedTopic.isBlank()) {
                return requestedTopic;
            }
        }

        return replyTopicPrefix;
    }

    private String extractPrompt(String payload) {
        int separator = payload.indexOf('\n');
        if (separator <= 0) {
            return payload;
        }

        String firstLine = payload.substring(0, separator).trim();
        String remainder = payload.substring(separator + 1).trim();
        if (firstLine.startsWith("reply-topic:") && !remainder.isBlank()) {
            return remainder;
        }

        return payload;
    }

    private String getConversationId(String replyTopic) {
        return "mqtt-" + clientId + "-" + replyTopic.replace('/', '-');
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
