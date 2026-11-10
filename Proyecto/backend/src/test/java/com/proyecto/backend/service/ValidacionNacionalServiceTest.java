package com.proyecto.backend.service;
import com.proyecto.backend.dto.*;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ValidacionNacionalServiceTest {
 @Mock LoteRepository lotes;
 @Mock OfertaRepository ofertas;
 @Mock OfertaService ofertaService;
 ValidacionNacionalService service; Lote lote; Oferta oferta;
 @BeforeEach void preparar() {
  service=new ValidacionNacionalService(lotes,ofertas,ofertaService);
  var emergencia=new Emergencia(); emergencia.setId(7L); emergencia.setBonitaCaseId("caso");
  lote=new Lote(); lote.setId(10L); lote.setEmergencia(emergencia); lote.setEstado(EstadoLote.ACTIVO);
  lote.setFechaCierreOfertas(LocalDateTime.now().minusHours(1));
  when(lotes.findById(10L)).thenReturn(Optional.of(lote));
  var ong=new Ong(); ong.setId(2L); var recurso=new Recurso(); recurso.setId(3L);
  var item=new ItemLote(); item.setRecurso(recurso);
  var detalle=new DetalleOferta(); detalle.setId(100L); detalle.setOng(ong); detalle.setItemLote(item); detalle.setCantidadOfrecida(100);
  oferta=new Oferta(); oferta.setId(20L); oferta.setLote(lote); oferta.setEstado(EstadoOferta.PENDIENTE); oferta.getDetalles().add(detalle);
 }
 void callback() { when(lotes.bloquearPorId(10L)).thenReturn(Optional.of(lote)); lenient().when(ofertas.findById(20L)).thenReturn(Optional.of(oferta)); }
 IntegracionNacional.Respuesta respuesta(boolean aprobada,List<IntegracionNacional.ItemSN> items) {
  return new IntegracionNacional.Respuesta(10L,"caso",List.of(new IntegracionNacional.Resultado(20L,aprobada,items)));
 }
 @Test void entregaPendientesConIdsYCantidadesSinLlamadasExternas() {
  when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
  assertThat(service.plan(7L,10L,"caso").ofertas().getFirst().items().getFirst()).isEqualTo(new IntegracionNacional.Item(100L,2L,3L,100));
  verifyNoInteractions(ofertaService);
 }
 @Test void omiteOfertasYaProcesadas() {
  oferta.setEstado(EstadoOferta.VALIDADA); when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
  assertThat(service.plan(7L,10L,"caso").ofertas()).isEmpty();
 }
 @Test void registraCantidadesRecibidasDesdeBonita() {
  callback(); service.registrar(7L,respuesta(true,List.of(new IntegracionNacional.ItemSN(2L,3L,40,"BLOQUEADA"))));
  verify(ofertaService).procesarValidacionSN(20L,new ValidacionSNRequest(true,"Resultado nacional recibido desde Bonita",List.of(new ItemValidadoRequest(100L,40))));
 }
 @Test void registraRechazoSinInventario() {
  callback(); service.registrar(7L,respuesta(false,List.of()));
  verify(ofertaService).procesarValidacionSN(20L,new ValidacionSNRequest(false,"Resultado nacional recibido desde Bonita",List.of()));
 }
 @Test void impideValidarDespuesDeDecision() {
  callback(); lote.setDecisionMunicipal(true);
  assertThatThrownBy(()->service.registrar(7L,respuesta(false,List.of()))).isInstanceOf(ReglaNegocioException.class); verifyNoInteractions(ofertaService);
 }
 @Test void rechazaRecursosAjenos() {
  callback(); assertThatThrownBy(()->service.registrar(7L,respuesta(true,List.of(new IntegracionNacional.ItemSN(99L,3L,40,"BLOQUEADA")))))
   .isInstanceOf(ReglaNegocioException.class); verifyNoInteractions(ofertaService);
 }
 @Test void rechazaCasoEquivocado() {
  assertThatThrownBy(()->service.plan(7L,10L,"otro")).isInstanceOf(ReglaNegocioException.class); verifyNoInteractions(ofertaService,ofertas);
 }
}
