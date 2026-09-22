-- Clientes
INSERT INTO clientes (nombre, telefono, email, tipo_documento, numero_documento) VALUES 
('Juan Perez', '3001234567', 'juan@example.com', 'CC', '10001'),
('Maria Gomez', '3007654321', 'maria@example.com', 'CC', '10002'),
('Empresa XYZ', '3009998888', 'contacto@xyz.com', 'NIT', '900123456'),
('Carlos Ruiz', '3001112233', 'carlos@example.com', 'CC', '10003'),
('Ana Torres', '3004445566', 'ana@example.com', 'CC', '10004');

-- Direcciones
INSERT INTO direcciones (calle, numero, ciudad, departamento_provincia, codigo_postal, pais) VALUES
('Calle 10', '10-20', 'Bogota', 'Cundinamarca', '110111', 'Colombia'),
('Carrera 50', '20-30', 'Medellin', 'Antioquia', '050001', 'Colombia'),
('Avenida 1', '5-10', 'Cali', 'Valle', '760001', 'Colombia');

-- Personal
INSERT INTO personal_logistico (nombre, email_corporativo, rol) VALUES
('Operador 1', 'op1@fedex.com', 'OPERADOR'),
('Supervisor 1', 'sup1@fedex.com', 'SUPERVISOR');

-- Envios (ejemplo estático, tracking manual)
INSERT INTO envios (tracking_number, prioridad, peso, dimensiones, costo, fecha_registro, fecha_estimada_entrega, estado_operativo, id_remitente, id_destinatario, id_direccion_origen, id_direccion_destino, id_empleado_creador) VALUES
('LOG-12345678-0', 'ESTANDAR', 5.0, '20x20x20', 20000.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PENDIENTE_RECOGIDA', 1, 2, 1, 2, 1),
('LOG-87654321-1', 'EXPRES', 2.0, '10x10x10', 29000.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'EN_TRANSITO', 3, 4, 2, 3, 1);

-- Historial inicial compatible con los envíos de ejemplo
INSERT INTO movimientos_envio (envio_id, estado, descripcion, ubicacion, fecha_hora) VALUES
(1, 'PENDIENTE_RECOGIDA', 'Envío creado', 'Bogota', CURRENT_TIMESTAMP),
(2, 'EN_TRANSITO', 'Envío en tránsito', 'Medellin', CURRENT_TIMESTAMP);
