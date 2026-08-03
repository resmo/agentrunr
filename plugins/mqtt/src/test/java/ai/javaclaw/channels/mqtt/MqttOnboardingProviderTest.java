package ai.javaclaw.channels.mqtt;

import ai.javaclaw.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttOnboardingProviderTest {

    @Mock
    Environment environment;

    @Mock
    ConfigurationManager configurationManager;

    @Test
    void stepMetadataIsCorrect() {
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);

        assertThat(provider.getStepId()).isEqualTo("mqtt");
        assertThat(provider.getStepTitle()).isEqualTo("MQTT");
        assertThat(provider.getTemplatePath()).isEqualTo("onboarding/steps/mqtt");
        assertThat(provider.isOptional()).isTrue();
    }

    @Test
    void processStepStoresValues() {
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);
        Map<String, Object> session = new HashMap<>();

        String result = provider.processStep(Map.of(
                "mqttServerUri", " tcp://broker:1883 ",
                "mqttClientId", " agentrunr ",
                "mqttUsername", " user ",
                "mqttPassword", " secret ",
                "mqttInstructionTopic", " in/topic ",
                "mqttReplyTopicPrefix", " out/topic "
        ), session);

        assertThat(result).isNull();
        assertThat(session).containsEntry(MqttOnboardingProvider.SESSION_SERVER_URI, "tcp://broker:1883");
        assertThat(session).containsEntry(MqttOnboardingProvider.SESSION_CLIENT_ID, "agentrunr");
        assertThat(session).containsEntry(MqttOnboardingProvider.SESSION_USERNAME, "user");
        assertThat(session).containsEntry(MqttOnboardingProvider.SESSION_PASSWORD, "secret");
        assertThat(session).containsEntry(MqttOnboardingProvider.SESSION_INSTRUCTION_TOPIC, "in/topic");
        assertThat(session).containsEntry(MqttOnboardingProvider.SESSION_REPLY_TOPIC_PREFIX, "out/topic");
    }

    @Test
    void processStepDefaultsClientId() {
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);
        Map<String, Object> session = new HashMap<>();

        String result = provider.processStep(Map.of(
                "mqttServerUri", "tcp://broker:1883",
                "mqttClientId", "",
                "mqttUsername", "",
                "mqttPassword", "",
                "mqttInstructionTopic", "in/topic",
                "mqttReplyTopicPrefix", "out/topic"
        ), session);

        assertThat(result).isNull();
        assertThat(session).containsEntry(MqttOnboardingProvider.SESSION_CLIENT_ID, "javaclaw-mqtt");
    }

    @Test
    void processStepReturnsErrorWhenRequiredValueIsMissing() {
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);

        String result = provider.processStep(Map.of(
                "mqttServerUri", "",
                "mqttInstructionTopic", "in/topic",
                "mqttReplyTopicPrefix", "out/topic"
        ), new HashMap<>());

        assertThat(result).isEqualTo("Enter the MQTT broker URI to continue.");
    }

    @Test
    void prepareModelUsesSessionValuesWhenPresent() {
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);
        Map<String, Object> session = Map.of(
                MqttOnboardingProvider.SESSION_SERVER_URI, "tcp://session:1883",
                MqttOnboardingProvider.SESSION_CLIENT_ID, "session-client",
                MqttOnboardingProvider.SESSION_USERNAME, "session-user",
                MqttOnboardingProvider.SESSION_PASSWORD, "session-password",
                MqttOnboardingProvider.SESSION_INSTRUCTION_TOPIC, "session/in",
                MqttOnboardingProvider.SESSION_REPLY_TOPIC_PREFIX, "session/out"
        );
        Map<String, Object> model = new HashMap<>();

        provider.prepareModel(session, model);

        assertThat(model).containsEntry("mqttServerUri", "tcp://session:1883");
        assertThat(model).containsEntry("mqttClientId", "session-client");
        assertThat(model).containsEntry("mqttUsername", "session-user");
        assertThat(model).containsEntry("mqttPassword", "session-password");
        assertThat(model).containsEntry("mqttInstructionTopic", "session/in");
        assertThat(model).containsEntry("mqttReplyTopicPrefix", "session/out");
    }

    @Test
    void prepareModelFallsBackToEnvironmentValues() {
        when(environment.getProperty("agent.channels.mqtt.server-uri", "")).thenReturn("tcp://env:1883");
        when(environment.getProperty("agent.channels.mqtt.client-id", "javaclaw-mqtt")).thenReturn("env-client");
        when(environment.getProperty("agent.channels.mqtt.username", "")).thenReturn("env-user");
        when(environment.getProperty("agent.channels.mqtt.password", "")).thenReturn("env-password");
        when(environment.getProperty("agent.channels.mqtt.instruction-topic", "")).thenReturn("env/in");
        when(environment.getProperty("agent.channels.mqtt.reply-topic-prefix", "")).thenReturn("env/out");
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);
        Map<String, Object> model = new HashMap<>();

        provider.prepareModel(Map.of(), model);

        assertThat(model).containsEntry("mqttServerUri", "tcp://env:1883");
        assertThat(model).containsEntry("mqttClientId", "env-client");
        assertThat(model).containsEntry("mqttUsername", "env-user");
        assertThat(model).containsEntry("mqttPassword", "env-password");
        assertThat(model).containsEntry("mqttInstructionTopic", "env/in");
        assertThat(model).containsEntry("mqttReplyTopicPrefix", "env/out");
    }

    @Test
    void saveConfigurationWritesAllProperties() throws IOException {
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);
        Map<String, Object> session = Map.of(
                MqttOnboardingProvider.SESSION_SERVER_URI, "tcp://broker:1883",
                MqttOnboardingProvider.SESSION_CLIENT_ID, "client",
                MqttOnboardingProvider.SESSION_USERNAME, "user",
                MqttOnboardingProvider.SESSION_PASSWORD, "password",
                MqttOnboardingProvider.SESSION_INSTRUCTION_TOPIC, "in/topic",
                MqttOnboardingProvider.SESSION_REPLY_TOPIC_PREFIX, "out/topic"
        );

        provider.saveConfiguration(session, configurationManager);

        verify(configurationManager).updateProperties(Map.of(
                "agent.channels.mqtt.server-uri", "tcp://broker:1883",
                "agent.channels.mqtt.client-id", "client",
                "agent.channels.mqtt.username", "user",
                "agent.channels.mqtt.password", "password",
                "agent.channels.mqtt.instruction-topic", "in/topic",
                "agent.channels.mqtt.reply-topic-prefix", "out/topic"
        ));
    }

    @Test
    void saveConfigurationDoesNothingWhenSessionIsIncomplete() throws IOException {
        MqttOnboardingProvider provider = new MqttOnboardingProvider(environment);

        provider.saveConfiguration(Map.of(
                MqttOnboardingProvider.SESSION_SERVER_URI, "tcp://broker:1883"
        ), configurationManager);

        verifyNoInteractions(configurationManager);
    }
}
