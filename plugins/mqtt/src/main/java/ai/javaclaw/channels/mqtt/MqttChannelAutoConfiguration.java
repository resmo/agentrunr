package ai.javaclaw.channels.mqtt;

import ai.javaclaw.agent.Agent;
import ai.javaclaw.channels.ChannelRegistry;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class MqttChannelAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "agent.channels.mqtt", name = {"server-uri", "instruction-topic", "reply-topic-prefix"})
    public MqttChannel mqttChannel(@Value("${agent.channels.mqtt.server-uri}") String serverUri,
                                   @Value("${agent.channels.mqtt.client-id:javaclaw-mqtt}") String clientId,
                                   @Value("${agent.channels.mqtt.username:}") String username,
                                   @Value("${agent.channels.mqtt.password:}") String password,
                                   @Value("${agent.channels.mqtt.instruction-topic}") String instructionTopic,
                                   @Value("${agent.channels.mqtt.reply-topic-prefix}") String replyTopicPrefix,
                                   Agent agent,
                                   ChannelRegistry channelRegistry) throws MqttException {
        return new MqttChannel(serverUri, clientId, username, password, instructionTopic, replyTopicPrefix, agent, channelRegistry);
    }
}
