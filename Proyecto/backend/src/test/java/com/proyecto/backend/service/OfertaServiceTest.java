package com.proyecto.backend.service;

import com.proyecto.backend.dto.DetalleOfertaRequest;
import com.proyecto.backend.dto.OfertaEdicionRequest;
import com.proyecto.backend.dto.OfertaRequest;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.mapper.OfertaMapper;
import com.proyecto.backend.model.DetalleOferta;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.EstadoOferta;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.InventarioOng;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Oferta;
import com.proyecto.backend.model.Ong;
import com.proyecto.backend.model.Recurso;
import com.proyecto.backend.repository.InventarioOngRepository;
import com.proyecto.backend.repository.ItemLoteRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.OfertaRepository;
import com.proyecto.backend.repository.OngRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfertaServiceTest {

    @Mock
    private LoteRepository loteRepository;
    @Mock
    private OngRepository ongRepository;
    @Mock
    private ItemLoteRepository itemLoteRepository;
    @Mock
    private InventarioOngRepository inventarioOngRepository;
    @Mock
    private OfertaRepository ofertaRepository;

    /** La ONG del usuario logueado en todos los casos: Cruz Solidaria (ongA). */
    private static final Long ONG_USUARIO = 1L;

    private OfertaService ofertaService;

    private Lote lote;
    private ItemLote itemRaciones;
    private ItemLote itemFrazadas;
    private Ong ongA;
    private Ong ongB;
    private Recurso raciones;
    private Recurso frazadas;

    @BeforeEach
    void setUp() {
        ofertaService = new OfertaService(
                loteRepository, ongRepository, itemLoteRepository, inventarioOngRepository,
                ofertaRepository, new OfertaMapper(new com.proyecto.backend.mapper.OngMapper()));

        Emergencia emergencia = new Emergencia();
        emergencia.setId(1L);
        emergencia.setZonaAfectada("Zona Norte");

        lote = new Lote();
        lote.setId(10L);
        lote.setTitulo("Asistencia alimentaria");
        lote.setEstado(EstadoLote.ACTIVO);
        lote.setEmergencia(emergencia);

        raciones = new Recurso();
        raciones.setId(100L);
        raciones.setNombre("Raciones de alimento");
        raciones.setUnidadMedida("raciones");

        frazadas = new Recurso();
        frazadas.setId(101L);
        frazadas.setNombre("Frazadas");
        frazadas.setUnidadMedida("unidades");

        itemRaciones = new ItemLote();
        itemRaciones.setId(200L);
        itemRaciones.setLote(lote);
        itemRaciones.setRecurso(raciones);
        itemRaciones.setCantidadRequerida(1000);

        itemFrazadas = new ItemLote();
        itemFrazadas.setId(201L);
        itemFrazadas.setLote(lote);
        itemFrazadas.setRecurso(frazadas);
        itemFrazadas.setCantidadRequerida(300);

        lote.setItems(List.of(itemRaciones, itemFrazadas));

        ongA = new Ong();
        ongA.setId(1L);
        ongA.setRazonSocial("Cruz Solidaria");

        ongB = new Ong();
        ongB.setId(2L);
        ongB.setRazonSocial("Manos Unidas");

        lenient().when(loteRepository.findById(10L)).thenReturn(java.util.Optional.of(lote));
    }

    @Test
    void rechazaOfertaAntesDeQueAbraLaConvocatoria() {
        lote.setFechaAperturaOfertas(java.time.LocalDateTime.now().plusHours(1));
        lote.setFechaCierreOfertas(java.time.LocalDateTime.now().plusDays(2));
        OfertaRequest request = new OfertaRequest(10L, Set.of(1L),
                List.of(new DetalleOfertaRequest(200L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.registrar(request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("todavía no abrió");
    }

    @Test
    void rechazaOfertaDespuesDeQueCerroLaConvocatoria() {
        lote.setFechaAperturaOfertas(java.time.LocalDateTime.now().minusDays(2));
        lote.setFechaCierreOfertas(java.time.LocalDateTime.now().minusMinutes(1));
        OfertaRequest request = new OfertaRequest(10L, Set.of(1L),
                List.of(new DetalleOfertaRequest(200L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.registrar(request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya cerró");
    }

    @Test
    void rechazaOfertaSobreLoteNoActivo() {
        lote.setEstado(EstadoLote.FINALIZADO);
        OfertaRequest request = new OfertaRequest(10L, Set.of(1L),
                List.of(new DetalleOfertaRequest(200L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.registrar(request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está abierto");
    }

    @Test
    void rechazaItemQueNoPerteneceAlLote() {
        when(ongRepository.findAllById(anyCollection())).thenReturn(List.of(ongA));

        OfertaRequest request = new OfertaRequest(10L, Set.of(1L),
                List.of(new DetalleOfertaRequest(999L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.registrar(request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no pertenece al lote");
    }

    @Test
    void rechazaOfertaQueNoIncluyeLaOngDelUsuario() {
        when(ongRepository.findAllById(anyCollection())).thenReturn(List.of(ongB));
        when(ongRepository.findById(ONG_USUARIO)).thenReturn(java.util.Optional.of(ongA));

        // El usuario es de Cruz Solidaria pero la oferta es solo de Manos Unidas.
        OfertaRequest request = new OfertaRequest(10L, Set.of(2L),
                List.of(new DetalleOfertaRequest(200L, 2L, 100)));

        assertThatThrownBy(() -> ofertaService.registrar(request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("debe incluir a tu ONG")
                .hasMessageContaining("Cruz Solidaria");
    }

    @Test
    void rechazaCantidadPorEncimaDelInventarioDeEsaOng() {
        when(ongRepository.findAllById(anyCollection())).thenReturn(List.of(ongA, ongB));

        InventarioOng inventarioA = new InventarioOng();
        inventarioA.setOng(ongA);
        inventarioA.setRecurso(raciones);
        inventarioA.setCantidadDisponible(800);

        InventarioOng inventarioB = new InventarioOng();
        inventarioB.setOng(ongB);
        inventarioB.setRecurso(raciones);
        inventarioB.setCantidadDisponible(500);

        when(inventarioOngRepository.findByOngIdIn(anyCollection()))
                .thenReturn(List.of(inventarioA, inventarioB));

        // Cruz Solidaria (ongA) solo tiene 800, aunque entre las dos sumen 1300.
        OfertaRequest request = new OfertaRequest(10L, Set.of(1L, 2L),
                List.of(
                        new DetalleOfertaRequest(200L, 1L, 900),
                        new DetalleOfertaRequest(200L, 2L, 400)
                ));

        assertThatThrownBy(() -> ofertaService.registrar(request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Cruz Solidaria");
    }

    @Test
    void rechazaOngSeleccionadaSinNingunAporte() {
        when(ongRepository.findAllById(anyCollection())).thenReturn(List.of(ongA, ongB));

        InventarioOng inventarioA = new InventarioOng();
        inventarioA.setOng(ongA);
        inventarioA.setRecurso(raciones);
        inventarioA.setCantidadDisponible(800);

        when(inventarioOngRepository.findByOngIdIn(anyCollection())).thenReturn(List.of(inventarioA));

        // Manos Unidas (ongB) está seleccionada pero no aporta ningún detalle.
        OfertaRequest request = new OfertaRequest(10L, Set.of(1L, 2L),
                List.of(new DetalleOfertaRequest(200L, 1L, 500)));

        assertThatThrownBy(() -> ofertaService.registrar(request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Manos Unidas");
    }

    @Test
    void registraOfertaValidaConDosOngs() {
        when(ongRepository.findAllById(anyCollection())).thenReturn(List.of(ongA, ongB));

        InventarioOng inventarioA = new InventarioOng();
        inventarioA.setOng(ongA);
        inventarioA.setRecurso(raciones);
        inventarioA.setCantidadDisponible(800);

        InventarioOng inventarioB = new InventarioOng();
        inventarioB.setOng(ongB);
        inventarioB.setRecurso(raciones);
        inventarioB.setCantidadDisponible(500);

        when(inventarioOngRepository.findByOngIdIn(anyCollection()))
                .thenReturn(List.of(inventarioA, inventarioB));

        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(invocation -> {
            Oferta oferta = invocation.getArgument(0);
            oferta.setId(500L);
            return oferta;
        });

        OfertaRequest request = new OfertaRequest(10L, Set.of(1L, 2L),
                List.of(
                        new DetalleOfertaRequest(200L, 1L, 800),
                        new DetalleOfertaRequest(200L, 2L, 200)
                ));

        OfertaResponse response = ofertaService.registrar(request, ONG_USUARIO);

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.estado()).isEqualTo("PENDIENTE");
        assertThat(response.ongs()).hasSize(2);
        assertThat(response.aportes()).hasSize(1);
        assertThat(response.aportes().get(0).totalOfrecido()).isEqualTo(1000);
    }

    @Test
    void listaLasOfertasDeLaOngConDesglosePorOng() {
        Oferta oferta = new Oferta();
        oferta.setId(7L);
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaOferta(java.time.LocalDateTime.of(2026, 9, 27, 10, 0));
        oferta.setLote(lote);
        oferta.setOngs(new java.util.HashSet<>(List.of(ongA, ongB)));
        for (Ong ong : List.of(ongA, ongB)) {
            DetalleOferta detalle = new DetalleOferta();
            detalle.setOferta(oferta);
            detalle.setOng(ong);
            detalle.setItemLote(itemRaciones);
            detalle.setCantidadOfrecida(300);
            oferta.getDetalles().add(detalle);
        }
        when(ofertaRepository.findByLoteIdAndOngId(10L, 1L)).thenReturn(List.of(oferta));

        List<OfertaResponse> resultado = ofertaService.listarDeOng(10L, 1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estadoEtiqueta()).isEqualTo("Pendiente");
        assertThat(resultado.get(0).ongs()).hasSize(2);
        assertThat(resultado.get(0).aportes().get(0).totalOfrecido()).isEqualTo(600);
        assertThat(resultado.get(0).aportes().get(0).porOng()).hasSize(2);
    }

    @Test
    void noDuplicaAportesSiElBagDeDetallesTraeRepetidos() {
        Oferta oferta = new Oferta();
        oferta.setId(8L);
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaOferta(java.time.LocalDateTime.now());
        oferta.setLote(lote);
        oferta.setOngs(new java.util.HashSet<>(List.of(ongA, ongB)));
        DetalleOferta detalle = new DetalleOferta();
        detalle.setOferta(oferta);
        detalle.setOng(ongA);
        detalle.setItemLote(itemRaciones);
        detalle.setCantidadOfrecida(800);
        // El mismo detalle dos veces, como produce un join fetch con dos colecciones.
        oferta.getDetalles().add(detalle);
        oferta.getDetalles().add(detalle);
        when(ofertaRepository.findByLoteIdAndOngId(10L, 1L)).thenReturn(List.of(oferta));

        OfertaResponse resultado = ofertaService.listarDeOng(10L, 1L).get(0);

        assertThat(resultado.aportes().get(0).totalOfrecido()).isEqualTo(800);
        assertThat(resultado.aportes().get(0).porOng()).hasSize(1);
    }

    // ---------- Edición y baja ----------

    /** Oferta pendiente de ongA + ongB: ongA 500 raciones, ongB 200 raciones. */
    private Oferta ofertaPendienteEnConsorcio() {
        Oferta oferta = new Oferta();
        oferta.setId(50L);
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaOferta(java.time.LocalDateTime.now().minusHours(1));
        oferta.setLote(lote);
        oferta.setOngs(new java.util.HashSet<>(List.of(ongA, ongB)));
        oferta.getDetalles().add(detalle(oferta, ongA, itemRaciones, 500));
        oferta.getDetalles().add(detalle(oferta, ongB, itemRaciones, 200));
        lenient().when(ofertaRepository.findById(50L)).thenReturn(java.util.Optional.of(oferta));
        lenient().when(ofertaRepository.save(any(Oferta.class))).thenAnswer(inv -> inv.getArgument(0));
        return oferta;
    }

    private DetalleOferta detalle(Oferta oferta, Ong ong, ItemLote item, int cantidad) {
        DetalleOferta detalle = new DetalleOferta();
        detalle.setOferta(oferta);
        detalle.setOng(ong);
        detalle.setItemLote(item);
        detalle.setCantidadOfrecida(cantidad);
        return detalle;
    }

    private InventarioOng inventario(Ong ong, Recurso recurso, int cantidad) {
        InventarioOng inventario = new InventarioOng();
        inventario.setOng(ong);
        inventario.setRecurso(recurso);
        inventario.setCantidadDisponible(cantidad);
        return inventario;
    }

    @Test
    void editaCantidadesActualizandoAgregandoYQuitandoCeldas() {
        Oferta oferta = ofertaPendienteEnConsorcio();
        DetalleOferta racionesA = oferta.getDetalles().get(0);
        when(inventarioOngRepository.findByOngIdIn(anyCollection())).thenReturn(List.of(
                inventario(ongA, raciones, 800), inventario(ongB, raciones, 500), inventario(ongB, frazadas, 100)));

        // ongA sube a 600 raciones, ongB deja las raciones y pasa a aportar 50 frazadas.
        OfertaEdicionRequest request = new OfertaEdicionRequest(List.of(
                new DetalleOfertaRequest(200L, 1L, 600),
                new DetalleOfertaRequest(201L, 2L, 50)));

        OfertaResponse response = ofertaService.actualizar(50L, request, ONG_USUARIO);

        assertThat(oferta.getDetalles()).hasSize(2);
        assertThat(oferta.getDetalles()).contains(racionesA);
        assertThat(racionesA.getCantidadOfrecida()).isEqualTo(600);
        assertThat(oferta.getFechaModificacion()).isNotNull();
        assertThat(response.fechaModificacion()).isNotNull();
        assertThat(response.aportes()).extracting(a -> a.totalOfrecido()).containsExactlyInAnyOrder(600, 50);
    }

    @Test
    void rechazaEdicionDeUnaOngQueNoParticipa() {
        ofertaPendienteEnConsorcio();
        OfertaEdicionRequest request = new OfertaEdicionRequest(List.of(new DetalleOfertaRequest(200L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.actualizar(50L, request, 3L))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void rechazaEdicionFueraDeLaVentana() {
        ofertaPendienteEnConsorcio();
        lote.setFechaCierreOfertas(java.time.LocalDateTime.now().minusMinutes(1));
        OfertaEdicionRequest request = new OfertaEdicionRequest(List.of(new DetalleOfertaRequest(200L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.actualizar(50L, request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya cerró");
    }

    @Test
    void rechazaEdicionDeOfertaNoPendiente() {
        ofertaPendienteEnConsorcio().setEstado(EstadoOferta.VALIDADA);
        OfertaEdicionRequest request = new OfertaEdicionRequest(List.of(new DetalleOfertaRequest(200L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.actualizar(50L, request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya no se puede modificar");
    }

    @Test
    void rechazaEdicionConUnaOngQueNoEstaEnLaOferta() {
        ofertaPendienteEnConsorcio();
        OfertaEdicionRequest request = new OfertaEdicionRequest(List.of(
                new DetalleOfertaRequest(200L, 1L, 100),
                new DetalleOfertaRequest(200L, 2L, 100),
                new DetalleOfertaRequest(200L, 3L, 100)));

        assertThatThrownBy(() -> ofertaService.actualizar(50L, request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está entre las ONGs de la oferta");
    }

    @Test
    void rechazaEdicionQueDejaUnaOngSinAportar() {
        ofertaPendienteEnConsorcio();
        when(inventarioOngRepository.findByOngIdIn(anyCollection())).thenReturn(List.of(
                inventario(ongA, raciones, 800), inventario(ongB, raciones, 500)));
        OfertaEdicionRequest request = new OfertaEdicionRequest(List.of(new DetalleOfertaRequest(200L, 1L, 100)));

        assertThatThrownBy(() -> ofertaService.actualizar(50L, request, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Manos Unidas");
    }

    @Test
    void eliminaLogicamenteSinBorrarDetalles() {
        Oferta oferta = ofertaPendienteEnConsorcio();

        // La elimina ongB, que también participa.
        ofertaService.eliminar(50L, 2L);

        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ELIMINADA);
        assertThat(oferta.getFechaModificacion()).isNotNull();
        assertThat(oferta.getDetalles()).hasSize(2);
    }

    @Test
    void rechazaBajaDeUnaOngQueNoParticipa() {
        ofertaPendienteEnConsorcio();

        assertThatThrownBy(() -> ofertaService.eliminar(50L, 3L))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void rechazaBajaFueraDeLaVentana() {
        ofertaPendienteEnConsorcio();
        lote.setFechaCierreOfertas(java.time.LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> ofertaService.eliminar(50L, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya cerró");
    }

    @Test
    void rechazaBajaDeOfertaYaEliminada() {
        ofertaPendienteEnConsorcio().setEstado(EstadoOferta.ELIMINADA);

        assertThatThrownBy(() -> ofertaService.eliminar(50L, ONG_USUARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya no se puede modificar");
    }

    @Test
    void devuelveLosLotesDondeLaOngYaOferto() {
        when(ofertaRepository.findLoteIdsConOfertaDeOng(1L)).thenReturn(List.of(10L, 11L));

        assertThat(ofertaService.lotesConOfertaDeOng(1L)).containsExactly(10L, 11L);
    }
}
