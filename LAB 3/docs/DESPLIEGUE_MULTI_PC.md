# Despliegue multi-PC

## PC1 - Windows

PC1 contiene RabbitMQ y la API de negocio. Los clientes se conectan solo a PC1.

```powershell
cd D:\proyecto_prestamos_distribuido_v8_pc1_orquestador\deploy\pc1_rabbitmq_business
copy .env.example .env
docker compose -f docker-compose.pc1.yml up -d --build
```

## PC2 - MySQL

```bash
cd ~/proyecto_prestamos_distribuido_v8_pc1_orquestador/deploy/pc2_mysql_node
cp .env.example .env
nano .env
# RABBIT_HOST=192.168.0.137
docker compose -f docker-compose.pc2.yml up -d --build
```

## PC3 - PostgreSQL

```bash
cd ~/proyecto_prestamos_distribuido_v8_pc1_orquestador/deploy/pc3_postgres_node
cp .env.example .env
nano .env
# RABBIT_HOST=192.168.0.137
docker compose -f docker-compose.pc3.yml up -d --build
```

## PC4 - MariaDB

```bash
cd ~/proyecto_prestamos_distribuido_v8_pc1_orquestador/deploy/pc4_mariadb_worker
cp .env.example .env
nano .env
# RABBIT_HOST=192.168.0.137
docker compose -f docker-compose.pc4.yml up -d --build
```

## Clientes

Usar IP de PC1:

```text
192.168.0.137:8080
```
