package com.proyecto.backend.service;

import com.proyecto.backend.client.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class DashboardBonitaClientTest {
    @Test void paginaElHistorialYEnviaLaSesionDeBonita() {
        var builder=RestClient.builder().baseUrl("http://bonita");
        var server=MockRestServiceServer.bindTo(builder).build();
        var client=new BonitaClient(builder.build());
        String pagina="["+String.join(",", java.util.Collections.nCopies(100,"{\"id\":\"1\"}"))+"]";
        server.expect(requestTo("http://bonita/API/bpm/archivedFlowNode?p=0&c=100"))
                .andExpect(header("X-Bonita-API-Token","token"))
                .andExpect(header("Cookie","JSESSIONID=session; X-Bonita-API-Token=token"))
                .andRespond(withSuccess(pagina,MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://bonita/API/bpm/archivedFlowNode?p=1&c=100"))
                .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        assertThat(client.datosDashboard(new BonitaSession("session","token"),"archivedFlowNode")).hasSize(100);
        server.verify();
    }
}
