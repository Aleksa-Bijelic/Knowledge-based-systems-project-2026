# Drools Knowledge-Based System

Drools Knowledge-Based System is an academic project for the *Knowledge-Based Systems* course. It demonstrates how a rule-based expert system can be built with the **Drools rule engine** and integrated into real web applications. The project contains two independent subsystems — a **bookstore** and a **bank** — where business decisions (recommendations, discounts, loan approval, fraud detection) are driven entirely by an explicitly authored knowledge base of `.drl` rules.


The whole system is a full-stack application: a **Java / Spring Boot** backend that hosts and executes the Drools rule bases, and separate **React** frontends that consume the REST APIs. All inference logic lives in external rule files, kept fully separate from the application code.


# Project Overview


The project is designed as a practical demonstration of **knowledge-based / expert-system development**. Instead of hard-coding decision logic, every inference is written as rules that the Drools engine reasons over at runtime. The platform contains two self-contained subsystems:

- **Bookstore** — catalog management, purchasing, ratings, and an intelligent recommendation engine built with forward-chaining rules.
- **Bank** — account management, card transactions, payments, loan-approval support and fraud detection, including backward-chaining queries and explainable decisions.

Both subsystems expose REST APIs consumed by dedicated React clients and share a common Maven module.


# The Knowledge Base


The core of the project is its rule knowledge base, authored with the Drools rule language across several `.drl` files.

## Forward-Chaining Rules (Bookstore)
- Collaborative-filtering style book recommendations for new and existing users
- Author and genre qualification, and similarity-based scoring
- Order and item discounting (quantity, expensive items, educational genres, order size)

## Backward-Chaining Rules (Bank)
- Loan-approval decision tree implemented as nested Drools **queries** (employment, income, credit history, risk, age limits)
- Risk scoring assembled from individual risk-factor rules
- **Explainability** — every decision produces readable decision-reason facts shown to the user

## Classification & Fraud Detection
- Book classification rules (new / popular / good / bad / neutral ratings) that drive recommendations
- Fraud-detection rules: many small transactions, large night-time transactions, unusual amounts, new locations and impossible travel


# Subsystems


## Bookstore (bookstore-service + bookstore-frontend)
- User registration and login, and a browsable book catalog
- Purchasing with an order workflow and item/order discount rules
- Ratings and review-driven recommendation scoring
- Personalized recommendations produced by the Drools rule base
- Genre-selection onboarding for new users

## Bank (bank-service + bank-frontend)
- Users, bank accounts, payment cards and transactions
- Card payment processing
- Loan requests with automated approval/rejection and detailed reasons
- Risk-level classification (LOW / MEDIUM / HIGH / VERY_HIGH)
- Fraud-detection dashboard and package-account features


# Technology Stack


## Backend
- Java 21
- Spring Boot 3.2.8
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- **Drools** rule engine (drools-core, drools-compiler, drools-mvel)


## Knowledge Engineering
- `.drl` rule files: forward chaining, backward-chaining queries, accumulate blocks and salience control


## Frontend
- React 18
- Vite build tooling
- React Router


## Data & Security
- PostgreSQL (production) / H2 (tests) via JPA
- JWT authentication (jjwt) and bCrypt password hashing
- Lombok


## Build
- Maven multi-module build (`common`, `bookstore-service`, `bank-service`)


# Architecture & Design


The project follows enterprise and knowledge-engineering principles:


- **Separation of rules from code** — all logic lives in external `.drl` files, keeping the knowledge base portable and maintainable
- Modular Maven multi-module structure with a shared `common` module
- Layered Spring architecture (controller → service → repository)
- Independent subsystems, each with its own backend and frontend
- Explainable rule design that produces decision reasons for end users
- Reusable fact, service and repository components across both subsystems


# Project Goals


The project was created to demonstrate:


- expert-system and rule-based development with Drools
- forward-chaining (production-system) inference
- backward-chaining (goal-driven) queries
- separation of business knowledge from application code
- explainable decision-making in a real domain
- full-stack integration of Java backends and React frontends


# Development Focus


The project emphasizes:


- a well-structured, maintainable rule knowledge base
- correctness of inference through unit and integration tests
- readable, explainable outcomes for end users
- modular, reusable system design
- real-world bookstore and banking workflows


# Notes


Drools Knowledge-Based System is an academic project intended to model real-world expert-system development — authoring and reasoning over explicit knowledge, integrating a rule engine into an enterprise backend, and delivering explainable decisions through modern web frontends. The repository layout is `/bookstore-service`, `/bank-service`, `/bookstore-frontend`, `/bank-frontend` and the shared `/common` module.
