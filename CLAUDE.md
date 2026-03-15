# MCP Telegram Server

## Описание проекта
MCP (Model Context Protocol) сервер для работы с Telegram через TDLib. Позволяет AI-ассистентам (Claude и др.) читать и отправлять сообщения от имени обычного пользователя Telegram (не бот).

## Стек технологий
- **Kotlin 2.0 + Spring Boot 3.3**
- **TDLib** (tdlight-java 3.4.4) — Telegram MTProto клиент, полностью реализован
- **PostgreSQL 16** — хранение истории сообщений, диалогов, пользователей
- **Redis 7** — кэш + pub/sub для real-time обновлений
- **WebSocket** (STOMP) — real-time обновления для админки
- **Thymeleaf + Bootstrap 5** — админка
- **Flyway** — миграции БД
- **JUnit 5 + Testcontainers** — тестирование (TDD)
- **Nginx** — reverse proxy на VPS

## Команды

```bash
# Запуск инфраструктуры локально
docker-compose up -d postgres redis

# Сборка (без тестов)
./gradlew build -x test

# Запуск unit-тестов (без Docker)
./gradlew test --tests "org.example.mcptelegram.mcp.*" \
               --tests "org.example.mcptelegram.security.RateLimitFilterTest" \
               --tests "org.example.mcptelegram.messaging.WebSocketBrokerTest" \
               --tests "org.example.mcptelegram.admin.AdminControllerTest"

# Запуск всех тестов (требует Docker)
./gradlew test

# Запуск приложения локально
./gradlew bootRun

# Остановка инфраструктуры
docker-compose down
```

## Структура пакетов

```
src/main/kotlin/org/example/mcptelegram/
├── McpTelegramApplication.kt
├── config/
│   ├── RedisConfig.kt                   # RedisTemplate, ObjectMapper (@Primary)
│   └── WebSocketConfig.kt               # STOMP endpoint /ws, /topic, /app
├── mcp/
│   ├── McpController.kt                 # POST /mcp — tools/list, tools/call
│   ├── McpToolHandler.kt                # Интерфейс для всех тулзов
│   ├── McpToolRegistry.kt               # Автосбор всех McpToolHandler бинов
│   └── tools/
│       ├── GetDialogsTool.kt            # get_dialogs (limit)
│       ├── GetNewMessagesTool.kt        # get_unread_messages
│       ├── GetLastMessagesTool.kt       # get_last_messages (dialog_id, count)
│       ├── SearchDialogTool.kt          # search_dialog (query)
│       └── SendMessageTool.kt           # send_message (dialog_id, text)
├── telegram/
│   ├── TelegramClient.kt                # Интерфейс (suspend функции)
│   ├── TdLibTelegramClient.kt           # Полная реализация через tdlight-java
│   ├── TelegramUpdateListener.kt        # TelegramUpdate → Redis pub/sub
│   └── model/
│       ├── ChatType.kt
│       ├── Dialog.kt
│       ├── Message.kt
│       └── TelegramUpdate.kt            # sealed class: NewMessage, ChatUpdated
├── messaging/
│   ├── DialogCacheService.kt            # Redis кэш
│   ├── RedisPubSubService.kt            # Pub/Sub канал telegram.updates
│   ├── TelegramPersistenceService.kt    # Upsert диалогов/сообщений в PostgreSQL
│   ├── WebSocketBroker.kt               # Redis subscriber → PostgreSQL + STOMP
│   └── dto/
│       ├── DialogDto.kt
│       ├── MessageDto.kt
│       └── TelegramUpdateEvent.kt
├── persistence/
│   ├── entity/
│   │   ├── UserEntity.kt
│   │   ├── DialogEntity.kt
│   │   ├── MessageEntity.kt
│   │   └── McpUserEntity.kt
│   └── repository/
│       ├── UserRepository.kt
│       ├── DialogRepository.kt
│       ├── MessageRepository.kt
│       └── McpUserRepository.kt
├── admin/
│   └── AdminController.kt              # /admin, /admin/dialogs, /admin/log
└── security/
    ├── SecurityConfig.kt                # httpBasic, CSRF off, stateless
    ├── RateLimitFilter.kt               # 30 req/min per IP, returns 429
    ├── McpUserDetailsService.kt         # UserDetailsService из БД mcp_users
    ├── McpUserService.kt                # createUser, authenticate (BCrypt)
    └── DataInitializer.kt              # Создаёт admin и mcp пользователей из env

src/main/resources/
├── application.yml                      # Все секреты через ${ENV_VAR}
├── logback-spring.xml                   # prod: TDLib логи заглушены
├── db/migration/V1__init.sql
├── templates/admin/
│   ├── dashboard.html
│   ├── dialogs.html
│   ├── dialog-detail.html
│   └── log.html
└── static/
    ├── js/admin.js                      # STOMP WebSocket + live log
    └── css/admin.css
```

## MCP Tools

| Tool | Параметры | Описание |
|------|-----------|----------|
| `get_dialogs` | `limit: Int = 100` | Список диалогов с unread_count |
| `search_dialog` | `query: String` | Поиск диалога по имени |
| `get_unread_messages` | — | Все непрочитанные сообщения |
| `get_last_messages` | `dialog_id: Long, count: Int = 10` | N последних сообщений |
| `send_message` | `dialog_id: Long, text: String` | Отправить сообщение |

> **Saved Messages** — поиск через `search_dialog("saved messages")` возвращает чат с самим собой

### MCP API (JSON-RPC)

```bash
# Список инструментов
curl -u mcp:PASSWORD -X POST http://SERVER/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'

# Вызов инструмента
curl -u mcp:PASSWORD -X POST http://SERVER/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_last_messages","arguments":{"dialog_id":123456,"count":10}},"id":2}'
```

## Архитектура потока данных

```
TDLib (MTProto, обычный пользователь)
    → TelegramUpdateListener
        → Redis Pub/Sub (telegram.updates)
            → WebSocketBroker
                ├── PostgreSQL (TelegramPersistenceService)
                └── STOMP /topic/messages → Admin UI

MCP Client (Claude) → POST /mcp (HTTP Basic Auth)
    → RateLimitFilter (30 req/min)
    → McpController → McpToolRegistry → McpToolHandler
        └── TelegramClient (TdLibTelegramClient) → TDLib
```

## TDLib авторизация

Первый запуск — авторизация через env переменные:

```bash
# Шаг 1: запустить, дождаться AuthorizationStateWaitCode, Ctrl+C
docker-compose run --rm app

# Шаг 2: передать код (и 2FA пароль если есть)
docker-compose run --rm \
  -e TELEGRAM_AUTH_CODE=12345 \
  -e TELEGRAM_AUTH_PASSWORD=yourpassword \
  app

# Шаг 3: после "authorized successfully" — запустить в фоне
docker-compose up -d app
```

Сессия сохраняется в Docker volume `tdlib_data` — повторная авторизация не нужна.

### Бэкап сессии

```bash
# Сохранить
docker run --rm -v mcp-telegram_tdlib_data:/data -v /opt:/backup \
  alpine tar -czf /backup/tdlib-session.tar.gz -C /data .

# Восстановить на новом сервере
docker volume create mcp-telegram_tdlib_data
docker run --rm -v mcp-telegram_tdlib_data:/data -v /opt:/backup \
  alpine tar -xzf /backup/tdlib-session.tar.gz -C /data
```

## База данных (PostgreSQL)

```sql
users        (id, telegram_id UNIQUE, username, first_name, last_name, created_at)
dialogs      (id, telegram_chat_id UNIQUE, type, title, last_message_at, updated_at)
messages     (id, dialog_id FK, sender_id FK nullable, text, telegram_message_id, sent_at, created_at)
mcp_users    (id, username UNIQUE, password_hash, created_at)
```

## Аутентификация

- **MCP endpoint** (`/mcp/**`) — HTTP Basic Auth, credentials из env `MCP_USERNAME` / `MCP_PASSWORD`
- **Admin UI** (`/admin/**`) — HTTP Basic Auth, credentials из env `MCP_ADMIN_USERNAME` / `MCP_ADMIN_PASSWORD`
- **Actuator** (`/actuator/health`) — без аутентификации, `show-details: never`
- **TDLib** — авторизация через `TELEGRAM_AUTH_CODE` / `TELEGRAM_AUTH_PASSWORD` env переменные

## Деплой на VPS

```bash
# 1. Скопировать проект
scp -i keyfile project.tar.gz root@SERVER:/opt/
tar -xzf /opt/project.tar.gz -C /opt/mcp-telegram

# 2. Настроить .env
cp .env.example .env && nano .env

# 3. Собрать и запустить
docker-compose --env-file .env up -d --build

# 4. Nginx (уже настроен на /etc/nginx/sites-available/mcp-telegram)
# Биндит 127.0.0.1:8080 → публичный порт 80

# 5. Firewall
ufw allow 22 && ufw allow 80 && ufw allow 443 && ufw enable

# 6. Автозапуск
systemctl enable mcp-telegram
```

## Подключение к Claude Desktop

Требует `mcp-remote` (npm):

```json
{
  "mcpServers": {
    "telegram": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://SERVER_IP/mcp",
        "--header",
        "Authorization: Basic BASE64(mcp:PASSWORD)"
      ]
    }
  }
}
```

Генерация Base64: `echo -n "mcp:PASSWORD" | base64`

## Конвенции кода

- Все секреты — только через `${ENV_VAR}` в application.yml, никаких дефолтов с реальными значениями
- TDD: тесты пишутся перед реализацией
- `TelegramClient` — интерфейс, мокируется в unit-тестах
- Интеграционные тесты расширяют `AbstractIntegrationTest` (Testcontainers)
- MCP тулзы возвращают `Map<String, Any?>` напрямую (не DTO)
- `@MockBean` из `org.springframework.boot.test.mock.mockito`
- Coroutines: `suspend` в `TelegramClient`, `runBlocking` в `McpController`

## Статус реализации

### ✅ Завершено (все фазы)

| Фаза | Описание | Тесты |
|------|----------|-------|
| 0 | Инфраструктура (Gradle, Docker, Flyway) | — |
| 1 | Persistence layer (4 entity + 4 repository) | Testcontainers |
| 2 | Redis layer (cache + pub/sub + DTO) | Testcontainers |
| 3 | TDLib полная реализация (авторизация, все методы) | Unit (mock) |
| 4 | WebSocket (STOMP + broker + persistence) | Unit + Testcontainers |
| 5 | MCP Protocol (5 tools + controller + registry) | Unit (mock) |
| 6 | Security (httpBasic + BCrypt + RateLimit) | Unit + Testcontainers |
| 7 | Admin UI (Thymeleaf + Bootstrap + live log) | Unit (mock) |
| 8 | VPS деплой (Docker + Nginx + systemd + ufw) | — |
| 9 | README + CLAUDE.md документация | — |

### ⏳ Планируется
- SSL/HTTPS через Let's Encrypt (нужен домен)
- Подключение к Claude Desktop через mcp-remote
