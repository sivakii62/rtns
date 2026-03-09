# RTNS — Real-Time Notification System

A Spring Boot application that delivers real-time notifications to users via **Server-Sent Events (SSE)**, backed by **MySQL** for offline persistence, and designed to run on **GCP Cloud Run** with **Cloud SQL**.

---

## How it works

- **User 1** creates a task assigned to **User 2** via `POST /tasks`
- The task is saved to MySQL and a notification is written as `PENDING`
- If **User 2 is online** (SSE stream open) — notification is pushed instantly and marked `DELIVERED`
- If **User 2 is offline** — notification stays `PENDING` in MySQL; when they reconnect all pending notifications are flushed immediately

For multi-instance Cloud Run deployments, **GCP Pub/Sub** acts as the cross-instance broadcast bus (see `PubSubPushController`).

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4 |
| Database | MySQL (Cloud SQL on GCP) |
| Real-time | SSE via `SseEmitter` |
| Cross-instance | GCP Pub/Sub (push subscription) |
| Hosting | GCP Cloud Run |

---

## Project Structure

```
src/main/java/com/demo/rtns/
├── controller/
│   ├── TaskController.java          # POST /tasks
│   ├── NotificationController.java  # GET  /notifications/stream?userId=
│   └── PubSubPushController.java    # POST /internal/pubsub/notify
├── service/
│   ├── TaskService.java             # create task + trigger notification
│   ├── NotificationService.java     # save, push via SSE, flush pending
│   └── SseEmitterRegistry.java      # in-memory userId → SseEmitter map
├── entity/
│   ├── Task.java                    # tasks table
│   └── Notification.java            # notifications table (PENDING/DELIVERED)
├── repository/
│   ├── TaskRepository.java
│   └── NotificationRepository.java
└── dto/
    ├── TaskRequest.java             # POST /tasks request body
    └── NotificationPayload.java     # SSE event data shape
```

---

## Local Setup

### Prerequisites
- Java 21
- Maven
- MySQL running locally

### 1. Create the database

```sql
CREATE DATABASE rtns_db;
```

### 2. Configure credentials

Edit `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rtns_db
    username: your_username
    password: your_password
```

### 3. Run

```bash
./mvnw spring-boot:run
```

JPA will auto-create the tables on first boot (`ddl-auto: update`).

---

## API Reference

### Create a task

```
POST /tasks
Content-Type: application/json

{
  "title": "Fix bug #42",
  "description": "Urgent fix needed",
  "assignedToUserId": 2,
  "createdByUserId": 1
}
```

### Subscribe to notification stream

```
GET /notifications/stream?userId=2
```

Returns an SSE stream. Connect once on login and keep it open. The client auto-reconnects via the browser's native `EventSource`.

**Angular example:**
```ts
const es = new EventSource('/notifications/stream?userId=2');
es.addEventListener('notification', (e) => {
  const payload = JSON.parse(e.data);
  console.log('New notification:', payload);
});
```

---

## Testing the flow

```bash
# Terminal 1 — User 2 opens the SSE stream
curl -N "http://localhost:8080/notifications/stream?userId=2"

# Terminal 2 — User 1 creates a task for User 2
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Fix bug","description":"urgent","assignedToUserId":2,"createdByUserId":1}'
```

If the stream is open, the notification arrives in Terminal 1 immediately.
Close Terminal 1 first, run the POST, then reconnect — the pending notification is delivered on reconnect.

---

## GCP Deployment Notes

- **Cloud Run** — set `min-instances=1` to reduce SSE cold-start drops
- **Cloud SQL** — update `application.yaml` with the Cloud SQL socket URL
- **Pub/Sub** — create a push subscription pointing to `https://<your-service-url>/internal/pubsub/notify`
- Secure `/internal/pubsub/notify` by verifying the `Authorization` header Pub/Sub sends
