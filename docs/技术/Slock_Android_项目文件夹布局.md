# Slock Android App 项目文件夹布局

## 根目录
```
Slock-Slock/
├── Slock_Android_MVP_开发文档.md   # 产品/开发主文档
├── docs/                            # 文档目录
├── project/                         # 项目目录（代码）
├── progress/                        # 进度管理
└── resources/                      # 资源文件
```

---

## docs/ — 文档目录
```
docs/
├── API/
│   ├── slock_api_副本.md           # 原始 API 逆向文档
│   ├── API_验证报告.md              # 验证结果
│   └── API_接口清单.md              # 规范化接口文档
├── 产品/
│   ├── 产品需求文档 PRD.md
│   ├── UI设计稿/                    # UI 设计文件
│   └── 交互流程图/
├── 技术/
│   ├── 技术选型报告.md
│   ├── 架构设计文档.md
│   └── 数据库设计.md
└── 会议记录/
    └── 2026-04-08_启动会议.md
```

---

## project/ — 代码目录
```
project/
├── SlockApp/                        # Android 主项目
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/slock/app/
│   │       │   ├── data/
│   │       │   │   ├── remote/
│   │       │   │   │   ├── api/    # Retrofit API 定义
│   │       │   │   │   └── socket/ # Socket.IO
│   │       │   │   ├── local/      # DataStore, Room
│   │       │   │   └── repository/
│   │       │   ├── domain/
│   │       │   │   ├── model/
│   │       │   │   ├── repository/
│   │       │   │   └── usecase/
│   │       │   ├── presentation/
│   │       │   │   ├── ui/
│   │       │   │   │   ├── auth/
│   │       │   │   │   ├── home/
│   │       │   │   │   ├── channel/
│   │       │   │   │   ├── thread/
│   │       │   │   │   └── agent/
│   │       │   │   └── viewmodel/
│   │       │   └── di/             # Hilt
│   │       └── res/
│   ├── build.gradle
│   └── settings.gradle
├── SlockServer/                     # 未来：后端服务（如需要）
└── SlockSdk/                       # 未来：复用 SDK
```

---

## progress/ — 进度管理
```
progress/
├── 任务看板.md                      # 当前任务状态
├── sprint计划/
│   ├── sprint_1_2026-04-xx.md
│   └── sprint_2_2026-04-xx.md
├── 周报/
│   └── 2026-04-xx_周报.md
└── 发布记录/
    └── v0.1.0_MVP.md
```

---

## resources/ — 资源文件
```
resources/
├── 图标/
│   ├── app_icon.png
│   └── feature_icons/
├── 启动页/
│   └── splash.png
└── 测试账号.md                      # 测试凭据
```

---

## 开发阶段规划

### Phase 1：基础设施（1-2周）
- [ ] 项目搭建（Gradle、Hilt、Compose）
- [ ] 认证模块（登录/注册/Token）
- [ ] Socket.IO 客户端封装

### Phase 2：核心功能（2-3周）
- [ ] 服务器切换
- [ ] 频道列表
- [ ] 消息收发（Socket.IO 实时）

### Phase 3：增强功能（2周）
- [ ] 线程支持
- [ ] Agent 交互
- [ ] 任务系统

### Phase 4：发布准备（1周）
- [ ] 通知推送
- [ ] 打包测试
- [ ] Play Store 上架

---

*创建时间：2026-04-08*
