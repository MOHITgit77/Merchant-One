# Merchant One

Merchant One is a modern, full-stack merchant management platform designed to streamline store setup, catalogue management, inventory tracking, and sales processing.

## 🚀 Features

- **Store Management**: Easily configure store details, operating hours, delivery settings, and payment preferences.
- **Product Catalogue**: Manage products, variants, pricing, and stock levels effortlessly.
- **Inventory Tracking**: Keep track of stock movements, adjustments, and purchase batches with a full ledger system.
- **Sales & POS**: Process new sales seamlessly with real-time inventory updates and receipt generation.
- **Dynamic Dashboard**: View daily revenue, recent sales, and low-stock alerts at a glance.

## 🛠️ Technology Stack

- **Frontend**: React 19, TypeScript, Vite, React Router, React Hook Form, Zod
- **Backend**: Java 21, Spring Boot 3, Spring Security (JWT), Hibernate/JPA, PostgreSQL
- **Infrastructure**: Docker, Docker Compose, Flyway Database Migrations

## 📦 Getting Started (Local Development)

The easiest way to run the entire application locally is using Docker Compose. This will spin up the PostgreSQL database, the Spring Boot backend API, and the React frontend.

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) & Docker Compose installed and running on your machine.

### Running the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/MOHITgit77/Merchant-One.git
   cd Merchant-One
   ```

2. Start the services using Docker Compose:
   ```bash
   docker compose up -d --build
   ```

3. Access the application:
   - **Frontend UI**: `http://localhost:5174`
   - **Backend API**: `http://localhost:8080`
   - **Swagger API Docs**: `http://localhost:8080/swagger-ui.html`

4. To stop the application:
   ```bash
   docker compose down
   ```

## 🧪 Testing

To run the automated backend test suite, use the Maven wrapper:
```bash
cd backend
./mvnw clean test
```

## 📝 License

Copyright © 2026. All rights reserved.
