CREATE TABLE IF NOT EXISTS mensajes_correo (
    id_correo BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(80) UNIQUE,
    id_usuario INT NOT NULL,
    asunto TEXT NOT NULL,
    cuerpo TEXT NOT NULL,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo_correo VARCHAR(40) DEFAULT 'solicitud',
    prioridad VARCHAR(20) DEFAULT 'media',
    estado_proceso VARCHAR(40) DEFAULT 'RECIBIDO',
    decision VARCHAR(60),
    riesgo_calculado INT,
    respuesta_ia TEXT
);

CREATE INDEX IF NOT EXISTS idx_correo_request_id ON mensajes_correo(request_id);
CREATE INDEX IF NOT EXISTS idx_correo_usuario_fecha ON mensajes_correo(id_usuario, fecha_envio);

INSERT INTO mensajes_correo
(request_id, id_usuario, asunto, cuerpo, tipo_correo, prioridad, estado_proceso, decision, riesgo_calculado, respuesta_ia)
VALUES
('seed-postgres-001', 4, 'Solicitud de prestamo personal', 'Solicito un prestamo de 4000 soles para capital de trabajo.', 'solicitud', 'media', 'HISTORICO', 'PRESTAMO_ACEPTADO', 28, 'Registro historico de prueba.'),
('seed-postgres-002', 5, 'Refinanciamiento de deuda', 'Tengo atraso y deseo refinanciar mi deuda actual.', 'refinanciamiento', 'alta', 'HISTORICO', 'REFINANCIAMIENTO_PROPUESTO', 63, 'Registro historico de prueba.'),
('seed-postgres-003', 6, 'Consulta de prestamo activo', 'Quiero consultar el estado de mi prestamo activo.', 'consulta', 'baja', 'HISTORICO', 'CONSULTA_REGISTRADA', 15, 'Registro historico de prueba.');
