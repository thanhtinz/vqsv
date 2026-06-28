# VQSV — VPS Deployment Guide

Deploy the full game backend (server + PostgreSQL + Redis + HTTPS reverse proxy)
on a fresh Ubuntu 22.04/24.04 VPS. ~10 minutes.

## 0. Requirements

- A VPS with a public IP (1 vCPU / 2 GB RAM is enough to start; 2 vCPU / 4 GB
  recommended once you have players).
- A domain name with an **A record** pointing at the VPS IP
  (e.g. `play.yourgame.com` → `203.0.113.10`).

## 1. Install Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker   # run docker without sudo
```

## 2. Get the code

```bash
sudo mkdir -p /opt/vqsv && sudo chown $USER /opt/vqsv
git clone https://github.com/thanhtinz/vqsv.git /opt/vqsv
cd /opt/vqsv
```

## 3. Configure secrets

```bash
cp .env.example .env
./scripts/gen-secrets.sh          # fills DB/Redis/JWT secrets automatically
nano .env                         # set PUBLIC_DOMAIN and ACME_EMAIL
```

## 4. Open the firewall

```bash
sudo ufw allow 22/tcp     # SSH
sudo ufw allow 80/tcp     # HTTP (Let's Encrypt challenge + redirect)
sudo ufw allow 443/tcp    # HTTPS (REST API + WebSocket)
sudo ufw allow 9090/tcp   # J2ME binary TCP gateway
sudo ufw enable
```

## 5. Launch

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

First boot builds the server image, runs Flyway migrations, and Caddy fetches a
TLS certificate for your domain. Watch progress:

```bash
docker compose -f docker-compose.prod.yml logs -f server caddy
```

## 6. Verify

```bash
curl https://$PUBLIC_DOMAIN/actuator/health        # {"status":"UP"}

# Register a test account:
curl -X POST https://$PUBLIC_DOMAIN/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"test","password":"test1234","playerName":"Tester"}'
```

The clients connect to:
| Client | Endpoint |
|--------|----------|
| Android / iOS / Web | `https://PUBLIC_DOMAIN` (REST) + `wss://PUBLIC_DOMAIN/ws` (STOMP) |
| J2ME (legacy) | `PUBLIC_DOMAIN:9090` (raw TCP) |

## 7. Operations

```bash
# Update to latest code:
git pull && docker compose -f docker-compose.prod.yml up -d --build

# Tail logs:
docker compose -f docker-compose.prod.yml logs -f server

# Stop / start:
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d

# Backups (add to cron — see deploy/backup-db.sh):
./deploy/backup-db.sh
```

## What this gives you vs. what's still business-side

**Handled by this deploy:** HTTPS, DB persistence + healthchecks, Redis with
auth + AOF persistence, automatic restarts, graceful shutdown, schema
migrations, log files, resource limits, DB backups.

**Still your responsibility before charging money:**
- A **payment gateway** (the in-game shop currently uses earned gold/medals;
  to sell currency you need MoMo/ZaloPay/VNPay/Stripe integration + a
  top-up endpoint and ledger).
- Game content scaling (more maps/pets/items beyond the seed data).
- An **admin/moderation** panel and anti-cheat for the authoritative server.
- Legal: Terms of Service, privacy policy, and (for Vietnam) a **G1 game
  licence** to operate a commercial online game.
- Monitoring/alerting (e.g. Uptime Kuma against `/actuator/health`) and
  off-site backup storage.
