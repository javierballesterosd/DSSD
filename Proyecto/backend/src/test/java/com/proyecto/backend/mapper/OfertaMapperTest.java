package com.proyecto.backend.mapper;

import com.proyecto.backend.model.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OfertaMapperTest {

    private final OfertaMapper mapper = new OfertaMapper(mock(OngMapper.class));

    private Oferta oferta(Integer aceptadaBomberos, Integer aceptadaCruzRoja) {
        var emergencia = new Emergencia();
        emergencia.setZonaAfectada("Zona");
        var lote = new Lote();
        lote.setId(1L);
        lote.setEmergencia(emergencia);
        var recurso = new Recurso();
        recurso.setNombre("Paramédicos");
        recurso.setUnidadMedida("personas");
        var item = new ItemLote();
        item.setId(5L);
        item.setRecurso(recurso);
        item.setCantidadRequerida(6);
        var oferta = new Oferta();
        oferta.setId(9L);
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setLote(lote);
        oferta.getDetalles().add(detalle(oferta, item, 3L, "Bomberos", 4, aceptadaBomberos));
        oferta.getDetalles().add(detalle(oferta, item, 1L, "Cruz Roja", 4, aceptadaCruzRoja));
        return oferta;
    }

    private DetalleOferta detalle(Oferta oferta, ItemLote item, Long ongId, String nombre, int ofrecida, Integer aceptada) {
        var ong = new Ong();
        ong.setId(ongId);
        ong.setRazonSocial(nombre);
        var detalle = new DetalleOferta();
        detalle.setId(ongId);
        detalle.setOferta(oferta);
        detalle.setItemLote(item);
        detalle.setOng(ong);
        detalle.setCantidadOfrecida(ofrecida);
        detalle.setCantidadAceptada(aceptada);
        return detalle;
    }

    @Test
    void conLaOfertaValidadaInformaCuantoSeAceptoDeCadaOng() {
        var respuesta = mapper.toResponse(oferta(2, 4));

        var aporte = respuesta.aportes().getFirst();
        assertThat(aporte.totalOfrecido()).isEqualTo(8);
        assertThat(aporte.totalAceptado()).isEqualTo(6);
        assertThat(aporte.porOng()).extracting(a -> a.razonSocial() + ":" + a.cantidadOfrecida() + "->" + a.cantidadAceptada())
                .containsExactlyInAnyOrder("Bomberos:4->2", "Cruz Roja:4->4");
    }

    @Test
    void mientrasLaOfertaEstaPendienteNoHayCantidadesAceptadas() {
        var aporte = mapper.toResponse(oferta(null, null)).aportes().getFirst();

        assertThat(aporte.totalAceptado()).isNull();
        assertThat(aporte.porOng()).allSatisfy(a -> assertThat(a.cantidadAceptada()).isNull());
    }
}
