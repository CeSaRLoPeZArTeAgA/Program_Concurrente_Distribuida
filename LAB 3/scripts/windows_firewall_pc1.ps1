# Ejecutar en PowerShell como Administrador.
New-NetFirewallRule -DisplayName "Prestamos PC1 API 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Any -ErrorAction SilentlyContinue
New-NetFirewallRule -DisplayName "RabbitMQ Management 15672" -Direction Inbound -Protocol TCP -LocalPort 15672 -Action Allow -Profile Any -ErrorAction SilentlyContinue
New-NetFirewallRule -DisplayName "RabbitMQ AMQP 5672" -Direction Inbound -Protocol TCP -LocalPort 5672 -Action Allow -Profile Any -ErrorAction SilentlyContinue
Write-Host "Reglas creadas: PC1 API 8080, RabbitMQ 15672 y 5672"
