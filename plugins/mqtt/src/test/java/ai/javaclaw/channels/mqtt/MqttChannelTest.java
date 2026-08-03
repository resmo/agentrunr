package ai.javaclaw.channels.mqtt;

import ai.javaclaw.agent.Agent;
import ai.javaclaw.channels.ChannelRegistry;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttChannelTest {

    @Mock
    MqttClient mqttClient;

    @Mock
    Agent agent;

    @Test
    void subscribesOnConstruction() throws Exception {
        channel();

        verify(mqttClient).setCallback(org.mockito.ArgumentMatchers.any());
        verify(mqttClient).connect(org.mockito.ArgumentMatchers.any());
        verify(mqttClient).subscribe("agents/instructions");
    }

    @Test
    void ignoresBlankPayloads() throws Exception {
        MqttChannel channel = channel();

        channel.messageArrived("agents/instructions", message("   "));

        verifyNoInteractions(agent);
    }

    @Test
    void usesDefaultReplyTopicWhenNoOverrideIsPresent() throws Exception {
        MqttChannel channel = channel();
        when(agent.respondTo(anyString(), anyString())).thenReturn("hi");

        channel.messageArrived("agents/instructions", message("hello"));

        verify(agent).respondTo(eq("mqtt-agentrunr-agents-replies"), eq("hello"));
        verify(mqttClient).publish(eq("agents/replies"), org.mockito.ArgumentMatchers.any(MqttMessage.class));
    }

    @Test
    void usesReplyTopicOverrideFromFirstLine() throws Exception {
        MqttChannel channel = channel();
        when(agent.respondTo(anyString(), anyString())).thenReturn("hi");

        channel.messageArrived("agents/instructions", message("reply-topic: agents/replies/device-1\nhello"));

        verify(agent).respondTo(eq("mqtt-agentrunr-agents-replies-device-1"), eq("hello"));
        verify(mqttClient).publish(eq("agents/replies/device-1"), org.mockito.ArgumentMatchers.any(MqttMessage.class));
    }

    @Test
    void keepsWholePayloadWhenOverrideHasNoBody() throws Exception {
        MqttChannel channel = channel();
        when(agent.respondTo(anyString(), anyString())).thenReturn("hi");

        channel.messageArrived("agents/instructions", message("reply-topic: agents/replies/device-1"));

        verify(agent).respondTo(eq("mqtt-agentrunr-agents-replies"), eq("reply-topic: agents/replies/device-1"));
        verify(mqttClient).publish(eq("agents/replies"), org.mockito.ArgumentMatchers.any(MqttMessage.class));
    }

    @Test
    void sendMessagePublishesToDefaultReplyTopic() throws Exception {
        MqttChannel channel = channel();

        channel.sendMessage("hello");

        verify(mqttClient).publish(eq("agents/replies"), org.mockito.ArgumentMatchers.any(MqttMessage.class));
    }

    @Test
    void closeDisconnectsAndClosesClient() throws Exception {
        MqttChannel channel = channel();
        when(mqttClient.isConnected()).thenReturn(true);

        channel.close();

        verify(mqttClient).disconnect();
        verify(mqttClient).close();
    }

    private MqttChannel channel() throws Exception {
        when(mqttClient.isConnected()).thenReturn(false);
        return new MqttChannel(
                "tcp://broker:1883",
                "agentrunr",
                "",
                "",
                "agents/instructions",
                "agents/replies",
                agent,
                new ChannelRegistry(),
                mqttClient
        );
    }

    private static MqttMessage message(String payload) {
        return new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
    }
}
