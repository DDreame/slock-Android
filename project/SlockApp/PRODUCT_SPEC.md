# SlockApp - Product Specification

## Product Overview

**SlockApp** is a comprehensive Android mobile client for the Slock.ai platform, enabling users to manage servers, channels, real-time messaging, and AI agents from their mobile devices. Built with modern Android architecture patterns, it delivers a native, responsive experience optimized for both phones and tablets.

### Key Highlights
- **Full-Featured Communication Platform**: Servers, channels, and real-time messaging
- **AI Agent Management**: Create, configure, and monitor AI agents
- **Real-Time Updates**: Socket.IO-powered live synchronization
- **Enterprise-Ready**: JWT authentication with secure token management

---

## Architecture Overview

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **UI** | Jetpack Compose | Modern declarative UI |
| **Architecture** | MVVM + Clean Architecture | Separation of concerns |
| **DI** | Hilt | Dependency injection |
| **Networking** | Retrofit + OkHttp | REST API communication |
| **Real-Time** | Socket.IO Client | Live event streaming |
| **Storage** | DataStore | Secure token & preferences |
| **Navigation** | Navigation Compose | Screen navigation |
| **Async** | Kotlin Coroutines + Flow | Reactive data streams |

### Project Structure

```
app/src/main/java/com/slock/app/
├── SlockApp.kt                      # Application entry point
├── di/                              # Hilt dependency modules
│   ├── AppModule.kt                 # App-level dependencies
│   ├── NetworkModule.kt             # Retrofit & Socket.IO setup
│   └── RepositoryModule.kt          # Repository bindings
├── data/
│   ├── api/
│   │   ├── ApiService.kt            # Retrofit API interface
│   │   └── Interceptors.kt          # Auth & refresh interceptors
│   ├── socket/
│   │   ├── SocketIOManager.kt       # Socket.IO connection manager
│   │   └── SocketEvents.kt          # Event definitions
│   ├── model/                       # Data models (DTOs)
│   │   ├── User.kt, Auth.kt, Token.kt
│   │   ├── Server.kt, Channel.kt, Message.kt
│   │   └── Agent.kt, Member.kt
│   └── repository/                  # Data repositories
│       ├── AuthRepository.kt
│       ├── ServerRepository.kt
│       ├── ChannelRepository.kt
│       ├── MessageRepository.kt
│       └── AgentRepository.kt
├── domain/
│   ├── model/                       # Domain models
│   └── usecase/                     # Business logic
│       ├── auth/, server/, channel/
│       ├── message/, agent/
│       └── Result.kt                # Result wrapper
├── ui/
│   ├── theme/                       # Compose theming
│   │   ├── Color.kt, Type.kt
│   │   └── Theme.kt
│   ├── navigation/                  # Navigation setup
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   ├── components/                  # Reusable UI components
│   │   ├── LoadingIndicator.kt
│   │   ├── ErrorMessage.kt
│   │   └── EmptyState.kt
│   ├── auth/                        # Authentication screens
│   │   ├── AuthViewModel.kt
│   │   ├── LoginScreen.kt
│   │   ├── RegisterScreen.kt
│   │   └── ForgotPasswordScreen.kt
│   ├── server/                      # Server management
│   │   ├── ServerViewModel.kt
│   │   ├── ServerListScreen.kt
│   │   └── ServerDetailScreen.kt
│   ├── channel/                     # Channel management
│   │   ├── ChannelViewModel.kt
│   │   ├── ChannelListScreen.kt
│   │   └── CreateChannelDialog.kt
│   ├── message/                     # Messaging
│   │   ├── MessageViewModel.kt
│   │   ├── MessageListScreen.kt
│   │   └── MessageInput.kt
│   └── agent/                       # Agent management
│       ├── AgentViewModel.kt
│       ├── AgentListScreen.kt
│       └── AgentDetailScreen.kt
└── util/
    ├── NetworkMonitor.kt            # Connectivity handling
    └── Extensions.kt                # Utility extensions
```

---

## API Integration

### Base Configuration
- **API Base URL**: `https://api.slock.ai`
- **Socket.IO URL**: `https://api.slock.ai`
- **Authentication**: JWT Bearer Token with Refresh Token rotation

### Endpoint Categories

#### Authentication Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | Email/password login |
| POST | `/auth/register` | New user registration |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/logout` | Invalidate tokens |
| GET | `/auth/me` | Get current user profile |
| PATCH | `/auth/me` | Update user profile |
| POST | `/auth/forgot-password` | Initiate password reset |
| POST | `/auth/reset-password` | Complete password reset |
| POST | `/auth/verify-email` | Verify email address |
| POST | `/auth/accept-invite` | Accept server invitation |

#### Server Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/servers` | List user's servers |
| POST | `/servers` | Create new server |
| DELETE | `/servers/{id}` | Delete server |
| GET | `/servers/{id}/members` | List server members |
| PATCH | `/servers/{id}/members/{memberId}` | Update member role |
| GET/PATCH | `/servers/{id}/sidebar-order` | Manage sidebar layout |

#### Channel Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/channels` | List channels |
| POST | `/channels` | Create channel |
| PATCH | `/channels/{id}` | Update channel |
| DELETE | `/channels/{id}` | Delete channel |
| POST | `/channels/{id}/join` | Join channel |
| POST | `/channels/{id}/leave` | Leave channel |
| POST | `/channels/{id}/read` | Mark messages as read |
| POST | `/channels/{id}/read-all` | Mark all as read |
| GET/POST/DELETE | `/channels/{id}/members` | Channel membership |
| GET | `/channels/dm` | Direct messages |
| POST | `/channels/dm` | Create DM |
| GET | `/channels/unread` | Unread counts |

#### Message Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/messages` | Send message |
| GET | `/messages/channel/{id}` | Get messages (paginated) |
| GET | `/messages/context/{id}` | Get message context |
| GET | `/messages/search` | Search messages |

#### Agent Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/agents` | List agents |
| POST | `/agents` | Create agent |
| PATCH | `/agents/{id}` | Update agent |
| DELETE | `/agents/{id}` | Delete agent |
| POST | `/agents/{id}/start` | Start agent |
| POST | `/agents/{id}/stop` | Stop agent |
| POST | `/agents/{id}/reset` | Reset agent |

### Socket.IO Events

#### Client Emissions
- `join:channel` - Subscribe to channel updates
- `leave:channel` - Unsubscribe from channel
- `sync:resume` - Resume connection after reconnect

#### Server Notifications
- `message:new` - New message received
- `message:updated` - Message edited
- `agent:activity` - Agent activity update
- `agent:created` / `agent:deleted` - Agent lifecycle
- `channel:updated` - Channel changes
- `channel:members-updated` - Member changes
- `task:created` / `task:updated` / `task:deleted` - Task events
- `machine:status` / `daemon:status` - System status

---

## Sprint Plan

| Sprint | Title | Duration | Focus |
|--------|-------|----------|-------|
| 1 | Project Foundation & Build Setup | Foundation | Gradle, Hilt, Theme, Navigation |
| 2 | Authentication Module | Auth | Login, Register, Token Management |
| 3 | Networking Layer & API Client | Infrastructure | Retrofit, Socket.IO, Models |
| 4 | Server & Channel Management | Core | Server/Channel CRUD, Navigation |
| 5 | Messaging System | Communication | Real-time Messages, Pagination |
| 6 | Agents Module | AI Features | Agent CRUD, Activity Monitoring |
| 7 | Polish & Responsive UI | Quality | Error Handling, Responsive Layouts |

### Sprint 1: Project Foundation
**Objective**: Establish the Android project with all build configurations, dependency injection, theming, and navigation infrastructure.

**Key Deliverables**:
- Complete `build.gradle.kts` with all dependencies
- `SlockApp.kt` Application class with Hilt
- DI modules (AppModule, NetworkModule, RepositoryModule)
- Compose theme with Slock.ai brand colors
- Navigation setup with NavHost and route definitions

### Sprint 2: Authentication Module
**Objective**: Build the complete authentication flow with secure token management.

**Key Deliverables**:
- `AuthRepository` with login, register, refresh, logout
- `TokenManager` using DataStore for secure storage
- `AuthViewModel` with UI state management
- Login, Register, ForgotPassword, EmailVerification screens
- Auth use cases implementing business logic

### Sprint 3: Networking Layer
**Objective**: Implement the networking infrastructure for API and real-time communication.

**Key Deliverables**:
- `ApiService` with all REST endpoints
- `AuthInterceptor` for JWT injection
- `TokenRefreshInterceptor` for automatic token refresh
- `SocketIOManager` for real-time events
- All data models (DTOs)
- Result wrapper with error handling

### Sprint 4: Server & Channel Management
**Objective**: Build server and channel management with hierarchical navigation.

**Key Deliverables**:
- `ServerRepository` and `ChannelRepository`
- Server list with creation and deletion
- Channel list with unread badges
- Server detail with member management
- ViewModels with state management
- Navigation between servers and channels

### Sprint 5: Messaging System
**Objective**: Implement real-time messaging with pagination and search.

**Key Deliverables**:
- `MessageRepository` with pagination support
- `MessageViewModel` with real-time Flow updates
- Message list with infinite scroll
- Message input component
- Socket.IO message event handling
- Unread message marking

### Sprint 6: Agents Module
**Objective**: Build AI agent management and monitoring capabilities.

**Key Deliverables**:
- `AgentRepository` with full CRUD and controls
- `AgentViewModel` with status state
- Agent list with status indicators
- Agent detail with configuration
- Agent activity event handling
- Create/Edit agent dialogs

### Sprint 7: Polish & Responsive UI
**Objective**: Final polish with comprehensive error handling and responsive layouts.

**Key Deliverables**:
- Error handling with user-friendly messages
- Loading states and shimmer placeholders
- Empty states for all lists
- Responsive layouts using WindowSizeClass
- Network connectivity monitoring
- Offline mode handling

---

## Design Language

### Color Palette

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| Primary | `#6366F1` | `#818CF8` | Brand actions, links |
| Secondary | `#8B5CF6` | `#A78BFA` | Accents, highlights |
| Background | `#FFFFFF` | `#111827` | Screen backgrounds |
| Surface | `#F3F4F6` | `#1F2937` | Cards, dialogs |
| OnSurface | `#111827` | `#F9FAFB` | Primary text |
| OnSurfaceVariant | `#6B7280` | `#9CA3AF` | Secondary text |
| Error | `#EF4444` | `#F87171` | Error states |
| Success | `#10B981` | `#34D399` | Success states |

### Typography
- **Headings**: Inter (600-700 weight)
- **Body**: Inter (400-500 weight)
- **Monospace**: JetBrains Mono (code blocks)

### Layout System
- **Spacing**: 4dp base unit (4, 8, 12, 16, 24, 32, 48)
- **Corner Radius**: 8dp (small), 12dp (medium), 16dp (large)
- **Elevation**: 0dp (flat), 2dp (raised), 8dp (floating)

---

## Quality Requirements

### Build Verification
- All sprints must pass `./gradlew assembleDebug`
- No compilation warnings in production code
- ProGuard/R8 rules for release builds

### Testing Strategy
- Unit tests for ViewModels and UseCases
- Integration tests for Repositories
- UI tests for critical user flows

### Performance Targets
- Cold start: < 2 seconds
- Screen transition: < 300ms
- Message list scroll: 60fps
- Memory footprint: < 150MB typical usage

---

## Next Steps

1. **Sprint 1 Execution**: Set up project foundation
2. **Continuous Integration**: Configure CI/CD pipeline
3. **Testing Infrastructure**: Set up testing frameworks
4. **Design Review**: Validate UI/UX with stakeholders
5. **API Contract**: Confirm API documentation alignment
