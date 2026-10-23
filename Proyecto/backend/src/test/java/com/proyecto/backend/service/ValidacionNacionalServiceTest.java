package com.proyecto.backend.service;

import com.proyecto.backend.client.SistemaNacionalClient;
import com.proyecto.backend.dto.ItemValidadoRequest;
import com.proyecto.backend.dto.OfertasConsolidadasResponse;
import com.proyecto.backend.dto.ValidacionSNRequest;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.OfertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidacionNacionalServiceTest {
    @Mock LoteRepository lotes;
    @Mock OfertaRepository ofertas;
    @Mock EmergenciaService emergencias;
    @Mock OfertaService ofertaService;
    @Mock SistemaNacionalClient nacional;
    ValidacionNacionalService service;
    Lote lote;
    Oferta oferta;

    @BeforeEach void preparar() {
        service = new ValidacionNacionalService(lotes, ofertas, emergencias, ofertaService, nacional);
        lote = new Lote(); lote.setId(10L); lote.setEstado(EstadoLote.ACTIVO);
        lote.setFechaCierreOfertas(LocalDateTime.now().minusHours(1));
        when(lotes.bloquearUltimoDeEmergencia(7L)).thenReturn(Optional.of(lote));
        oferta = new Oferta(); oferta.setId(20L); oferta.setEstado(EstadoOferta.PENDIENTE);
        var ong = new Ong(); ong.setId(2L);
        var recurso = new Recurso(); recurso.setId(3L);
        var item = new ItemLote(); item.setRecurso(recurso);
        var detalle = new DetalleOferta(); detalle.setId(100L); detalle.setOng(ong); detalle.setItemLote(item);
        oferta.getDetalles().add(detalle);
    }

    void prepararSN() {
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
        when(nacional.autenticar()).thenReturn("jwt");
        when(emergencias.obtenerOfertasConsolidadas(7L))
                .thenReturn(new OfertasConsolidadasResponse(7L, 10L, List.of()));
    }

    @Test void registraLasCantidadesReservadasPorSnEnElInventarioLocal() {
        prepararSN();
        when(nacional.reservarTemporal(eq("jwt"), any())).thenReturn(Optional.of(
                new SistemaNacionalClient.Reservas(List.of(new SistemaNacionalClient.ItemReserva(2L, 3L, 40, "BLOQUEADA")))));
        service.validar(7L);
        verify(nacional).enviarOfertas(eq("jwt"), any());
        verify(ofertaService).procesarValidacionSNYBloquearRecursos(20L,
                new ValidacionSNRequest(true, "Reserva temporal confirmada por SN", List.of(new ItemValidadoRequest(100L, 40))));
    }

    @Test void noVuelveAReservarOfertasYaProcesadas() {
        oferta.setEstado(EstadoOferta.VALIDADA);
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
        service.validar(7L);
        verifyNoInteractions(nacional, emergencias, ofertaService);
    }

    @Test void reservaSinReenviarElJsonQueYaEnvioBonita() {
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
        when(nacional.autenticar()).thenReturn("jwt");
        when(nacional.reservarTemporal(eq("jwt"), any())).thenReturn(Optional.of(
                new SistemaNacionalClient.Reservas(List.of(new SistemaNacionalClient.ItemReserva(2L, 3L, 40, "BLOQUEADA")))));

        service.validar(7L, true);

        verify(nacional, never()).enviarOfertas(any(), any());
        verifyNoInteractions(emergencias);
        verify(ofertaService).procesarValidacionSNYBloquearRecursos(20L,
                new ValidacionSNRequest(true, "Reserva temporal confirmada por SN", List.of(new ItemValidadoRequest(100L, 40))));
    }

    @Test void registraRechazoNacionalSinBloquearInventario() {
        prepararSN();
        when(nacional.reservarTemporal(eq("jwt"), any())).thenReturn(Optional.empty());
        service.validar(7L);
        verify(ofertaService).procesarValidacionSNYBloquearRecursos(20L,
                new ValidacionSNRequest(false, "El SN no pudo reservar los recursos", List.of()));
    }

    @Test void impideValidarDespuesDeLaDecisionMunicipal() {
        lote.setDecisionMunicipal(true);
        assertThatThrownBy(() -> service.validar(7L)).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(nacional, ofertaService);
    }

    @Test void rechazaRecursosAjenosDevueltosPorSn() {
        prepararSN();
        when(nacional.reservarTemporal(eq("jwt"), any())).thenReturn(Optional.of(
                new SistemaNacionalClient.Reservas(List.of(new SistemaNacionalClient.ItemReserva(99L, 3L, 40, "BLOQUEADA")))));
        assertThatThrownBy(() -> service.validar(7L)).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(ofertaService);
    }
}
