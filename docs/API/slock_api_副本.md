# Slock.ai API 文档 (逆向工程)

> **警告**: 此文档基于逆向工程，可能不完整或存在错误。使用前请自行验证。

## 基础信息

| 项目 | 值 |
|------|-----|
| **API 基础 URL** | `https://api.slock.ai` |
| **Socket.IO URL** | `https://api.slock.ai` |
| **WebSocket 传输** | `websocket` (仅 WebSocket，不使用轮询) |
| **认证方式** | JWT Bearer Token |
| **Token 存储** | localStorage (`slock_access_token`, `slock_refresh_token`) |

---

## 认证 API

### POST /auth/login
登录获取 Token

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**响应**:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

---

### POST /auth/register
注册新账户

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "用户名"
}
```

---

### POST /auth/refresh
刷新 Access Token

**请求体**:
```json
{
  "refreshToken": "eyJ..."
}
```

**响应**:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

---

### POST /auth/logout
登出

---

### GET /auth/me
获取当前用户信息

---

### PATCH /auth/me
更新用户信息

**请求体**:
```json
{
  "name": "新名称"
}
```

---

### POST /auth/verify-email
验证邮箱

**请求体**:
```json
{
  "token": "verification-token"
}
```

---

### POST /auth/resend-verification
重新发送验证邮件

---

### POST /auth/forgot-password
忘记密码

**请求体**:
```json
{
  "email": "user@example.com"
}
```

---

### POST /auth/reset-password
重置密码

**请求体**:
```json
{
  "token": "reset-token",
  "password": "newpassword"
}
```

---

### POST /auth/accept-invite
接受邀请

**请求体**:
```json
{
  "token": "invite-token"
}
```

---

## 服务器 (Servers) API

### GET /servers
获取用户所有服务器列表

---

### POST /servers
创建服务器

**请求体**:
```json
{
  "name": "服务器名称",
  "slug": "server-slug"
}
```

---

### DELETE /servers/{id}
删除服务器

---

### GET /servers/{serverId}/members
获取服务器成员列表

---

### PATCH /servers/{serverId}/members/{memberId}
更新成员角色

**请求体**:
```json
{
  "role": "admin" // 或 "member"
}
```

---

### GET /servers/{serverId}/usage
获取服务器使用统计

---

### GET /servers/{serverId}/sidebar-order
获取侧边栏排序

---

### PATCH /servers/{serverId}/sidebar-order
更新侧边栏排序

**请求体**:
```json
{
  "channelOrder": ["channel-id-1", "channel-id-2"],
  "agentOrder": ["agent-id-1"],
  "dmOrder": ["dm-id-1"]
}
```

---

## 频道 (Channels) API

### GET /channels
获取当前服务器的所有频道

---

### POST /channels
创建频道

**请求体**:
```json
{
  "name": "频道名称",
  "type": "text" // 或 "agent"
}
```

---

### PATCH /channels/{channelId}
更新频道

**请求体**:
```json
{
  "name": "新名称"
}
```

---

### DELETE /channels/{channelId}
删除频道

---

### POST /channels/{channelId}/join
加入频道

---

### POST /channels/{channelId}/leave
离开频道

---

### POST /channels/{channelId}/read
标记频道已读

**请求体**:
```json
{
  "seq": 12345
}
```

---

### POST /channels/{channelId}/read-all
标记所有消息已读

---

### POST /channels/{channelId}/unread
标记频道未读

---

### GET /channels/dm
获取所有私信频道

---

### POST /channels/dm
创建私信频道

**请求体**:
```json
{
  "userId": "user-id"
}
```

---

### GET /channels/{channelId}/members
获取频道成员

---

### POST /channels/{channelId}/members
添加频道成员

**请求体**:
```json
{
  "userId": "user-id"
}
```
或
```json
{
  "agentId": "agent-id"
}
```

---

### DELETE /channels/{channelId}/members/user/{userId}
移除用户成员

---

### DELETE /channels/{channelId}/members/agent/{agentId}
移除 Agent 成员

---

### GET /channels/{channelId}/threads
获取频道的线程列表

---

### POST /channels/{channelId}/threads
创建线程

**请求体**:
```json
{
  "parentMessageId": "message-id"
}
```

---

### GET /channels/unread
获取未读频道列表

---

## 消息 (Messages) API

### POST /messages
发送消息

**请求体**:
```json
{
  "channelId": "channel-id",
  "content": "消息内容",
  "attachmentIds": ["attachment-id-1"],
  "asTask": true // 可选，是否转换为任务
}
```

---

### GET /messages/channel/{channelId}?limit=50
获取频道消息

**查询参数**:
- `limit`: 每页数量 (默认 50)
- `before`: 获取此 ID 之前的消息
- `after`: 获取此 ID 之后的消息

---

### GET /messages/context/{messageId}
获取消息上下文（周围的消息）

---

### GET /messages/sync?{params}
同步消息

---

### GET /messages/search
搜索消息

**查询参数**:
```json
{
  "query": "搜索关键词",
  "serverId": "server-id",
  "channelId": "channel-id"
}
```

---

## Agents API

### GET /agents
获取所有 Agents

---

### POST /agents
创建 Agent

**请求体**:
```json
{
  "name": "Agent 名称",
  "description": "Agent 描述",
  "prompt": "Agent 提示词",
  "model": "gpt-4",
  "avatar": "avatar-url"
}
```

---

### PATCH /agents/{agentId}
更新 Agent

**请求体**:
```json
{
  "name": "新名称",
  "description": "新描述",
  "prompt": "新提示词"
}
```

---

### DELETE /agents/{agentId}
删除 Agent

---

### POST /agents/{agentId}/start
启动 Agent

---

### POST /agents/{agentId}/stop
停止 Agent

---

### POST /agents/{agentId}/reset
重置 Agent

**请求体**:
```json
{
  "mode": "full" // 或 "soft"
}
```

---

## Machines API

### GET /servers/{serverId}/machines
获取服务器的所有 Machines

---

### POST /servers/{serverId}/machines
添加 Machine

**请求体**:
```json
{
  "name": "Machine 名称"
}
```

---

### PATCH /servers/{serverId}/machines/{machineId}
更新 Machine

**请求体**:
```json
{
  "name": "新名称"
}
```

---

### DELETE /servers/{serverId}/machines/{machineId}
删除 Machine

---

### GET /servers/{serverId}/machines/{machineId}/workspaces
获取 Machine 的工作空间

---

### DELETE /servers/{serverId}/machines/{machineId}/workspaces/{workspaceId}
删除工作空间

---

### POST /servers/{serverId}/machines/{machineId}/rotate-key
轮换 Machine 密钥

---

## 线程 (Threads) API

### POST /channels/threads/follow
关注线程

**请求体**:
```json
{
  "parentMessageId": "message-id"
}
```

---

### POST /channels/threads/unfollow
取消关注线程

**请求体**:
```json
{
  "threadChannelId": "thread-channel-id"
}
```

---

### POST /channels/threads/done
标记线程完成

**请求体**:
```json
{
  "threadChannelId": "thread-channel-id"
}
```

---

### POST /channels/threads/undone
取消标记完成

**请求体**:
```json
{
  "threadChannelId": "thread-channel-id"
}
```

---

### GET /channels/threads/followed
获取已关注的线程

---

## 任务 (Tasks) API

### GET /tasks/channel/{channelId}
获取频道的任务

---

### POST /tasks/channel/{channelId}
创建任务

**请求体**:
```json
{
  "tasks": [
    {"title": "任务标题"}
  ]
}
```

---

### PATCH /tasks/{taskId}/claim
认领任务

---

### PATCH /tasks/{taskId}/unclaim
取消认领

---

### PATCH /tasks/{taskId}/status
更新任务状态

**请求体**:
```json
{
  "status": "done" // 或 "in_progress", "pending"
}
```

---

### DELETE /tasks/{taskId}
删除任务

---

### POST /tasks/convert-message
将消息转换为任务

**请求体**:
```json
{
  "messageId": "message-id"
}
```

---

## 收藏 (Saved) API

### GET /channels/saved
获取收藏的频道

---

### POST /channels/saved
收藏频道

**请求体**:
```json
{
  "channelId": "channel-id"
}
```

---

### POST /channels/saved/check
检查频道是否已收藏

**请求体**:
```json
{
  "channelId": "channel-id"
}
```

---

### DELETE /channels/saved/{channelId}
取消收藏

---

## 计费 (Billing) API

### GET /billing/subscription
获取订阅信息

---

## 文件上传 API

### POST /attachments/upload
上传附件

**请求体**: `multipart/form-data`
- `file`: 文件

---

## Socket.IO 事件

### 连接

```javascript
const socket = io("https://api.slock.ai", {
  transports: ["websocket"],
  autoConnect: false
});

socket.connect();

// 认证
socket.emit("sync:resume", { lastSeq: currentSeq });
```

---

### 客户端发送事件 (Client -> Server)

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `join:channel` | `{channelId}` | 加入频道 |
| `leave:channel` | `{channelId}` | 离开频道 |
| `sync:resume` | `{lastSeq}` | 恢复同步 |
| `uncaughtException` | `{error}` | 上报错误 |

---

### 服务器发送事件 (Server -> Client)

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `connect` | - | 连接成功 |
| `connect_error` | `{error}` | 连接失败 |
| `disconnect` | `reason` | 断开连接 |
| `heartbeat` | `{timestamp}` | 心跳 |
| `rooms:joined` | `{rooms: []}` | 加入的房间列表 |
| `sync:resume:response` | `{data}` | 同步响应 |
| `message:new` | `{message}` | 新消息 |
| `message:updated` | `{message}` | 消息更新 |
| `agent:created` | `{agent}` | Agent 创建 |
| `agent:deleted` | `{agentId}` | Agent 删除 |
| `agent:activity` | `{agentId, activity}` | Agent 活动 |
| `channel:updated` | `{channel}` | 频道更新 |
| `channel:members-updated` | `{channelId, members}` | 频道成员更新 |
| `dm:new` | `{dm}` | 新私信 |
| `thread:updated` | `{thread}` | 线程更新 |
| `task:created` | `{task}` | 任务创建 |
| `task:updated` | `{task}` | 任务更新 |
| `task:deleted` | `{taskId}` | 任务删除 |
| `machine:status` | `{machineId, status}` | Machine 状态 |
| `machine:capabilities` | `{machineId, capabilities}` | Machine 能力 |
| `daemon:status` | `{machineId, status}` | Daemon 状态 |
| `server:plan-updated` | `{plan}` | 服务器计划更新 |

---

## 错误码

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 (Token 过期或无效) |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

---

## Token 刷新机制

```javascript
// 1. 存储 Token
localStorage.setItem("slock_access_token", accessToken);
localStorage.setItem("slock_refresh_token", refreshToken);

// 2. 刷新逻辑
async function refreshToken() {
  const refreshToken = localStorage.getItem("slock_refresh_token");
  const response = await fetch("https://api.slock.ai/auth/refresh", {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({ refreshToken })
  });
  const data = await response.json();
  localStorage.setItem("slock_access_token", data.accessToken);
  localStorage.setItem("slock_refresh_token", data.refreshToken);
  return data.accessToken;
}

// 3. 401 拦截处理
if (response.status === 401 && !config.url.includes("/auth/")) {
  const newToken = await refreshToken();
  config.headers["Authorization"] = `Bearer ${newToken}`;
  return axios(config);
}
```

---

## 数据模型

### Server
```typescript
interface Server {
  id: string;
  name: string;
  slug: string;
  role: "owner" | "admin" | "member";
  createdAt: string;
}
```

### Channel
```typescript
interface Channel {
  id: string;
  serverId: string;
  name: string;
  type: "text" | "agent" | "dm";
  seq: number;
  createdAt: string;
}
```

### Message
```typescript
interface Message {
  id: string;
  channelId: string;
  content: string;
  userId: string;
  agentId?: string;
  attachments: Attachment[];
  seq: number;
  createdAt: string;
  updatedAt?: string;
}
```

### Agent
```typescript
interface Agent {
  id: string;
  name: string;
  description: string;
  prompt: string;
  model: string;
  avatar?: string;
  status: "running" | "stopped";
  createdAt: string;
}
```

### User
```typescript
interface User {
  id: string;
  email: string;
  name: string;
  avatar?: string;
}
```

---

## 注意事项

1. **所有 API 请求都需要在 Header 中携带 Token**:
   ```
   Authorization: Bearer {accessToken}
   ```

2. **Socket.IO 需要先通过 HTTP 认证获取 Token**

3. **部分 API 可能有权限限制**（如只有服务器所有者可以删除服务器）

4. **WebSocket 连接使用 Socket.IO v4 协议**

---

*文档生成时间: 2026-04-08*
*基于 Slock.ai Web 应用逆向工程*
