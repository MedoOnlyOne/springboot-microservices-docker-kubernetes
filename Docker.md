# Spring Boot Microservice Dockerization Strategy

This document outlines the professional approach to containerizing Spring Boot applications using **Multi-Stage Builds** and **Layer Caching** for optimized performance and security.

---

## 🏗️ 1. The Multi-Stage Strategy
The core principle is to separate the **Build Environment** from the **Runtime Environment**. This prevents "image bloat" by ensuring build tools (Maven/JDK) never reach production.

### Stage 1: The "Kitchen" (Build)
* **Base Image:** Full JDK + Maven/Gradle (e.g., `maven:3.9-eclipse-temurin-17`).
* **Purpose:** Compile source code, run tests, and package the `.jar` file.
* **Outcome:** Contains source code, local `.m2` repository, and compilers (~800MB+).

### Stage 2: The "Dining Room" (Run)
* **Base Image:** Slim JRE (e.g., `eclipse-temurin:17-jre-alpine`).
* **Purpose:** Run the compiled artifact.
* **Outcome:** Contains *only* the Java Runtime and your `.jar`.
* **Benefit:** Reduced "attack surface" and smaller image size (~150MB).

---

## ⚡ 2. Layer Caching & The "Two-Step" Copy
Docker builds images in sequential layers. If a layer's input files haven't changed, Docker reuses the **cache**.

### The Problem with `COPY . .`
If you copy everything at once, any minor code change (even a typo in a Controller) invalidates the cache for the entire step. Docker is forced to re-download all Maven dependencies, which can take several minutes.

### The Optimized Solution
1. **Copy `pom.xml` only:** Docker checks if your dependencies changed.
2. **Run `mvn dependency:go-offline`:** This "heavy" step downloads libraries into a layer. Since `pom.xml` rarely changes, this layer is almost always cached.
3. **Copy `src/` and build:** Only your code changes are processed.
* **Result:** Build time drops from minutes to seconds.

---
## 🛡️ 3. Security & Best Practices for Spring Boot Containers

### 🛡️ 3.1 Principle of Least Privilege
By default, Docker containers run as the **root** user. This is a significant security risk. If an attacker exploits a vulnerability in your Spring Boot app, they gain root access to the entire container.
* **Best Practice:** Define a non-root user in your Dockerfile to run the application.

### 📦 3.2 Minimize the Attack Surface (Alpine Linux)
Using the `-alpine` base image reduces the number of pre-installed binaries.
* **Why:** A standard Ubuntu-based image includes tools like `curl`, `wget`, `git`, and `apt`. Alpine removes these.
* **Result:** A hacker has fewer "tools" to work with if they gain entry to your container.

### 🏗️ 3.3 Environment Isolation with `WORKDIR`
Always use `WORKDIR /app`.
* **Why:** Without it, your files are copied into the root directory (`/`), mixing your app with system folders like `/bin`, `/etc`, and `/lib`.
* **Result:** Prevents accidental overwriting of system files and keeps the filesystem clean.

### 🚫 3.4 The `.dockerignore` Requirement
Never allow the `COPY . .` command to pull in local build artifacts or sensitive metadata.
* **Essential Exclusions:**
  * `target/`: Prevents local JARs from conflicting with container-built JARs.
  * `.git/`: Prevents history and metadata leakage.
  * `.env`: Prevents hardcoded local secrets from being baked into the image.
---

## 🛠️ 4. Handling Database Connections with Environment Variables

Spring Boot is designed to automatically map environment variables to its `application.properties` or `application.yml` settings.

---

### 🔌 4.1 The Spring Boot Convention
Spring Boot converts `UPPER_CASE` environment variables to `camelCase` properties.
* **Env Var:** `SPRING_DATASOURCE_URL`
* **Maps to:** `spring.datasource.url`

### 🛠️ 4.2 The `docker-compose.yml` Setup



```yaml
services:
  accountsdb:
    image: mysql
    container_name: accounts-db
    ports:
      - 3306:3306
    volumes:
      - accounts_data:/var/lib/mysql
    networks:
      - medobank
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: accounts
    healthcheck:
      test: [ "CMD", "mysqladmin" ,"ping", "-h", "localhost" ]
      timeout: 10s
      retries: 10
      interval: 10s
      start_period: 10s
  loansdb:
    image: mysql
    container_name: loans-db
    ports:
      - 3307:3306
    volumes:
      - loans_data:/var/lib/mysql
    networks:
      - medobank
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: loans
    healthcheck:
      test: [ "CMD", "mysqladmin" ,"ping", "-h", "localhost" ]
      timeout: 10s
      retries: 10
      interval: 10s
      start_period: 10s
  cardsdb:
    image: mysql
    container_name: cards-db
    ports:
      - 3308:3306
    volumes:
      - cards_data:/var/lib/mysql
    networks:
      - medobank
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: cards
    healthcheck:
      test: [ "CMD", "mysqladmin" ,"ping", "-h", "localhost" ]
      timeout: 10s
      retries: 10
      interval: 10s
      start_period: 10s
  accounts:
    build: ./Accounts
    image: "medo3m/accounts:medobank"
    container_name: accounts-ms
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://accountsdb:3306/accounts
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
    deploy:
      resources:
        limits:
          memory: 700m
    networks:
      - medobank
    depends_on:
      accountsdb:
        condition: service_healthy
    healthcheck:
      test: [ "CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
  loans:
    build: ./Loans
    image: "medo3m/loans:medobank"
    container_name: loans-ms
    ports:
      - "8090:8090"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://loansdb:3306/loans
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
    deploy:
      resources:
        limits:
          memory: 700m
    networks:
      - medobank
    depends_on:
      loansdb:
        condition: service_healthy
    healthcheck:
      test: [ "CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8090/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
  cards:
    build: ./Cards
    image: "medo3m/cards:medobank"
    container_name: cards-ms
    ports:
      - "9000:9000"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://cardsdb:3306/cards
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
    deploy:
      resources:
        limits:
          memory: 700m
    networks:
      - medobank
    depends_on:
      cardsdb:
        condition: service_healthy
    healthcheck:
      test: [ "CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9000/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
volumes:
  accounts_data:
  loans_data:
  cards_data:
networks:
  medobank:
    driver: "bridge"
```

---

## ✨ 5. Enhanced Dockerfile Implementation - Production Refinements

This section documents the improvements applied to the current production Dockerfile for the Accounts microservice.

### 🔒 5.1 Non-Root User Implementation

#### What Changed
```dockerfile
# Add non-root user for security
RUN addgroup --system appuser && adduser --system --group appuser

USER appuser
```

#### Why It Matters
- **Security:** Running as a non-root user (`appuser`) limits the damage if an attacker exploits a vulnerability
- **Compliance:** Many security standards (CIS Docker Benchmark, Kubernetes Pod Security Standards) require non-root users
- **Principle:** Implements the "Principle of Least Privilege" - containers only get the minimum permissions needed

#### How It Works
- `addgroup`: Creates a system group named `appuser`
- `adduser`: Creates a system user belonging to that group
- `USER appuser`: Switches all subsequent commands to run as this user

---

### 🏷️ 5.2 Metadata Labels

#### What Changed
```dockerfile
LABEL maintainer="mohamed.medhat2199@gmail.com"
LABEL description="Spring Boot Accounts Microservice"
```

#### Why It Matters
- **Tracking:** Metadata helps identify who maintains the image
- **Documentation:** Describes the purpose of the image at a glance
- **Automation:** CI/CD pipelines can parse labels to automate deployments
- **Governance:** Image registries use labels to organize and filter images

#### Best Practice
Always include:
- `maintainer`: Contact information for the image owner
- `description`: Brief purpose of the microservice
- Optional: `version`, `build-date`, `vcs-url` for enhanced tracking

---

### ❤️ 5.3 Health Checks

#### What Changed
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

#### Why It Matters
- **Orchestration:** Kubernetes and Docker Compose use health checks to determine container readiness
- **Automatic Recovery:** Failed containers can be automatically restarted
- **Load Balancing:** Unhealthy containers are removed from service traffic
- **Monitoring:** Health status is exposed to monitoring systems

#### HEALTHCHECK Parameters Explained
| Parameter | Value | Purpose |
|-----------|-------|---------|
| `--interval` | 30s | Check health every 30 seconds |
| `--timeout` | 10s | Wait maximum 10 seconds for a response |
| `--start-period` | 40s | Allow 40 seconds for app startup before checking |
| `--retries` | 3 | Mark as unhealthy after 3 consecutive failures |

#### Requirements
- **Spring Boot Actuator** must be on the classpath: `spring-boot-starter-actuator`
- **Health endpoint** must be exposed (default: `/actuator/health`)
- **Port** must match your application port (8080 for Accounts)

#### Adding Actuator to pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

### 🔐 5.4 File Ownership

#### What Changed
```dockerfile
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appuser app.jar
```

#### Why It Matters
- **Security:** The `appuser` needs to own the JAR file to execute it properly
- **Permission Enforcement:** Prevents other users from modifying the application
- **Container Isolation:** Ensures the application can't access files outside its scope

#### How It Works
- After copying the JAR from the build stage, it's owned by root by default
- `chown` changes ownership to the `appuser` user and group
- When the container switches to `USER appuser`, it can now execute the JAR

---

### 🎯 5.5 Complete Refined Dockerfile

```dockerfile
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S appuser && adduser -S appuser -G appuser

# Copy the JAR from the build stage to the production image
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar
#RUN chown appuser:appuser app.jar (using COPY and chown in one command to reduce the layers and container size)

# Add metadata labels
LABEL maintainer="mohamed.medhat2199@gmail.com"
LABEL description="Spring Boot Accounts Microservice"

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Switch to non-root user
USER appuser

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📋 Summary Table: Before vs After

| Aspect | Before | After | Benefit |
|--------|--------|-------|---------|
| **Running User** | root | appuser | ✅ Enhanced security |
| **Metadata** | ❌ None | ✅ Labels | ✅ Better tracking |
| **Health Monitoring** | ❌ No | ✅ HEALTHCHECK | ✅ Auto-recovery in K8s |
| **File Ownership** | root:root | appuser:appuser | ✅ Permission control |
| **Java Version** | 17 | 21 (LTS) | ✅ Modern features & support |
| **Security Posture** | Basic | Enterprise-grade | ✅ Production-ready |
