# BookEase

Service discovery and appointment booking platform. The backend is a Spring Boot 3 / Java 21 modular monolith on MariaDB. The frontend is Angular 19, talking to the REST API through a reverse proxy.

## Stack

- Java 21, Spring Boot 3.4, Spring Security JWT, Flyway, MariaDB
- Angular 19 standalone components
- Every REST operation has a unique OpenAPI `operationId` for future AgentCore tools

## Prerequisites

- Java 21
- Maven
- Node.js 22+
- MariaDB with databases `bookease` and `bookease_test`

Default local credentials:

```
DB_URL=jdbc:mariadb://127.0.0.1:3306/bookease
DB_USERNAME=bookease
DB_PASSWORD=bookease
JWT_SECRET=dev-only-jwt-secret-change-me-32b
```

Production must set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET`. Do not commit secrets.

## Backend

```
cd backend
export JAVA_HOME=/usr/lib/jvm/jdk-21.0.12.1+1
export PATH=$JAVA_HOME/bin:$PATH
mvn test
mvn -DskipTests package
java -jar target/bookease-0.0.1-SNAPSHOT.jar
```

The API listens on port 8080.

- Health: `/actuator/health`
- OpenAPI: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`

Register always creates a `USER`. Promote a user to `ADMIN` or `PROVIDER` in MariaDB when needed:

```
UPDATE users SET role='ADMIN' WHERE email='you@example.com';
```

A USER can send a become-provider request from `/become-provider`. The account stays USER until an admin approves it, which promotes the account to PROVIDER.

## Role workspaces

- Guest/USER: Discover, book, appointments, reminders, become-provider request
- PROVIDER: incoming appointments, business profile, services, availability. No customer booking.
- ADMIN: provider approvals and categories. No customer or provider workspace.

After login: USER lands on Discover, PROVIDER on incoming appointments, ADMIN on approvals.

## Frontend

```
cd frontend
npm install
npx ng serve --host 0.0.0.0 --port 4200
```

The Angular dev server proxies `/api` and `/actuator` to `http://localhost:8080` and allows hosts under `.monkeycode-ai.live`.

## Roles

- USER: discover providers, book, cancel, reschedule, reminders
- PROVIDER: business profile, services, schedules, breaks, confirm/complete/no-show
- ADMIN: categories and provider approval

Identity always comes from the JWT. The client never supplies a `userId` as the owner.
