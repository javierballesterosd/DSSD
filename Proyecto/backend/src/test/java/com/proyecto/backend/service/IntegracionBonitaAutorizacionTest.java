package com.proyecto.backend.service;
import com.proyecto.backend.controller.ValidacionNacionalController;
import com.proyecto.backend.dto.IntegracionNacional;
import com.proyecto.backend.exception.UnauthenticatedException;
import com.proyecto.backend.security.BonitaTokenValidator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class IntegracionBonitaAutorizacionTest {
 @Test void losTresEndpointsExigenElTokenDeIntegracion() {
  var validaciones=mock(ValidacionNacionalService.class);
  var decisiones=mock(DecisionMunicipalService.class);
  var tokens=mock(BonitaTokenValidator.class);
  var controller=new ValidacionNacionalController(validaciones,decisiones,tokens);
  var respuesta=new IntegracionNacional.Respuesta(10L,"7",List.of());
  assertThatThrownBy(()->controller.plan(7L,10L,"7",null)).isInstanceOf(UnauthenticatedException.class);
  assertThatThrownBy(()->controller.validar(7L,respuesta,"Bearer incorrecto")).isInstanceOf(UnauthenticatedException.class);
  assertThatThrownBy(()->controller.confirmar(7L,respuesta,null)).isInstanceOf(UnauthenticatedException.class);
  verifyNoInteractions(validaciones,decisiones);
 }
 @Test void elCallbackAutorizadoDelegaSoloLaActualizacionLocal() {
  var validaciones=mock(ValidacionNacionalService.class);
  var decisiones=mock(DecisionMunicipalService.class);
  var tokens=mock(BonitaTokenValidator.class);when(tokens.isValid("Bearer prueba")).thenReturn(true);
  var controller=new ValidacionNacionalController(validaciones,decisiones,tokens);
  var respuesta=new IntegracionNacional.Respuesta(10L,"7",List.of());
  controller.confirmar(7L,respuesta,"Bearer prueba");
  verify(decisiones).confirmar(7L,respuesta);verifyNoInteractions(validaciones);
 }
}
