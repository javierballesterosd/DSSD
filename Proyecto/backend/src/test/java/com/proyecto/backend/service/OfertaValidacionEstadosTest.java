package com.proyecto.backend.service;

import com.proyecto.backend.dto.ItemValidadoRequest;
import com.proyecto.backend.dto.ValidacionSNRequest;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
public class OfertaValidacionEstadosTest {

    @Autowired private OfertaService ofertaValidacionService;
    @Autowired private InventarioOngRepository inventarioOngRepository;
    @Autowired private OngRepository ongRepository;
    @Autowired private RecursoRepository recursoRepository;
    @Autowired private OfertaRepository ofertaRepository;
    @Autowired private MunicipioRepository municipioRepository;
    @Autowired private EmergenciaRepository emergenciaRepository;
    @Autowired private LoteRepository loteRepository;
    @Autowired private ItemLoteRepository itemLoteRepository;

    private Long ofertaId;
    private Long detalleFrazadasId;
    private Long detalleMedicosId;
    private Long ongId;
    private Long recursoFrazadasId;
    private Long recursoMedicosId;

    @BeforeEach
    public void setupDb() {
        Municipio municipio = municipioRepository.findById(1L).get();
        Ong ong = ongRepository.findById(1L).get();
        this.ongId = ong.getId();

        Recurso frazadas = recursoRepository.findById(3L).get(); // Frazadas
        this.recursoFrazadasId = frazadas.getId();
        
        Recurso medicos = recursoRepository.findById(10L).get(); // Médicos
        this.recursoMedicosId = medicos.getId();

        // Reseteamos el stock inicial para los tests (200 Frazadas, 30 Médicos)
        InventarioOng invFrazadas = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoFrazadasId).get();
        invFrazadas.setCantidadDisponible(200);
        inventarioOngRepository.save(invFrazadas);

        InventarioOng invMedicos = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoMedicosId).get();
        invMedicos.setCantidadDisponible(30);
        inventarioOngRepository.save(invMedicos);

        Emergencia emergencia = new Emergencia();
        emergencia.setDescripcion("Emergencia test estados");
        emergencia.setNivelGravedad(NivelGravedad.ALTA);
        emergencia.setZonaAfectada("Zona Centro");
        emergencia.setFechaRegistro(LocalDateTime.now());
        emergencia.setMunicipio(municipio);
        emergencia = emergenciaRepository.save(emergencia);

        Lote lote = new Lote();
        lote.setTitulo("Lote mixto");
        lote.setEstado(EstadoLote.ACTIVO);
        lote.setFechaCreacion(LocalDateTime.now());
        lote.setEmergencia(emergencia);
        lote = loteRepository.save(lote);

        // Creamos y configuramos el ítem de Frazadas
        ItemLote itemFrazadas = new ItemLote();
        itemFrazadas.setLote(lote);
        itemFrazadas.setRecurso(frazadas);
        itemFrazadas.setCantidadRequerida(150);
        itemFrazadas = itemLoteRepository.save(itemFrazadas);

        // Creamos y configuramos el ítem de Médicos
        ItemLote itemMedicos = new ItemLote();
        itemMedicos.setLote(lote);
        itemMedicos.setRecurso(medicos);
        itemMedicos.setCantidadRequerida(50);
        itemMedicos = itemLoteRepository.save(itemMedicos);

        Oferta oferta = new Oferta();
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaOferta(LocalDateTime.now());
        oferta.setLote(lote);

        // La ONG ofrece 100 frazadas y 30 médicos
        DetalleOferta detFrazadas = new DetalleOferta();
        detFrazadas.setOferta(oferta);
        detFrazadas.setItemLote(itemFrazadas);
        detFrazadas.setOng(ong);
        detFrazadas.setCantidadOfrecida(100);

        DetalleOferta detMedicos = new DetalleOferta();
        detMedicos.setOferta(oferta);
        detMedicos.setItemLote(itemMedicos);
        detMedicos.setOng(ong);
        detMedicos.setCantidadOfrecida(30);

        oferta.getDetalles().add(detFrazadas);
        oferta.getDetalles().add(detMedicos);
        oferta = ofertaRepository.save(oferta);

        this.ofertaId = oferta.getId();
        
        // Extraemos los IDs autogenerados de los detalles para usarlos en el test
        this.detalleFrazadasId = oferta.getDetalles().stream()
                .filter(d -> d.getItemLote().getRecurso().getId().equals(3L)).findFirst().get().getId();
        this.detalleMedicosId = oferta.getDetalles().stream()
                .filter(d -> d.getItemLote().getRecurso().getId().equals(10L)).findFirst().get().getId();
    }

    @Test
    public void testAprobacionTotal() {
        // SN aprueba exactamente lo que la ONG ofreció
        ValidacionSNRequest validacion = new ValidacionSNRequest(true, "Aprobado", List.of(
                new ItemValidadoRequest(detalleFrazadasId, 100),
                new ItemValidadoRequest(detalleMedicosId, 30)
        ));

        ofertaValidacionService.procesarValidacionSN(ofertaId, validacion);

        Oferta ofertaActualizada = ofertaRepository.findById(ofertaId).get();
        assertEquals(EstadoOferta.VALIDADA, ofertaActualizada.getEstado());

        // Descuenta todo lo ofrecido
        assertEquals(100, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoFrazadasId).get().getCantidadDisponible()); // 200 - 100
        assertEquals(0, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoMedicosId).get().getCantidadDisponible()); // 30 - 30
    }

    @Test
    public void testAceptacionParcialPorRecorte() {
        // SN aprueba menos frazadas (40) de las 100 ofrecidas, y todos los médicos (30)
        ValidacionSNRequest validacion = new ValidacionSNRequest(true, "Recorte por logística", List.of(
                new ItemValidadoRequest(detalleFrazadasId, 40),
                new ItemValidadoRequest(detalleMedicosId, 30)
        ));

        ofertaValidacionService.procesarValidacionSN(ofertaId, validacion);

        Oferta ofertaActualizada = ofertaRepository.findById(ofertaId).get();
        assertEquals(EstadoOferta.ACEPTADA_PARCIAL, ofertaActualizada.getEstado());

        // Solo descuenta las 40 frazadas aceptadas
        assertEquals(160, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoFrazadasId).get().getCantidadDisponible()); // 200 - 40
        assertEquals(0, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoMedicosId).get().getCantidadDisponible());
    }

    @Test
    public void testAceptacionParcialPorRechazoItem() {
        // SN aprueba las 100 frazadas, pero rechaza completamente los médicos (0)
        ValidacionSNRequest validacion = new ValidacionSNRequest(true, "No se necesitan médicos", List.of(
                new ItemValidadoRequest(detalleFrazadasId, 100),
                new ItemValidadoRequest(detalleMedicosId, 0)
        ));

        ofertaValidacionService.procesarValidacionSN(ofertaId, validacion);

        Oferta ofertaActualizada = ofertaRepository.findById(ofertaId).get();
        assertEquals(EstadoOferta.ACEPTADA_PARCIAL, ofertaActualizada.getEstado());

        // Descuenta las frazadas, pero el stock de médicos queda intacto
        assertEquals(100, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoFrazadasId).get().getCantidadDisponible());
        assertEquals(30, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoMedicosId).get().getCantidadDisponible()); // 30 - 0
    }

    @Test
    public void testRechazoTotal() {
        // SN rechaza toda la oferta (aprobada = false)
        ValidacionSNRequest validacion = new ValidacionSNRequest(false, "ONG sin certificaciones al día", List.of());

        ofertaValidacionService.procesarValidacionSN(ofertaId, validacion);

        Oferta ofertaActualizada = ofertaRepository.findById(ofertaId).get();
        assertEquals(EstadoOferta.RECHAZADA, ofertaActualizada.getEstado());

        // El inventario no se toca en absoluto
        assertEquals(200, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoFrazadasId).get().getCantidadDisponible());
        assertEquals(30, inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoMedicosId).get().getCantidadDisponible());
    }
} 
