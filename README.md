# Merchant One

Merchant One is a modern, full-stack merchant management platform designed to streamline store setup, catalogue management, inventory tracking, and sales processing.

## Features

- **Store Management**: Easily configure store details, operating hours, delivery settings, and payment preferences.
- **Product Catalogue**: Manage products, variants, pricing, and stock levels effortlessly.
- **Inventory Tracking**: Keep track of stock movements, adjustments, and purchase batches with a full ledger system.
- **Order Management**: Receive orders from customers, manage order lifecycle (Accept, Prepare, Ready, Complete, Cancel), with automated inventory reservation and deduction.
- **Public Storefront APIs**: Expose public catalog and order creation endpoints for building customer-facing storefronts.
- **Sales & POS**: Process direct in-store sales seamlessly with real-time inventory updates and receipt generation.
- **Dynamic Dashboard**: View daily revenue, recent sales, and low-stock alerts at a glance.

## Technology Stack

- **Frontend**: React 19, TypeScript, Vite, React Router, React Hook Form, Zod
- **Backend**: Java 21, Spring Boot 3, Spring Security (JWT), Hibernate/JPA, PostgreSQL
- **Infrastructure**: Docker, Docker Compose, Flyway Database Migrations

## Getting Started (Local Development)

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

## Testing

To run the automated backend test suite, use the Maven wrapper:
```bash
cd backend
./mvnw clean test
```

## Environment Configuration

A sample environment configuration is provided in `.env.example`. 
Copy it to `.env` and configure your local variables before running docker-compose:
```bash
cp .env.example .env
```

## API Documentation

- **Storefront API Documentation:** The customer-facing API contracts are documented in [docs/Storefront_API.md](./docs/Storefront_API.md).
- **Merchant REST API:** Interactive Swagger documentation is available at `http://localhost:8080/swagger-ui.html` when the backend is running.

## Main Merchant Workflows

1. **Store Setup:** Register, create a store profile, and configure hours, delivery, and payments.
2. **Catalog Management:** Create categories, add products, variants, and pricing.
3. **Inventory Management:** Stock in items, manage purchase batches, and track stock ledger.
4. **Order Management:** Accept customer orders, reserve stock, and process them through to completion.
5. **Direct Sales (POS):** Process in-store sales and print invoices.

## License

Copyright © 2026. All rights reserved.
