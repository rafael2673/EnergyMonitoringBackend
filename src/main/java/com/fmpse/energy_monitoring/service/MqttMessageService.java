package com.fmpse.energy_monitoring.service;

import com.fmpse.energy_monitoring.model.SensorData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fmpse.energy_monitoring.repository.SensorDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

@Service
public class MqttMessageService {

    private final SensorDataRepository sensorDataRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MqttMessageService(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

    public void processMessage(MqttMessage message) throws Exception {
        // Converte a mensagem JSON para um objeto JsonNode
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());

        // Extrai os valores de corrente e tensão
        float corrente = (float) jsonNode.get("corrente").asDouble();
        float tensao = (float) jsonNode.get("tensao").asDouble();

        // Calcula a potência e a energia total
        float potencia = corrente * tensao;
        float energiaTotal = potencia * (5.0f / 3600.0f); // Supondo intervalo de 5 segundos

        // Cria um novo objeto SensorData
        SensorData sensorData = new SensorData();
        sensorData.setDeviceId(jsonNode.get("device_id").asText());
        sensorData.setCorrente(corrente);
        sensorData.setTensao(tensao);
        sensorData.setEnergiaTotal(energiaTotal);
        sensorData.setCusto(energiaTotal * 0.095f); // Exemplo de cálculo de custo
        sensorData.setTimestamp(LocalDateTime.now());

        // Salva os dados no banco de dados
        sensorDataRepository.save(sensorData);
    }
}
