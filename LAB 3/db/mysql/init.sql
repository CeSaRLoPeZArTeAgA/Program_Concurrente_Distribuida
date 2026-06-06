CREATE DATABASE IF NOT EXISTS prestamos_texto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE prestamos_texto;

CREATE TABLE IF NOT EXISTS mensajes_texto (
    id_mensaje BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(80) UNIQUE,
    id_usuario INT NOT NULL,
    contenido TEXT NOT NULL,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo_mensaje VARCHAR(40) DEFAULT 'solicitud',
    canal VARCHAR(40) DEFAULT 'WhatsApp',
    estado_proceso VARCHAR(40) DEFAULT 'RECIBIDO',
    decision VARCHAR(60),
    riesgo_calculado INT,
    respuesta_ia TEXT,
    INDEX idx_request_id (request_id),
    INDEX idx_usuario_fecha (id_usuario, fecha_envio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO mensajes_texto
(request_id, id_usuario, contenido, tipo_mensaje, canal, estado_proceso, decision, riesgo_calculado, respuesta_ia)
VALUES
('seed-mysql-001', 1, 'Solicito un prestamo de 2500 soles, tengo sueldo estable y trabajo formal.', 'solicitud', 'WhatsApp', 'HISTORICO', 'PRESTAMO_ACEPTADO', 18, 'Registro historico de prueba.'),
('seed-mysql-002', 2, 'Quiero refinanciar mi prestamo porque tengo atraso de dos cuotas.', 'refinanciamiento', 'SMS', 'HISTORICO', 'REFINANCIAMIENTO_PROPUESTO', 58, 'Registro historico de prueba.'),
('seed-mysql-003', 3, 'Necesito prestamo urgente de 9000 soles, tengo deuda pendiente y mora.', 'solicitud', 'WhatsApp', 'HISTORICO', 'PRESTAMO_RECHAZADO', 84, 'Registro historico de prueba.');
