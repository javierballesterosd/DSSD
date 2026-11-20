package com.proyecto.backend.dto;

import com.proyecto.backend.model.NivelGravedad;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmergenciaRequestValidacionTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private EmergenciaRequest pedido(List<ActividadRequest> actividades) {
        return new EmergenciaRequest(NivelGravedad.ALTA, "Plaza Moreno", "Inundación en la plaza central", actividades);
    }

    @Test
    void exigeAlMenosUnaActividad() {
        assertThat(validator.validate(pedido(null))).extracting(v -> v.getMessage())
                .contains("La emergencia debe tener al menos una actividad");
        assertThat(validator.validate(pedido(List.of()))).extracting(v -> v.getMessage())
                .contains("La emergencia debe tener al menos una actividad");
    }

    @Test
    void conUnaActividadEsValida() {
        assertThat(validator.validate(pedido(List.of(new ActividadRequest("Evacuación", null))))).isEmpty();
    }
}
