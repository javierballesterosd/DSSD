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
    // hayHabilitadas: alguna oferta del lote quedó habilitada por el SN (también las ya validadas antes de un reintento)
    public record Plan(Long emergenciaId, Long loteId, String caseId, Boolean decision,
                       boolean confirmada, boolean hayHabilitadas, List<Oferta> ofertas) {}
    /** Cierre del lote cuando el SN no dejó recursos habilitados en ninguna oferta. */
    public record SinRecursos(@NotNull Long loteId, @NotNull String caseId) {}
    public record ItemSN(@NotNull String ongCodigo, @NotNull Long recursoId,
                         @NotNull Integer cantidad, @NotNull String estado) {}
    public record Resultado(@NotNull Long ofertaId, @NotNull Boolean aprobada,
                            @NotNull List<@Valid ItemSN> items) {}
    public record Respuesta(@NotNull Long loteId, @NotNull String caseId,
                            @NotNull List<@Valid Resultado> resultados) {}
}
