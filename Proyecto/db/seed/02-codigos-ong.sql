-- Actualiza bases anteriores al identificador comun de ONG sin reiniciar sus datos.
-- Solo completa codigos faltantes de las organizaciones conocidas por ambos seeds.
BEGIN;

ALTER TABLE ong ADD COLUMN IF NOT EXISTS codigo varchar(36);

UPDATE ong AS o
SET codigo = compartido.codigo
FROM (VALUES
    ('/ONG/CruzRojaLaPlata', '57f6bace-9f0e-4c1b-9ac8-0e3b09069e29'),
    ('/ONG/CaritasBuenosAires', '6a85312b-c5b0-415b-be1a-2244b612c77b'),
    ('/ONG/BomberosLaPlata', 'cd35316c-5093-4826-89dd-4a525bb2c127'),
    ('/ONG/BancoAlimentosBA', '6017970a-9d32-4129-8a99-644127a74868'),
    ('/ONG/TechoBuenosAires', '16c85edc-ccb4-40f1-b648-bb28a9111fdc')
) AS compartido(bonita_group_path, codigo)
WHERE o.bonita_group_path = compartido.bonita_group_path
  AND (o.codigo IS NULL OR btrim(o.codigo) = '');

CREATE UNIQUE INDEX IF NOT EXISTS ux_ong_codigo ON ong (codigo);

COMMIT;
