# Sistema distribuido de préstamos - versión estable v6

Esta versión está preparada para despliegue por separado en varias PCs.

## Distribución esperada

| PC | Sistema | IP | Servicio |
|---|---|---:|---|
| PC1 | Windows + Docker Desktop | `192.168.0.137` | RabbitMQ |
| PC2 | Ubuntu Server sin GUI | `192.168.0.31` | MySQL + Nodo Python |
| PC3 | Ubuntu Server sin GUI | definir | PostgreSQL + Nodo Node.js |
| PC4 | Ubuntu Server sin GUI | definir | MariaDB + Nodo Borg IA Cubes Java |
| PC5+ | Windows/Linux | varias | Clientes gráficos |

RabbitMQ actúa como middleware central. La lógica de negocio se ejecuta en el nodo Borg IA Cubes Java de PC4.

## Orden de despliegue

1. PC1 RabbitMQ.
2. PC2 MySQL + Python.
3. PC3 PostgreSQL + Node.js.
4. PC4 MariaDB + Borg IA Cubes Java.
5. Clientes gráficos.

## Comandos rápidos

### PC1 RabbitMQ

```powershell
cd D:\proyecto_prestamos_distribuido\deploy\pc1_rabbitmq
copy .env.example .env
docker compose -f docker-compose.pc1.yml up -d --build
```

Abrir:

```text
http://10.93.3.181:15672
usuario: admin
clave: adminpass
```

Ejecutar firewall en PowerShell como administrador:

```powershell
.\scripts\windows_firewall_rabbitmq.ps1
```

### PC2 MySQL + Python

```bash
cd ~/proyecto_prestamos_distribuido/deploy/pc2_mysql_node
cp .env.example .env
nano .env     # RABBIT_HOST=192.168.0.137
docker compose -f docker-compose.pc2.yml up -d --build
curl http://localhost:8001/health
```

### PC3 PostgreSQL + Node.js

```bash
cd ~/proyecto_prestamos_distribuido/deploy/pc3_postgres_node
cp .env.example .env
nano .env     # RABBIT_HOST=192.168.0.137
docker compose -f docker-compose.pc3.yml up -d --build
curl http://localhost:8002/health
```

### PC4 MariaDB + Borg IA Cubes Java

```bash
cd ~/proyecto_prestamos_distribuido/deploy/pc4_mariadb_borg
cp .env.example .env
nano .env     # RABBIT_HOST=192.168.0.137
docker compose -f docker-compose.pc4.yml up -d --build
curl http://localhost:8003/health
```

## Clientes

Python Tkinter:

```powershell
cd clients\python_tkinter
python client_gui_python.py
```

HTML/JS: abrir `clients/html_js/index.html`.

Java Swing:

```powershell
cd clients\java_swing
javac PrestamosSwingClient.java
java PrestamosSwingClient
```

C# WinForms:

```powershell
cd clients\csharp_winforms
dotnet run
```

## Apagar sin perder datos

En cada PC:

```bash
docker compose -f ARCHIVO_COMPOSE.yml stop
```

No usar:

```bash
docker compose down -v
docker volume prune
docker system prune --volumes
```

## Datos semilla incluidos

Esta versión trae datos de prueba en las tres bases de datos:

- MySQL: mensajes SMS/WhatsApp y `outbox_texto`.
- PostgreSQL: correos y `outbox_correo`.
- MariaDB: usuarios, cuentas, préstamos y decisiones históricas demo.

Al iniciar los nodos con RabbitMQ disponible, los eventos de outbox se publican automáticamente y Borg IA Cubes genera decisiones reales. Ver detalles en `docs/DATOS_SEMILLA_Y_LOGICA.md`.
