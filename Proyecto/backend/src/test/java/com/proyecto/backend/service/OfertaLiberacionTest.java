package com.proyecto.backend.service;

import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
public class OfertaLiberacionTest {

    @Autowired private OfertaService ofertaService;
    @Autowired private InventarioOngRepository inventarioOngRepository;
    @Autowired private OngRepository ongRepository;
    @Autowired private RecursoRepository recursoRepository;
    @Autowired private OfertaRepository ofertaRepository;
    @Autowired private MunicipioRepository municipioRepository;
    @Autowired private EmergenciaRepository emergenciaRepository;
    @Autowired private LoteRepository loteRepository;
    @Autowired private ItemLoteRepository itemLoteRepository;

    private Long ofertaId;
    private Long ongId;
    private Long recursoId;

    @BeforeEach
    public void setupDb() {
        Municipio municipio = municipioRepository.findById(1L).get();
        Ong ong = ongRepository.findById(1L).get();
        this.ongId = ong.getId();

        Recurso recurso = recursoRepository.findById(3L).get(); // Frazadas
        this.recursoId = recurso.getId();

        // 1. Simulamos el inventario DESPUÉS de una validación.
        // Supongamos que el total original era 200, y el SN bloqueó 40. 
        // El stock actual en la base de datos debe ser 160.
        InventarioOng inventario = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoId).get();
        inventario.setCantidadDisponible(160);
        inventarioOngRepository.save(inventario);

        Emergencia emergencia = new Emergencia();
        emergencia.setDescripcion("Emergencia test liberación");
        emergencia.setNivelGravedad(NivelGravedad.ALTA);
        emergencia.setZonaAfectada("Zona Sur");
        emergencia.setFechaRegistro(LocalDateTime.now());
        emergencia.setMunicipio(municipio);
        emergencia = emergenciaRepository.save(emergencia);

        Lote lote = new Lote();
        lote.setTitulo("Lote para liberar");
        lote.setEstado(EstadoLote.ACTIVO);
        lote.setFechaCreacion(LocalDateTime.now());
        lote.setEmergencia(emergencia);
        lote = loteRepository.save(lote);

        ItemLote itemLote = new ItemLote();
        itemLote.setLote(lote);
        itemLote.setRecurso(recurso);
        itemLote.setCantidadRequerida(100);
        itemLote = itemLoteRepository.save(itemLote);

        // 2. Creamos una oferta simulando que YA fue validada parcialmente por el SN
        Oferta oferta = new Oferta();
        oferta.setEstado(EstadoOferta.ACEPTADA_PARCIAL);
        oferta.setFechaOferta(LocalDateTime.now());
        oferta.setLote(lote);

        DetalleOferta detalle = new DetalleOferta();
        detalle.setOferta(oferta);
        detalle.setItemLote(itemLote);
        detalle.setOng(ong);
        detalle.setCantidadOfrecida(100); 
        detalle.setCantidadAceptada(40); // El SN bloqueó solo 40 unidades
        
        oferta.getDetalles().add(detalle);
        oferta = ofertaRepository.save(oferta);

        this.ofertaId = oferta.getId();
    }

    @Test
    public void testLiberarPorNoAdjudicacion() {
        // Ejecución: El municipio descarta la oferta
        ofertaService.liberarRecursosOferta(ofertaId, EstadoOferta.NO_ADJUDICADA);

        // Verificación de Estado
        Oferta ofertaActualizada = ofertaRepository.findById(ofertaId).get();
        assertEquals(EstadoOferta.NO_ADJUDICADA, ofertaActualizada.getEstado());

        // Verificación de Inventario: Debe devolver exactamente las 40 aceptadas (160 + 40 = 200)
        InventarioOng invFinal = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoId).get();
        assertEquals(200, invFinal.getCantidadDisponible());
    }

    @Test
    public void testLiberarPorFinalizacion() {
        // Preparación: Avanzamos la oferta a estado "En ejecución"
        Oferta oferta = ofertaRepository.findById(ofertaId).get();
        oferta.setEstado(EstadoOferta.EN_EJECUCION);
        ofertaRepository.save(oferta);

        // Ejecución: La emergencia termina y los recursos vuelven a la ONG
        ofertaService.liberarRecursosOferta(ofertaId, EstadoOferta.FINALIZADA);

        // Verificación
        Oferta ofertaActualizada = ofertaRepository.findById(ofertaId).get();
        assertEquals(EstadoOferta.FINALIZADA, ofertaActualizada.getEstado());

        InventarioOng invFinal = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoId).get();
        assertEquals(200, invFinal.getCantidadDisponible());
    }

    @Test
    public void testFallaPorNuevoEstadoInvalido() {
        // Ejecución: Intentamos pasar a un estado que no libera recursos (ej. PENDIENTE)
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class, () -> {
            ofertaService.liberarRecursosOferta(ofertaId, EstadoOferta.PENDIENTE);
        });

        assertEquals("Estado inválido. Solo se liberan recursos al pasar a NO_ADJUDICADA o FINALIZADA", excepcion.getMessage());
        
        // Verificación: El inventario no se tocó
        InventarioOng invFinal = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoId).get();
        assertEquals(160, invFinal.getCantidadDisponible());
    }

    @Test
    public void testFallaPorOfertaYaLiberada() {
        // Preparación: Modificamos la oferta a un estado donde ya no hay recursos comprometidos
        Oferta oferta = ofertaRepository.findById(ofertaId).get();
        oferta.setEstado(EstadoOferta.RECHAZADA);
        ofertaRepository.save(oferta);

        // Ejecución: Intentamos liberar de nuevo
        IllegalStateException excepcion = assertThrows(IllegalStateException.class, () -> {
            ofertaService.liberarRecursosOferta(ofertaId, EstadoOferta.NO_ADJUDICADA);
        });

        assertEquals("La oferta ya se encuentra en un estado donde sus recursos no están comprometidos", excepcion.getMessage());
    }
}