# MCP Telegram Server

## Описание проекта
MCP (Model Context Protocol) сервер для работы с Telegram через TDLib. Позволяет получать диалоги, сообщения и отправлять сообщения через MCP-тулзы. Работает от имени обычного пользователя Telegram (не бот).

## Стек технологий
- **Kotlin 2.0 + Spring Boot 3.3**
- **TDLib** (tdlight-java) — Telegram MTProto клиент (заглушка, ожидает реализации)
- **PostgreSQL 16** — хранение истории сообщений, диалогов, пользователей
- **Redis 7** — кэш диалогов/сообщений + pub/sub для real-time обновлений
- **WebSocket** (STOMP) — real-time обновления для админки
- **Thymeleaf + Bootstrap 5** — админка
- **Flyway** — миграции БД
- **JUnit 5 + Testcontainers** — тестирование (TDD)

## Команды

```bash
# Запуск инфраструктуры
docker-compose up -d

# Сборка (только компиляция, без тестов)
./gradlew build -x test

# Компиляция
./gradlew compileKotlin

# Запуск тестов (требует Docker для Testcontainers)
./gradlew test

# Запуск только unit-тестов (без Docker)
./gradlew test --tests "org.example.mcptelegram.mcp.*" \
               --tests "org.example.mcptelegram.messaging.WebSocketBrokerTest"

# Запуск приложения
./gradlew bootRun

# Остановка инфраструктуры
docker-compose down
```

## Структура пакетов

```
src/main/kotlin/org/example/mcptelegram/
├── McpTelegramApplication.kt            # Точка входа
├── config/
│   ├── RedisConfig.kt                   # RedisTemplate, ObjectMapper (@Primary), ListenerContainer
│   └── WebSocketConfig.kt              # STOMP endpoint /ws, /topic, /app
├── mcp/
│   ├── McpController.kt                # POST /mcp — tools/list, tools/call
│   ├── McpToolHandler.kt               # Интерфейс для всех тулзов
│   ├── McpToolRegistry.kt              # Автосбор всех McpToolHandler бинов
│   └── tools/
│       ├── GetDialogsTool.kt           # get_dialogs
│       ├── GetNewMessagesTool.kt       # get_new_messages
│       ├── GetLastMessagesTool.kt      # get_last_messages (dialog_id, count)
│       └── SendMessageTool.kt          # send_message (dialog_id, text)
├── telegram/
│   ├── TelegramClient.kt               # Интерфейс (suspend функции)
│   ├── TdLibTelegramClient.kt          # Заглушка — бросает UnsupportedOperationException
│   ├── TelegramUpdateListener.kt       # TelegramUpdate → Redis pub/sub
│   └── model/
│       ├── ChatType.kt
│       ├── Dialog.kt
│       ├── Message.kt
│       └── TelegramUpdate.kt           # sealed class: NewMessage, ChatUpdated
├── messaging/
│   ├── DialogCacheService.kt           # Redis кэш dialog:{chatId}, messages:{chatId}:last
│   ├── RedisPubSubService.kt           # Pub/Sub канал telegram.updates
│   ├── TelegramPersistenceService.kt   # Upsert диалогов/сообщений в PostgreSQL
│   ├── WebSocketBroker.kt              # Redis subscriber → PostgreSQL + STOMP
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
│   └── AdminController.kt             # /admin, /admin/dialogs, /admin/dialogs/{id}, /admin/log
└── security/
    ├── SecurityConfig.kt               # httpBasic, CSRF disabled, /actuator permitAll
    ├── McpUserDetailsService.kt        # UserDetailsService из БД mcp_users
    ├── McpUserService.kt               # createUser, authenticate (BCrypt)
    └── DataInitializer.kt             # Создаёт admin/admin при первом запуске

src/main/resources/
├── application.yml
├── db/migration/V1__init.sql
├── templates/admin/
│   ├── dashboard.html
│   ├── dialogs.html
│   ├── dialog-detail.html
│   └── log.html
└── static/
    ├── js/admin.js                     # STOMP WebSocket + live log
    └── css/admin.css

src/test/kotlin/org/example/mcptelegram/
├── AbstractIntegrationTest.kt          # Testcontainers: PostgreSQL + Redis
├── persistence/                        # UserRepositoryTest, DialogRepositoryTest, ...
├── messaging/                          # DialogCacheServiceTest, RedisPubSubServiceTest, ...
├── telegram/                           # TelegramUpdateListenerTest
├── mcp/                                # McpControllerTest + tools tests
├── security/                           # SecurityConfigTest, McpUserServiceTest, ...
└── admin/                              # AdminControllerTest
```

## MCP Tools

| Tool | Параметры | Описание |
|------|-----------|----------|
| `get_dialogs` | — | Список всех диалогов с метаданными |
| `get_new_messages` | — | Все непрочитанные сообщения |
| `get_last_messages` | `dialog_id: Long, count: Int = 20` | N последних сообщений диалога |
| `send_message` | `dialog_id: Long, text: String` | Отправить сообщение в диалог |

### MCP API (JSON-RPC)

```bash
# Список инструментов
curl -u mcp:changeme -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'

# Вызов инструмента
curl -u mcp:changeme -X POST http://localhost:8080/mcp \
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
                └── STOMP /topic/messages → Admin UI (live лог)

MCP Client → POST /mcp (HTTP Basic Auth)
    → McpController → McpToolRegistry → McpToolHandler
        ├── Redis Cache (DialogCacheService)
        └── TelegramClient (TdLibTelegramClient)
```

## База данных (PostgreSQL)

```sql
users        (id, telegram_id UNIQUE, username, first_name, last_name, created_at)
dialogs      (id, telegram_chat_id UNIQUE, type, title, last_message_at, updated_at)
messages     (id, dialog_id FK, sender_id FK nullable, text, telegram_message_id, sent_at, created_at)
mcp_users    (id, username UNIQUE, password_hash, created_at)
```

Индексы: `messages(dialog_id, sent_at)`, `messages(telegram_message_id)`, `dialogs(telegram_chat_id)`

## Redis

- **Кэш**: `dialog:{chatId}` (TTL 5 мин), `messages:{chatId}:last` (TTL 1 мин)
- **Pub/Sub**: канал `telegram.updates`, сообщения — JSON сериализованный `TelegramUpdateEvent`

## Аутентификация

- **MCP endpoint** (`/mcp/**`) — HTTP Basic Auth, credentials: `mcp` / `changeme` (application.yml)
- **Admin UI** (`/admin/**`) — HTTP Basic Auth, credentials из БД `mcp_users` (по умолчанию `admin`/`admin`)
- **Actuator** (`/actuator/**`) — без аутентификации
- **TDLib** — номер телефона + код + опционально 2FA (при первом запуске, требует реализации)

## Конфигурация (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mcp_telegram
    username: postgres
    password: postgres
  data:
    redis:
      host: localhost
      port: 6379

telegram:
  api-id: YOUR_API_ID        # получить на my.telegram.org
  api-hash: YOUR_API_HASH
  phone: YOUR_PHONE_NUMBER
  database-directory: ./tdlib-data

mcp:
  auth:
    username: mcp
    password: changeme
```

## Статус реализации

### ✅ Завершено (фазы 0–7)

| Фаза | Описание | Тесты |
|------|----------|-------|
| 0 | Инфраструктура (Gradle, Docker, Flyway, config) | — |
| 1 | Persistence layer (4 entity + 4 repository) | Testcontainers |
| 2 | Redis layer (cache + pub/sub + DTO) | Testcontainers |
| 3 | Telegram Client (интерфейс + заглушка + listener) | Unit (mock) |
| 4 | WebSocket (STOMP + broker + persistence service) | Unit + Testcontainers |
| 5 | MCP Protocol (4 tools + controller + registry) | Unit (mock) |
| 6 | Security (httpBasic + BCrypt + DataInitializer) | Testcontainers |
| 7 | Admin UI (Thymeleaf + Bootstrap + live log) | Unit (mock) |

### ⏳ Ожидает

#### Фаза 8: Документация
- [ ] Создать `README.md` — описание проекта, требования (Java 21, Docker), быстрый старт, ASCII архитектурная диаграмма, MCP tools API с примерами запросов/ответов

#### TDLib реализация (требует реального Telegram аккаунта)
- [ ] Получить `api-id` и `api-hash` на [my.telegram.org](https://my.telegram.org)
- [ ] Добавить зависимость `tdlight-java` в `build.gradle.kts` (репозиторий JitPack)
- [ ] Реализовать `TdLibTelegramClient`:
  - [ ] Инициализация TDLib параметров (api_id, api_hash, database_directory)
  - [ ] Авторизация: waitPhoneNumber → waitCode → waitPassword → authorizationStateReady
  - [ ] `getDialogs`: `TdApi.GetChats` → маппинг в `List<Dialog>`
  - [ ] `getMessages`: `TdApi.GetChatHistory` → маппинг в `List<Message>`
  - [ ] `sendMessage`: `TdApi.SendMessage` → маппинг в `Message`
  - [ ] `getUnreadMessages`: `getDialogs` + фильтр `unreadCount > 0` + `getMessages`
  - [ ] `setUpdateHandler`: регистрация `ResultHandler` на TDLib client
- [ ] Добавить тест `TdLibTelegramClientTest` с `@Disabled("requires real Telegram account")`
- [ ] Проверить end-to-end: `./gradlew bootRun` → авторизация → MCP `get_dialogs` → список диалогов

## Конвенции кода

- TDD: тесты пишутся перед реализацией
- `TelegramClient` — интерфейс, используется для мокирования в unit-тестах
- Интеграционные тесты расширяют `AbstractIntegrationTest` (Testcontainers: PostgreSQL + Redis)
- `TdLibTelegramClient` интеграционные тесты помечаются `@Disabled("requires real Telegram account")`
- DTO классы (`messaging/dto/`) для передачи данных между слоями, Entity для JPA
- `@MockBean` из `org.springframework.boot.test.mock.mockito` (Spring Boot 3.3)
- Coroutines: `suspend` функции в `TelegramClient`, `runBlocking` в `McpController`

## Запуск и проверка

```bash
# 1. Инфраструктура
docker-compose up -d

# 2. Все тесты
./gradlew test

# 3. Запуск приложения (TDLib пока заглушка)
./gradlew bootRun

# 4. Проверка MCP
curl -u mcp:changeme http://localhost:8080/mcp \
  -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'

# 5. Открыть админку
open http://localhost:8080/admin   # логин: admin / admin
```
