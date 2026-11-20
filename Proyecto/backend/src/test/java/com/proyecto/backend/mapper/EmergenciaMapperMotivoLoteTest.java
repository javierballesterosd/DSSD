package com.proyecto.backend.mapper;

import com.proyecto.backend.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmergenciaMapperMotivoLoteTest {

    private final EmergenciaMapper mapper = new EmergenciaMapper();

    private Emergencia emergencia(boolean requiereActualizacion) {
        var region = new Region();
        region.setBonitaGroupPath("/Municipio/Region1");
        var municipio = new Municipio();
        municipio.setId(1L);
        municipio.setNombre("La Plata");
        municipio.setRegion(region);
        var emergencia = new Emergencia();
        emergencia.setId(2L);
        emergencia.setMunicipio(municipio);
        emergencia.setNivelGravedad(NivelGravedad.ALTA);
        emergencia.setFechaRegistro(LocalDateTime.now());
        emergencia.setRequiereActualizacion(requiereActualizacion);
        return emergencia;
    }

    private Lote lote(EstadoLote estado, Boolean decision) {
        var lote = new Lote();
        lote.setId(5L);
        lote.setEstado(estado);
        lote.setDecisionMunicipal(decision);
        return lote;
    }

    @Test
    void loteCanceladoSinDecisionMunicipalEsPorFaltaDeRecursos() {
        var respuesta = mapper.toParaLoteResponse(emergencia(false), Optional.of(lote(EstadoLote.CANCELADO, null)));

        assertThat(respuesta.motivoLoteCancelado()).isEqualTo("SIN_RECURSOS");
    }

    @Test
    void loteCanceladoPorElMunicipioEsRechazo() {
        var respuesta = mapper.toParaLoteResponse(emergencia(false), Optional.of(lote(EstadoLote.CANCELADO, false)));

        assertThat(respuesta.motivoLoteCancelado()).isEqualTo("RECHAZO_MUNICIPAL");
    }

    @Test
    void sinLoteOConLoteVigenteNoHayMotivo() {
        assertThat(mapper.toParaLoteResponse(emergencia(false), Optional.empty()).motivoLoteCancelado()).isNull();
        assertThat(mapper.toParaLoteResponse(emergencia(false), Optional.of(lote(EstadoLote.ACTIVO, null)))
                .motivoLoteCancelado()).isNull();
    }
}
