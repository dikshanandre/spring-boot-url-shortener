# Spring Boot URL Shortener — Complete Project Documentation & Reference Guide

## 1. Project Overview
This project is a full-stack, production-ready **URL Shortener Web Application** built with Spring Boot. It allows users to shorten long URLs, manage their links securely, handle user authentication and authorization, track database schema changes cleanly via migrations, and run both in a streamlined local development setup and as a fully containerized multi-container Docker deployment.

---

## 2. Complete Tech Stack & Dependencies (`pom.xml`)

### Core Runtime & Framework
* **Java 21** (Compiled targeting Java 21, executed locally on JDK 24)
* **Spring Boot `4.1.2`**: Core application framework providing auto-configuration, embedded Tomcat server, and dependency injection.

### Web Layer, UI & Validation
* **Spring Web MVC (`spring-boot-starter-webmvc`)**: Handles HTTP requests, REST/MVC controllers, URL redirection logic, and routing.
* **Thymeleaf (`spring-boot-starter-thymeleaf`)**: Server-side HTML template engine for rendering dynamic views.
* **Thymeleaf Layout Dialect (`thymeleaf-layout-dialect`)**: Enables reusable base layouts (headers, footers, navigation bars) across multiple HTML pages without duplicating boilerplate code.
* **Bootstrap `5.3.3` (via WebJars)**: Frontend CSS framework packaged directly as a Maven dependency for responsive styling without relying on external CDNs.
* **Jakarta Bean Validation (`spring-boot-starter-validation`)**: Validates incoming user inputs (e.g., ensuring valid URL formats, non-empty fields, and proper registration inputs) using annotations like `@NotBlank`, `@URL`, and `@Size`.

### Database, ORM & Migrations
* **PostgreSQL (`postgresql` driver, versions `16-alpine` & `17`)**: Primary relational database storing users, roles, and shortened URL mappings.
* **Spring Data JPA (`spring-boot-starter-data-jpa`)**: Abstraction over Hibernate ORM for database entity mapping, repository interfaces, pagination, and custom queries.
* **Flyway (`spring-boot-starter-flyway` & `flyway-database-postgresql`)**: Version-controlled database migration tool. Instead of letting Hibernate auto-generate tables (`ddl-auto`), Flyway executes incremental SQL scripts (e.g., `V1__create_tables.sql`, `V2__insert_sample_data.sql`) on startup so database schemas remain predictable and safe across all environments.
* **H2 Console (`spring-boot-h2console`)**: Included for quick local database inspection and lightweight test execution when needed.

### Security
* **Spring Security (`spring-boot-starter-security`)**: Protects endpoints, manages user login/logout sessions, encrypts passwords (`BCryptPasswordEncoder`), and enforces role-based access control (`USER` vs `ADMIN`).
* **Thymeleaf Spring Security Extras (`thymeleaf-extras-springsecurity6`)**: Allows conditional UI rendering inside HTML templates (e.g., showing "Login" vs "Logout" or admin-only buttons using `sec:authorize`).

### DevOps, Containerization & Testing
* **Spring Boot Docker Compose (`spring-boot-docker-compose`)**: Automatically detects `compose.yaml` during local development and manages the lifecycle of infrastructure containers (PostgreSQL) when starting the app from the IDE.
* **Spring Boot Maven Plugin (`spring-boot-maven-plugin`)**: Configured with Cloud Native Buildpacks to compile and package the application directly into an OCI-compliant Docker image without writing a manual `Dockerfile`.
* **Testing Starters (`spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, `spring-boot-starter-flyway-test`)**: Unit and integration testing support.

---

## 3. Architectural Breakdown & How Everything Works Together

### A. Database & Schema Management (PostgreSQL + Flyway)
1. **Why Flyway was used:** Relying on Hibernate's `spring.jpa.hibernate.ddl-auto=update` is risky in real-world apps because it cannot safely handle complex schema refactoring or seed data. Flyway tracks applied migrations in a `flyway_schema_history` table inside PostgreSQL.
2. **Startup Flow:** When the Spring Boot application starts, Flyway connects to PostgreSQL *before* Hibernate initializes, checks for any unapplied `V*__*.sql` scripts in `src/main/resources/db/migration`, executes them in order, and then hands over validation to JPA/Hibernate (`ddl-auto=validate`).

### B. Security & Authentication Flow (Spring Security)
1. **Password Hashing:** User passwords are never stored in plain text; Spring Security hashes them using `BCrypt` during registration.
2. **Authorization Rules:**
   * Public routes (Home page, Login, Registration, and short URL redirection endpoints like `/{shortKey}`) are accessible without authentication.
   * Protected routes (Dashboard, Creating custom/private links, Managing user URLs, Admin panels) require an authenticated session.
3. **UI Integration:** `thymeleaf-extras-springsecurity6` inspects the active `SecurityContext` to dynamically adjust navigation links and display user-specific data.

### C. Dual-Environment Setup (Local Development vs. Full Containerized Stack)
A key architectural pattern implemented in this project is separating **local IDE development** from **full containerized deployment** using two distinct Compose files:

#### 1. Local Development Workflow (`./compose.yaml` + IntelliJ IDEA)
* **Purpose:** Fast code-compile-run cycles inside IntelliJ IDEA while still using a real PostgreSQL database instead of an in-memory mock.
* **How it works:**
  * Root `compose.yaml` contains **only** the `postgres` service mapped to host port `5433` (to avoid conflicting with any local Windows PostgreSQL installation on `5432`).
  * `application.properties` points to `jdbc:postgresql://localhost:5433/urlshortener`.
  * When clicking **Run** in IntelliJ, `spring-boot-docker-compose` automatically runs `docker compose up` on the root `compose.yaml`, waits for PostgreSQL to be ready, connects the app, and stops the container when the IDE run stops.

```yaml
# Root compose.yaml (For Local IDE Development)
services:
  postgres:
    image: 'postgres:16-alpine'
    environment:
      - POSTGRES_DB=urlshortener
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_USER=postgres
    ports:
      - '5433:5432'
```

#### 2. Containerizing the Application (Cloud Native Buildpacks)
* **Purpose:** Package the Spring Boot JAR, an optimized JRE, and proper memory/layer configurations into a portable Docker image without maintaining a `Dockerfile`.
* **`pom.xml` Configuration:**
  ```xml
  <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <configuration>
          <image>
              <name>dikshan/${project.artifactId}</name>
          </image>
      </configuration>
  </plugin>
  ```
* Executing `.\mvnw spring-boot:build-image` builds `dikshan/url-shortner:latest` directly into the local Docker daemon.

#### 3. Full Multi-Container Deployment (`./docker/compose.yaml`)
* **Purpose:** Run the entire system (both the Spring Boot app and PostgreSQL) inside Docker containers, simulating a production server deployment.
* **Networking Detail:** Inside a Docker network, `localhost` refers to the container itself—not the host machine. Therefore, `docker/compose.yaml` overrides `application.properties` using environment variables so the app container connects to the database via its Docker service name (`postgres`) on internal port `5432`:

```yaml
# docker/compose.yaml (For Full Containerized Orchestration)
services:
  postgres:
    image: 'postgres:17'
    environment:
      - 'POSTGRES_DB=urlshortener'
      - 'POSTGRES_PASSWORD=postgres'
      - 'POSTGRES_USER=postgres'
    ports:
      - '5433:5432'

  url-shortener-app:
    image: dikshan/url-shortner:latest
    container_name: url-shortener-app
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/urlshortener
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    ports:
      - "8080:8080"
    restart: unless-stopped
    depends_on:
      - postgres
```

---

## 4. Troubleshooting Log: Problems Faced & Solutions Implemented

### Problem 1: Database Connection Refused During Maven Build (`localhost:5433`)
* **Symptom:** Running `.\mvnw package` or `.\mvnw spring-boot:build-image` failed during the test execution phase (`UrlShortnerApplicationTests`) with a HikariPool / PostgreSQL `Connection refused` error.
* **Root Cause:** Maven runs JUnit integration tests by default before packaging or building an image. The Spring context tried to boot up, initialize Flyway, and connect to `localhost:5433`, which failed if Docker Desktop wasn't running or the database container wasn't active.
* **Solution:** Either ensure Docker Desktop is active so `spring-boot-docker-compose` automatically provisions the database during test runs, or bypass the test phase during image creation using:
  ```cmd
  .\mvnw spring-boot:build-image -DskipTests
  ```

### Problem 2: `pull access denied for dikshan/url-shortner:latest` When Running from IntelliJ
* **Symptom:** Clicking **Run** in IntelliJ IDEA immediately crashed with `Error response from daemon: pull access denied for dikshan/url-shortner, repository does not exist or may require 'docker login'`.
* **Root Cause:** Both the `postgres` service and the `url-shortener-app` service were placed inside the root `compose.yaml` before the local Docker image had been built. When starting from IntelliJ, `spring-boot-docker-compose` tried to spin up *every* service in the root `compose.yaml`, couldn't find `dikshan/url-shortner:latest` locally, and attempted to download it from Docker Hub.
* **Solution:** Separated development and deployment configurations:
  1. Kept **only** the `postgres` service in the root `compose.yaml` for IntelliJ IDEA runs.
  2. Moved the full two-service stack (`postgres` + `url-shortener-app`) into `docker/compose.yaml`, and always ran `.\mvnw spring-boot:build-image` *before* running `docker compose up -d`.

### Problem 3: Tutorial vs. Local Configuration Mismatches (Database Name & Ports)
* **Symptom:** Copying the reference repository's `compose.yaml` directly caused authentication/database-not-found errors and image pull failures.
* **Root Cause:**
  1. The reference repo referenced the instructor's public Docker Hub image (`sivaprasadreddy/spring-boot-url-shortener`).
  2. The reference repo used `POSTGRES_DB=postgres` on port `5432`, while our `application.properties` was configured for `POSTGRES_DB=urlshortener` on host port `5433`.
* **Solution:** Customized `docker/compose.yaml` to use `image: dikshan/url-shortner:latest`, set `POSTGRES_DB=urlshortener`, mapped host port `5433:5432` for external DB tools, and set `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/urlshortener` for internal container-to-container communication.

### Problem 4: Port `8080` Address Already in Use (`BindException`)
* **Symptom:** Running `docker compose up -d` inside the `docker/` folder started `docker-postgres-1`, but failed on `url-shortener-app` with:
  `ports are not available: exposing port TCP 0.0.0.0:8080 -> 127.0.0.1:0: bind: Only one usage of each socket address is normally permitted.`
* **Root Cause:** The Spring Boot application was still actively running inside IntelliJ IDEA and holding onto host port `8080` when Docker tried to bind the containerized app to the same port.
* **Solution:** Stopped the running application in IntelliJ IDEA, cleaned up the partial compose state with `docker compose down`, and re-ran `docker compose up -d`.

### Problem 5: YAML Indentation & List Formatting Errors
* **Symptom:** Docker Compose rejected `compose.yaml` or misread environment variables and port mappings.
* **Root Cause:** YAML requires strict 2-space indentation and keeping list items (`- POSTGRES_DB=urlshortener` or `- '5433:5432'`) on the same line as their hyphen (`-`).
* **Solution:** Standardized indentation across both `compose.yaml` and `docker/compose.yaml`.

---

## 5. Essential Command Reference & Cheat Sheet

### Local Development (IntelliJ IDEA)
1. Start **Docker Desktop**.
2. Ensure root `compose.yaml` only contains `postgres`.
3. Click **Run** on `UrlShortnerApplication.java` in IntelliJ.
4. Access app at: `http://localhost:8080`

### Full Docker Container Deployment
1. **Stop IntelliJ run** (to free port `8080`).
2. **Build the Spring Boot Docker Image** (from project root `D:\Projects\url-shortner`):
   ```cmd
   .\mvnw spring-boot:build-image
   ```
   *(Or skip tests for faster builds: `.\mvnw spring-boot:build-image -DskipTests`)*
3. **Navigate to the `docker` directory:**
   ```cmd
   cd docker
   ```
4. **Start PostgreSQL and the App Container in the background:**
   ```cmd
   docker compose up -d
   ```
5. **Check running container status or logs:**
   ```cmd
   docker compose ps
   docker logs -f url-shortener-app
   ```
6. **Stop and remove containers when finished:**
   ```cmd
   docker compose down
   ```
