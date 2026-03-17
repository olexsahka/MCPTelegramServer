# MCP Telegram Server

MCP (Model Context Protocol) server for Telegram. Lets AI assistants (Claude, etc.) read and send Telegram messages on your behalf via MCP tools. Runs as a regular Telegram user — not a bot.

## Requirements

- Java 21+
- Docker + Docker Compose
- Telegram account with API credentials from [my.telegram.org](https://my.telegram.org)

## Quick Start

### 1. Get Telegram API credentials

Go to [my.telegram.org](https://my.telegram.org) → API development tools → create an app.
You'll get `api_id` and `api_hash`.

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env`:
```env
TELEGRAM_API_ID=your_api_id
TELEGRAM_API_HASH=your_api_hash
TELEGRAM_PHONE=+79001234567

POSTGRES_PASSWORD=strong_password
REDIS_PASSWORD=strong_password
MCP_PASSWORD=strong_password
MCP_ADMIN_PASSWORD=strong_password
```

### 3. Build and start

```bash
docker-compose up -d --build
```

### 4. Authorize Telegram (first run only)

On first start, TDLib needs a confirmation code sent to your phone.
Pass it via environment variable — no interactive input needed:

```bash
# First run: TDLib sends code to your phone
docker-compose up app
# Wait for "AuthorizationStateWaitCode" in logs, then Ctrl+C

# Re-run with the code (and 2FA password if enabled)
docker-compose run --rm -e TELEGRAM_AUTH_CODE=12345 -e TELEGRAM_AUTH_PASSWORD=yourpassword app
# Wait for "TDLib: authorized successfully", then Ctrl+C

# Start in background — session is saved, no re-auth needed
docker-compose up -d app
```

Session is saved in a Docker volume — you won't need to re-authorize on restart.

### 5. Verify

```bash
curl -u mcp:your_mcp_password -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

---

## Architecture

```
Telegram (MTProto)
    └── TDLib (tdlight-java)
            └── TdLibTelegramClient
                    ├── MCP Tools ──────────────── POST /mcp  ← AI client (Claude, etc.)
                    │       get_dialogs
                    │       search_dialog
                    │       get_unread_messages
                    │       get_last_messages
                    │       send_message
                    │
                    └── TelegramUpdateListener
                            └── Redis Pub/Sub
                                    └── WebSocketBroker
                                            ├── PostgreSQL (history)
                                            └── STOMP /topic/messages → Admin UI
```

---

## MCP Tools API

All requests: `POST /mcp` with HTTP Basic Auth.

### tools/list

```bash
curl -u mcp:password -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

### get_dialogs

Returns list of dialogs with unread counts.

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "get_dialogs",
    "arguments": { "limit": 50 }
  },
  "id": 2
}
```

Response:
```json
[
  { "chat_id": 123456, "type": "PRIVATE", "title": "Alice", "unread_count": 3 },
  { "chat_id": -1001234, "type": "SUPERGROUP", "title": "Dev Team", "unread_count": 0 }
]
```

### search_dialog

Search dialogs by name.

```json
{
  "method": "tools/call",
  "params": { "name": "search_dialog", "arguments": { "query": "Alice" } }
}
```

### get_unread_messages

All unread messages across all dialogs.

```json
{
  "method": "tools/call",
  "params": { "name": "get_unread_messages", "arguments": {} }
}
```

### get_last_messages

Last N messages from a dialog.

```json
{
  "method": "tools/call",
  "params": {
    "name": "get_last_messages",
    "arguments": { "dialog_id": 123456, "count": 10 }
  }
}
```

### send_message

Send a message to a dialog.

```json
{
  "method": "tools/call",
  "params": {
    "name": "send_message",
    "arguments": { "dialog_id": 123456, "text": "Hello!" }
  }
}
```

---

## Admin UI

Available at `http://localhost:8080/admin` (credentials: `admin` / your `MCP_ADMIN_PASSWORD`).

- Live message log via WebSocket
- Browse dialogs and message history

---

## Security

| Layer | Protection |
|-------|-----------|
| Network | PostgreSQL and Redis ports not exposed externally |
| App port | Bound to `127.0.0.1` only — requires Nginx reverse proxy for external access |
| Auth | HTTP Basic Auth (BCrypt) on all endpoints |
| Rate limiting | 30 requests/minute per IP, returns 429 |
| Connections | Tomcat max 20 connections, 10 threads |
| Secrets | All credentials via env variables, never hardcoded |
| Logs | Phone number and message content not logged in prod |
| TDLib session | Stored in Docker volume, encrypted by OS filesystem |

### Recommended VPS setup (Nginx + SSL)

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
}
```

```bash
# Firewall: only SSH, HTTP, HTTPS
ufw allow 22
ufw allow 80
ufw allow 443
ufw enable
```

---

## Connect from Android Emulator (Local Development)

This section describes how to connect an Android app (or any local MCP client) running in an Android emulator to the server running on the same machine.

### How it works

Android emulator maps `10.0.2.2` → `localhost` of the host machine. So if the server runs on `localhost:8080`, the emulator reaches it at `http://10.0.2.2:8080`.

### Step 1 — Start server locally (without Docker)

```bash
# Start only infrastructure
docker-compose up -d postgres redis

# Run app directly (no container — emulator can reach it via 10.0.2.2)
./gradlew bootRun
```

> **Why not `docker-compose up app`?** When the app runs inside Docker, it binds to `127.0.0.1` of the container network, not the host. The emulator cannot reach it. Running via `./gradlew bootRun` binds to the host's `localhost`, which is accessible at `10.0.2.2` from the emulator.

Alternatively, if you want to use Docker, change the app port binding in `docker-compose.yml`:
```yaml
services:
  app:
    ports:
      - "0.0.0.0:8080:8080"   # expose on all interfaces, not just 127.0.0.1
```

### Step 2 — Authorize TDLib (first run only)

```bash
# First run — TDLib sends code to your phone
./gradlew bootRun
# Wait for "AuthorizationStateWaitCode" in logs, then Ctrl+C

# Re-run with the code
TELEGRAM_AUTH_CODE=12345 TELEGRAM_AUTH_PASSWORD=yourpassword ./gradlew bootRun
# Wait for "authorized successfully", then Ctrl+C

# Normal run — session is saved, no re-auth needed
./gradlew bootRun
```

Session is saved in `./tdlib-data/` directory locally.

### Step 3 — Allow cleartext HTTP in Android app

Add to `res/xml/network_security_config.xml`:
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

Reference it in `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### Step 4 — MCP client connection

```
MCP endpoint:  http://10.0.2.2:8080/mcp
Auth:          HTTP Basic — username: mcp, password: your MCP_PASSWORD
```

Example with `mcp-remote` (for Claude Desktop testing from same machine):
```json
{
  "mcpServers": {
    "telegram": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://localhost:8080/mcp",
        "--header",
        "Authorization: Basic BASE64(mcp:YOUR_MCP_PASSWORD)",
        "--allow-http"
      ]
    }
  }
}
```

### Address reference

| Client environment | Server address |
|--------------------|---------------|
| Android Emulator (AVD) | `http://10.0.2.2:8080` |
| Genymotion | `http://10.0.3.2:8080` |
| Physical device (same WiFi) | `http://192.168.x.x:8080` (find with `ipconfig`/`ifconfig`) |
| Claude Desktop (same machine) | `http://localhost:8080` |

### Verify connection from emulator

```bash
# From host — confirm server is up
curl -u mcp:YOUR_MCP_PASSWORD -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

If the server responds, the emulator will reach it at `10.0.2.2:8080` with the same credentials.

---

## Connect to Claude Desktop

This server uses **HTTP transport** (JSON-RPC over HTTP). Claude Desktop requires a local stdio bridge — use [`mcp-remote`](https://github.com/geelen/mcp-remote).

### Step 1 — Install mcp-remote

```bash
npm install -g mcp-remote
```

### Step 2 — Add to Claude Desktop config

Open `claude_desktop_config.json`:
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "telegram": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://YOUR_SERVER_IP/mcp",
        "--header",
        "Authorization: Basic bWNwOllvdXJQYXNzd29yZA=="
      ]
    }
  }
}
```

The `Authorization` header value is Base64 of `mcp:YOUR_MCP_PASSWORD`.
Generate it:

```bash
echo -n "mcp:YOUR_MCP_PASSWORD" | base64
```

### Step 3 — Restart Claude Desktop

After saving the config, restart Claude Desktop. You should see **telegram** in the tools list.

### Verify tools are available

Ask Claude:
> "What Telegram tools do you have?"

Claude should list: `get_dialogs`, `search_dialog`, `get_unread_messages`, `get_last_messages`, `send_message`.

### Example usage

> "Show my unread Telegram messages"
> "Find dialog with John and send him hello"
> "What are the last 10 messages from the Work chat?"

---

## Development

```bash
# Start infrastructure
docker-compose up -d postgres redis

# Run application locally
./gradlew bootRun

# Run unit tests (no Docker required)
./gradlew test --tests "org.example.mcptelegram.mcp.*" \
               --tests "org.example.mcptelegram.security.RateLimitFilterTest" \
               --tests "org.example.mcptelegram.messaging.WebSocketBrokerTest" \
               --tests "org.example.mcptelegram.admin.AdminControllerTest"

# Run all tests (requires Docker)
./gradlew test

# Build without tests
./gradlew build -x test
```

## Stack

- Kotlin 2.0 + Spring Boot 3.3
- TDLib via [tdlight-java](https://github.com/tdlight-team/tdlight-java)
- PostgreSQL 16 (message history)
- Redis 7 (pub/sub + cache)
- WebSocket STOMP (admin live log)
- Thymeleaf + Bootstrap 5 (admin UI)
- Flyway (DB migrations)
- JUnit 5 + Testcontainers
