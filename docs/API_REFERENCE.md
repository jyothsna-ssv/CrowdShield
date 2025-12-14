# 📡 CrowdShield API Reference

Complete API documentation for all endpoints.

## Base URL

```
http://localhost:8080
```

## Authentication

Admin endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer {jwt-token}
```

## Content Endpoints

### Submit Text Content

Submit text content for moderation.

**Endpoint:** `POST /api/content/text`

**Request:**
```json
{
  "user_id": "user123",
  "text": "Your text content here"
}
```

**Response:**
```json
{
  "contentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Content submitted for moderation"
}
```

**Status Codes:**
- `200 OK`: Content submitted successfully
- `400 Bad Request`: Invalid request body
- `429 Too Many Requests`: Rate limit exceeded

---

### Submit Image Content

Submit image URL for moderation.

**Endpoint:** `POST /api/content/image`

**Request:**
```json
{
  "user_id": "user123",
  "image_url": "https://example.com/image.jpg"
}
```

**Response:**
```json
{
  "contentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Content submitted for moderation"
}
```

---

### Get Content Status

Retrieve moderation status and results for a content item.

**Endpoint:** `GET /api/content/{contentId}`

**Response:**
```json
{
  "contentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SAFE",
  "userId": "user123",
  "contentType": "TEXT",
  "scores": {
    "toxicity": 0.1,
    "hate": 0.05,
    "sexual": 0.02,
    "violence": 0.01
  },
  "label": "SAFE",
  "createdAt": "2025-12-13T20:00:00"
}
```

**Status Codes:**
- `200 OK`: Content found
- `404 Not Found`: Content not found

**Status Values:**
- `PENDING`: Content submitted, waiting for processing
- `PROCESSING`: Currently being analyzed
- `SAFE`: Moderation passed
- `FLAGGED`: Content flagged as inappropriate
- `ERROR`: Processing failed

---

## Admin Authentication

### Login

Authenticate as admin and receive JWT token.

**Endpoint:** `POST /api/admin/auth/login`

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "authenticated": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Status Codes:**
- `200 OK`: Login successful
- `401 Unauthorized`: Invalid credentials

---

### Check Auth Status

Check if current token is valid.

**Endpoint:** `GET /api/admin/auth/status`

**Headers:**
```
Authorization: Bearer {jwt-token}
```

**Response:**
```json
{
  "authenticated": true,
  "message": "Authenticated",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## Admin Dashboard

### Get Statistics

Get dashboard statistics.

**Endpoint:** `GET /api/admin/dashboard/statistics`

**Headers:**
```
Authorization: Bearer {jwt-token}
```

**Response:**
```json
{
  "total": 100,
  "pending": 5,
  "processing": 2,
  "safe": 80,
  "flagged": 10,
  "error": 3
}
```

---

### Get All Content

Get paginated list of all content.

**Endpoint:** `GET /api/admin/dashboard/content`

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `status` (optional): Filter by status (PENDING, PROCESSING, SAFE, FLAGGED, ERROR)

**Headers:**
```
Authorization: Bearer {jwt-token}
```

**Example:**
```
GET /api/admin/dashboard/content?page=0&size=20&status=FLAGGED
```

**Response:**
```json
{
  "content": [
    {
      "contentId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "FLAGGED",
      "userId": "user123",
      "contentType": "TEXT",
      "preview": "Your work is garbage...",
      "createdAt": "2025-12-13T20:00:00",
      "scores": {
        "toxicity": 0.85,
        "hate": 0.7,
        "sexual": 0.1,
        "violence": 0.2
      },
      "label": "FLAGGED"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "size": 20
}
```

---

### Get Flagged Content

Get all flagged content.

**Endpoint:** `GET /api/admin/flagged`

**Headers:**
```
Authorization: Bearer {jwt-token}
```

**Response:**
```json
[
  {
    "contentId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123",
    "type": "TEXT",
    "preview": "Your work is garbage...",
    "status": "FLAGGED",
    "scores": {
      "toxicity": 0.85,
      "hate": 0.7,
      "sexual": 0.1,
      "violence": 0.2
    },
    "createdAt": "2025-12-13T20:00:00"
  }
]
```

---

### Override Decision

Manually override moderation decision.

**Endpoint:** `POST /api/admin/action`

**Headers:**
```
Authorization: Bearer {jwt-token}
Content-Type: application/json
```

**Request:**
```json
{
  "contentId": "550e8400-e29b-41d4-a716-446655440000",
  "newLabel": "SAFE",
  "note": "False positive, manually reviewed"
}
```

**Response:**
```json
{
  "success": true,
  "previousLabel": "FLAGGED",
  "newLabel": "SAFE"
}
```

**Status Codes:**
- `200 OK`: Override successful
- `404 Not Found`: Content not found

---

### Get Moderation History

Get complete moderation history for a content item.

**Endpoint:** `GET /api/admin/history/{contentId}`

**Headers:**
```
Authorization: Bearer {jwt-token}
```

**Response:**
```json
{
  "contentId": "550e8400-e29b-41d4-a716-446655440000",
  "initialDecision": {
    "label": "FLAGGED",
    "scores": {
      "toxicity": 0.85,
      "hate": 0.7,
      "sexual": 0.1,
      "violence": 0.2
    },
    "timestamp": "2025-12-13T20:00:00"
  },
  "adminOverrides": [
    {
      "adminId": "admin",
      "previousLabel": "FLAGGED",
      "newLabel": "SAFE",
      "note": "False positive",
      "timestamp": "2025-12-13T20:05:00"
    }
  ]
}
```

---

### Fix Stuck Processing Items

Reset content items stuck in PROCESSING status.

**Endpoint:** `POST /api/admin/fix-stuck-processing`

**Headers:**
```
Authorization: Bearer {jwt-token}
```

**Response:**
```json
{
  "message": "Fixed stuck processing items",
  "count": 5
}
```

---

## Rules Management

### Get Current Rules

Get current moderation thresholds.

**Endpoint:** `GET /api/rules`

**Response:**
```json
{
  "toxicityThreshold": 0.7,
  "hateThreshold": 0.6,
  "sexualThreshold": 0.6,
  "violenceThreshold": 0.6
}
```

---

### Update Rules

Update moderation thresholds.

**Endpoint:** `POST /api/rules`

**Request:**
```json
{
  "toxicityThreshold": 0.7,
  "hateThreshold": 0.6,
  "sexualThreshold": 0.5,
  "violenceThreshold": 0.6
}
```

**Response:**
```json
{
  "toxicityThreshold": 0.7,
  "hateThreshold": 0.6,
  "sexualThreshold": 0.5,
  "violenceThreshold": 0.6
}
```

**Validation:**
- All thresholds must be between 0.0 and 1.0

---

## System Endpoints

### Health Check

Check application health.

**Endpoint:** `GET /actuator/health`

**Response:**
```json
{
  "status": "UP"
}
```

---

### API Info

Get API information.

**Endpoint:** `GET /api/info`

**Response:**
```json
{
  "service": "CrowdShield - Real-Time Content Moderation API",
  "version": "1.0.0",
  "status": "operational",
  "endpoints": {
    "health": "/actuator/health",
    "content": "/api/content",
    "admin": "/api/admin",
    "rules": "/api/rules"
  }
}
```

---

## WebSocket Endpoints

### WebSocket Connection

Connect to WebSocket for real-time updates.

**Endpoint:** `ws://localhost:8080/ws`

**Protocol:** STOMP over SockJS

**Subscribe to:** `/topic/status/{contentId}`

**Message Format:**
```json
{
  "stage": "PROCESSING",
  "progress": 60,
  "status": "PROCESSING"
}
```

**Stages:**
- `PENDING`: 10% progress
- `QUEUED`: 30% progress
- `PROCESSING`: 60% progress
- `AI_COMPLETED`: 90% progress
- `DONE`: 100% progress (with final status)

---

## Error Responses

All errors follow this format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2025-12-13T20:00:00"
}
```

**Common Error Codes:**
- `INVALID_REQUEST`: Invalid request body
- `CONTENT_NOT_FOUND`: Content ID not found
- `UNAUTHORIZED`: Authentication required
- `RATE_LIMIT_EXCEEDED`: Too many requests
- `INTERNAL_ERROR`: Server error

---

## Rate Limiting

- **Limit**: 100 requests per minute per IP
- **Response**: `429 Too Many Requests` when exceeded
- **Headers**: Rate limit information in response headers

---

## Content Types

### Text Content
- Maximum length: No limit (configurable)
- Encoding: UTF-8

### Image Content
- Format: URL (HTTP/HTTPS)
- Supported: Any publicly accessible image URL
- Processing: URL is sent to moderation API

---

## Status Lifecycle

```
PENDING → PROCESSING → SAFE/FLAGGED
              ↓
           ERROR (on failure)
```

---

## Best Practices

1. **Store JWT tokens securely** (localStorage or sessionStorage)
2. **Include JWT token** in Authorization header for admin endpoints
3. **Handle rate limits** with exponential backoff
4. **Poll status** if WebSocket unavailable
5. **Validate content** before submission
6. **Handle errors gracefully** with user-friendly messages

