# CI/CD Docker Compose

This folder contains a Docker Compose setup for local development of Zookeeper, Kafka (2 brokers), and the `clnui-tools` helper container.

Important notes
- This compose file is intended for local development only (no TLS/SASL). Do not use these settings in production.
- The compose file expects a `.env` file in the same directory to provide image names and versions.
- Kafka brokers advertise two external addresses so clients on the host/LAN can reach them by hostname or IP. Containers in the same Docker network use the internal advertised names (kafka1:9092, kafka2:9092).

Fallback strategy (automatic and manual)
- We provide a single helper script `start.sh` which regenerates `.env` with your current LAN IP and local hostname, and manages the docker-compose stack (up/down/restart/logs/ps).
- The `start.sh` script writes two values into `.env`:
  - `KAFKA_ADVERTISED_IP` — the detected primary LAN IP used by EXTERNAL_IP listeners.
  - `KAFKA_ADVERTISED_IP_FALLBACKS` — a comma-separated list of fallback addresses (previous IPs, host.docker.internal, 127.0.0.1, plus any provided values). This is for documentation and client use; the compose file will read `KAFKA_ADVERTISED_IP` to set advertised listeners.
- The compose file publishes two external ports per broker so either the hostname-based address or the IP-based address will be routable from other machines.

Client bootstrap recommendations (fallback order)
1. HOSTNAME-based addresses (e.g. `Rahuls-MacBook-Pro.local:29092, Rahuls-MacBook-Pro.local:39092`) — convenient if mDNS works on your LAN.
2. IP-based addresses from `.env` (e.g. `192.168.0.36:29093, 192.168.0.36:39093`) — reliable if the IP is stable.
3. `host.docker.internal` (for containers that need to reach the host directly) or `localhost` (if you're on the host machine) — use when appropriate.

Recommended `.env` (example written by `start.sh`)

```
# Primary detected LAN IP (used for EXTERNAL_IP advertised listener)
KAFKA_ADVERTISED_IP=192.168.0.36
# Comma-separated fallbacks that clients may try if primary changes
KAFKA_ADVERTISED_IP_FALLBACKS=192.168.0.35,host.docker.internal,127.0.0.1
KAFKA_ADVERTISED_HOST=Rahuls-MacBook-Pro.local

# Zookeeper & Kafka images (pin to specific versions)
ZOOKEEPER_IMAGE=confluentinc/cp-zookeeper:7.9.4
KAFKA_IMAGE=confluentinc/cp-kafka:7.9.4
CLNUI_TOOLS_IMAGE=clnui-tools:latest

# Build args for clnui-tools
KAFKA_VERSION=3.6.1
SCALA_VERSION=2.13
```

Quick run (single-tool workflow)

```bash
# from ci-cd/docker
# regenerate .env and start the stack (recommended)
./start.sh up

# start without updating .env (useful if you want to keep the current advertised IP)
./start.sh up --no-env

# stop the stack
./start.sh down

# restart (updates .env first)
./start.sh restart

# follow logs
./start.sh logs

# just regenerate .env (optionally provide a comma-separated fallback list)
./start.sh update-env 192.168.0.35,backup-host.local
```

Notes and configuration details
- We publish two external ports per-broker so clients can connect either by hostname (EXTERNAL_HOST) or by IP (EXTERNAL_IP). If you prefer fewer ports, you can remove the second published port and only advertise the hostname or IP.
- Ensure your macOS firewall permits incoming connections to the published ports if you want other machines to connect.
- If your LAN uses mDNS (Bonjour), hostname.local should resolve automatically; otherwise prefer the IP-based addresses.

Troubleshooting
- If `start.sh` cannot detect an IP, set `KAFKA_ADVERTISED_IP` manually in `.env` or pass a fallback when running `./start.sh update-env`.
- If a client cannot connect, verify the port is published on the host and reachable from the client machine (use `nc -vz HOST PORT`).

Security & production notes
- For production, configure TLS and authentication for Kafka, increase replication factors, use at least 3 Kafka brokers, and manage persistence and backups for Zookeeper and Kafka data.

Start script behavior and fallbacks
- `start.sh` prefers the currently detected LAN IP (via macOS-specific methods then generic parsing). If detection fails, it keeps the previous `KAFKA_ADVERTISED_IP` from `.env` (if present). It also writes a `KAFKA_ADVERTISED_IP_FALLBACKS` CSV that contains prior IPs and common fallbacks. Use those fallbacks in your clients if your host IP changes frequently.
