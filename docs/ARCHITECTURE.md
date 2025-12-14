# 🏗️ CrowdShield Architecture

## System Overview

CrowdShield is built using a microservices-inspired architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend Layer                          │
│  ┌──────────────┐              ┌──────────────┐            │
│  │ index.html  │              │  admin.html  │            │
│  │  (User UI)  │              │ (Admin UI)  │            │
│  └──────┬───────┘              └──────┬───────┘            │
│         │                             │                     │
│         └─────────────┬───────────────┘                     │
│                       │                                       │
│              WebSocket/HTTP                                  │
└───────────────────────┼───────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Layer (Spring Boot)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              REST Controllers                         │  │
│  │  - ContentController                                  │  │
│  │  - AdminController                                    │  │
│  │  - AdminDashboardController                           │  │
│  │  - RulesController                                    │  │
│  │  - AdminAuthController                                │  │
│  └───────────────────┬───────────────────────────────────┘  │
│                       │                                      │
│  ┌───────────────────▼───────────────────────────────────┐  │
│  │              Service Layer                             │  │
│  │  - ContentService      - ModerationService            │  │
│  │  - QueueService         - RuleEngineService           │  │
│  │  - AdminService         - WebSocketService             │  │
│  └───────────────────┬───────────────────────────────────┘  │
│                       │                                      │
│  ┌───────────────────▼───────────────────────────────────┐  │
│  │              Repository Layer                         │  │
│  │  - ContentRepository                                  │  │
│  │  - ModerationResultRepository                         │  │
│  │  - ModerationRuleRepository                           │  │
│  │  - AdminActionRepository                              │  │
│  └───────────────────┬───────────────────────────────────┘  │
└───────────────────────┼───────────────────────────────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
        ▼                               ▼
┌───────────────┐              ┌───────────────┐
│  PostgreSQL   │              │     Redis     │
│   Database    │              │    Queues     │
└───────────────┘              └───────┬───────┘
                                       │
                                       ▼
                              ┌───────────────┐
                              │    Worker     │
                              │   Processor   │
                              └───────┬───────┘
                                      │
                                      ▼
                              ┌───────────────┐
                              │  OpenAI API    │
                              │  (or Mock)    │
                              └───────────────┘
```

## Component Details

### 1. Frontend Layer

#### User Interface (`index.html`)
- Content submission form
- Real-time progress bar
- WebSocket client integration
- Status polling fallback
- Results display

#### Admin Interface (`admin.html`)
- Admin login form
- Dashboard with statistics
- Content management
- Rules configuration
- JWT token management

### 2. API Layer

#### Controllers
- **ContentController**: Handles content submission and status retrieval
- **AdminController**: Admin actions (override, history, fix stuck items)
- **AdminDashboardController**: Dashboard statistics and content viewing
- **AdminAuthController**: JWT-based authentication
- **RulesController**: Moderation rules management
- **HomeController**: Root endpoints and API info

### 3. Service Layer

#### ContentService
- Content CRUD operations
- Status management
- Content retrieval

#### ModerationService
- Moderation result persistence
- Status updates
- WebSocket notifications

#### QueueService
- Job enqueueing
- Queue management
- Retry logic

#### RuleEngineService
- Threshold evaluation
- Label determination
- Rule management

#### WebSocketService
- Real-time progress updates
- Status notifications
- Error messaging

#### AdminService
- Flagged content retrieval
- Admin override operations
- Moderation history

### 4. Data Layer

#### PostgreSQL Tables
- **content**: Stores submitted content
- **moderation_results**: Stores moderation scores and labels
- **moderation_rules**: Stores threshold configurations
- **admin_actions**: Stores admin override history
- **moderation_jobs**: Tracks job processing

#### Redis Queues
- **moderation:jobs**: Main queue for new jobs
- **moderation:retry**: Retry queue for failed jobs
- **moderation:dlq**: Dead-letter queue for permanently failed jobs

### 5. Worker Layer

#### ModerationWorker
- Polls Redis queues
- Processes moderation jobs
- Calls ML API
- Applies rule engine
- Updates status
- Sends WebSocket updates

### 6. External Services

#### OpenAI Moderation API
- Text moderation
- Image moderation (via URL)
- Returns toxicity, hate, sexual, violence scores

#### Mock Moderation (Fallback)
- Heuristic-based scoring
- Detects toxic patterns
- Used when API unavailable

## Data Flow

### Content Submission Flow

```
1. User submits content via UI/API
   ↓
2. ContentController receives request
   ↓
3. ContentService saves to database (status: PENDING)
   ↓
4. QueueService adds job to Redis queue
   ↓
5. WebSocketService sends initial update (10%)
   ↓
6. Response returned to user with contentId
```

### Processing Flow

```
1. ModerationWorker polls Redis queue
   ↓
2. Worker updates status to PROCESSING
   ↓
3. WebSocketService sends update (60%)
   ↓
4. MLModerationClient calls OpenAI API
   ↓
5. Receive moderation scores
   ↓
6. RuleEngineService evaluates scores
   ↓
7. ModerationService saves result
   ↓
8. ContentService updates status (SAFE/FLAGGED)
   ↓
9. WebSocketService sends final update (100%)
```

### Error Handling Flow

```
1. API call fails
   ↓
2. Check error type:
   - 429 (Rate Limit) → Fallback to mock
   - 5xx (Server Error) → Retry queue
   - 4xx (Client Error) → DLQ
   ↓
3. Retry with exponential backoff
   ↓
4. Max retries exceeded → DLQ
   ↓
5. Status updated to ERROR
```

## Security Architecture

### Authentication Flow

```
1. Admin submits credentials
   ↓
2. AdminAuthController validates
   ↓
3. JwtUtil generates JWT token
   ↓
4. Token returned to client
   ↓
5. Client includes token in Authorization header
   ↓
6. JwtAuthenticationFilter validates token
   ↓
7. SecurityContext set with admin role
   ↓
8. Request proceeds to controller
```

### Authorization

- **Public Endpoints**: Content submission, status check, rules (read)
- **Admin Endpoints**: Dashboard, content management, rules (write), admin actions
- **JWT Validation**: All admin endpoints require valid JWT token

## Scalability Considerations

### Horizontal Scaling
- Stateless API servers (JWT tokens)
- Redis queues support multiple workers
- PostgreSQL connection pooling

### Performance Optimizations
- Asynchronous job processing
- Connection pooling (Redis, PostgreSQL)
- Efficient database queries with indexes
- WebSocket for real-time updates (reduces polling)

### Monitoring Points
- Queue lengths (Redis)
- Worker processing time
- API response times
- Database query performance
- WebSocket connection count

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL 12+
- **Cache/Queue**: Redis 6+
- **WebSocket**: STOMP over SockJS
- **Security**: Spring Security + JWT
- **Build Tool**: Maven 3.6+
- **ML API**: OpenAI Moderation API

