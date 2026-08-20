# Architecture

This document describes how the Android client, API, and frontend fit together. All diagrams are Mermaid diagrams and render directly on GitHub.

## System Overview

```mermaid
flowchart LR
    subgraph Client["Browser"]
        FE["React admin UI\nRefine resources"]
    end

    subgraph Identity["Identity provider"]
        Authentik["Authentik\nSMS 2FA stage"]
    end

    subgraph Backend["Spring Boot API"]
        Security["Security filter chains\nJWT, API key, device WS key"]
        Controllers["REST controllers"]
        AuthentikOtp["AuthentikController\n/internal/authentik/otp"]
        WsDevice["/ws/device\nDeviceWebSocketHandler"]
        WsBrowser["/ws/sms\nBrowserSmsStatusWebSocketHandler"]
        Services["SmsService, QuotaService, Admin services"]
        Registry["DeviceConnectionRegistry\nPendingAckRegistry"]
    end

    subgraph Storage["Data"]
        Pg[(PostgreSQL)]
        Cache["Caffeine portability cache"]
    end

    subgraph Phone["Android client"]
        Foreground["GatewayForegroundService"]
        WsClient["WsClient"]
        SmsSender["SmsSender"]
        Receivers["SMS sent/delivered/received receivers"]
        Room[(Room database)]
    end

    FE -->|REST /api/v1| Security
    Authentik -->|Bearer token + OTP request| Security
    FE <-.->|WSS /ws/sms| WsBrowser
    Security --> Controllers --> Services
    Security --> AuthentikOtp --> Services
    Services --> Pg
    Services --> Cache
    Services --> Registry --> WsDevice
    WsDevice <-.->|WSS /ws/device| WsClient
    WsClient --> Foreground
    Foreground --> SmsSender --> Network["Cellular network"]
    Receivers --> Foreground
    Foreground --> Room
```

## SMS Send Sequence

```mermaid
sequenceDiagram
    participant User as Browser user
    participant FE as Frontend
    participant API as API
    participant DB as PostgreSQL
    participant Device as Android device
    participant Net as Cellular network

    User->>FE: Fill phone number and message
    FE->>API: POST /api/v1/sms
    API->>API: Authenticate JWT or X-API-Key
    API->>DB: Persist Message(PENDING)
    API->>Device: send_sms over /ws/device
    Device->>DB: none
    Device-->>API: ack(PENDING)
    API-->>FE: 200 MessageResponse
    Device->>Net: SmsManager multipart send
    Net-->>Device: sent/delivery PendingIntent
    Device-->>API: sms_status sent/delivered/failed
    API->>DB: Update Message.status
    API-->>FE: message_status over /ws/sms
```

## Authentik SMS 2FA Sequence

```mermaid
sequenceDiagram
    participant Auth as Authentik
    participant API as API
    participant DB as PostgreSQL
    participant Device as Android device
    participant Net as Cellular network
    participant User as Login user

    Auth->>API: POST /internal/authentik/otp
    API->>API: Validate AUTHENTIK_API_TOKEN bearer token
    API->>API: Render SMS_OTP_TEMPLATE with otpCode
    API->>DB: Persist OTP message with PENDING state
    API->>Device: send_sms over /ws/device
    Device-->>API: ack PENDING
    Device->>Net: Send OTP SMS through SmsManager
    Net-->>User: Deliver OTP code
    Device-->>API: sms_status sent/delivered/failed
    API->>DB: Update message state
```

Authentik SMS 2FA uses the same delivery pipeline as normal SMS, but it enters through the internal `POST /internal/authentik/otp` endpoint. That endpoint is protected by the `AUTHENTIK_API_TOKEN` bearer token and sends the OTP body through `SmsService`.

## Android Client

```mermaid
flowchart TB
    Main["MainActivity\npermissions + Compose host"] --> UI["Home and Settings screens"]
    UI --> Settings["Settings\nDataStore"]
    Boot["BootCompletedReceiver"] --> Service["GatewayForegroundService"]
    UI -->|start/stop| Service
    Service --> Settings
    Service --> Ws["WsClient\nOkHttp WebSocket"]
    Ws <--> ApiWs["API /ws/device"]
    Service --> Sender["SmsSender"]
    Sender --> AndroidSms["Android SmsManager"]
    AndroidSms --> Sent["SmsSentReceiver"]
    AndroidSms --> Delivered["SmsDeliveredReceiver"]
    Received["SmsReceivedReceiver"] --> Bus["SmsStatusBus"]
    Sent --> Bus
    Delivered --> Bus
    Bus --> Service
    Service --> Room[(Room messages and outbox)]
    Service --> Notification["Foreground notification"]
```

The Android app stores its backend URL, API key, device ID, boot behavior, and inbound forwarding toggle in DataStore. The foreground service owns the WebSocket and keeps a local Room outbox so outbound frames can be replayed after reconnects.

## API

```mermaid
flowchart TB
    Http["HTTP request"] --> Chains["SecurityConfig filter chains"]
    Authentik2["Authentik SMS 2FA"] -->|POST /internal/authentik/otp| Chains
    Chains --> AuthentikApi["AuthentikController\n/internal/authentik/otp"]
    Chains --> SmsApi["SmsController\n/api/v1/sms"]
    Chains --> ApiKeys["ApiKeyController\n/api/v1/api-keys"]
    Chains --> Plans["PlanAdminController\n/api/v1/admin/plans"]
    Chains --> Users["UserAdminController\n/api/v1/admin/users"]
    Chains --> Portability["PortabilityController\n/api/v1/portability"]
    AuthentikApi --> SmsService["SmsService"]
    SmsApi --> SmsService
    SmsService --> Quota["QuotaService"]
    SmsService --> GatewayClient["WebSocketSmsGatewayClient"]
    GatewayClient --> Registry["DeviceConnectionRegistry"]
    Registry --> DeviceWs["/ws/device sessions"]
    SmsService --> Messages[(messages table)]
    ApiKeys --> KeyRepo[(api_keys table)]
    Plans --> PlanRepo[(plans table)]
    Users --> UserRepo[(users table)]
    Portability --> PortClient["PortabilityClient"] --> Cache["Caffeine cache"]
```

REST endpoints:

| Area | Endpoint | Auth |
| --- | --- | --- |
| Messages | `GET /api/v1/sms`, `POST /api/v1/sms`, `POST /api/v1/sms/otp` | JWT user/admin or `X-API-Key`. |
| API keys | `GET /api/v1/api-keys`, `POST /api/v1/api-keys`, `DELETE /api/v1/api-keys/{id}` | JWT user/admin. |
| Plans | `/api/v1/admin/plans` | JWT admin. |
| Users | `/api/v1/admin/users` | JWT admin. |
| Portability | `GET /api/v1/portability/{phoneNumber}` | JWT user/admin or `X-API-Key`. |
| Device WebSocket | `GET /ws/device` | Bearer `SMS_DEVICE_WS_SHARED_KEY` plus `X-Device-Id`. |
| Browser WebSocket | `GET /ws/sms?access_token=...` | JWT user/admin. |
| Authentik SMS 2FA | `POST /internal/authentik/otp` | Static bearer token from `AUTHENTIK_API_TOKEN`. |

## Frontend

```mermaid
flowchart TB
    App["App.tsx"] --> Refine["Refine shell"]
    Refine --> Resources["Resources\nsms, portability, api-keys, plans, users"]
    Refine --> Auth["auth-provider\nOIDC PKCE"]
    Refine --> MockAuth["mock-auth-provider\nVITE_MOCK=true"]
    Refine --> Access["access-control\nrole based UI gates"]
    Resources --> DataProvider["data-provider\nfetch wrapper"]
    DataProvider --> Api["sms-gateway-api REST"]
    SmsPage["SmsSend page"] --> Api
    SmsPage <-.-> BrowserWs["/ws/sms\nstatus subscriptions"]
    PortabilityPage["Portability page"] --> PortabilityApi["/api/v1/portability/{phoneNumber}"]
```

The frontend uses `VITE_API_BASE_URL` for REST and derives WebSocket URLs from that same base URL. When `VITE_MOCK=true`, it uses the local mock auth provider and mock server for development.