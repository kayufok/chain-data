#!/usr/bin/env bash
set -euo pipefail

# Nginx reverse proxy for chain-data.xrftech.net -> spring-boot-app:8080
# Usage: ./nginx-proxy.sh start|stop|reload|status

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOMAIN="chain-data.xrftech.net"
NETWORK_NAME="nginx-net"
CONTAINER_NAME="nginx-reverse-proxy"
NGINX_CONF_DIR="$SCRIPT_DIR/nginx/conf.d"
NGINX_CERT_DIR="$SCRIPT_DIR/nginx/certs"

ensure_network() {
  if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    echo "INFO: Creating docker network '$NETWORK_NAME'"
    docker network create "$NETWORK_NAME" >/dev/null
  else
    echo "INFO: Network '$NETWORK_NAME' already exists"
  fi
}

ensure_dirs() {
  mkdir -p "$NGINX_CONF_DIR" "$NGINX_CERT_DIR"
}

write_nginx_conf() {
  local conf="$NGINX_CONF_DIR/chain-data.conf"
  cat > "$conf" <<'NGINX'
upstream chain_data_backend {
    server spring-boot-app:8080;
    keepalive 64;
}

map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

server {
    listen 80;
    server_name chain-data.xrftech.net;

    # Optionally redirect to https
    # return 301 https://$host$request_uri;

    location / {
        proxy_pass http://chain_data_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_read_timeout 300;
    }
}

server {
    listen 443 ssl;
    server_name chain-data.xrftech.net;

    ssl_certificate     /etc/nginx/certs/chain-data.crt;
    ssl_certificate_key /etc/nginx/certs/chain-data.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://chain_data_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_read_timeout 300;
    }
}
NGINX
}

ensure_certs() {
  local crt="$NGINX_CERT_DIR/chain-data.crt"
  local key="$NGINX_CERT_DIR/chain-data.key"
  if [[ -f "$crt" && -f "$key" ]]; then
    echo "INFO: TLS cert already exists at $crt"
    return
  fi
  echo "INFO: Generating self-signed TLS certificate for $DOMAIN"
  openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
    -keyout "$key" -out "$crt" -subj "/CN=$DOMAIN" >/dev/null 2>&1 || {
      echo "ERROR: openssl is required to generate a self-signed certificate" >&2
      exit 1
    }
}

start_proxy() {
  ensure_network
  ensure_dirs
  write_nginx_conf
  ensure_certs

  if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "INFO: Removing existing container $CONTAINER_NAME"
    docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  fi

  echo "INFO: Starting nginx reverse proxy container $CONTAINER_NAME"
  docker run -d --name "$CONTAINER_NAME" --restart unless-stopped \
    --network "$NETWORK_NAME" \
    -p 80:80 -p 443:443 \
    -v "$NGINX_CONF_DIR":/etc/nginx/conf.d:ro \
    -v "$NGINX_CERT_DIR":/etc/nginx/certs:ro \
    nginx:stable-alpine >/dev/null

  echo "SUCCESS: nginx reverse proxy is running"
}

stop_proxy() {
  if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "INFO: Stopping container $CONTAINER_NAME"
    docker rm -f "$CONTAINER_NAME" >/dev/null || true
    echo "SUCCESS: nginx reverse proxy stopped"
  else
    echo "INFO: Container $CONTAINER_NAME not found"
  fi
}

reload_proxy() {
  if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "INFO: Reloading nginx configuration"
    docker exec "$CONTAINER_NAME" nginx -s reload >/dev/null
    echo "SUCCESS: nginx configuration reloaded"
  else
    echo "ERROR: $CONTAINER_NAME is not running" >&2
    exit 1
  fi
}

status_proxy() {
  if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "RUNNING: $CONTAINER_NAME"
    docker ps --filter name=$CONTAINER_NAME
  else
    echo "STOPPED: $CONTAINER_NAME"
  fi
}

case "${1:-}" in
  start) start_proxy ;;
  stop) stop_proxy ;;
  reload) reload_proxy ;;
  status) status_proxy ;;
  *)
    echo "Usage: $0 {start|stop|reload|status}" >&2
    exit 1
    ;;
esac


