# Ejecutar desde la raíz DSSD: python Modelado/build_integracion_bonita.py
from pathlib import Path
import os, shutil, subprocess
from zipfile import ZipFile, ZIP_DEFLATED
classpath = [Path.home()/'.m2/repository/org/bonitasoft/engine/bonita-common/10.4.0/bonita-common-10.4.0.jar',
 Path.home()/'.m2/repository/org/codehaus/groovy/groovy/3.0.25/groovy-3.0.25.jar',
 Path.home()/'.m2/repository/org/codehaus/groovy/groovy-json/3.0.25/groovy-json-3.0.25.jar']
javac=shutil.which('javac')
if not javac: raise SystemExit('Se necesita un JDK 21 o superior en PATH')
classes=Path('Proyecto/backend/target/connector-classes')
classes.mkdir(parents=True,exist_ok=True)
subprocess.run([javac,'--release','21','-encoding','UTF-8','-cp',os.pathsep.join(map(str,classpath)),
 '-d',str(classes),'Modelado/IntegracionNacionalConnector.java'],check=True)
for ext in ['def','impl']: shutil.copyfile('Modelado/rescuesync-nacional.'+ext,classes/('rescuesync-nacional.'+ext))
with ZipFile('Proyecto/backend/target/rescuesync-nacional-1.0.1.jar','w',ZIP_DEFLATED) as z:
 for f in classes.rglob('*'):
  if f.is_file(): z.write(f,f.relative_to(classes).as_posix())
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from xml.dom import minidom as D
from uuid import uuid4
with ZipFile('Modelado/rescueSync-V1.10.bos') as z: entries={n:z.read(n) for n in z.namelist()}
proc=next(n for n in entries if n.endswith('.proc')); d=D.parseString(entries[proc]); ns='http://www.omg.org/XMI'
def children(e,tag): return [c for c in e.childNodes if getattr(c,'tagName',None)==tag]
def named(name): return next(e for e in d.getElementsByTagName('*') if e.getAttribute('name')==name)
def new(tag,typ=None,**attrs):
 e=d.createElement(tag); e.setAttributeNS(ns,'xmi:id','_'+uuid4().hex)
 if typ: e.setAttributeNS(ns,'xmi:type',typ)
 for k,v in attrs.items(): e.setAttribute(k,str(v))
 return e
pool=named('Sistema P1');pool.setAttribute('version','2.0');d.getElementsByTagName('process:MainProcess')[0].setAttribute('version','2.0')
old=named('Reservar recursos y registrar validacion local');old.parentNode.removeChild(old)
def connector(task,name,operation,event):
 c=new('connectors','process:Connector',name=name,definitionId='rescuesync-nacional',definitionVersion='1.0.0',event=event)
 config=new('configuration','connectorconfiguration:ConnectorConfiguration',definitionId='rescuesync-nacional',version='1.0.0',modelVersion='9')
 for key,value in [('operacion',operation),('emergenciaId','emergenciaId'),('loteId','loteId'),('aceptar','ofertasAceptadas')]:
  param=new('parameters','connectorconfiguration:ConnectorParameter',key=key)
  e=new('expression','expression:Expression',name=value,content=value,type='TYPE_CONSTANT' if key=='operacion' else 'TYPE_VARIABLE',returnType='java.lang.String' if key=='operacion' else 'java.lang.Boolean' if key=='aceptar' else 'java.lang.Long')
  if key!='operacion':
   var=next(v for v in children(pool,'data') if v.getAttribute('name')==value)
   e.appendChild(new('referencedElements','process:Data',name=value,dataType=var.getAttribute('dataType')))
  param.appendChild(e);config.appendChild(param)
 c.appendChild(config);named(task).appendChild(c)
connector('Validar en el Sistema Nacional (POST Cloud)','Reservar temporalmente en SN y registrar resultado local','VALIDAR','ON_ENTER')
connector('Registrar compromiso y bloqueo','Comprometer en SN y confirmar adjudicacion local','ACEPTAR','ON_ENTER')
# La salida de la revisiÃ³n no devuelve el proceso al municipio hasta liberar y confirmar localmente.
connector('Ver ofertas validadas','Liberar en SN y confirmar rechazo local','RECHAZAR','ON_FINISH')
# Las operaciones del contrato se aplican al finalizar la tarea antes de los conectores de salida.
# Usar el contrato directamente para evitar depender de ese orden del motor.
c=named('Liberar en SN y confirmar rechazo local')
p=next(p for p in c.getElementsByTagName('parameters') if p.getAttribute('key')=='aceptar')
e=children(p,'expression')[0];e.setAttribute('name','aceptaOfertas');e.setAttribute('content','aceptaOfertas');e.setAttribute('type','TYPE_CONTRACT_INPUT')
for child in list(e.childNodes): e.removeChild(child)
e.appendChild(new('referencedElements','process:ContractInput',name='aceptaOfertas',type='BOOLEAN'))
named('Registrar compromiso y bloqueo').setAttribute('documentation','Bonita verifica y confirma reservas nacionales; luego notifica el resultado al backend local. Reintentar este conector ante fallos.')
for name in ['Registrar emergencia','Obtener ofertas ','Enviar ofertas']:
 pass
entries.pop(proc);entries[proc.rsplit('/',1)[0]+'/Proceso 1-2.0.proc']=d.toxml(encoding='UTF-8')
root=proc.split('/')[0]
entries[root+'/app/src/main/java/com/rescuesync/bonita/IntegracionNacionalConnector.java']=Path('Modelado/IntegracionNacionalConnector.java').read_bytes()
jar=Path('Proyecto/backend/target/rescuesync-nacional-1.0.1.jar').read_bytes()
repo=root+'/app/connector-repository/com/rescuesync/rescuesync-nacional/1.0.1/'
entries[repo+'rescuesync-nacional-1.0.1.jar']=jar
entries[repo+'rescuesync-nacional-1.0.1.pom']=b'<project><modelVersion>4.0.0</modelVersion><groupId>com.rescuesync</groupId><artifactId>rescuesync-nacional</artifactId><version>1.0.1</version></project>' 
pomkey=root+'/app/pom.xml'
pom=entries[pomkey].decode()
pom=pom.replace('</dependencies>', '<dependency><groupId>com.rescuesync</groupId><artifactId>rescuesync-nacional</artifactId><version>1.0.1</version><scope>compile</scope></dependency></dependencies>')
pom=pom.replace('</project>', '<repositories><repository><id>rescuesync-embedded</id><url>file://${project.basedir}/connector-repository</url><releases><checksumPolicy>ignore</checksumPolicy></releases></repository></repositories></project>')
entries[pomkey]=pom.encode()
for ext in ['def','impl']:
 entries[root+'/app/src/main/resources/rescuesync-nacional.'+ext]=Path('Modelado/rescuesync-nacional.'+ext).read_bytes()
with ZipFile('Modelado/rescueSync-V2.0.bos','w',ZIP_DEFLATED) as z:
 for name,content in entries.items(): z.writestr(name,content)
Path('Modelado/Proceso 1-2.0.proc').write_bytes(d.toxml(encoding='UTF-8'))
for name,content in entries.items():
 dest=Path('Proyecto/backend/target/bonita-orquestacion')/Path(*Path(name).parts[1:]);dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(content)
print('BOS 2.0 generado con conector nacional propio ejecutado en el motor.')
