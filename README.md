# Explore With Me

Приложение для организации событий и участия в них. Пользователи создают события, другие подают заявки на участие, оставляют комментарии. Реализована микросервисная архитектура.

---

## Архитектура

### Модули проекта

- **core** — бизнес-сервисы и клиенты для взаимодействия между ними  
- **infra** — инфраструктура: Config Server, Eureka (Discovery), Gateway  
- **stats** — сервис статистики просмотров (stats-server) и клиент для него (stats-client)

### Сервисы (core)

| Сервис | Назначение |
|--------|------------|
| **user-service** | Пользователи: CRUD, получение по id и списком по ids |
| **event-service** | События, категории, подборки; основная бизнес-логика событий |
| **request-service** | Заявки на участие в событиях |
| **comment-service** | Комментарии к событиям |

Запросы извне идут через **Gateway**; сервисы находят друг друга через **Eureka** и общаются по внутреннему API (Feign-клиенты).

- **event-service** вызывает: user-client, request-client, stats-client  
- **request-service** вызывает: user-client, event-client  
- **comment-service** вызывает: user-client, event-client  

Вызовы к другим сервисам обёрнуты в **Resilience4j** (Circuit Breaker, Retry); для stats при недоступности используется fallback (запрос не падает).

### Общий код (core)

- **core-common** — общие исключения и глобальная обработка ошибок (`GlobalErrorHandler`, `ApiError`, исключения: `NotFoundException`, `ValidationException`, `ConflictException`, `AccessDeniedException`, `BusinessRuleException`, `ServiceUnavailableException`).
- **user-client**, **event-client**, **request-client** — Feign-клиенты и DTO для вызова user-service, event-service, request-service. Подключаются как зависимости к сервисам, которым нужен соответствующий API.

---

## Конфигурация

Настройки сервисов хранятся в **Config Server** и подтягиваются при старте по имени приложения.

Расположение конфигов:

```
infra/config-server/src/main/resources/config/
├── core/
│   ├── user-service/application.yml
│   ├── event-service/application.yml
│   ├── request-service/application.yml
│   └── comment-service/application.yml
├── infra/
│   └── gateway-server/application.yml
└── stats/
    └── stats-server/application.yml
```

В конфигах задаются: порт (часто `port: 0` для выдачи порта через Eureka), БД (PostgreSQL), подключение к Eureka, параметры Resilience4j (circuit breaker, retry).

---

## Внутренний API (взаимодействие сервисов)

Эти эндпоинты предназначены для вызова только другими сервисами (через Feign), не через Gateway. Имена сервисов — как в Eureka (`user-service`, `event-service`, `request-service`).

### 1. User Service (user-client)

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/users/{id}` | Получить пользователя по id |
| GET | `/users?ids={ids}` | Получить пользователей по списку id (например, `ids=1,2,3`) |

**Ответ (UserDto):** `id`, `name`, `email`.

---

### 2. Event Service (event-client)

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/internal/events/{id}` | Получить данные события для внутреннего использования (ограниченный DTO) |

**Ответ (InternalEventDto):**  
`id`, `initiatorId`, `participantLimit`, `requestModeration`, `state` (enum события).

---

### 3. Request Service (request-client)

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/internal/requests/confirmed-count?eventIds={eventIds}` | Количество подтверждённых заявок по списку id событий (например, `eventIds=1,2,3`) |

**Ответ:** список `CountConfirmedRequestsByEventId`: `eventId`, `countConfirmedRequests`.

---

### Резюме по клиентам

- **user-client** — обращается к `user-service`, эндпоинты `/users`, `/users/{id}`.  
- **event-client** — обращается к `event-service`, эндпоинт `/internal/events/{id}`.  
- **request-client** — обращается к `request-service`, эндпоинт `/internal/requests/confirmed-count`.  

Сервисы используют не Feign-интерфейсы напрямую, а обёртки с Resilience4j (`ResilientUserClient`, `ResilientEventClient`, `ResilientRequestClient`, в event-service также `ResilientStatsClient` для stats-server).

---