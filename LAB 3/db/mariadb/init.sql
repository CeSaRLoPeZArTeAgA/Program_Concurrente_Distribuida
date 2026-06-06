CREATE DATABASE IF NOT EXISTS prestamos_finanzas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE prestamos_finanzas;

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    correo VARCHAR(160),
    telefono VARCHAR(40),
    direccion VARCHAR(200),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cuentas (
    id_cuenta BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    saldo DECIMAL(12,2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) DEFAULT 'ACTIVA',
    fecha_apertura DATE,
    tipo_cuenta VARCHAR(50),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prestamos (
    id_prestamo BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    tasa_interes DECIMAL(6,2) DEFAULT 12.50,
    plazo INT DEFAULT 12,
    estado VARCHAR(30) DEFAULT 'ACTIVO',
    fecha_inicio DATE,
    fecha_fin DATE,
    tipo_prestamo VARCHAR(60),
    cuotas_no_pagadas INT DEFAULT 0,
    dias_mora INT DEFAULT 0,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    INDEX idx_usuario_estado (id_usuario, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS decisiones_prestamo (
    id_decision BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(80),
    source VARCHAR(40),
    id_origen BIGINT,
    id_usuario INT NOT NULL,
    intencion VARCHAR(60),
    monto_solicitado DECIMAL(12,2),
    decision VARCHAR(80) NOT NULL,
    riesgo INT NOT NULL,
    motivo TEXT,
    fecha_decision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    INDEX idx_request_id (request_id),
    INDEX idx_usuario_fecha (id_usuario, fecha_decision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO usuarios (id_usuario, nombre, dni, correo, telefono, direccion) VALUES
(1, 'Cesar Lopez', '70000001', 'cesar@example.com', '999111001', 'Lima'),
(2, 'Ana Torres', '70000002', 'ana@example.com', '999111002', 'Arequipa'),
(3, 'Luis Ramos', '70000003', 'luis@example.com', '999111003', 'Cusco'),
(4, 'Maria Quispe', '70000004', 'maria@example.com', '999111004', 'Trujillo'),
(5, 'Jorge Salazar', '70000005', 'jorge@example.com', '999111005', 'Piura'),
(6, 'Lucia Flores', '70000006', 'lucia@example.com', '999111006', 'Tacna')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre);

INSERT INTO cuentas (id_usuario, saldo, estado, fecha_apertura, tipo_cuenta) VALUES
(1, 9500.00, 'ACTIVA', '2023-01-15', 'AHORROS'),
(2, 1800.00, 'ACTIVA', '2022-06-10', 'SUELDO'),
(3, 120.00, 'ACTIVA', '2021-03-20', 'AHORROS'),
(4, 7500.00, 'ACTIVA', '2020-11-05', 'EMPRESARIAL'),
(5, 650.00, 'ACTIVA', '2024-02-01', 'AHORROS'),
(6, 4200.00, 'ACTIVA', '2023-08-12', 'SUELDO');

INSERT INTO prestamos (id_usuario, monto, tasa_interes, plazo, estado, fecha_inicio, fecha_fin, tipo_prestamo, cuotas_no_pagadas, dias_mora) VALUES
(1, 3000.00, 10.50, 12, 'PAGADO', '2023-01-01', '2023-12-31', 'PERSONAL', 0, 0),
(2, 5000.00, 15.00, 18, 'ACTIVO', '2024-01-01', '2025-06-30', 'PERSONAL', 1, 12),
(3, 12000.00, 22.00, 24, 'ACTIVO', '2023-07-01', '2025-06-30', 'CONSUMO', 4, 95),
(4, 8000.00, 13.00, 18, 'ACTIVO', '2024-03-01', '2025-08-30', 'NEGOCIO', 0, 0),
(5, 6500.00, 20.00, 24, 'ACTIVO', '2023-10-01', '2025-09-30', 'CONSUMO', 3, 60),
(6, 2000.00, 11.00, 10, 'ACTIVO', '2024-08-01', '2025-06-01', 'PERSONAL', 0, 0);

INSERT INTO decisiones_prestamo (request_id, source, id_origen, id_usuario, intencion, monto_solicitado, decision, riesgo, motivo) VALUES
('seed-decision-001', 'demo', 1, 1, 'solicitud', 2500.00, 'PRESTAMO_ACEPTADO', 18, 'Usuario con saldo alto y sin mora relevante.'),
('seed-decision-002', 'demo', 2, 3, 'solicitud', 9000.00, 'PRESTAMO_RECHAZADO', 84, 'Usuario con mora alta y varias cuotas vencidas.'),
('seed-decision-003', 'demo', 3, 5, 'refinanciamiento', 6500.00, 'REFINANCIAMIENTO_PROPUESTO', 63, 'Usuario con atraso moderado; se recomienda refinanciamiento.');
