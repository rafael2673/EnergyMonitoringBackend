package com.fmpse.energy_monitoring.config;

import com.fmpse.energy_monitoring.service.MqttMessageService;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MqttConfig {

    private static final Logger logger = LoggerFactory.getLogger(MqttConfig.class);

    private final MqttMessageService mqttMessageService;

    private static final String MQTT_BROKER = "tcp://test.mosquitto.org:1883";
    private static final String MQTT_TOPIC = "energy_monitoring_rafa";
    private static final String MQTT_CLIENT_ID = "spring_mqtt_client";

    public MqttConfig(MqttMessageService mqttMessageService) {
        this.mqttMessageService = mqttMessageService;
    }

    @Bean
    public IMqttClient mqttClient() throws MqttException {
        IMqttClient mqttClient = new MqttClient(MQTT_BROKER, MQTT_CLIENT_ID);
        MqttConnectOptions options = mqttConnectOptions();

        mqttClient.setCallback(mqttCallback(mqttClient, options));

        // Conexão inicial e assinatura
        connectAndSubscribe(mqttClient, options);
        return mqttClient;
    }

    private void connectAndSubscribe(IMqttClient mqttClient, MqttConnectOptions options) {
        try {
            if (!mqttClient.isConnected()) {
                logger.info("Connecting to MQTT broker: {}", MQTT_BROKER);
                mqttClient.connect(options);
                logger.info("Successfully connected to MQTT broker.");

                // Assinatura no tópico
                subscribeToTopic(mqttClient);
            } else {
                logger.info("Client is already connected to MQTT broker.");
            }
        } catch (MqttException e) {
            logger.error("Error during connection or subscription: {}", e.getMessage(), e);
            // Opção de reconexão em caso de erro inicial
            attemptReconnect(mqttClient, options);
        }
    }

    private void subscribeToTopic(IMqttClient mqttClient) {
        try {
            mqttClient.subscribe(MQTT_TOPIC, (topic, message) -> {
                logger.info("Message received on topic {}: {}", topic, new String(message.getPayload()));
                mqttMessageService.processMessage(message);
            });
            logger.info("Subscribed to topic '{}'.", MQTT_TOPIC);
        } catch (MqttException e) {
            logger.error("Failed to subscribe to topic '{}': {}", MQTT_TOPIC, e.getMessage(), e);
        }
    }

    private void attemptReconnect(IMqttClient mqttClient, MqttConnectOptions options) {
        new Thread(() -> {
            while (!mqttClient.isConnected()) {
                try {
                    logger.info("Attempting to reconnect to MQTT broker...");
                    mqttClient.connect(options);
                    logger.info("Reconnected to MQTT broker successfully.");

                    // Reinscrição no tópico após reconexão
                    subscribeToTopic(mqttClient);
                } catch (MqttException e) {
                    logger.error("Reconnection attempt failed: {}", e.getMessage(), e);
                }

                try {
                    Thread.sleep(5000); // Aguarda 5 segundos antes de tentar novamente
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Reconnect thread interrupted: {}", e.getMessage(), e);
                }
            }
        }).start();
    }

    @Bean
    public MqttCallback mqttCallback(IMqttClient mqttClient, MqttConnectOptions options) {
        return new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                logger.warn("MQTT connection lost. Reason: {}", cause.getMessage());

                // Tentar reconectar ao perder conexão
                attemptReconnect(mqttClient, options);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                logger.info("Message received: [Topic: {}, Payload: {}]", topic, new String(message.getPayload()));
                mqttMessageService.processMessage(message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    logger.info("Message delivery complete: {}", token.getMessage());
                } catch (MqttException e) {
                    logger.error("Error retrieving message from token: {}", e.getMessage(), e);
                }
            }
        };
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setKeepAliveInterval(45); // Intervalo keep-alive para manter a conexão ativa
        options.setCleanSession(true);   // Remove informações anteriores da sessão
        options.setConnectionTimeout(60); // Timeout máximo de espera para conexão
        options.setAutomaticReconnect(true); // Habilitar reconexão automática
        return options;
    }
}