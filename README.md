# Wallet Service

A simple backend service that supports wallet creation, credits, debits, and transfers with strong consistency guarantees and idempotent transaction handling.

This project was implemented as an interview coding task, with emphasis on correctness, transactional safety, and production-grade design.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [API Endpoints](#api-endpoints)
- [Running the Application](#running-the-application)
- [Database Initialization](#database-initialization)
- [Error Handling](#error-handling)
- [Testing Notes](#testing-notes)
- [Possible Enhancements / Next Steps](#possible-enhancements--next-steps)

  
---  

## Features

- Create wallets
- Credit and debit wallets
- Transfer funds between wallets atomically
- Idempotent transaction handling using a stored `idempotencyKey`
- Strong consistency using database transactions
- Monetary values stored in **minor units (integers)** — no floating point arithmetic
- Structured logging and clean error handling
- Dockerized for easy startup and review

---  

## Tech Stack

- **Java 21**
- **Spring Boot**
- **PostgreSQL**
- **Spring Data JPA / Hibernate**
- **Docker & Docker Compose**

---  

## API Base Path

All endpoints are prefixed with:


    /api/v1/wallets  


  
---  

## Design Decisions (High Level)

### Idempotency

Each transaction request includes an `idempotencyKey`.

- The key is stored in the database
- A unique constraint ensures the same transaction is never applied twice
- Retried requests return the result of the original transaction

The **database is the final line of defense**.
  
---  

### Atomicity & Consistency

- All balance-affecting operations run inside database transactions
- Wallet rows are locked when balances are updated
- Transfers debit and credit wallets atomically

---  

### Money Representation

- All monetary values are stored as **minor units** (`Long`)
- Example: `1000` = ₦10.00 (kobo)
- No floating point arithmetic is used

---  

### Schema Management

The application uses:

### Configuration

`spring.jpa.hibernate.ddl-auto=validate`



This means:

- Hibernate does **not** create or modify tables
- The application fails fast if the schema is missing or invalid
- The database schema is created explicitly using SQL init scripts on first startup

---  

## Running the Application

### Prerequisites

- Docker
- Docker Compose

_No local Java, Maven, or PostgreSQL installation is required._

### Start the application

`docker compose up --build`


This will:

- Start PostgreSQL
-   Initialize the database schema
- Start the Spring Boot application
- Validate the schema on startup


### Application URL
    http://localhost:8080




## Reset Everything (Fresh Start)

    docker compose down -v  
    docker compose up --build  


## Database Initialization

On first startup, PostgreSQL executes SQL scripts located in:


    db-init/ 



These scripts:
- Create tables
- Create indexes
- Enforce idempotency constraints

**They run only once, when the database volume is empty.**

## API Endpoints

**Create Wallet**

    POST /api/v1/wallets  

**Response**


    {  
    "id": 1,  
    "balance": 0,  
    "createdAt": "2025-12-19T10:00:00",  
    "updatedAt": "2025-12-19T10:00:00"  
    } 



**Credit Wallet**

    POST /api/v1/wallets/{id}/credit  



**Body Parameters**


    amount (Long, required) — minor units  
      
    idempotencyKey (String, required)  


Example


    curl -X POST \  
    "http://localhost:8080/api/v1/wallets/1/credit?amount=1000&idempotencyKey=credit-1"  


**Debit Wallet**

    POST /api/v1/wallets/{id}/debit  



### Rules


- Balance must not go negative
- Idempotent retries are safe


**Example**

    curl -X POST \  
    "http://localhost:8080/api/v1/wallets/1/debit?amount=500&idempotencyKey=debit-1"  


**Transfer Between Wallets**

    POST /api/v1/wallets/transfer  



Body Parameters


    fromWalletId  
      
    toWalletId  
      
    amount  
      
    idempotencyKey  


Example


    curl -X POST \  
    "http://localhost:8080/api/v1/wallets/transfer?fromWalletId=1&toWalletId=2&amount=300&idempotencyKey=transfer-1"  


- Debit and credit occur atomically
- Safe to retry with the same idempotency key



**Get Wallet Details**

    GET /api/v1/wallets/{id}  


Example

    curl http://localhost:8080/api/v1/wallets/1  


### Error Handling


- Domain-specific exceptions are used (e.g. insufficient balance,
  wallet not found)
- Errors return consistent, structured response
- Validation is performed at the controller layer
- Meaningful logs are emitted for operational visibility


### Testing Notes

**This project focuses on:**


- Correct business behavior



- Transactional safety



- Idempotency guarantees


The provided cURL examples allow easy manual verification of:
- Happy paths
- Validation failures
- Idempotent retries
- Concurrent-safe behavior


## Possible Enhancements / Next Steps

This implementation focuses on correctness, transactional safety, and
idempotency, as required for the exercise. In a production environment,
the following enhancements would be natural next steps:

- **Authentication & Authorization**
  - Secure endpoints using JWT or OAuth2
  - Wallet access scoped per authenticated user

- **Optimistic Locking (optional)**
  - Currently, pessimistic locks ensure balance consistency.
  - Optimistic locking (`@Version`) could be introduced for high-concurrency environments
    where the risk of conflicts is low, reducing lock contention.

- **Pagination & Query APIs**
  - Add paginated transaction history endpoints
  - Support filtering by date range and transaction type

- **Observability**
  - Metrics (e.g. Micrometer + Prometheus)
  - Distributed tracing for transaction flows

- **Resilience & Scalability**
  - Database connection pool tuning
  - Rate limiting and request throttling
  - Read replicas for non-mutating queries

- **Schema Migrations**
  - Introduce Flyway or Liquibase for controlled schema evolution

These were intentionally kept out of scope to maintain clarity and focus
for the interview task.


### Summary

- Clean, readable, production-style code
- Database-enforced correctness
- Strong transactional guarantees
- One-command startup for reviewers
- Easy to understand and easy to verify


This service is designed to be simple to run, simple to review, and hard to break.