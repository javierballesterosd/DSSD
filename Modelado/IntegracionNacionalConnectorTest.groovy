import com.rescuesync.bonita.IntegracionNacionalConnector
import org.bonitasoft.engine.connector.EngineExecutionContext
import com.sun.net.httpserver.HttpServer
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

// Prueba aislada: solo servidores HTTP efímeros, sin tocar Bonita, SN ni bases reales.
def server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
def calls = []
def state = [decision: null, confirmada: false, pending: true, reserva: null,
             failCallback: false, insufficient: false, accepted: 0, rejected: 0]
def item = [ongId: 2, recursoId: 3, cantidad: 40]
def answer = { exchange, int code, Object body ->
    byte[] bytes = body == null ? new byte[0] : JsonOutput.toJson(body).getBytes('UTF-8')
    if (code == 204) exchange.sendResponseHeaders(code, -1)
    else { exchange.sendResponseHeaders(code, bytes.length); exchange.responseBody.write(bytes) }
    exchange.close()
}
server.createContext('/') { e ->
    try {
        String path = e.requestURI.path
        calls << e.requestMethod + ' ' + path
        boolean local = path.startsWith('/api/')
        if (path != '/login') assert e.requestHeaders.getFirst('Authorization') == 'Bearer ' + (local ? 'local-test' : 'jwt-test')
        if (path == '/login') { answer(e, 200, [token:'jwt-test']); return }
        if (path.endsWith('/integracion-nacional')) {
            boolean include = state.decision == null ? state.pending : !state.confirmada
            answer(e,200,[decision:state.decision,confirmada:state.confirmada,
                          ofertas: include ? [[ofertaId:20,items:[item]]] : []]); return
        }
        if (path == '/reservas') {
            answer(e,state.reserva == null ? 404 : 200,state.reserva == null ? [:] : [items:[item + [estado:state.reserva]]]); return
        }
        if (path == '/reservas/temporal') {
            if (state.insufficient) { answer(e,409,[:]); return }
            state.reserva='BLOQUEADA'; answer(e,201,[items:[item+[estado:state.reserva]]]); return
        }
        if (path == '/reservas/definitiva' || path == '/reservas/liberacion') {
            state.reserva=path.endsWith('definitiva') ? 'COMPROMETIDA' : 'LIBERADA'
            answer(e,200,[items:[item+[estado:state.reserva]]]); return
        }
        def body = new JsonSlurper().parseText(e.requestBody.getText('UTF-8'))
        assert body.caseId == '7' && body.loteId == 10
        if (path.endsWith('/validacion-sn')) {
            assert body.resultados.size() == 1
            if (body.resultados[0].aprobada) state.accepted++ else state.rejected++
            state.pending=false
        } else if (path.endsWith('/confirmacion')) {
            assert body.resultados[0].items[0].estado == (state.decision ? 'COMPROMETIDA' : 'LIBERADA')
            if (state.failCallback) { state.failCallback=false; answer(e,503,[:]); return }
            state.confirmada=true
        } else throw new AssertionError('Ruta inesperada '+path)
        answer(e,204,null)
    } catch (Throwable ex) { ex.printStackTrace(); answer(e,500,[:]) }
}
server.start()
def env = File.createTempFile('rescuesync-connector-', '.env')
try {
    def base = 'http://127.0.0.1:'+server.address.port
    env.text = "NACIONAL_BASE_URL=${base}\nBONITA_BACKEND_URL=${base}\nNACIONAL_USERNAME=test\nNACIONAL_PASSWORD=test\nBONITA_OFFERS_API_TOKEN=local-test\n"
    System.setProperty('rescuesync.env',env.absolutePath)
    def run = { operation, decision ->
        def connector = new IntegracionNacionalConnector()
        def context = new EngineExecutionContext(); context.processInstanceId=7L
        connector.executionContext=context
        connector.inputParameters=[operacion:operation,emergenciaId:7L,loteId:10L,aceptar:decision]
        connector.execute()
    }
    run('VALIDAR',false)
    assert state.accepted == 1 && state.reserva == 'BLOQUEADA'
    int temporal = calls.count { it == 'POST /reservas/temporal' }
    run('VALIDAR',false)
    assert calls.count { it == 'POST /reservas/temporal' } == temporal

    state.decision=true; state.failCallback=true
    boolean failed=false
    try { run('ACEPTAR',true) } catch (Exception expected) { failed=true }
    assert failed && state.reserva == 'COMPROMETIDA' && !state.confirmada
    run('ACEPTAR',true)
    assert state.confirmada
    int commits=calls.count { it == 'POST /reservas/definitiva' }
    run('ACEPTAR',true)
    assert calls.count { it == 'POST /reservas/definitiva' } == commits
    int before=calls.size();run('RECHAZAR',true);assert calls.size()==before

    state.decision=false;state.confirmada=false;state.reserva='BLOQUEADA';state.failCallback=true
    failed=false
    try { run('RECHAZAR',false) } catch (Exception expected) { failed=true }
    assert failed && state.reserva=='LIBERADA' && !state.confirmada
    run('RECHAZAR',false);assert state.confirmada

    state.decision=null;state.confirmada=false;state.pending=true;state.reserva=null;state.insufficient=true
    run('VALIDAR',false)
    assert state.rejected==1 && !state.pending
    println 'Conector verificado: reserva, 409, compromiso, liberación y reintentos tras fallo del callback.'
} finally { server.stop(0);env.delete();System.clearProperty('rescuesync.env') }
