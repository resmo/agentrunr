package ai.javaclaw.channels.mqtt;

import ai.javaclaw.configuration.ConfigurationManager;
import ai.javaclaw.onboarding.OnboardingProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(54)
public class MqttOnboardingProvider implements OnboardingProvider {

    static final String SESSION_SERVER_URI = "onboarding.mqtt.server-uri";
    static final String SESSION_CLIENT_ID = "onboarding.mqtt.client-id";
    static final String SESSION_USERNAME = "onboarding.mqtt.username";
    static final String SESSION_PASSWORD = "onboarding.mqtt.password";
    static final String SESSION_INSTRUCTION_TOPIC = "onboarding.mqtt.instruction-topic";
    static final String SESSION_REPLY_TOPIC_PREFIX = "onboarding.mqtt.reply-topic-prefix";

    private static final String SERVER_URI_PROPERTY = "agent.channels.mqtt.server-uri";
    private static final String CLIENT_ID_PROPERTY = "agent.channels.mqtt.client-id";
    private static final String USERNAME_PROPERTY = "agent.channels.mqtt.username";
    private static final String PASSWORD_PROPERTY = "agent.channels.mqtt.password";
    private static final String INSTRUCTION_TOPIC_PROPERTY = "agent.channels.mqtt.instruction-topic";
    private static final String REPLY_TOPIC_PREFIX_PROPERTY = "agent.channels.mqtt.reply-topic-prefix";

    private final Environment env;

    public MqttOnboardingProvider(Environment env) {
        this.env = env;
    }

    @Override
    public boolean isOptional() {return true;}

    @Override
    public String getStepId() {return "mqtt";}

    @Override
    public String getStepTitle() {return "MQTT";}

    @Override
    public String getTemplatePath() {return "onboarding/steps/mqtt";}

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
        model.put("mqttServerUri", session.getOrDefault(SESSION_SERVER_URI, env.getProperty(SERVER_URI_PROPERTY, "")));
        model.put("mqttClientId", session.getOrDefault(SESSION_CLIENT_ID, env.getProperty(CLIENT_ID_PROPERTY, "javaclaw-mqtt")));
        model.put("mqttUsername", session.getOrDefault(SESSION_USERNAME, env.getProperty(USERNAME_PROPERTY, "")));
        model.put("mqttPassword", session.getOrDefault(SESSION_PASSWORD, env.getProperty(PASSWORD_PROPERTY, "")));
        model.put("mqttInstructionTopic", session.getOrDefault(SESSION_INSTRUCTION_TOPIC, env.getProperty(INSTRUCTION_TOPIC_PROPERTY, "")));
        model.put("mqttReplyTopicPrefix", session.getOrDefault(SESSION_REPLY_TOPIC_PREFIX, env.getProperty(REPLY_TOPIC_PREFIX_PROPERTY, "")));
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        String serverUri = formParams.getOrDefault("mqttServerUri", "").trim();
        String clientId = formParams.getOrDefault("mqttClientId", "").trim();
        String username = formParams.getOrDefault("mqttUsername", "").trim();
        String password = formParams.getOrDefault("mqttPassword", "").trim();
        String instructionTopic = formParams.getOrDefault("mqttInstructionTopic", "").trim();
        String replyTopicPrefix = formParams.getOrDefault("mqttReplyTopicPrefix", "").trim();

        if (serverUri.isBlank()) {
            return "Enter the MQTT broker URI to continue.";
        }
        if (instructionTopic.isBlank()) {
            return "Enter the MQTT topic used for incoming instructions.";
        }
        if (replyTopicPrefix.isBlank()) {
            return "Enter the MQTT topic or topic prefix used for outgoing replies.";
        }

        session.put(SESSION_SERVER_URI, serverUri);
        session.put(SESSION_CLIENT_ID, clientId.isBlank() ? "javaclaw-mqtt" : clientId);
        session.put(SESSION_USERNAME, username);
        session.put(SESSION_PASSWORD, password);
        session.put(SESSION_INSTRUCTION_TOPIC, instructionTopic);
        session.put(SESSION_REPLY_TOPIC_PREFIX, replyTopicPrefix);
        return null;
    }

    @Override
    public void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws IOException {
        String serverUri = (String) session.get(SESSION_SERVER_URI);
        String clientId = (String) session.get(SESSION_CLIENT_ID);
        String username = (String) session.get(SESSION_USERNAME);
        String password = (String) session.get(SESSION_PASSWORD);
        String instructionTopic = (String) session.get(SESSION_INSTRUCTION_TOPIC);
        String replyTopicPrefix = (String) session.get(SESSION_REPLY_TOPIC_PREFIX);

        if (serverUri != null && clientId != null && instructionTopic != null && replyTopicPrefix != null) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put(SERVER_URI_PROPERTY, serverUri);
            properties.put(CLIENT_ID_PROPERTY, clientId);
            properties.put(USERNAME_PROPERTY, username);
            properties.put(PASSWORD_PROPERTY, password);
            properties.put(INSTRUCTION_TOPIC_PROPERTY, instructionTopic);
            properties.put(REPLY_TOPIC_PREFIX_PROPERTY, replyTopicPrefix);
            configurationManager.updateProperties(properties);
        }
    }
}
