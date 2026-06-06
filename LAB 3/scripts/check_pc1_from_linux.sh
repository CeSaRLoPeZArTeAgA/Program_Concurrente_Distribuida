#!/usr/bin/env bash
set -e
PC1=${1:-192.168.0.137}
echo "Probando PC1 API: http://$PC1:8080/api/health"
curl -v --connect-timeout 5 "http://$PC1:8080/api/health" || true
echo
echo "Probando RabbitMQ: http://$PC1:15672/api/overview"
curl -v --connect-timeout 5 -u admin:adminpass "http://$PC1:15672/api/overview" || true
