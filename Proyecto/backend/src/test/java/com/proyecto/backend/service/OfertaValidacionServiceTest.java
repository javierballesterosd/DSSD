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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
public class OfertaValidacionServiceTest {

    @Autowired
    private OfertaService ofertaValidacionService;

    @Autowired
    private InventarioOngRepository inventarioOngRepository;

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private MunicipioRepository municipioRepository;

    @Autowired
    private EmergenciaRepository emergenciaRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private ItemLoteRepository itemLoteRepository;

    private Long ofertaId1;
    private Long ofertaId2;
    private Long detalleIdOferta1;
    private Long detalleIdOferta2;
    private Long ongId;
    private Long recursoId;

    @BeforeEach
    public void setupDb() {
        // 1. Recuperamos los datos estáticos directamente de la semilla SQL
        Municipio municipio = municipioRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Falta Municipio ID 1 (La Plata) en la semilla"));
        
        Ong ong = ongRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Falta ONG ID 1 (Cruz Roja) en la semilla"));
        this.ongId = ong.getId();

        Recurso recurso = recursoRepository.findById(3L)
                .orElseThrow(() -> new RuntimeException("Falta Recurso ID 3 (Frazadas) en la semilla"));
        this.recursoId = recurso.getId();

        InventarioOng inventario = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoId).get();
        inventario.setCantidadDisponible(200);
        inventarioOngRepository.save(inventario);
        
        // 2. Creamos la Emergencia y el Lote (que la semilla deja vacíos por depender de Bonita)
        Emergencia emergencia = new Emergencia();
        emergencia.setDescripcion("Emergencia de prueba concurrencia La Plata");
        emergencia.setNivelGravedad(NivelGravedad.ALTA);
        emergencia.setZonaAfectada("Zona Norte");
        emergencia.setFechaRegistro(LocalDateTime.now());
        emergencia.setMunicipio(municipio);
        emergencia = emergenciaRepository.save(emergencia);

        Lote lote = new Lote();
        lote.setTitulo("Lote de frazadas");
        lote.setEstado(EstadoLote.ACTIVO); 
        lote.setFechaCreacion(LocalDateTime.now());
        lote.setEmergencia(emergencia);
        lote = loteRepository.save(lote);

        ItemLote itemLote = new ItemLote();
        itemLote.setLote(lote);
        itemLote.setRecurso(recurso);
        itemLote.setCantidadRequerida(300);
        itemLote = itemLoteRepository.save(itemLote);

        // 3. Crear Oferta 1 (y su detalle en cascada)
        Oferta oferta1 = new Oferta();
        oferta1.setEstado(EstadoOferta.PENDIENTE);
        oferta1.setFechaOferta(LocalDateTime.now());
        oferta1.setLote(lote);

        DetalleOferta detalle1 = new DetalleOferta();
        detalle1.setOferta(oferta1);
        detalle1.setItemLote(itemLote);
        detalle1.setOng(ong);
        detalle1.setCantidadOfrecida(150); // Ofrecemos 150 frazadas
        
        oferta1.getDetalles().add(detalle1); 
        oferta1 = ofertaRepository.save(oferta1); 
        
        this.ofertaId1 = oferta1.getId();
        this.detalleIdOferta1 = oferta1.getDetalles().iterator().next().getId(); 

        // 4. Crear Oferta 2 (y su detalle en cascada)
        Oferta oferta2 = new Oferta();
        oferta2.setEstado(EstadoOferta.PENDIENTE);
        oferta2.setFechaOferta(LocalDateTime.now());
        oferta2.setLote(lote);

        DetalleOferta detalle2 = new DetalleOferta();
        detalle2.setOferta(oferta2);
        detalle2.setItemLote(itemLote);
        detalle2.setOng(ong);
        detalle2.setCantidadOfrecida(150); // Ofrecemos 150 frazadas
        
        oferta2.getDetalles().add(detalle2); 
        oferta2 = ofertaRepository.save(oferta2); 
        
        this.ofertaId2 = oferta2.getId();
        this.detalleIdOferta2 = oferta2.getDetalles().iterator().next().getId();
    }

    @Test
    public void testBloqueoPesimistaInventarioLocal() throws InterruptedException {
        // La semilla indica que Cruz Roja (ID 1) tiene 200 frazadas en inventario.
        // Simulamos que el SN aprueba ambas ofertas por 150 frazadas cada una.
        ValidacionSNRequest validacion1 = new ValidacionSNRequest(true, "OK",
                List.of(new ItemValidadoRequest(detalleIdOferta1, 150)));

        ValidacionSNRequest validacion2 = new ValidacionSNRequest(true, "OK",
                List.of(new ItemValidadoRequest(detalleIdOferta2, 150)));

        int hilosConcurrentes = 2;
        ExecutorService executor = Executors.newFixedThreadPool(hilosConcurrentes);
        CountDownLatch latchLargada = new CountDownLatch(1);
        CountDownLatch latchLlegada = new CountDownLatch(hilosConcurrentes);

        AtomicInteger exitosas = new AtomicInteger(0);
        AtomicInteger fallidas = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                latchLargada.await();
                ofertaValidacionService.procesarValidacionSN(ofertaId1, validacion1);
                exitosas.incrementAndGet();
            } catch (Exception e) {
                fallidas.incrementAndGet();
            } finally {
                latchLlegada.countDown();
            }
        });

        executor.submit(() -> {
            try {
                latchLargada.await();
                ofertaValidacionService.procesarValidacionSN(ofertaId2, validacion2);
                exitosas.incrementAndGet();
            } catch (Exception e) {
                fallidas.incrementAndGet(); 
            } finally {
                latchLlegada.countDown();
            }
        });

        latchLargada.countDown();
        latchLlegada.await();

        assertEquals(1, exitosas.get(), "Solo una validación debió procesarse (consumió 150 de las 200 frazadas)");
        assertEquals(1, fallidas.get(), "La segunda validación debió rebotar porque solo quedaban 50 frazadas tras el bloqueo");

        // El stock final debe ser 50 (200 iniciales de la semilla - 150 de la petición ganadora)
        // Usamos el método de lectura simple para no requerir una transacción activa en el test
        InventarioOng inventarioFinal = inventarioOngRepository.findByOngIdAndRecursoId(ongId, recursoId).get();
        assertEquals(50, inventarioFinal.getCantidadDisponible());
    }
        

}