# Accounts Microservice - Demo Guide

This guide shows you how to use the Accounts Microservice without authentication.

---

## How It Works

This is a **simple demo** for creating and managing bank accounts. No login/authentication is required.

**Flow:**
```
Customer → API → Create Customer + Account → Database
          ↓
        View Account Details
          ↓
        Update Account
          ↓
        Delete Account
```

---

## Start the Service

```bash
# Using Docker Compose (recommended)
cd /home/medo/dev/springboot-microservices-docker-kubernetes
docker-compose up -d accounts-service mysql

# OR Run directly with Maven
cd Accounts
mvn spring-boot:run
```

**Access:** `http://localhost:8080`

---

## API Endpoints

All endpoints are **public** - no authentication needed.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/accounts/create` | Create new customer and account |
| GET | `/api/v1/accounts/fetch?mobileNumber=xxx` | Fetch account details |
| PUT | `/api/v1/accounts/update` | Update account details |
| DELETE | `/api/v1/accounts/delete?mobileNumber=xxx` | Delete account |

**API Documentation:** `http://localhost:8080/swagger-ui.html`

---

## Examples

### 1. Create Account

```bash
curl -X POST http://localhost:8080/api/v1/accounts/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "mobileNumber": "1234567890",
    "accountsDto": {
      "accountType": "Savings",
      "branchAddress": "123 Main Street, New York"
    }
  }'
```

**Response (201 Created):**
```json
{
  "statusCode": "201",
  "statusMsg": "201"
}
```

---

### 2. Fetch Account Details

```bash
curl "http://localhost:8080/api/v1/accounts/fetch?mobileNumber=1234567890"
```

**Response (200 OK):**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "mobileNumber": "1234567890",
  "accountsDto": {
    "accountNumber": 1234567890,
    "accountType": "Savings",
    "branchAddress": "123 Main Street, New York"
  }
}
```

---

### 3. Update Account

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/update \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john.updated@example.com",
    "mobileNumber": "1234567890",
    "accountsDto": {
      "accountNumber": 1234567890,
      "accountType": "Current",
      "branchAddress": "456 Oak Avenue, New York"
    }
  }'
```

**Response (200 OK):**
```json
{
  "statusCode": "200",
  "statusMsg": "Request processed successfully"
}
```

---

### 4. Delete Account

```bash
curl -X DELETE "http://localhost:8080/api/v1/accounts/delete?mobileNumber=1234567890"
```

**Response (200 OK):**
```json
{
  "statusCode": "200",
  "statusMsg": "Request processed successfully"
}
```

---

## Validation Rules

| Field | Validation | Example |
|-------|-------------|---------|
| name | 5-30 characters | "John Doe" |
| email | Valid email | "john@example.com" |
| mobileNumber | Exactly 10 digits | "1234567890" |
| accountType | Savings, Current, Salary, etc. | "Savings" |
| branchAddress | Required | "123 Main St" |

**Validation Error Response (400 Bad Request):**
```json
{
  "apiPath": "/api/v1/accounts/create",
  "errorCode": "400",
  "errorMessage": "MobileNumber must be 10 digits",
  "errorTime": "2024-02-18T12:00:00"
}
```

---

## Error Handling

| HTTP Status | Description |
|-------------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation failed) |
| 404 | Not Found (customer/account doesn't exist) |
| 417 | Expectation Failed (update/delete failed) |
| 500 | Internal Server Error |

---

## Project Structure

```
Accounts/
├── src/main/java/com/medo/accounts/
│   ├── controller/          # REST API endpoints
│   │   └── AccountsController.java
│   ├── service/             # Business logic
│   │   ├── IAccountsService.java
│   │   └── impl/AccountServiceImpl.java
│   ├── repository/          # Database access
│   │   ├── AccountsRepository.java
│   │   └── CustomerRepository.java
│   ├── entity/              # Database tables
│   │   ├── Accounts.java
│   │   ├── Customer.java
│   │   └── BaseEntity.java
│   ├── dto/                 # Data Transfer Objects
│   │   ├── AccountsDto.java
│   │   ├── CustomerDto.java
│   │   ├── ResponseDto.java
│   │   └── ErrorResponseDto.java
│   ├── mapper/              # Entity ↔ DTO conversion
│   │   ├── AccountsMapper.java
│   │   └── CustomerMapper.java
│   ├── exception/           # Error handling
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   └── CustomerAlreadyExistsException.java
│   ├── audit/               # JPA Auditing
│   │   └── AuditAwareImpl.java
│   └── constants/           # Constants
│       └── AccountsConstants.java
├── src/main/resources/
│   ├── application.yml       # Configuration
│   └── db/migration/        # Flyway database migrations
│       ├── V1__Create_customers_table.sql
│       ├── V2__Create_accounts_table.sql
│       └── V3__Create_indexes_and_triggers.sql
└── pom.xml
```

---

## Database Tables

### customers
| Column | Type | Description |
|--------|------|-------------|
| customer_id | BIGINT (PK) | Customer ID |
| name | VARCHAR(100) | Customer name |
| email | VARCHAR(100) | Email address |
| mobile_number | VARCHAR(20) (UNIQUE) | Phone number |
| created_at | TIMESTAMP | Creation time |
| created_by | VARCHAR(50) | Created by |
| updated_at | TIMESTAMP | Last update |
| updated_by | VARCHAR(50) | Updated by |

### accounts
| Column | Type | Description |
|--------|------|-------------|
| account_number | BIGINT (PK) | Account number |
| customer_id | BIGINT (FK) | References customers.customer_id |
| account_type | VARCHAR(50) | Account type |
| branch_address | VARCHAR(200) | Branch location |
| created_at | TIMESTAMP | Creation time |
| created_by | VARCHAR(50) | Created by |
| updated_at | TIMESTAMP | Last update |
| updated_by | VARCHAR(50) | Updated by |

---

## Testing with Swagger UI

1. Open browser: `http://localhost:8080/swagger-ui.html`
2. Click on any endpoint
3. Click "Try it out"
4. Enter request body
5. Click "Execute"

---

## Notes

- **No Authentication**: All endpoints are public for demo purposes
- **Account Number**: Automatically generated (10 digits)
- **Mobile Number**: Used as unique identifier for customers
- **Database**: Uses Flyway for schema migrations
- **Validation**: Uses Jakarta Bean Validation
