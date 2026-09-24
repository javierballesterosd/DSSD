package com.proyecto.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class BonitaClient {

    private final RestClient restClient;

    @Value("${bonita.process.definition-id:0}")
    private String processDefinitionId;

    @Value("${bonita.url:http://localhost:8080}")
    private String bonitaUrl;

    public BonitaClient() {
        this.restClient = RestClient.builder().build();
    }

    /**
     * Inicia una instancia del proceso de emergencia en Bonita enviando el contrato requerido.
     */
    public String iniciarProcesoEmergencia(Long emergenciaId, String nivelGravedad) {
        Map<String, Object> contractPayload = Map.of(
                "emergenciaInput", Map.of(
                        "emergenciaId", emergenciaId,
                        "nivelGravedad", nivelGravedad
                )
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri(bonitaUrl + "/API/bpm/process/{id}/instantiation", processDefinitionId)
                    .body(contractPayload)
                    .retrieve()
                    .body(Map.class);

            return response != null && response.get("caseId") != null
                    ? response.get("caseId").toString()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }
}