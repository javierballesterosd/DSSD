package com.proyecto.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Contratos internos entre Bonita y la aplicación local. Nunca contienen credenciales nacionales. */
public final class IntegracionNacional {
    private IntegracionNacional() {}

    // La ONG viaja por su código común: el id numérico es propio de cada base
    public record Item(Long detalleOfertaId, String ongCodigo, Long recursoId, Integer cantidad) {}
    public record Oferta(Long ofertaId, List<Item> items) {}
    public record Plan(Long emergenciaId, Long loteId, String caseId, Boolean decision,
                       boolean confirmada, List<Oferta> ofertas) {}
    public record ItemSN(@NotNull String ongCodigo, @NotNull Long recursoId,
                         @NotNull Integer cantidad, @NotNull String estado) {}
    public record Resultado(@NotNull Long ofertaId, @NotNull Boolean aprobada,
                            @NotNull List<@Valid ItemSN> items) {}
    public record Respuesta(@NotNull Long loteId, @NotNull String caseId,
                            @NotNull List<@Valid Resultado> resultados) {}
}
