# Slock Android App MVP 开发文档

## 1. 产品定位

**一句话描述**：移动端的 Slock.ai 客户端，让用户随时随地与团队和 Agent 保持连接。

**核心价值**：实时协作 + Agent 交互 + 消息推送

---

## 2. MVP 功能范围

### 2.1 认证模块
- [ ] 邮箱密码登录
- [ ] 注册新账户
- [ ] Token 存储与自动刷新
- [ ] 登出

### 2.2 服务器与频道
- [ ] 获取服务器列表
- [ ] 切换当前服务器
- [ ] 获取频道列表
- [ ] 进入/离开频道

### 2.3 消息系统
- [ ] 发送消息
- [ ] 实时接收消息（Socket.IO）
- [ ] 消息分页加载
- [ ] 未读消息标记

### 2.4 线程（Threads）
- [ ] 查看频道线程列表
- [ ] 在线程中回复

### 2.5 任务系统
- [ ] 查看频道任务列表
- [ ] 创建任务
- [ ] 任务状态更新

### 2.6 Agent 交互
- [ ] 查看 Agent 列表
- [ ] 向 Agent 发送消息
- [ ] 接收 Agent 回复

### 2.7 通知推送
- [ ] 新消息通知
- [ ] @提及通知
- [ ] 通知点击跳转

---

## 3. 技术架构

### 3.1 技术栈
- **语言**：Kotlin
- **UI**：Jetpack Compose
- **架构**：MVVM + Clean Architecture
- **网络**：Retrofit + OkHttp
- **WebSocket**：Socket.IO Client (Kotlin)
- **本地存储**：DataStore（Token）+ Room（缓存）
- **DI**：Hilt
- **异步**：Kotlin Coroutines + Flow

### 3.2 项目结构
```
com.slock.app/
├── data/
│   ├── remote/
│   │   ├── api/          # REST API
│   │   └── socket/       # Socket.IO
│   ├── local/            # DataStore, Room
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── ui/
│   │   ├── auth/
│   │   ├── home/
│   │   ├── channel/
│   │   ├── thread/
│   │   └── agent/
│   └── viewmodel/
└── di/
```

### 3.3 API 基础配置
- **Base URL**：`https://api.slock.ai/api/`
- **认证 Header**：`Authorization: Bearer {token}`
- **服务器 Header**：`X-Server-Id: {serverId}`

---

## 4. API 对接清单

### 4.1 认证
| 方法 | 路径 | 状态 |
|------|------|------|
| POST | /api/auth/login | ✅已验证 |
| POST | /api/auth/register | ✅已验证 |
| POST | /api/auth/refresh | 待验证 |
| GET | /api/auth/me | ✅已验证 |

### 4.2 服务器
| 方法 | 路径 | 状态 |
|------|------|------|
| GET | /api/servers | ✅已验证 |
| POST | /api/servers | ✅已验证 |
| GET | /api/servers/{id} | ✅已验证 |
| GET | /api/servers/{id}/members | ✅已验证 |
| GET | /api/servers/{id}/usage | ✅已验证 |

### 4.3 频道
| 方法 | 路径 | 状态 |
|------|------|------|
| GET | /api/channels | ✅已验证 |
| POST | /api/channels | ✅已验证 |
| POST | /api/channels/{id}/join | 待验证 |
| POST | /api/channels/{id}/leave | 待验证 |

### 4.4 消息
| 方法 | 路径 | 状态 |
|------|------|------|
| POST | /api/messages | ✅已验证 |
| GET | /api/messages/channel/{channelId} | ✅已验证 |
| GET | /api/messages/context/{messageId} | 待验证 |

### 4.5 线程
| 方法 | 路径 | 状态 |
|------|------|------|
| GET | /api/channels/{id}/threads | ✅已验证 |
| POST | /api/channels/threads/follow | 待验证 |

### 4.6 任务
| 方法 | 路径 | 状态 |
|------|------|------|
| GET | /api/tasks/channel/{channelId} | 待验证 |
| POST | /api/tasks/channel/{channelId} | 待验证 |
| PATCH | /api/tasks/{id}/status | 待验证 |

### 4.7 Socket.IO ✅已验证
- 连接：`https://api.slock.ai`
- 协议：Socket.IO v4，WebSocket
- 认证事件：`sync:resume`
- 订阅事件：`join:channel`, `leave:channel`
- 接收事件：`rooms:joined`, `message:new`, `message:updated`, `thread:updated`, `task:created`, `task:updated`

---

## 5. 开发优先级

### P0（必须）
1. 登录/注册
2. 服务器切换
3. 频道列表
4. 消息发送/接收
5. Socket.IO 实时通信

### P1（重要）
6. 线程回复
7. Agent 交互
8. 未读标记

### P2（增强）
9. 任务系统
10. 通知推送
11. 离线支持

---

## 6. 待解决问题

1. ~~**Socket.IO 验证**：@X 正在构建客户端验证~~ ✅ 已完成
2. **Token 刷新机制**：需完整验证自动刷新流程
3. **消息同步**：历史消息加载策略待定

---

*文档版本：v0.1*
*最后更新：2026-04-08*
