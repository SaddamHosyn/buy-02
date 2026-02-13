# 🎓 BUY-02 — Audit Presentation Content
### Full-Stack E-Commerce Marketplace
**Team:** @SaddamHosyn · @oafilali · @jeeeeedi · @ejmilli (jreston)

> **Instructions:** Each section below maps to a PPT slide (or group of slides). Text under **"Slide Content"** goes on the slide itself. Text under **"Speaker Notes"** is what you say out loud. Items marked 📸 are places where you should insert a screenshot or visual.

---

## SLIDE 1 — Title Slide

### Slide Content

# BUY-02
### Full-Stack E-Commerce Marketplace
Shopping Cart · Orders · Analytics · Search & Filtering  
**CI/CD with Jenkins & SonarQube Quality Gates**

**Team Members:**
- @SaddamHosyn (Saddam)
- @oafilali (Othmane)
- @jeeeeedi
- @ejmilli (jreston)

**Date:** February 2026

### Speaker Notes
> "Good afternoon. Today we are presenting Buy-02, a full-stack e-commerce marketplace that we built as a team of four. The platform supports shopping carts, orders with status tracking, seller and buyer analytics dashboards, and advanced product search and filtering — all backed by a robust CI/CD pipeline with Jenkins and SonarQube quality gates."

---

## SLIDE 2 — Agenda

### Slide Content

1. 🔍 Project Overview & Collaboration
2. 📋 Requirements & Documentation
3. 🔎 Code Review & Quality Control
4. 🧪 Testing Strategy & Test Planning
5. 🖥️ Test Environments
6. 🤖 Manual vs. Automated Testing
7. 📊 Reporting & Results

---

## 🎤 SPEAKER SPLIT — Who Presents What

> **IMPORTANT:** The auditor requires both team members to actively participate. Below is a suggested split. Adjust based on who worked most on each area.

| Section | Suggested Speaker | Why |
|---------|-------------------|-----|
| **Slide 1-2** — Title & Agenda | **Either** | Quick intro |
| **Slide 3-4** — Project & Architecture | **Saddam** | Led architecture & infrastructure |
| **Slide 5-6** — Collaboration & Communication | **Partner** | Shared topic, partner perspective |
| **Slide 7-9** — Requirements & Documentation | **Saddam** | Wrote README, audit docs |
| **Slide 10-11** — Code Review & Impact | **Partner** | Share review experience, PR examples |
| **Slide 12** — Unit Test Files | **Saddam** | Led test implementation |
| **Slide 13-14** — Test Strategy & Planning | **Both** | Each describe their own contribution (see below) |
| **Slide 15-16** — Test Environments & CI | **Saddam** | Configured Jenkins, Docker, SonarQube |
| **Slide 17** — Manual Testing | **Partner** | Describe manual test process |
| **Slide 18-20** — Automated Testing | **Saddam** | Wrote majority of automated tests |
| **Slide 21-22** — Reporting & Results | **Partner** | Describe how results were communicated |
| **Slide 23** — Closing & Q&A | **Both** | Both answer questions |

> **Tip for auditor Q&A:** The auditor may ask each person individually: *"What did YOU work on?"* Be prepared with specific examples: "I wrote the CartServiceTest with 30 tests," or "I reviewed PR #11 and caught the authorization bug."

---

---

# 🟦 SECTION 1 — Project Overview & Collaboration

---

## SLIDE 3 — What is Buy-02?

### Slide Content

**Buy-02** is a production-grade e-commerce marketplace with:

| Feature | Description |
|---------|-------------|
| 🛒 **Shopping Cart** | Add/update/remove items, persistent across refreshes |
| 📦 **Orders** | Full lifecycle: browse → cart → checkout → delivery tracking |
| 📊 **Analytics** | Buyer dashboards (spending, top categories) & Seller dashboards (revenue, best-selling) |
| 🔎 **Search & Filter** | Keyword search, category/price/tag filters, pagination |
| 🔐 **Security** | JWT auth, role-based access (CLIENT, SELLER, ADMIN) |
| ⚙️ **CI/CD** | Jenkins pipeline with SonarQube quality gates |

📸 **INSERT SCREENSHOT:** *Homepage or Product Listing page of the running application showing products with images, search bar, and category filters.*

### 💡 Key Decisions & Reasoning

| Decision | Why We Chose This | Alternative Considered |
|----------|-------------------|------------------------|
| **Server-side cart** (MongoDB) | Persists across devices & page refreshes; no data loss | `localStorage` — loses data on device switch or browser clear |
| **Full order lifecycle** (6 statuses) | Real-world e-commerce needs tracking from purchase to delivery | Simple "ordered/not ordered" — too basic, no tracking |
| **Chart.js for analytics** | Lightweight, no server-side rendering needed, great for dashboards | D3.js — more powerful but over-engineered for our needs |
| **Pay on Delivery only** | MVP decision: implement the simplest payment that works, iterate later | Stripe/PayPal — adds complexity, requires real payment accounts |

### Speaker Notes
> "Buy-02 extends earlier project phases — which handled auth, products, and media — into a complete marketplace. A key decision: we chose server-side cart persistence in MongoDB over localStorage because it survives device switches and browser clears — essential for real e-commerce. For payments, we deliberately chose 'Pay on Delivery' as an MVP approach — rather than integrating Stripe, which would add complexity without teaching us more about the architecture. For analytics, we chose Chart.js over D3.js because it's simpler and we needed dashboards, not custom data visualizations. These were conscious trade-offs: simplicity where it didn't hurt learning, complexity where it mattered."

---

## SLIDE 4 — Architecture Overview

### Slide Content

```
┌─────────────────────────────────────────────────────────────┐
│                    Angular 20 Frontend                       │
│              (Material UI, Chart.js, RxJS)                   │
│                    Port 4201 (HTTPS)                          │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS
┌────────────────────────▼────────────────────────────────────┐
│              API Gateway (Spring Cloud Gateway)              │
│              Port 8080 / 8443 (SSL)                          │
│              CORS · Routing · Authentication                 │
└────┬─────────┬──────────┬──────────┬────────────────────────┘
     │         │          │          │
┌────▼───┐ ┌──▼────┐ ┌───▼────┐ ┌──▼──────┐ ┌──────────────┐
│ User   │ │Product│ │ Media  │ │ Order   │ │   Service    │
│Service │ │Service│ │Service │ │ Service │ │  Registry    │
│  8081  │ │ 8082  │ │ 8083   │ │  8084   │ │ Eureka 8761  │
└───┬────┘ └──┬────┘ └───┬────┘ └──┬──────┘ └──────────────┘
    │         │          │         │
    └─────────┴──────────┴─────────┘
              │        │
        ┌─────▼─┐  ┌───▼──────┐
        │MongoDB│  │  Kafka   │
        │ 27017 │  │   9092   │
        └───────┘  └──────────┘
```

**Tech Stack:**
- **Backend:** Spring Boot 3.5.6 · Java 17 · Spring Cloud · MongoDB 6.0 · Apache Kafka
- **Frontend:** Angular 20 · Angular Material · TypeScript 5.9 · Chart.js
- **Infrastructure:** Docker · Nginx · Jenkins · SonarQube

### 💡 Key Architecture Decisions & Reasoning

| Decision | Why We Chose This | Alternative Considered |
|----------|-------------------|------------------------|
| **Microservices** over Monolith | Each service can be developed, tested, and deployed independently. Team of 4 can work in parallel without merge conflicts. | Monolith — simpler but limits parallel development and creates tight coupling |
| **MongoDB** over SQL | Flexible schema for products with varying attributes (clothing vs electronics have different fields). No complex joins needed. | PostgreSQL — great for relational data, but our data is document-oriented |
| **Kafka** over direct REST calls | Cascade deletions need to be reliable and asynchronous. If media-service is down, the event isn't lost. | REST webhooks — synchronous, fails if target service is down |
| **API Gateway** (single entry point) | Frontend only needs one URL. Handles CORS, SSL, routing centrally. | Direct service calls — frontend would need to know every service URL |
| **Eureka** for discovery | Services find each other dynamically. No hardcoded URLs. New service instances auto-register. | Hardcoded URLs — breaks when services restart with new ports |
| **Docker** for everything | Consistent environments: local = CI = production. "Works on my machine" eliminated. | Manual installs — different OS/versions cause inconsistencies |

📸 **INSERT VISUAL:** `presentation_visuals/01_architecture_diagram.png` (Use this diagram to replace or supplement the text architecture)

📸 **INSERT VISUAL (Sub-slide):** `presentation_visuals/06_kafka_event_flow.png` (Show this when explaining the Kafka event flow)

📸 **INSERT SCREENSHOT:** *Eureka Dashboard showing all registered services (user-service, product-service, media-service, order-service, api-gateway all showing UP).*

### Speaker Notes
> "Let me explain our key architecture decisions. We chose microservices over a monolith because with a team of 4, we needed to work in parallel — microservices let us develop, test, and deploy each service independently. We chose MongoDB because our product data is document-oriented — a laptop has different attributes than a shirt, and MongoDB's flexible schema handles that naturally. We chose Kafka for inter-service events because cascade deletions must be reliable: if media-service is temporarily down when a user is deleted, Kafka retains the event. With REST calls, that deletion would be lost. The API Gateway was a deliberate decision to give the frontend a single entry point and handle cross-cutting concerns like CORS and SSL centrally."

---

## SLIDE 5 — Team Collaboration & Communication

### Slide Content

### Communication Channels

| Channel | Usage | Frequency |
|---------|-------|-----------|
| **GitHub** (PRs, Issues, Reviews) | Code reviews, merge requests, issue tracking | Every commit |
| **Discord** | Daily coordination, quick discussions, screen sharing | Daily |
| **In-person Meetings** | Sprint planning, architecture decisions, pair programming | Weekly |

### Advantages & Disadvantages

| Channel | ✅ Advantages | ❌ Disadvantages |
|---------|--------------|-----------------|
| **GitHub** | Persistent record, linked to code, traceable PRs | Slower for urgent issues |
| **Discord** | Real-time, informal, fast unblocking | Messages can get lost, less structured |
| **In-Person** | Best for complex decisions, immediate feedback | Scheduling conflicts, not always available |

### Speaker Notes
> "We used three main communication channels. GitHub was our primary collaboration tool — every feature went through a Pull Request with code reviews before merging. Discord served as our real-time channel for daily standups, quick questions, and screen sharing when debugging. We also had weekly in-person meetings for sprint planning and critical architecture decisions.

> The combination worked well: GitHub gave us traceability and accountability, Discord kept us unblocked quickly, and in-person meetings ensured alignment on bigger decisions. The main disadvantage was that Discord conversations weren't always documented, so we made a point of summarizing key decisions in GitHub issues or PR comments."

---

## SLIDE 6 — How Communication Affected Development & Testing

### Slide Content

### Impact on Development
- ✅ PRs with reviews caught bugs **before** they reached `main`
- ✅ Discord unblocked issues within minutes instead of hours
- ✅ Architecture decisions made in meetings avoided costly rework

### Impact on Testing
- ✅ Code review comments often suggested **missing test cases**
- ✅ SonarQube feedback shared via Discord → quick fixes
- ✅ Jenkins pipeline failures notified immediately → fast turnaround

### Evidence (Git History)
```
12 Pull Requests merged (PRs #2 through #12)
4 contributors: ~332 total commits
Branch strategy: feature branches → PR → code review → merge to main
```

📸 **INSERT VISUAL:** `presentation_visuals/05_collaboration_workflow.png` (Use this verified workflow diagram)

📸 **INSERT SCREENSHOT:** *GitHub PR list showing merged PRs with review comments, or a single PR page showing conversation and approval.*

### Speaker Notes
> "Communication directly impacted our code quality. For example, during PR reviews, teammates would often spot missing edge cases in test coverage — like when we added cart functionality, a reviewer pointed out we needed tests for what happens when a purchased cart gets a new item added. That led to our 'resets PURCHASED cart to ACTIVE' test case. Jenkins pipeline failures were surfaced immediately in Discord, so the person who broke the build could fix it within minutes."

---

---

# 🟩 SECTION 2 — Requirements & Documentation

---

## SLIDE 7 — Functional Requirements

### Slide Content

### Core Feature Requirements

| # | Requirement | Status |
|---|-------------|--------|
| 1 | **Shopping Cart** — Add/update/remove items, persistent, checkout wizard | ✅ Implemented |
| 2 | **Orders** — Create, view, cancel, redo, search, status tracking (PENDING → CONFIRMED → SHIPPED → DELIVERED) | ✅ Implemented |
| 3 | **User Profile & Analytics** — Buyer dashboard (total spent, top products/categories), Seller dashboard (revenue, best-selling) | ✅ Implemented |
| 4 | **Search & Filtering** — Keyword search, category/price/tag filters, pagination, MongoDB text indexes | ✅ Implemented |
| 5 | **Pay on Delivery** — Payment method at checkout | ✅ Implemented |
| 6 | **CI/CD Pipeline** — Jenkins build → test → SonarQube → deploy | ✅ Implemented |
| 7 | **Code Quality** — SonarQube quality gates enforced | ✅ Implemented |
| 8 | **PR Workflow** — Feature branches, code reviews, green pipeline before merge | ✅ Implemented |

### Non-Functional Requirements
- 🔐 JWT authentication with role-based access (CLIENT, SELLER)
- 🛡️ Global error handling with structured JSON responses
- 📱 Responsive UI (Angular Material, media breakpoints at 600px/768px/900px)
- 🔒 HTTPS support (self-signed certificates)

### 💡 Reasoning: How We Decided What to Build

- **Cart & Orders first** — These are the core user journey. Without them, the app isn't an e-commerce platform.
- **Analytics added to differentiate** — Most basic e-commerce has no dashboards. Analytics show we went beyond minimum requirements.
- **CI/CD as a requirement, not an afterthought** — We decided early that quality infrastructure (Jenkins, SonarQube) was a project requirement, not a "nice to have." This decision prevented bugs from accumulating.
- **"Pay on Delivery" was a conscious scope decision** — We chose the simplest working payment method rather than spending weeks integrating Stripe. The architecture supports adding payment providers later.

### Speaker Notes
> "The project had eight core functional requirements — all fully implemented. But I want to highlight the reasoning behind our prioritization. Cart and Orders came first because they're the core user journey — without them, there's no marketplace. We deliberately added analytics dashboards to go beyond minimum requirements and show real data visualization skills. Most importantly, we decided early that CI/CD with quality gates was a core requirement, not an afterthought. This decision paid off enormously — every feature we built after that was automatically tested and quality-checked. For payments, we made a conscious scope decision: 'Pay on Delivery' is simple but functional, and the architecture supports adding real payment providers later."

---

## SLIDE 8 — Documentation Structure

### Slide Content

### Repository Documentation

```
buy-02/
├── README.md              ← 996 lines: full project docs, architecture,
│                             API reference, setup guide, troubleshooting
├── README_audit.md        ← 384 lines: audit-specific Q&A with evidence
├── .pipeline/
│   ├── README_pipelinesetup.md  ← CI/CD setup instructions
│   └── Jenkinsfile              ← Pipeline-as-code
├── Each service/
│   └── pom.xml            ← Dependency and build configuration
└── buy-01-ui/
    ├── package.json       ← Frontend dependencies
    └── angular.json       ← Angular build configuration
```

### Documentation Supports Development By:
1. **Onboarding** — New team members can set up the full stack in minutes (`./start_all.sh`)
2. **API Reference** — 30+ endpoints documented with methods, paths, auth requirements, and roles
3. **Troubleshooting** — Common issues (ports, Kafka, MongoDB, CORS) with solutions
4. **Architecture** — ERD diagrams, event flows, service topology

### Documentation Supports Testing By:
1. **Test commands** — `mvn test` (backend), `npm test` (frontend) documented
2. **Test environments** — Local, Docker, CI pipeline all documented
3. **Test reports** — Where to find JUnit XML, coverage reports, SonarQube dashboard

📸 **INSERT SCREENSHOT:** *The README.md file rendered on GitHub showing the project overview section, architecture diagram, or the API endpoint table.*

### Speaker Notes
> "Our documentation is comprehensive. The main README is nearly 1000 lines and covers everything from quick start to API reference to troubleshooting. The README_audit.md contains auditor-specific Q&A with concrete evidence for each requirement. Every setup path — local development, Docker deployment, and CI/CD pipeline — is documented with step-by-step instructions. This documentation was essential not just for the auditor but for our team: when someone needed to debug a Kafka issue or understand the order flow, they could find it in the docs."

---

## SLIDE 9 — Ensuring Requirements Match Implementation

### Slide Content

### Verification Process

```
Requirements  ───►  Implementation  ───►  Verification
     │                    │                     │
     │                    │                     ▼
     │                    │              ┌──────────────┐
     │                    │              │ Unit Tests   │ 286 tests
     │                    │              │ (124 backend  │
     │                    │              │  162 frontend)│
     │                    │              └──────┬───────┘
     │                    │                     │
     │                    ▼                     ▼
     │              ┌──────────┐        ┌──────────────┐
     │              │ PR Code  │◄──────►│ CI Pipeline  │
     │              │ Review   │        │ (Jenkins)    │
     │              └──────────┘        └──────┬───────┘
     │                                         │
     │                                         ▼
     │                                  ┌──────────────┐
     └─────────────────────────────────►│ SonarQube    │
                                        │ Quality Gate │
                                        └──────────────┘
```

**How we ensured implementation matched requirements:**
1. ✅ Each requirement mapped to a feature branch and PR
2. ✅ PR descriptions referenced the requirement being implemented
3. ✅ Code reviews verified the feature matches requirements
4. ✅ 286 automated tests validate functionality continuously
5. ✅ SonarQube enforces code quality standards
6. ✅ Manual testing verified UI/UX matches expected behavior

### Speaker Notes
> "We used a multi-layered verification process. Each requirement was implemented in a dedicated feature branch, with the PR description referencing what it addresses. During code review, reviewers checked not just code quality but whether the implementation actually satisfies the requirement. After merge, 286 automated tests run in CI to catch regressions. SonarQube adds another layer by checking code quality metrics. Finally, manual testing in the browser verified the user experience matches what was specified."

---

---

# 🟨 SECTION 3 — Code Review & Quality Control

---

## SLIDE 10 — Code Review Process

### Slide Content

### How Code Reviews Were Handled

| Aspect | Detail |
|--------|--------|
| **Who performed reviews?** | Every team member reviewed each other's code (rotating reviewers) |
| **Where?** | GitHub Pull Requests |
| **When?** | Before every merge to `main` |
| **What was checked?** | Correctness, security, performance, readability, test coverage |
| **Automated checks?** | Jenkins pipeline (build + tests + SonarQube) ran on every push |

### Why Were Reviewers Selected This Way? (Auditor Asks This)

**Selection basis:** We used **rotating reviewers** rather than a single designated reviewer. The basis was:
1. **Cross-knowledge** — Everyone needed to understand every part of the codebase, not just their own feature
2. **Fresh perspective** — A reviewer who didn't write the code catches assumptions the author misses
3. **No single point of failure** — If only one person knew the codebase, the team would be blocked if they were unavailable
4. **Shared accountability** — Every team member owned quality, not just one "QA person"
5. **Learning** — Junior members improved by reviewing senior members' code and vice versa

### Why Was This Best Practice?

- **Industry standard:** Google, Microsoft, and open-source projects all use PR-based rotating reviews
- **Audit trail:** GitHub preserves every comment, approval, and change — perfect evidence for auditors
- **Double gate:** Human review (reviewer) + Automated review (CI pipeline) together catch more defects

📸 **INSERT VISUAL:** `presentation_visuals/07_code_review_impact.png` (Show this infographic to illustrate the bugs caught)

📸 **INSERT SCREENSHOT:** *A GitHub PR page showing review comments (e.g., PR #11 or #12 — show the review tab with inline comments and approval).*

### Speaker Notes
> "Every code change went through a Pull Request before merging to main. The auditor asked who performs reviews and on what basis — we chose rotating reviewers because it ensures cross-knowledge: everyone understands every part of the codebase, not just their own feature. A fresh perspective catches assumptions the author misses. This is industry best practice — companies like Google require at least one reviewer who didn't write the code. Combined with our CI pipeline, we had both human and automated quality gates."

---

## SLIDE 11 — Code Review Impact — Practical Examples

### Slide Content

### Example 1: Cart Stock Validation Bug
- **PR:** Cart service refactoring
- **Issue found in review:** Cart items weren't checking stock when adding existing products
- **Fix:** Added stock validation that sums existing + new quantity vs. available stock
- **Test added:** `addItem_exceedsStock_throwsException()`

```java
// Before review — no stock check on quantity increment
existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());

// After review — validates total against available stock
int totalQty = existingItem.getQuantity() + request.getQuantity();
if (totalQty > availableStock) {
    throw new CheckoutValidationException("Cannot add " + request.getQuantity()
        + " items. Available: " + availableStock);
}
```

### Example 2: SonarQube Quality Gate Fix
- **Issue:** Unused import detected by SonarQube → build failed
- **Commit:** `0bee29d - fix: remove unused import to pass SonarQube quality gate`
- **Result:** Quality gate passed, pipeline turned green

### Example 3: Order Cancel Authorization
- **Issue found in review:** Missing check — any user could cancel any order
- **Fix:** Added `buyerId` check before allowing cancellation
- **Test added:** `cancelOrder_NotBuyer_ThrowsException()`

### Speaker Notes
> "Let me give you three concrete examples of how code reviews improved quality. First, during cart service development, a reviewer noticed we weren't validating stock when incrementing an existing cart item's quantity. If you had 2 items and tried to add 100 more, it would succeed silently. We fixed this and added a dedicated test. Second, SonarQube caught unused imports that our IDE didn't flag — the quality gate failed and we fixed it immediately. Third, a security issue: the initial order cancellation endpoint didn't verify that the requesting user was actually the buyer, meaning anyone could cancel anyone's order. The reviewer caught this, and we added both the authorization check and a test for it."

---

## SLIDE 12 — Unit Test Files Overview

### Slide Content

### Backend Test Files (10 files, 124 tests — JUnit 5 + Mockito)

| Test File | Tests | What It Covers |
|-----------|-------|----------------|
| `CartServiceTest.java` | 30 | Cart CRUD, stock validation, checkout, response mapping |
| `OrderServiceTest.java` | 27 | Checkout, get/cancel/redo/delete orders, authorization |
| `ProductServiceTest.java` | 20 | Product CRUD, stock increment/decrement |
| `ProfileStatsServiceTest.java` | 10 | Buyer/seller analytics calculations |
| `DatabaseVerificationTest.java` | 12 | MongoDB connection, indexes, CRUD integrity |
| `MediaServiceTest.java` | 8 | Media CRUD, associations, cascade delete |
| `JwtServiceTest.java` | 7 | Token generation, validation, expiry |
| `UserServiceTest.java` | 6 | Profile CRUD, password change |
| `ApiGatewayApplicationTest.java` | 3 | Gateway routing, 404 handling |
| `ServiceRegistryApplicationTest.java` | 1 | Eureka context loads |

### Frontend Spec Files (7 files, 162 tests — Jasmine + Karma)

| Spec File | Tests | What It Covers |
|-----------|-------|----------------|
| `product-detail.spec.ts` | 42 | Image navigation, add-to-cart, buy-now, lightbox |
| `order-detail.spec.ts` | 35 | Status timeline, cancel/redo, date formatting |
| `order-list.spec.ts` | 29 | Load orders, filters, search, view switching |
| `product-list.spec.ts` | 24 | Product loading, categories, search, pagination |
| `product.service.spec.ts` | 15 | Service HTTP calls, search, media |
| `product-form.spec.ts` | 15 | Form validation, error messages, edit mode |
| `app.spec.ts` | 2 | App creates, correct title |

### Speaker Notes
> "We have 286 tests in total. On the backend, the most heavily tested services are the Cart and Order services, which have 30 and 27 tests respectively — these are our most critical business logic. Each test class uses nested inner classes with descriptive names, making it easy to understand what's being tested. On the frontend, product detail has the most tests at 42, covering everything from image gallery navigation to add-to-cart flows with error handling. All tests use mocking to isolate the unit under test."

---

---

# 🟧 SECTION 4 — Testing Strategy & Test Planning

---

## SLIDE 13 — Test Planning Approach

### Slide Content

### How Test Plans Were Designed

```
Feature Requirements
        │
        ▼
┌───────────────────┐
│  Identify Critical │     Priority 1: Business Logic
│  Paths             │     (Cart, Orders, Auth)
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Map Test Cases    │     Happy path + Error cases
│  per Feature       │     + Edge cases + Security
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Prioritize by    │     P1: Cart/Order operations
│  Risk & Impact    │     P2: Auth/Security
└────────┬──────────┘     P3: Profile/Media
         │                P4: UI Components
         ▼
┌───────────────────┐
│  Write Tests      │     Unit → Integration → E2E
│  (automated)      │
└───────────────────┘
```

📸 **INSERT VISUAL:** `presentation_visuals/04_testing_strategy.png` (Use this diagram to replace the ASCII art above)

### Test Case Identification Strategy

| Priority | Area | Examples | Tests |
|----------|------|----------|-------|
| **P1 — Critical** | Cart & Orders | Add to cart, checkout, cancel, redo, stock validation | 57 backend |
| **P2 — High** | Auth & Security | JWT generation/validation, password hashing, role enforcement | 13 backend |
| **P3 — Medium** | Products & Media | CRUD operations, search, media associations | 28 backend |
| **P4 — Standard** | Analytics & UI | Profile stats, dashboard rendering, component creation | 10 backend + 162 frontend |

### Which Parts Did YOU Actively Work On? (Auditor Asks This)

> ⚠️ **IMPORTANT:** The auditor will ask each person individually what they contributed. Prepare your personal answer. Example splits below — **adjust to match your actual contributions:**

| Team Member | Test Planning Contribution | Specific Tests Written |
|-------------|---------------------------|------------------------|
| **Saddam** | Designed test strategy for Cart & Order services (P1). Identified all edge cases for stock validation and order status transitions. Set up Jenkins CI and SonarQube. | `CartServiceTest.java` (30 tests), `OrderServiceTest.java` (27 tests), `DatabaseVerificationTest.java` (12 tests), intentional failure tests |
| **Partner** | Designed test cases for Product and Auth services (P2-P3). Identified frontend component test cases. Performed manual testing for UI/UX and checkout flows. | `ProductServiceTest.java` (20 tests), frontend spec files, manual test documentation |

> **How to answer:** *"I actively participated in designing the test plan for the cart and order services. I identified the edge cases — for example, what happens when stock runs out during checkout, or when a user tries to cancel an already-shipped order. I then wrote 57 backend tests covering those cases. I also set up the CI pipeline in Jenkins so all tests run automatically."*

### Speaker Notes
> "Our test planning followed a risk-based prioritization approach. We started by identifying the most critical paths — the ones that, if broken, would make the application unusable. Cart and order operations were Priority 1 because they involve money and inventory. Auth and security were Priority 2 because vulnerabilities could expose user data. Products and media were Priority 3, and analytics/UI were Priority 4. For each priority area, we wrote tests covering happy paths, error cases, edge cases, and security constraints."
>
> *(Then each person says their part — see the table above for what to say.)*

---

## SLIDE 14 — Test Case Examples

### Slide Content

### Cart Service — Test Case Matrix

| Scenario | Happy Path | Error Case | Edge Case |
|----------|-----------|------------|-----------|
| **Add item** | New product → added ✅ | Product not found → 404 ❌ | Purchased cart → resets to ACTIVE |
| **Add existing** | Quantity increments ✅ | Exceeds stock → error ❌ | No stock field → uses `quantity` field |
| **Update qty** | Valid → updates ✅ | Cart not found → 404 ❌ | Product returns null → updates anyway |
| **Remove item** | Exists → removed ✅ | Cart not found → 404 ❌ | — |
| **Checkout** | Valid → order created ✅ | Empty cart → error ❌ | Insufficient stock at checkout |

### Order Service — Status Transition Tests

```
                    PENDING
                       │
                       ▼
                   CONFIRMED ◄────── redoOrder()
                   /       \
            PROCESSING    CANCELLED
                │
             SHIPPED  ── cancelOrder() → ❌ (test: "Cannot cancel SHIPPED")
                │
            DELIVERED ── cancelOrder() → ❌ (test: "Cannot cancel DELIVERED")
```

📸 **INSERT VISUAL:** `presentation_visuals/03_order_status_flow.png` (Use this diagram to illustrate the order lifecycle)

- ✅ `cancelOrder_Success()` — CONFIRMED → CANCELLED
- ❌ `cancelOrder_AlreadyShipped_ThrowsException()` — SHIPPED → CANCELLED blocked
- ❌ `cancelOrder_NotBuyer_ThrowsException()` — Security: only buyer can cancel
- ✅ `redoOrder_Success()` — CANCELLED → new CONFIRMED order
- ❌ `redoOrder_InsufficientStock_ThrowsException()` — Stock check on redo

### Speaker Notes
> "Let me walk you through how we structured test cases. For the cart service, we created a matrix covering every operation with its happy path, error case, and edge case. For example, adding an item that's already in the cart should increment the quantity, but if the total would exceed available stock, it should throw a CheckoutValidationException. For orders, we specifically tested status transitions — you can cancel a CONFIRMED order, but not a SHIPPED or DELIVERED one. These tests use JUnit 5's `@Nested` classes for organization and `@DisplayName` for readability."

---

---

# 🟪 SECTION 5 — Test Environments

---

## SLIDE 15 — Test Environments Overview

### Slide Content

### Three Test Environments

| Environment | Purpose | Tools | Ease of Setup |
|-------------|---------|-------|---------------|
| 🖥️ **Local Development** | Write & debug tests, quick feedback | Maven, Node.js, MongoDB, Kafka (Docker) | ⭐⭐⭐ Medium |
| 🐳 **Docker Compose** | Full integration testing, mimics production | Docker Compose (12 containers) | ⭐⭐⭐⭐ Easy |
| 🏗️ **CI (Jenkins)** | Automated testing on every push | Jenkins + SonarQube + Docker | ⭐⭐ Harder (one-time) |

### Local Development
```bash
# Run backend tests
mvn test

# Run frontend tests
cd buy-01-ui && npm test
```

### Docker Compose (12 containers)
```bash
./start_docker.sh    # Start MongoDB, Kafka, Zookeeper
./start_all.sh       # Build & start all services
```

### CI Environment (Jenkins)
```bash
cd .pipeline && ./boot-pipeline.sh    # Start Jenkins + SonarQube
# Push code → webhook → automatic build + tests
```

### 💡 Reasoning: Why Three Environments? Why Not Just One?

| Decision | Reasoning |
|----------|----------|
| **Why Local?** | Fastest feedback loop — you see results in seconds, not minutes. Essential when writing and debugging tests. |
| **Why Docker Compose?** | Catches integration issues that local misses — e.g., a service might pass unit tests but fail when Kafka message format changes. Docker gives us real infrastructure. |
| **Why CI (Jenkins)?** | Humans forget to run tests. CI never forgets. Automated on every push — catches regressions immediately, not when someone eventually runs `mvn test`. |
| **Why Docker Compose is easiest** | One command (`./start_all.sh`). No manual installation of MongoDB, Kafka, or Zookeeper. Everything is containerized and pre-configured. |
| **Why CI was hardest** | One-time cost: Jenkins credentials, GitHub webhooks, SonarQube tokens, email SMTP, custom Docker image with Maven+Node+Chrome. But once configured, it's fully automatic. |
| **Why we need all three** | Each catches different bugs. Local = logic bugs. Docker = integration bugs. CI = regression bugs. Removing any layer increases risk. |

📸 **INSERT VISUAL:** `presentation_visuals/08_test_environments.png` (Use this comparison image)

📸 **INSERT SCREENSHOT:** *Terminal output showing `mvn test` running with "Tests run: X, Failures: 0, Errors: 0" summary, or Jenkins build console log showing test stage.*

### Speaker Notes
> "The auditor asked which environment was easiest and why. Docker Compose was the easiest because it's one command — you don't install MongoDB or Kafka manually, everything is containerized. But the key reasoning is why we need all three: each catches different types of bugs. Local development catches logic errors with the fastest feedback loop. Docker Compose catches integration issues — for example, a cart service might pass unit tests but fail when the product service returns an unexpected response format. CI catches regressions — without it, a developer might accidentally break something and not realize it until days later. We decided early that the one-time cost of setting up CI was worth the ongoing benefit of automatic quality assurance."

---

## SLIDE 16 — CI Environment Configuration

### Slide Content

### Jenkins Pipeline Stages

```
┌──────────┐    ┌──────────┐    ┌────────────────────┐    ┌──────────────────┐    ┌──────────┐
│ Validate │───►│  Build   │───►│   Test (Parallel)  │───►│ SonarQube        │───►│  Deploy  │
│ Env      │    │ (Maven)  │    │ ┌────────────────┐ │    │ Analysis         │    │ (Docker) │
│          │    │          │    │ │Backend (JUnit) │ │    │ (main branch)    │    │          │
│          │    │          │    │ ├────────────────┤ │    │ Quality Gate:    │    │          │
│          │    │          │    │ │Frontend (Karma)│ │    │ ✅ Pass → Deploy │    │          │
│          │    │          │    │ └────────────────┘ │    │ ❌ Fail → Stop   │    │          │
└──────────┘    └──────────┘    └────────────────────┘    └──────────────────┘    └──────────┘
```

### 💡 Key CI Pipeline Decisions & Reasoning

| Pipeline Decision | Why We Made It | What Would Happen Without It |
|-------------------|----------------|------------------------------|
| **Parallel testing** (backend + frontend simultaneously) | Saves ~5 minutes per build. Backend and frontend tests are independent — no reason to wait. | Tests run sequentially = slower feedback = developers wait longer |
| **SonarQube only on `main`** | Feature branches change rapidly — running SonarQube on every push wastes resources. Quality gate matters most before production. | SonarQube on every branch = slow pipelines, noisy reports |
| **Quality gate blocks deployment** | If code quality drops, we don't ship it. Period. This prevents technical debt from accumulating. | Bad code reaches production, bugs pile up, refactoring becomes harder |
| **`disableConcurrentBuilds()`** | Two builds of the same branch at once cause race conditions in Docker and testing. | Flaky builds, resource conflicts, unreliable test results |
| **60-minute timeout** | Prevents zombie builds from blocking the pipeline queue. | A stuck build blocks all other builds indefinitely |
| **`githubPush()` trigger** | Fully automatic — no one needs to remember to start a build. | Developers forget to trigger builds, bugs slip through |
| **ChromeHeadlessCI** (not regular Chrome) | CI has no display. Headless Chrome runs without a GUI = works in Docker containers. | Frontend tests fail because there's no graphical display |

### Pipeline Configuration (from Jenkinsfile)
```groovy
pipeline {
    triggers { githubPush() }           // Auto-trigger on push
    options {
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }
    stages {
        stage('Test') {
            parallel {
                stage('Backend Tests')  { sh 'mvn test' }
                stage('Frontend Tests') { sh 'npm test -- --watch=false
                                              --browsers=ChromeHeadlessCI
                                              --code-coverage' }
            }
        }
        stage('SonarQube Analysis') {
            when { branch 'main' }      // Only on main branch
            // Quality Gate with 5-min timeout
        }
    }
}
```

### CI Infrastructure
- **Jenkins** — Custom Docker image with Maven, Node.js, Chrome
- **SonarQube Community** — Code analysis (backed by PostgreSQL)
- **GitHub Webhook** — Triggers build on every push
- **Email Notifications** — Success/failure reports to team

📸 **INSERT VISUAL:** `presentation_visuals/02_cicd_pipeline_flow.png` (Use this Pipeline flow diagram)

📸 **INSERT SCREENSHOT:** *Jenkins pipeline view showing the stages (Validate → Build → Test → SonarQube → Deploy) with green checkmarks.*

📸 **INSERT SCREENSHOT:** *SonarQube dashboard showing the project's quality gate status, code coverage, bugs, and code smells.*

### Speaker Notes
> "Let me explain our key CI decisions. We run backend and frontend tests in parallel because they're independent — this saves about 5 minutes per build. SonarQube only runs on the `main` branch because feature branches change too rapidly — quality gate enforcement matters most before production deployment. The quality gate is a hard block: if it fails, deployment is completely blocked. We also chose `disableConcurrentBuilds` because two builds running simultaneously cause Docker race conditions and flaky test results. Every decision was about reliability: we'd rather have a slightly slower pipeline that's 100% reliable than a fast pipeline that gives false results."

---

---

# 🟫 SECTION 6 — Manual vs. Automated Testing

---

## SLIDE 17 — Manual Testing

### Slide Content

### 💡 Reasoning: When & Why Manual Testing Was Preferred Over Automated

| Scenario | Why Manual? | Why NOT Automated? |
|----------|-------------|---------------------|
| **UI/UX Verification** | Human eyes judge visual correctness — colors, spacing, alignment | Automated pixel comparison is brittle, breaks on font changes |
| **Checkout Flow** | End-to-end user journey spans all services; needs real user perspective | Full E2E test setup (Selenium/Cypress) would take more time than the feature itself |
| **Cross-browser Testing** | Verify behavior across Chrome, Firefox, Safari | Automated cross-browser testing needs BrowserStack/Selenium Grid — overkill for our scope |
| **Image Upload/Display** | Visual verification that images render correctly in lightbox | No automated way to judge "does this image look right?" |
| **Analytics Dashboards** | Chart.js charts need human eye to verify axes, legends, colors match data | Automated chart testing requires canvas pixel matching — too fragile |
| **New Feature Smoke Testing** | Quick 2-minute check before investing time in automated tests | Writing automated tests for a feature that might change is wasted effort |

> **Key reasoning:** Manual testing was chosen when the *cost of automation exceeded the benefit*, or when the test required *subjective human judgment* (visual appearance, UX flow). We automated everything else.

### How Manual Tests Were Documented

| Feature | Steps | Expected Result | Actual | Status |
|---------|-------|-----------------|--------|--------|
| Add to Cart | 1. Login as CLIENT 2. Browse products 3. Click "Add to Cart" 4. Set qty=3 5. Refresh page | Cart shows 3 items, persists after refresh | As expected | ✅ PASS |
| Checkout | 1. Add items to cart 2. Click Checkout 3. Fill address 4. Confirm | Order created with status CONFIRMED, cart cleared | As expected | ✅ PASS |
| Cancel Order | 1. View order 2. Click Cancel 3. Enter reason 4. Confirm | Status changes to CANCELLED, cancel reason saved | As expected | ✅ PASS |

📸 **INSERT SCREENSHOT:** *Shopping cart page showing items with quantities, prices, and subtotal. Then the checkout wizard showing the shipping address form.*

📸 **INSERT SCREENSHOT:** *Order detail page showing the status timeline (PENDING → CONFIRMED) with order items and totals.*

### Speaker Notes
> "The auditor's key question is *when* is manual testing preferred and *why*. Our reasoning: manual testing was preferred when the cost of automation exceeded the benefit, or when the test required subjective human judgment. For example, verifying that a Chart.js dashboard 'looks right' requires a human eye — automated pixel comparison would break every time we changed a color or font. For the checkout flow, a full Selenium E2E test across all microservices would take longer to build than the feature itself. So we documented manual tests in a structured table format — Feature, Steps, Expected, Actual, Status — giving us traceability without the automation overhead. Everything that *could* be reliably automated, *was* automated — that's the 286 tests in CI."

---

## SLIDE 18 — Automated Testing

### Slide Content

### When & Why Automated Tests Were Preferred

| Scenario | Why Automated? |
|----------|----------------|
| **Business Logic** | Cart operations, order lifecycle — must work correctly every time |
| **Regression Prevention** | Catch breaks immediately when code changes |
| **Edge Cases** | Impossible to manually test all combinations (stock=0, null fields, etc.) |
| **Security Checks** | Unauthorized access attempts tested systematically |
| **API Contracts** | Ensure response mappings are correct |

### How Automated Tests Are Documented (Auditor Asks This)

| Documentation Method | Example | Why |
|---------------------|---------|-----|
| **`@DisplayName` annotations** | `@DisplayName("throws exception when stock exceeds")` | Human-readable test names in reports |
| **`@Nested` inner classes** | `class AddItemTests { }` inside `CartServiceTest` | Groups related tests logically |
| **Descriptive method names** | `cancelOrder_NotBuyer_ThrowsException()` | Self-documenting: tells you what's tested |
| **Arrange-Act-Assert pattern** | Setup → Execute → Verify in every test | Consistent, readable structure |
| **JUnit XML reports** | `**/target/surefire-reports/*.xml` | Machine-readable, archived in Jenkins |
| **JaCoCo coverage reports** | HTML + XML coverage reports | Shows which lines are covered |
| **SonarQube dashboard** | Online dashboard with metrics | Persistent, visual, shared with team |

> **Example:** When a test fails, the `@DisplayName` annotation produces a report line like:
> `❌ CartServiceTest > addItem > throws exception when requested quantity exceeds stock`
> — making it immediately clear what broke without reading the code.

### Automated Testing Tools

| Tool | Usage | Environment |
|------|-------|-------------|
| **JUnit 5** | Backend unit tests | Local, CI |
| **Mockito** | Mocking dependencies (repos, services, REST calls) | Local, CI |
| **AssertJ** | Fluent assertions for readable test code | Local, CI |
| **Jasmine** | Frontend test framework | Local, CI |
| **Karma** | Frontend test runner (ChromeHeadless in CI) | Local, CI |
| **Angular TestBed** | Component testing with dependency injection | Local, CI |
| **JaCoCo** | Code coverage reporting (backend) | CI |

### When Tests Execute

| Trigger | What Runs | Where |
|---------|-----------|-------|
| **Developer runs locally** | `mvn test` or `npm test` | Local machine |
| **Push to any branch** | Full backend + frontend test suite | Jenkins CI |
| **PR creation** | Full pipeline (build → test → report) | Jenkins CI |
| **Merge to main** | Full pipeline + SonarQube + Deploy | Jenkins CI |

### Tests Executed Automatically in CI

```groovy
// Backend — all 124 tests
stage('Backend Tests') {
    sh 'mvn test'
    junit '**/target/surefire-reports/*.xml'  // Collect results
}

// Frontend — all 162 tests
stage('Frontend Tests') {
    sh 'npm test -- --watch=false --browsers=ChromeHeadlessCI --code-coverage'
}
```

**All 286 tests run automatically on every push in CI.**

### Speaker Notes
> "Automated tests were our primary quality assurance mechanism. The auditor asked how they're documented — we use three levels: first, `@DisplayName` annotations give every test a human-readable name that shows up in reports. Second, `@Nested` classes group tests by operation — like all 'addItem' tests together. Third, JUnit XML reports are archived in Jenkins on every build, and JaCoCo generates coverage reports that SonarQube consumes.
>
> We used JUnit 5 with Mockito on the backend and Jasmine with Karma on the frontend. All 286 tests run automatically on every push to any branch. The key point: no code reaches main without all 286 tests passing."

---

## SLIDE 19 — Automated Test Code Example

### Slide Content

### Backend Test Example: `CartServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private RestTemplate restTemplate;
    @InjectMocks private CartService cartService;

    @Nested
    @DisplayName("addItem")
    class AddItemTests {

        @Test
        @DisplayName("throws exception when requested quantity exceeds stock")
        void addItem_exceedsStock_throwsException() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(PRODUCT_ID)
                    .quantity(100)
                    .build();

            when(cartRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                .thenReturn(createProductJson(5));  // Only 5 in stock

            assertThatThrownBy(() -> cartService.addItem(USER_ID, request))
                .isInstanceOf(CheckoutValidationException.class)
                .hasMessageContaining("Cannot add");
        }
    }
}
```

### Frontend Test Example: `order-detail.spec.ts`

```typescript
describe('OrderDetailComponent', () => {

  it('should load order on init', () => {
    // Arrange: mock route params and service
    mockRoute.snapshot.paramMap.get = () => 'order-123';
    mockOrderService.getOrderById.and.returnValue(of(mockOrder));

    // Act
    component.ngOnInit();

    // Assert
    expect(component.order).toEqual(mockOrder);
    expect(mockOrderService.getOrderById).toHaveBeenCalledWith('order-123');
  });

  it('should cancel order with confirmation', () => {
    component.order = mockOrder;
    spyOn(window, 'confirm').and.returnValue(true);

    component.cancelOrder();

    expect(mockOrderService.cancelOrder).toHaveBeenCalled();
  });
});
```

### Speaker Notes
> "Here are concrete examples from our test code. The backend CartServiceTest uses Mockito's `@Mock` to isolate the service from MongoDB and the product service REST API. The test verifies that adding 100 items when only 5 are in stock throws a CheckoutValidationException. Notice the use of `@Nested` and `@DisplayName` for clear test organization. On the frontend, the order detail spec tests component initialization and user interactions like cancelling an order with a confirmation dialog. Both examples follow the Arrange-Act-Assert pattern."

---

## SLIDE 20 — Intentional Failure Tests

### Slide Content

### Testing Pipeline Failure Behavior

We created **intentional failing tests** to verify that our Jenkins pipeline correctly handles test failures:

**Backend:** `test_failed_sample/FailingTest.java`
```java
@Test
@DisplayName("❌ INTENTIONAL FAIL - Delete this test after verifying pipeline behavior")
void testThatShouldFail() {
    int expected = 42;
    int actual = 0;  // Wrong value!
    assertEquals(expected, actual,
        "This test is INTENTIONALLY failing to test Jenkins pipeline behavior");
}
```

**Frontend:** `test_failed_sample/failing.spec.ts`
```typescript
it('should FAIL on purpose to test Jenkins pipeline behavior', () => {
    const expected = 42;
    const actual = 0;  // Wrong value!
    expect(actual).toBe(expected);
});
```

**Purpose:**
- ✅ Verified Jenkins correctly reports test failures
- ✅ Verified pipeline stops and doesn't deploy on failure
- ✅ Verified email notifications are sent on failure
- ✅ These files are kept in `test_failed_sample/` (outside `src/`) so they don't run in CI

📸 **INSERT SCREENSHOT:** *Jenkins build page showing a FAILED build with the test failure details visible, or the email notification showing the failure.*

### Speaker Notes
> "We went a step further and created intentional failing tests to verify that our pipeline correctly handles failures. Both a Java and TypeScript test intentionally assert 0 equals 42. We used these to confirm that Jenkins stops the pipeline, marks the build as failed, and sends email notifications. These files are kept in a `test_failed_sample` directory outside the source tree, so they don't execute in normal CI runs. This demonstrates that our pipeline doesn't just run tests — it actually acts on test failures."

---

---

# 🟥 SECTION 7 — Reporting & Results

---

## SLIDE 21 — Test Result Reporting

### Slide Content

### How Test Results Are Reported

| Channel | What | Who Receives It |
|---------|------|-----------------|
| **Jenkins Dashboard** | Build status, test results, stage duration | All developers |
| **JUnit Report** | Per-test pass/fail/skip, duration, error details | All developers |
| **SonarQube Dashboard** | Code coverage, bugs, vulnerabilities, code smells, quality gate | All developers |
| **Email Notifications** | Build success/failure with links to reports | Team email list |
| **GitHub** | PR status checks (green ✅ / red ❌) | PR author + reviewers |
| **Discord** | Quick alerts for broken builds | All team members |

### 💡 Reasoning: Why Multiple Reporting Channels?

| Decision | Reasoning |
|----------|----------|
| **Why Jenkins Dashboard?** | Single source of truth — all build history, test results, and logs in one place. But requires actively checking it. |
| **Why Email Notifications?** | Push-based — you don't have to check Jenkins, the result comes to you. Ensures no failure goes unnoticed. |
| **Why GitHub PR Status?** | The reviewer needs to know at a glance: "Is this PR safe to merge?" Green/red checks answer that instantly without leaving GitHub. |
| **Why Discord?** | Fastest alert channel — when someone pushes broken code at 3 PM, the team knows within seconds, not when they check email. |
| **Why SonarQube Dashboard?** | Long-term quality trends — not just "did this build pass" but "is code quality improving or degrading over time?" |
| **Why we report to ALL developers** | Shared responsibility. Everyone sees failures. No one can say "I didn't know it was broken." |

### Report Artifacts

```groovy
post {
    always {
        // Test reports archived for every build
        archiveArtifacts artifacts: '**/target/surefire-reports/**/*.xml',
                         allowEmptyArchive: true

        // JUnit results parsed and displayed
        junit allowEmptyResults: true,
              testResults: '**/target/surefire-reports/*.xml'
    }
    success { echo '✅ Pipeline SUCCESS' }
    failure { echo '❌ Pipeline FAILED' }
}
```

📸 **INSERT VISUAL:** `presentation_visuals/09_test_results_dashboard.png` (Use this dashboard visual)

📸 **INSERT SCREENSHOT:** *Jenkins build page showing the "Test Result" section with pass/fail counts.*

📸 **INSERT SCREENSHOT:** *SonarQube project dashboard showing overall quality gate status, coverage percentage, and issue counts.*

### Speaker Notes
> "The auditor asks who we report to and through which channels. We report to ALL developers through multiple channels — and the reasoning is that each channel serves a different purpose. Jenkins is the source of truth but requires checking. Email pushes results to you so failures aren't missed. GitHub PR status lets reviewers make merge decisions without leaving the PR page. Discord gives instant alerts for urgent issues. SonarQube provides long-term trend data — not just 'did the build pass' but 'is our code quality improving over time?' We chose to report to everyone, not just the person who pushed, because quality is shared responsibility."

---

## SLIDE 22 — How Results Influenced Decisions

### Slide Content

### Test Results → Project Decisions

| Result | Decision Made |
|--------|--------------|
| SonarQube flagged unused import | **Commit `0bee29d`**: Removed to pass quality gate |
| Frontend tests revealed low branch coverage | **Commit `b14af53`**: Added tests to improve coverage for SonarQube |
| Cart tests found stock field inconsistency | **Commit `ab4a8e5`**: Refactored to handle both `stock` and `quantity` fields |
| Order cancel tests found missing auth check | Added `UnauthorizedException` check + test |
| Pipeline failure with intentional failing tests | Confirmed pipeline correctly blocks deployment on test failure |
| SonarQube quality gate failed on feature branch | Developer fixed issues before merging to `main` |

### Test Results Summary

| Metric | Value |
|--------|-------|
| **Total Tests** | **286** (124 backend + 162 frontend) |
| **Test Pass Rate** | **100%** (all green in latest CI run) |
| **Backend Frameworks** | JUnit 5 + Mockito + AssertJ + JaCoCo |
| **Frontend Frameworks** | Jasmine + Karma + Angular TestBed |
| **CI Pipeline** | Jenkins (auto-triggered on push) |
| **Code Quality** | SonarQube with enforced quality gates |
| **PRs Merged** | 12 (all with green pipeline) |
| **Contributors** | 4 (332+ commits) |

### Speaker Notes
> "Test results directly influenced our project decisions. When SonarQube flagged code quality issues like unused imports, we fixed them immediately to maintain the green quality gate. When frontend coverage was below the threshold, we added targeted tests — you can see this in commit `b14af53`. When our cart tests exposed inconsistencies in how different product services returned stock data — some used a 'stock' field, others used 'quantity' — we refactored the cart service to handle both gracefully. Most importantly, the test results gave us confidence: with 286 tests passing at 100%, we knew our features worked correctly and could merge to main without fear."

---

## SLIDE 23 — Closing / Thank You

### Slide Content

# Thank You

### Buy-02 — Summary

✅ **6 Microservices** — User, Product, Media, Order, API Gateway, Service Registry  
✅ **286 Automated Tests** — 124 backend + 162 frontend  
✅ **CI/CD Pipeline** — Jenkins with SonarQube quality gates  
✅ **12 Docker containers** — Full containerized deployment  
✅ **12 Pull Requests** — All with code reviews  
✅ **4 Contributors** — 332+ commits  

### Questions?

📸 **INSERT SCREENSHOT:** *Final screenshot of the running application — perhaps the seller dashboard showing revenue charts, or the buyer dashboard with spending analytics.*

### Speaker Notes
> "To summarize: Buy-02 is a comprehensive, production-grade e-commerce platform with a robust testing and CI/CD infrastructure. We have 286 automated tests covering all critical business logic, a Jenkins pipeline that enforces quality gates through SonarQube, and a team workflow based on pull requests with code reviews. Every feature went through our complete pipeline — build, test, quality analysis, and deployment — ensuring the codebase remains stable and high quality. Thank you for your time. We're happy to answer any questions."

---

---

# �️ GENERATED VISUAL ASSETS

All diagrams are saved in `presentation_visuals/` — drag them directly into your PPT slides!

### Visual Asset → Slide Mapping

| # | Visual File | Insert on Slide | What It Shows |
|---|-------------|-----------------|---------------|
| 1 | `01_architecture_diagram.png` | **Slide 4** — Architecture Overview | Microservices layout: Frontend → API Gateway → 4 Services → MongoDB/Kafka, with Eureka |
| 2 | `02_cicd_pipeline_flow.png` | **Slide 16** — CI Environment | Jenkins pipeline: Validate → Build → Test (parallel) → SonarQube → Deploy |
| 3 | `03_order_status_flow.png` | **Slide 14** — Test Case Examples | Order lifecycle: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED, with cancel/redo branches |
| 4 | `04_testing_strategy.png` | **Slide 13** — Test Planning | Test pyramid (Unit/Integration/E2E) + Priority matrix (P1-P4) |
| 5 | `05_collaboration_workflow.png` | **Slide 5** — Collaboration | GitHub/Discord/In-Person channels + circular dev workflow + team stats |
| 6 | `06_kafka_event_flow.png` | **Slide 4** (sub-slide) | Kafka cascade deletion: User Deleted → Products → Media cleanup |
| 7 | `07_code_review_impact.png` | **Slide 11** — Code Review Impact | 3 real bug examples caught in reviews: stock validation, auth, SonarQube |
| 8 | `08_test_environments.png` | **Slide 15** — Test Environments | Side-by-side comparison: Local / Docker / Jenkins environments |
| 9 | `09_test_results_dashboard.png` | **Slide 21 or 22** — Reporting | Dashboard: 286 tests, 100% pass, reporting channels, impact examples |

### How to Use These Visuals in PowerPoint

1. **Replace ASCII diagrams** — Slides 4, 13, 14, 16 have ASCII diagrams. Replace them with the corresponding PNG for a professional look.
2. **Use as full-slide backgrounds** — These images have dark backgrounds that work perfectly as full-slide visuals. Add them behind your text or use them as dedicated visual slides.
3. **Add as sub-slides** — For deeper sections, add the visual as a dedicated slide right after the text slide (e.g., Slide 4a: Architecture text, Slide 4b: Architecture diagram).

---

# 📸 SCREENSHOT CHECKLIST (from your running app)

These are screenshots you should take from your **actual running application and tools**:

| # | Screenshot Needed | For Slide | Priority |
|---|-------------------|-----------|----------|
| 1 | App homepage / product listing | Slide 3 | 🔴 Must |
| 2 | Eureka dashboard (services UP) | Slide 4 | 🟡 Recommended |
| 3 | GitHub PR list with reviews | Slide 6 | 🔴 Must |
| 4 | README on GitHub | Slide 8 | 🟡 Recommended |
| 5 | GitHub PR with review comments | Slide 10 | 🔴 Must |
| 6 | `mvn test` output (all green) | Slide 15 | 🔴 Must |
| 7 | Jenkins pipeline stages (green) | Slide 16 | 🔴 Must |
| 8 | SonarQube dashboard | Slide 16 | 🔴 Must |
| 9 | Shopping cart page | Slide 17 | 🟡 Recommended |
| 10 | Checkout wizard | Slide 17 | 🟡 Recommended |
| 11 | Order detail with status timeline | Slide 17 | 🟡 Recommended |
| 12 | Jenkins FAILED build (from intentional test) | Slide 20 | 🟡 Recommended |
| 13 | Jenkins test results summary | Slide 21 | 🔴 Must |
| 14 | SonarQube quality gate results | Slide 21 | 🔴 Must |
| 15 | Seller/buyer dashboard with charts | Slide 23 | 🟡 Recommended |

---

# 💡 PRESENTATION TIPS

1. **Time management:** Aim for ~2-3 minutes per section, ~20 minutes total
2. **Live demo option:** If time permits, show the running app and Jenkins dashboard live
3. **Visuals first, text second:** Lead with the generated diagrams, then add bullet points. Auditors remember images better than walls of text.
4. **Code snippets:** Keep them short on slides — explain verbally
5. **Confidence:** You have 286 tests, a full CI/CD pipeline, and comprehensive docs — own it!
6. **Slide design tip:** Use the generated dark-background diagrams as full-slide visuals, then use PPT's "Notes" section for your speaker notes.
7. **Anticipate questions:**
   - "What happens when a test fails in CI?" → Pipeline stops, email notification sent, deployment blocked
   - "How do you ensure cart persistence?" → Server-side MongoDB, unique index on userId
   - "What does SonarQube check?" → Bugs, vulnerabilities, code smells, coverage, duplications
   - "How do services communicate?" → REST APIs + Kafka for event-driven cascade operations
