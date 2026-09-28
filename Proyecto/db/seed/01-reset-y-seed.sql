-- RescueSync: reset total de la base + datos de ejemplo compartidos por el equipo.
-- DESTRUCTIVO: borra municipios, recursos, ONGs, emergencias y todo lo que depende de ellos
-- (lotes, ítems, ofertas, detalles, actividades, inventario).
-- Uso (desde la raíz del repo, con el contenedor postgres_db levantado):
--   docker exec -i postgres_db psql -U postgres -d rescuesync < db/seed/01-reset-y-seed.sql
-- Correrlo ANTES de levantar el backend con la entidad Ong que tiene bonita_group_path.

BEGIN;

-- 0) Reset total (RESTART IDENTITY reinicia los ids; CASCADE vacía las tablas dependientes)
TRUNCATE TABLE municipio, recurso, ong, emergencia RESTART IDENTITY CASCADE;

-- 1) Vínculo con Bonita (la tabla está vacía, agregar NOT NULL es seguro)
ALTER TABLE ong ADD COLUMN IF NOT EXISTS bonita_group_path varchar(200);

INSERT INTO municipio (nombre) VALUES ('La Plata');

INSERT INTO recurso (nombre, unidad_medida) VALUES
  ('Raciones de alimento', 'raciones'),   -- id 1
  ('Paramédicos', 'personas'),            -- id 2
  ('Frazadas', 'unidades'),               -- id 3
  ('Bombas de agua', 'unidades');         -- id 4

-- 2) ONGs. Clave de negocio = path del subgrupo en la organización RescueSync de Bonita.
INSERT INTO ong (razon_social, bonita_group_path) VALUES
  ('Cruz Roja Argentina – Filial La Plata',     '/ONG/CruzRojaLaPlata'),     -- id 1
  ('Cáritas Arquidiócesis de Buenos Aires',     '/ONG/CaritasBuenosAires'),  -- id 2
  ('Bomberos Voluntarios de La Plata',          '/ONG/BomberosLaPlata'),     -- id 3
  ('Fundación Banco de Alimentos Buenos Aires', '/ONG/BancoAlimentosBA'),    -- id 4
  ('Techo Argentina – Regional Buenos Aires',   '/ONG/TechoBuenosAires');    -- id 5

ALTER TABLE ong ALTER COLUMN bonita_group_path SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_ong_bonita_group_path ON ong (bonita_group_path);

-- 3) Inventario (ong_id, recurso_id, cantidad_disponible)
INSERT INTO inventario_ong (ong_id, recurso_id, cantidad_disponible) VALUES
  (1,2,10),(1,3,200),          -- Cruz Roja: paramédicos, frazadas
  (2,1,800),(2,3,150),         -- Cáritas: raciones, frazadas
  (3,2,6),(3,4,4),             -- Bomberos: paramédicos, bombas de agua
  (4,1,1500),                  -- Banco de Alimentos: raciones
  (5,3,300),(5,4,2);           -- Techo: frazadas, bombas de agua

-- 4) Emergencia, lotes e ítems de ejemplo
INSERT INTO emergencia (descripcion, nivel_gravedad, zona_afectada, fecha_registro, municipio_id)
  VALUES ('Desborde del arroyo tras 200mm en 12 horas. Barrios bajos evacuados.',
          'ALTA', 'Zona Norte', NOW(), 1);

INSERT INTO lote (titulo, estado, fecha_creacion, emergencia_id) VALUES
  ('Asistencia alimentaria', 'ACTIVO', NOW(), 1),
  ('Equipo sanitario de emergencia', 'ACTIVO', NOW(), 1);

INSERT INTO item_lote (lote_id, recurso_id, cantidad_requerida) VALUES
  (1,1,1000),(1,3,300),
  (2,2,5),(2,4,3);

COMMIT;
