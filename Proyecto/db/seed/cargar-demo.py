"""Carga escenarios reales mediante las APIs, sin borrar datos existentes.

Con Docker y el proceso de Bonita levantados, desde Proyecto:
    python db/seed/cargar-demo.py

Solo requiere Python 3. Busca la version mas alta del BOS en Modelado, lee sus
cuentas de ejemplo y conserva el avance
en .dashboard-demo-state.json para retomar la carga. No almacena contrasenas.
"""

import argparse
import http.cookiejar
import json
import os
import re
from pathlib import Path
import sys
import time
from datetime import datetime, timedelta, timezone
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import zipfile


PROYECTO = Path(__file__).resolve().parents[2]
ESTADO = Path(__file__).with_name(".dashboard-demo-state.json")
ZONA = timezone(timedelta(hours=-3))
PREFIJO = "[DEMO DASHBOARD v1]"

# Operador, coordinador, gravedad, resultado y aportes (usuario, recurso, cantidad).
# Los casos sin decision permanecen en Ver ofertas validadas.
ESCENARIOS = [
    ("operador.laplata", "coord.norte", "BAJA", True, [("ong.cruzroja", 3, 2)]),
    ("operador.laplata", "coord.norte", "ALTA", False, [("ong.caritas", 3, 3)]),
    ("operador.citybell", "coord.norte", "MEDIA", True, [("ong.caritas", 1, 4)]),
    ("operador.citybell", "coord.norte", "CRITICA", None,
     [("ong.cruzroja", 3, 2), ("ong.caritas", 3, 2)]),
    ("operador.berisso", "coord.sur", "ALTA", True, [("ong.bomberos.1", 4, 1)]),
    ("operador.berisso", "coord.sur", "MEDIA", None,
     [("ong.bomberos.1", 7, 1), ("ong.caritas", 8, 2)]),
    ("operador.lobos", "coord.region3", "CRITICA", True, [("ong.techo.1", 6, 1)]),
    ("operador.lobos", "coord.region3", "BAJA", False, [("ong.bancoalimentos.1", 1, 5)]),
    ("operador.quilmes", "coord.region4", "MEDIA", True,
     [("ong.caritas", 5, 10), ("ong.cruzroja", 8, 2)]),
    ("operador.quilmes", "coord.region4", "ALTA", None, [("ong.techo.1", 3, 4)]),
]


class ErrorCarga(RuntimeError):
    pass


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.cookies = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))

    def pedir(self, metodo, ruta, datos=None, formulario=False):
        cabeceras = {"Accept": "application/json"}
        contenido = None
        if datos is not None:
            contenido = (urllib.parse.urlencode(datos) if formulario else json.dumps(datos)).encode()
            cabeceras["Content-Type"] = (
                "application/x-www-form-urlencoded" if formulario else "application/json")
        for cookie in self.cookies:
            if cookie.name == "X-Bonita-API-Token":
                cabeceras[cookie.name] = cookie.value
        request = urllib.request.Request(self.base + ruta, data=contenido,
                                         headers=cabeceras, method=metodo)
        try:
            with self.http.open(request, timeout=60) as respuesta:
                cuerpo = respuesta.read().decode("utf-8")
                return json.loads(cuerpo) if cuerpo.strip() else None
        except urllib.error.HTTPError as error:
            # No imprimir cuerpos de respuestas de login, cookies ni credenciales.
            mensaje = ""
            if "login" not in ruta:
                try:
                    cuerpo = json.loads(error.read().decode("utf-8"))
                    mensaje = str(cuerpo.get("message", cuerpo.get("mensaje", "")))[:250]
                except (ValueError, AttributeError, UnicodeDecodeError):
                    pass
            raise ErrorCarga(f"{metodo} {ruta.split('?')[0]}: HTTP {error.code}. {mensaje}") from None
        except urllib.error.URLError:
            raise ErrorCarga(f"No se pudo conectar con {self.base}") from None

    def listar_bonita(self, recurso, filtros=()):
        resultado = []
        for pagina in range(10000):
            parametros = [("p", pagina), ("c", 100)] + [("f", f) for f in filtros]
            filas = self.pedir("GET", "/API/bpm/" + recurso + "?" + urllib.parse.urlencode(parametros))
            if not isinstance(filas, list):
                raise ErrorCarga("Bonita devolvio una lista invalida")
            resultado.extend(filas)
            if len(filas) < 100:
                return resultado
        raise ErrorCarga("Demasiadas paginas en Bonita")


def config_env():
    valores = {}
    archivo = PROYECTO / ".env"
    if archivo.exists():
        for linea in archivo.read_text(encoding="utf-8-sig").splitlines():
            if "=" in linea and not linea.lstrip().startswith("#"):
                nombre, valor = linea.split("=", 1)
                valores[nombre.strip()] = valor.strip().strip('"').strip("'")
    return valores


def buscar_bos():
    modelado = PROYECTO.parent / "Modelado"

    def version(archivo):
        numero = re.search(r"-V(\d+(?:\.\d+)*)\.bos$", archivo.name, re.IGNORECASE)
        numeros = tuple(map(int, numero.group(1).split("."))) if numero else ()
        return numeros, archivo.stat().st_mtime, archivo.name

    for archivo in sorted(modelado.glob("*.bos"), key=version, reverse=True):
        with zipfile.ZipFile(archivo) as bos:
            if any(n.endswith("/organizations/RescueSync.xml") for n in bos.namelist()):
                return archivo
    raise ErrorCarga("No se encontro un BOS con la organizacion RescueSync en Modelado")


def cuentas_bos(archivo):
    with zipfile.ZipFile(archivo) as bos:
        ruta = next((n for n in bos.namelist() if n.endswith("/organizations/RescueSync.xml")), None)
        if ruta is None:
            raise ErrorCarga("El BOS no incluye la organizacion RescueSync")
        organizacion = ET.fromstring(bos.read(ruta))
    cuentas = {}
    for usuario in organizacion.iter():
        if usuario.tag.split("}")[-1] != "user":
            continue
        password = next((n.text for n in usuario if n.tag.split("}")[-1] == "password"), None)
        cuentas[usuario.attrib["userName"]] = password
    return cuentas


def guardar(estado):
    temporal = ESTADO.with_suffix(".tmp")
    temporal.write_text(json.dumps(estado, indent=2), encoding="utf-8")
    temporal.replace(ESTADO)


def esperar(descripcion, consulta, condicion, segundos=180):
    limite = time.monotonic() + segundos
    while time.monotonic() < limite:
        resultado = consulta()
        if condicion(resultado):
            return resultado
        time.sleep(2)
    raise ErrorCarga(f"Tiempo agotado esperando {descripcion}. Revisa el caso en Bonita y vuelve a ejecutar la carga.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend", help="URL del backend accesible desde esta computadora")
    parser.add_argument("--bonita", default="http://localhost:8080/bonita")
    parser.add_argument("--bos", type=Path, help="BOS especifico; por defecto se detecta en Modelado")
    args = parser.parse_args()
    config = config_env()
    backend = args.backend or "http://localhost:" + config.get("BACKEND_PORT", "8081")
    archivo_bos = args.bos or buscar_bos()
    print(f"Leyendo cuentas de {archivo_bos.name}", flush=True)
    cuentas = cuentas_bos(archivo_bos)
    sesiones = {}
    usuarios = {}
    bonitos = {}

    def sesion(nombre):
        if nombre not in sesiones:
            if not cuentas.get(nombre):
                raise ErrorCarga(f"Falta la cuenta de ejemplo {nombre} en el BOS")
            api = Api(backend)
            usuarios[nombre] = api.pedir("POST", "/api/auth/login",
                                       {"username": nombre, "password": cuentas[nombre]})
            sesiones[nombre] = api
        return sesiones[nombre]

    def bonita(nombre):
        if nombre not in bonitos:
            api = Api(args.bonita)
            api.pedir("POST", "/loginservice", {"username": nombre,
                      "password": cuentas[nombre], "redirect": "false"}, formulario=True)
            bonitos[nombre] = api
        return bonitos[nombre]

    print("Comprobando backend, Bonita, usuarios y catalogos...", flush=True)
    sesion("auditor.nacional")
    motor = bonita("auditor.nacional")
    procesos = motor.listar_bonita("process", ["name=Sistema P1", "activationState=ENABLED"])
    if not procesos:
        raise ErrorCarga("Desplega y habilita Sistema P1 en Bonita Studio, con RUTA_ENV configurado")
    # La API Community permite consultar parametros, pero no modificarlos.
    # Detectar una ruta ajena antes de crear casos evita dejarlos con conectores fallidos.
    if urllib.parse.urlparse(args.bonita).hostname in ("localhost", "127.0.0.1", "::1"):
        archivo_env = PROYECTO / ".env"
        if not archivo_env.is_file():
            raise ErrorCarga("Falta el archivo Proyecto/.env que necesitan los conectores de Bonita")
        for proceso in procesos:
            ruta = f"/API/bpm/processParameter/{proceso['id']}/RUTA_ENV"
            parametro = motor.pedir("GET", ruta)
            configurado = str(parametro.get("value", "")).strip()
            if not configurado or not Path(configurado).is_file():
                raise ErrorCarga("RUTA_ENV de Bonita no apunta a un archivo existente. "
                                 "Configura en Studio: " + archivo_env.as_posix()
                                 + " y vuelve a ejecutar el proceso antes de retomar la carga")
    for operador, coordinador, _, _, aportes in ESCENARIOS:
        sesion(operador)
        sesion(coordinador)
        if usuarios[operador].get("regionId") != usuarios[coordinador].get("regionId"):
            raise ErrorCarga(f"La region de {operador} no coincide con {coordinador}")
        for ong, _, _ in aportes:
            sesion(ong)
    recursos = sesiones["auditor.nacional"].pedir("GET", "/api/recursos")
    recursos_ids = {r["id"] for r in recursos}
    for _, _, _, _, aportes in ESCENARIOS:
        for ong, recurso, cantidad in aportes:
            if recurso not in recursos_ids:
                raise ErrorCarga(f"Falta el recurso de ejemplo {recurso}")
            inventario = sesion(ong).pedir("GET", "/api/ongs/inventario?ongIds=" + str(usuarios[ong]["ongId"]))
            disponible = next((r["cantidadDisponible"] for fila in inventario for r in fila["recursos"]
                               if r["recursoId"] == recurso), 0)
            if disponible < cantidad:
                raise ErrorCarga(f"{ong} no tiene stock suficiente del recurso {recurso}")

    estado = json.loads(ESTADO.read_text(encoding="utf-8")) if ESTADO.exists() else {}
    if estado and estado.get("backend") != backend:
        raise ErrorCarga("El avance guardado pertenece a otro backend. Usa la misma URL de la carga anterior.")
    estado["backend"] = backend
    escenarios = estado.setdefault("escenarios", {})
    if escenarios:
        casos = {str(c["id"]): c for c in motor.listar_bonita("case")}
        archivados = {str(c["sourceObjectId"]) for c in motor.listar_bonita("archivedCase")}
        perdidos = []
        for clave, registro in escenarios.items():
            caso_id = str(registro.get("caseId", ""))
            if not caso_id:
                continue
            if caso_id in archivados:
                continue
            if caso_id not in casos:
                perdidos.append(clave)
                continue
            variable = motor.pedir("GET", f"/API/bpm/caseVariable/{caso_id}/emergenciaId")
            if str(variable.get("value")) != str(registro.get("emergenciaId")):
                perdidos.append(clave)
        if perdidos:
            print("Bonita perdio o reemplazo casos de la carga anterior. Creando una nueva tanda...", flush=True)
            estado.setdefault("cargasAnteriores", []).append({
                "prefijo": estado.get("prefijo", PREFIJO), "escenarios": escenarios})
            estado["prefijo"] = PREFIJO + " Carga " + datetime.now(ZONA).strftime("%Y%m%d-%H%M%S")
            estado["escenarios"] = {}
            escenarios = estado["escenarios"]
            guardar(estado)
    prefijo = estado.get("prefijo", PREFIJO)

    for indice, (operador, coordinador, gravedad, decision, aportes) in enumerate(ESCENARIOS, 1):
        clave = f"{indice:02d}"
        descripcion = f"{prefijo} Escenario {clave} - {gravedad}"
        registro = escenarios.setdefault(clave, {})
        api = sesion(operador)
        existentes = api.pedir("GET", "/api/emergencias/mias")
        emergencia = next((e for e in existentes if e["descripcion"] == descripcion), None)
        if emergencia is None:
            if registro.get("emergenciaId"):
                raise ErrorCarga(f"La emergencia del escenario {clave} fue eliminada. Revisa el avance guardado.")
            for intento in range(100):
                try:
                    emergencia = api.pedir("POST", "/api/emergencias", {
                        "nivelGravedad": gravedad, "zonaAfectada": f"Zona demo {clave}", "descripcion": descripcion})
                    break
                except ErrorCarga as error:
                    # El backend cancela el caso recien creado si su ID ya pertenece a otra emergencia.
                    # Tras reiniciar el motor puede reutilizar IDs anteriores hasta avanzar la secuencia.
                    if "ya est\u00e1 vinculado a otra emergencia local" not in str(error):
                        raise
                    print("Bonita reutilizo un ID anterior; intentando el siguiente caso...", flush=True)
            else:
                raise ErrorCarga("Bonita sigue reutilizando IDs locales tras 100 intentos; revisa las bases")
            registro.update(emergenciaId=emergencia["id"], caseId=emergencia["bonitaCaseId"])
            guardar(estado)
        emergencia_id = emergencia["id"]
        registro["emergenciaId"] = emergencia_id
        if not registro.get("caseId"):
            # Recupera la identidad si se perdio la respuesta al crear el caso.
            for caso in bonita(operador).listar_bonita("case"):
                variable = bonita(operador).pedir("GET", f"/API/bpm/caseVariable/{caso['id']}/emergenciaId")
                if str(variable.get("value")) == str(emergencia_id):
                    registro["caseId"] = str(caso["id"])
                    break
            if not registro.get("caseId"):
                raise ErrorCarga(f"No se encontro el caso de la emergencia {emergencia_id}")
        if registro.get("completo"):
            print(f"Escenario {clave}: ya cargado, emergencia {emergencia_id}.", flush=True)
            continue
        lote_id = registro.get("loteId") or emergencia.get("loteId")
        if not lote_id:
            cantidades = {}
            for _, recurso, cantidad in aportes:
                cantidades[recurso] = cantidades.get(recurso, 0) + cantidad
            ahora = datetime.now(ZONA).replace(tzinfo=None, microsecond=0)
            lote = sesion(coordinador).pedir("POST", f"/api/emergencias/{emergencia_id}/lotes", {
                "titulo": descripcion,
                "fechaAperturaOfertas": ahora.isoformat(),
                "fechaCierreOfertas": (ahora + timedelta(seconds=45)).isoformat(),
                "items": [{"recursoId": r, "cantidadRequerida": q} for r, q in cantidades.items()]})
            lote_id = lote["id"]
        else:
            lote = sesion(coordinador).pedir("GET", f"/api/lotes/{lote_id}")
        registro["loteId"] = lote_id
        guardar(estado)
        ong_principal = aportes[0][0]
        ofertas = sesion(ong_principal).pedir("GET", f"/api/ofertas/mias?loteId={lote_id}")
        if not ofertas:
            if datetime.fromisoformat(lote["fechaCierreOfertas"]) <= datetime.now(ZONA).replace(tzinfo=None):
                raise ErrorCarga(f"El lote demo {lote_id} cerro sin ofertas. Revisa el caso antes de continuar.")
            items = {item["recursoId"]: item["id"] for item in lote["items"]}
            sesion(ong_principal).pedir("POST", "/api/ofertas", {
                "loteId": lote_id,
                "ongIds": sorted({usuarios[ong]["ongId"] for ong, _, _ in aportes}),
                "detalles": [{"itemLoteId": items[recurso], "ongId": usuarios[ong]["ongId"],
                              "cantidadOfrecida": cantidad} for ong, recurso, cantidad in aportes]})
        print(f"Escenario {clave}: emergencia {emergencia_id}, caso {registro['caseId']}, lote {lote_id}.", flush=True)

    print("Esperando que Bonita cierre las ventanas y valide las ofertas en el SN...", flush=True)
    for indice, (operador, _, _, decision, _) in enumerate(ESCENARIOS, 1):
        registro = escenarios[f"{indice:02d}"]
        if registro.get("completo"):
            continue
        for tarea in motor.listar_bonita("activity", ["caseId=" + str(registro["caseId"])]):
            if tarea.get("state") == "failed":
                motor.pedir("PUT", f"/API/bpm/activity/{tarea['id']}", {"state": "replay"})
                print(f"Reintentando {tarea['name']} del caso {registro['caseId']}.", flush=True)
        ruta = f"/api/emergencias/{registro['emergenciaId']}/recursos-aceptados"
        api = sesion(operador)
        recursos = esperar(f"validacion de emergencia {registro['emergenciaId']}",
                           lambda: api.pedir("GET", ruta),
                           lambda r: r["puedeDecidir"] or r["confirmada"])
        if decision is not None and not recursos["confirmada"]:
            if decision and not recursos["recursos"]:
                raise ErrorCarga(f"El SN no habilito recursos para la emergencia {registro['emergenciaId']}")
            # Esperas breves distintas producen duraciones reales diferentes para el ranking.
            time.sleep(indice % 3 + 1)
            api.pedir("POST", f"/api/emergencias/{registro['emergenciaId']}/decision-municipal", {
                "loteId": registro["loteId"], "aceptar": decision})
            esperar("confirmacion de decision", lambda: api.pedir("GET", ruta), lambda r: r["confirmada"])
        registro["completo"] = True
        guardar(estado)
        resultado = "pendiente de decision" if decision is None else "aceptado" if decision else "rechazado"
        print(f"Emergencia {registro['emergenciaId']}: {resultado}.", flush=True)

    print("Carga terminada. Abri http://localhost y entra como auditor.nacional.", flush=True)
    print("Hay datos de hoy para cinco municipios, cuatro gravedades, cinco ONG y varios recursos.", flush=True)
    print("No se generan casos finalizados: el cierre del proceso todavia esta pendiente de implementacion.", flush=True)


if __name__ == "__main__":
    bloqueo = ESTADO.with_suffix(".lock")
    try:
        descriptor = os.open(bloqueo, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    except FileExistsError:
        print("Ya hay una carga en curso. Si fue interrumpida, elimina solo el archivo .lock y retoma.", file=sys.stderr)
        sys.exit(1)
    try:
        os.close(descriptor)
        main()
    except (ErrorCarga, OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"Carga detenida: {error}", file=sys.stderr)
        print("El avance quedo guardado. Corregi la causa y ejecuta el mismo comando para retomar.", file=sys.stderr)
        sys.exit(1)
    finally:
        bloqueo.unlink(missing_ok=True)
