package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.dto.emergencia.EmergenciaRequestDTO;
import com.proyecto.backend.dto.emergencia.EmergenciaResponseDTO;
import com.proyecto.backend.exception.ResourceNotFoundException;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.proyecto.backend.dto.lote.EmergenciaLoteResponseDTO;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.repository.LoteRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmergenciaService {

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;
    private final BonitaClient bonitaClient;
    private final EmergenciaMapper emergenciaMapper;
    private final LoteRepository loteRepository;

    @Transactional
    public EmergenciaResponseDTO registrarEmergencia(EmergenciaRequestDTO requestDTO) {
        // 1. Obtener municipio
        Municipio municipio = municipioRepository.findById(requestDTO.getMunicipioId())
                .orElseThrow(() -> new ResourceNotFoundException("Municipio no encontrado con ID: " + requestDTO.getMunicipioId()));

        // 2. Mapear y guardar emergencia localmente
        Emergencia emergencia = emergenciaMapper.toEntity(requestDTO, municipio);
        emergencia.setFechaRegistro(LocalDateTime.now());
        Emergencia emergenciaGuardada = emergenciaRepository.save(emergencia);

        // 3. Instanciar proceso en Bonita BPM
        String caseId = bonitaClient.iniciarProcesoEmergencia(
                emergenciaGuardada.getId(),
                emergenciaGuardada.getNivelGravedad().name()
        );

        // 4. Guardar id de caso devuelto por Bonita
        if (caseId != null) {
            emergenciaGuardada.setBonitaCaseId(caseId);
            emergenciaGuardada = emergenciaRepository.save(emergenciaGuardada);
        }

        return emergenciaMapper.toDto(emergenciaGuardada);
    }

    @Transactional(readOnly = true)
    public List<EmergenciaLoteResponseDTO> obtenerEmergenciasParaLotes() {

        return emergenciaRepository.findAll()
                .stream()
                .map(emergencia -> {

                    Optional<Lote> ultimoLote =
                            loteRepository.findFirstByEmergenciaIdOrderByIdDesc(
                                    emergencia.getId()
                            );

                    return new EmergenciaLoteResponseDTO(
                            emergencia.getId(),
                            emergencia.getDescripcion(),
                            emergencia.getNivelGravedad(),
                            emergencia.getZonaAfectada(),
                            emergencia.getMunicipio().getId(),
                            emergencia.getFechaRegistro(),
                            ultimoLote.map(Lote::getId).orElse(null),
                            ultimoLote.map(Lote::getEstado).orElse(null)
                    );
                })
                .toList();
    }
}