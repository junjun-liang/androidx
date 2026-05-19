# RemoteCompose 与 Glance/Widget 生态系统关系文档

> 项目路径：`/home/meizu/Documents/my_android_projects/androidx/compose/remote`
> 生成日期：2026-05-19

---

## 目录

1. [RemoteCompose 在 Android 生态中的定位](#1-remotecompose-在-android-生态中的定位)
2. [与 Glance 框架的关系](#2-与-glance-框架的关系)
3. [AppWidget 集成架构](#3-appwidget-集成架构)
4. ["帮我制作微件"功能架构](#4-帮我制作微件功能架构)
5. [核心特性与平台支持](#5-核心特性与平台支持)
6. [技术实现细节](#6-技术实现细节)
7. [附录](#7-附录)

---

## 1. RemoteCompose 在 Android 生态中的定位

### 1.1 整体生态位

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Android UI 生态系统架构图                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         应用层 (Applications)                        │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │   │
│  │  │   主屏幕     │  │  Wear OS    │  │ Android Auto│  │  通知栏    │ │   │
│  │  │   微件      │  │   表盘      │  │   车载      │  │  快捷操作  │ │   │
│  │  │  (Widgets)  │  │  (WatchFace)│  │   (Car)     │  │            │ │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬──────┘ │   │
│  │         │                │                │               │        │   │
│  │         └────────────────┴────────────────┴───────────────┘        │   │
│  │                              │                                     │   │
│  │                              ▼                                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐  │   │
│  │  │                    Jetpack Glance                              │  │   │
│  │  │  - 声明式微件 API (Compose-like)                               │  │   │
│  │  │  - 向后兼容层 (GlanceTemplate)                                 │  │   │
│  │  │  - 自动生成 RemoteViews                                       │  │   │
│  │  └─────────────────────────────────────────────────────────────┘  │   │
│  │                              │                                     │   │
│  │                              ▼                                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐  │   │
│  │  │                 RemoteCompose (底层框架)                       │  │   │
│  │  │  - 二进制协议序列化/反序列化                                    │  │   │
│  │  │  - RPN 表达式引擎 (动画/交互)                                  │  │   │
│  │  │  - 播放器端本地渲染                                            │  │   │
│  │  │  - 触摸交互处理                                                │  │   │
│  │  └─────────────────────────────────────────────────────────────┘  │   │
│  │                              │                                     │   │
│  │                              ▼                                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐  │   │
│  │  │              Android System (RemoteViews.DrawInstructions)     │  │   │
│  │  │  - Android 16+ (Vanilla Ice Cream): 原生支持                   │  │   │
│  │  │  - Android 15 及以下: GlanceTemplate 降级                      │  │   │
│  │  └─────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         AI 层 (Gemini)                               │   │
│  │  ┌─────────────────────────────────────────────────────────────┐  │   │
│  │  │              "帮我制作微件" (Make My Widget)                   │  │   │
│  │  │  - 自然语言 -> RemoteCompose 代码生成                          │  │   │
│  │  │  - 自适应大小调整                                              │  │   │
│  │  │  - 主屏幕 / Wear OS 自动优化                                   │  │   │
│  │  └─────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 RemoteCompose 的核心价值

RemoteCompose 解决了传统 Android Widget 系统的几个关键痛点：

| 痛点 | 传统 RemoteViews | RemoteCompose 解决方案 |
|------|-----------------|----------------------|
| 交互能力有限 | 仅支持点击 PendingIntent | 本地触摸处理、滑动、动画 |
| 动画支持差 | 无原生动画支持 | RPN 表达式驱动的本地动画 |
| 更新开销大 | 每次更新需重新传输完整布局 | 二进制协议 + 状态本地化 |
| 表现力受限 | 有限的视图类型 | 自定义绘制、粒子系统、Canvas |
| 跨设备适配难 | 需为不同设备单独开发 | 自适应布局 + 统一协议 |

---

## 2. 与 Glance 框架的关系

### 2.1 Glance 架构演进

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Glance 架构演进图                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Glance 1.x (传统模式)                                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   @Composable                                                       │   │
│  │   fun MyWidget() {                                                  │   │
│  │       GlanceButton(...)  // 有限组件集                              │   │
│  │   }                                                                 │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   GlanceTemplate (模板系统)                                          │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   RemoteViews (传统 Android 微件视图)                                │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Glance 2.x + RemoteCompose (新模式)                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   @Composable                                                       │   │
│  │   fun MyWidget() {                                                  │   │
│  │       RemoteText(...)     // 更丰富组件                             │   │
│  │       RemoteCanvas { ... } // 自定义绘制                             │   │
│  │       RemoteModifier.clickable { ... } // 交互                      │   │
│  │   }                                                                 │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   RemoteCompose (二进制协议)                                         │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   RemoteViews.DrawInstructions (Android 16+)                        │   │
│  │   或 GlanceTemplate (Android 15 及以下)                             │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 RemoteCompose 作为 Glance 的底层引擎

根据 Google 官方发布说明：

> "Glance 可让您轻松构建高质量的微件，现在借助名为 RemoteCompose 的新底层框架，它获得了强大的新功能。"

RemoteCompose 为 Glance 提供了以下增强：

1. **更丰富、更优质的互动**：
   - SnapScroll（吸附滚动）
   - 富有表现力的按钮（Expressive Buttons）
   - 粒子效果（Particle Effects）

2. **内置向后兼容性**：
   - Android 16+ (Vanilla Ice Cream)：原生支持 RemoteCompose
   - Android 15 及以下：通过 GlanceTemplate 优雅降级

---

## 3. AppWidget 集成架构

### 3.1 Widget 类层次结构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    RemoteCompose Widget 类架构                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                              AppWidgetProvider                               │
│                                   (Android)                                  │
│                                     │                                        │
│                                     ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    RemoteComposeWidget                               │    │
│  │  (remote-creation-compose/widgets/RemoteComposeWidget.kt)            │    │
│  │                                                                     │    │
│  │  - useCompose: Boolean (选择 Compose 或 Procedural API)             │    │
│  │  - widget: AbstractRCWidget (实际 Widget 实现)                      │    │
│  │                                                                     │    │
│  │  方法:                                                              │    │
│  │  - onUpdate() -> widget.createRemoteView()                          │    │
│  │  - onReceive() -> widget.updateRemoteView() (处理点击)              │    │
│  │  - Content() @Composable (子类重写)                                 │    │
│  │  - ProceduralContent() (子类重写)                                   │    │
│  │                                                                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                     │                                        │
│           ┌─────────────────────────┼─────────────────────────┐              │
│           │                         │                         │              │
│           ▼                         ▼                         ▼              │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐      │
│  │    RCWidget     │      │ ProceduralRCWidget│     │  (自定义实现)    │      │
│  │  (Compose API)  │      │  (Procedural API) │     │                 │      │
│  │                 │      │                   │     │                 │      │
│  │ - 使用 @RemoteComposable│ - 使用 RemoteComposeWriter│                 │      │
│  │ - captureSingleRemoteDocument│ - 直接序列化缓冲区    │                 │      │
│  │ - 支持 Lambda 交互 │ - 支持 PendingIntent  │                 │      │
│  └─────────────────┘      └─────────────────┘      └─────────────────┘      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Widget 创建与更新流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Widget 创建与更新流程                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. Widget 首次创建 (onUpdate)                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   AppWidgetManager 请求更新                                          │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   RemoteComposeWidget.onUpdate()                                    │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────────────┐  │   │
│  │   │ RCWidget.createRemoteView()                                  │  │   │
│  │   │                                                              │  │   │
│  │   │ 1. WidgetLambdaAction.clear()                                │  │   │
│  │   │ 2. captureSingleRemoteDocument {                             │  │   │
│  │   │      CompositionLocalProvider(LocalWidget) {                 │  │   │
│  │   │        Content(context, widgetId)  // 用户定义的 @Composable │  │   │
│  │   │      }                                                       │  │   │
│  │   │    }                                                         │  │   │
│  │   │ 3. 返回 CapturedDocument (包含序列化后的 byte[])             │  │   │
│  │   │                                                              │  │   │
│  │   └─────────────────────────────────────────────────────────────┘  │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   getRemoteView(context, document, provider, widgetId)              │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────────────┐  │   │
│  │   │ RemoteViews.DrawInstructions.Builder(listOf(document.bytes))│  │   │
│  │   │                                                              │  │   │
│  │   │ + 为每个 clickable 区域设置 PendingIntent:                   │  │   │
│  │   │   rv.setOnClickPendingIntent(intentId, pendingIntent)       │  │   │
│  │   │                                                              │  │   │
│  │   └─────────────────────────────────────────────────────────────┘  │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   appWidgetManager.updateAppWidget(widgetId, remoteViews)           │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  2. Widget 点击交互 (onReceive)                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   用户点击 Widget 区域                                               │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   PendingIntent 触发 Broadcast -> onReceive()                       │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   RemoteComposeWidget.onReceive()                                   │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────────────┐  │   │
│  │   │ updateWidgets(context, widgetId, lambdaToRun)               │  │   │
│  │   │                                                              │  │   │
│  │   │ 1. 重新 capture document (获取最新状态)                     │  │   │
│  │   │ 2. WidgetLambdaAction.run(lambdaId) // 执行对应的 Lambda    │  │   │
│  │   │ 3. 重新 capture document (应用状态变更后)                   │  │   │
│  │   │ 4. 生成新的 RemoteViews 并更新                               │  │   │
│  │   │                                                              │  │   │
│  │   └─────────────────────────────────────────────────────────────┘  │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 交互机制：LambdaAction

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    WidgetLambdaAction 交互机制                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  创建阶段                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   @RemoteComposable                                                 │   │
│  │   fun MyWidget() {                                                  │   │
│  │       RemoteBox(                                                    │   │
│  │           modifier = RemoteModifier.clickable {                     │   │
│  │               // 这个 Lambda 被包装为 WidgetLambdaAction            │   │
│  │               counter.value++                                       │   │
│  │           }                                                         │   │
│  │       )                                                             │   │
│  │   }                                                                 │   │
│  │                                                                     │   │
│  │   序列化时:                                                         │   │
│  │   - 生成 HostAction(actionId = widgetId * 1000 + counter)         │   │
│  │   - counter++                                                       │   │
│  │   - 将 Lambda 存入 WidgetLambdaAction.map[actionId]               │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  点击阶段                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   用户点击 -> PendingIntent 携带 actionId                           │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   onReceive() 提取 actionId                                         │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   WidgetLambdaAction.run(actionId)                                  │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   map[actionId]?.content?.invoke() // 执行 Lambda                   │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   状态变更 -> 重新 capture -> 更新 RemoteViews                      │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. "帮我制作微件"功能架构

### 4.1 功能概述

> "RemoteCompose 还是'帮我制作微件'功能背后的引擎，用户可以问问 Gemini 构建完全自适应的自定义微件，这些微件可以无缝调整大小并针对用户的主屏幕或 Wear OS 手表进行优化。"

### 4.2 AI 生成微件架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                 "帮我制作微件" (Make My Widget) 架构                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  用户输入                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   "帮我做一个显示天气的圆形微件"                                       │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────────────┐  │   │
│  │   │                    Gemini (LLM)                              │  │   │
│  │   │                                                              │  │   │
│  │   │ 1. 理解用户意图 -> 微件类型、布局、数据源                      │  │   │
│  │   │ 2. 生成 RemoteCompose 代码 (@RemoteComposable)               │  │   │
│  │   │ 3. 考虑自适应布局 (不同尺寸)                                   │  │   │
│  │   │                                                              │  │   │
│  │   │ 输出:                                                        │  │   │
│  │   │ ```kotlin                                                    │  │   │
│  │   │ @RemoteComposable                                            │  │   │
│  │   │ fun WeatherWidget() {                                        │  │   │
│  │   │   RemoteBox(                                                 │  │   │
│  │   │     modifier = RemoteModifier.size(100.rdp)                  │  │   │
│  │   │                 .background(RemoteBrush.radial(...))         │  │   │
│  │   │   ) {                                                        │  │   │
│  │   │     RemoteText("24°C", fontSize = 32.sp)                     │  │   │
│  │   │     RemoteImage(weatherIcon)                                 │  │   │
│  │   │   }                                                          │  │   │
│  │   │ }                                                            │  │   │
│  │   │ ```                                                          │  │   │
│  │   └─────────────────────────────────────────────────────────────┘  │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────────────┐  │   │
│  │   │              RemoteCompose 编译/序列化                        │  │   │
│  │   │                                                              │  │   │
│  │   │ captureSingleRemoteDocument { WeatherWidget() }              │  │   │
│  │   │   -> CapturedDocument (byte[])                               │  │   │
│  │   │                                                              │  │   │
│  │   └─────────────────────────────────────────────────────────────┘  │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────────────┐  │   │
│  │   │                    自适应调整                                 │  │   │
│  │   │                                                              │  │   │
│  │   │ - 主屏幕尺寸: 2x2, 4x2, 1x1 等                              │  │   │
│  │   │ - Wear OS: 圆形表盘优化                                       │  │   │
│  │   │ - 使用 RemoteModifier 的响应式尺寸                            │  │   │
│  │   │                                                              │  │   │
│  │   └─────────────────────────────────────────────────────────────┘  │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────────────┐  │   │
│  │   │              RemoteViews.DrawInstructions                     │  │   │
│  │   │                                                              │  │   │
│  │   │ 安装到主屏幕 / Wear OS 表盘                                   │  │   │
│  │   │                                                              │  │   │
│  │   └─────────────────────────────────────────────────────────────┘  │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 GEMINI.md 中的 API 规范

`remote-creation-compose/GEMINI.md` 文件定义了 RemoteCompose Creation API 的规范，这些规范确保 AI 生成的代码符合 RemoteCompose 的要求：

| 规范 | 说明 |
|------|------|
| **Remote-First API** | 所有公共绘制 API 优先使用 "Remote" 类型 |
| **RemoteFloat** | 表示常量值或动态表达式（通过 ID），不可直接用于算术运算 |
| **RemotePaint** | 允许标准 Paint 属性与远程 ID 关联的桥接类 |
| **类型映射** | Float→RemoteFloat, String→RemoteString, Color→RemoteColor 等 |
| **增量 API 设计** | 从下游消费者（如 remote-material3）需要的最小方法集开始 |

---

## 5. 核心特性与平台支持

### 5.1 平台支持矩阵

| 平台 | 支持状态 | 说明 |
|------|---------|------|
| Android 16+ (Vanilla Ice Cream) | ✅ 原生支持 | `RemoteViews.DrawInstructions` 原生 API |
| Android 15 (Baklava) | ✅ 向后兼容 | 通过 GlanceTemplate 降级 |
| Android 14 及以下 | ✅ 向后兼容 | GlanceTemplate 优雅降级 |
| Wear OS | ✅ 支持 | 针对圆形/方形表盘优化 |
| Android Auto | ✅ 支持 | 车载微件新场景 |

### 5.2 关键特性

#### 5.2.1 SnapScroll（吸附滚动）

RemoteCompose 支持在微件中实现吸附滚动效果，让用户在滑动时内容自动吸附到特定位置。

```kotlin
RemoteColumn(
    modifier = RemoteModifier
        .fillMaxSize()
        .verticalScroll(),
) {
    // 内容项会自动支持吸附滚动
}
```

#### 5.2.2 富有表现力的按钮（Expressive Buttons）

支持更丰富的按钮交互效果：

```kotlin
RemoteBox(
    modifier = RemoteModifier
        .size(120.rdp, 48.rdp)
        .background(RemoteBrush.linearGradient(...))
        .border(2f, 24f, Color.White)
        .clickable { /* 动作 */ }
        .ripple() // 涟漪效果
) {
    RemoteText("Click Me", fontSize = 16.sp)
}
```

#### 5.2.3 粒子效果（Particle Effects）

RemoteCompose 内置粒子系统，支持在微件中实现高性能粒子动画：

```kotlin
// 创建 100 个粒子
val variables = Array(4) { RFloat(this, 0f) }
val psId = createParticles(
    variables,
    arrayOf(cx, cy, rand() - 0.5f, rand() - 0.5f),
    pCount = 100
)

// 每帧更新和绘制
particlesLoops(psId, updateEquations = arrayOf(
    px + dx * dt,
    py + dy * dt,
    dx,
    dy + 9.8f * dt // 重力
)) {
    drawCircle(px.toFloat(), py.toFloat(), 5f)
}
```

---

## 6. 技术实现细节

### 6.1 Android 16+ 原生支持

```kotlin
// Android 16 (Vanilla Ice Cream) 引入的 API
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
val drawInstructions = RemoteViews.DrawInstructions.Builder(
    listOf(document.bytes)
).build()

val remoteViews = RemoteViews(drawInstructions)
appWidgetManager.updateAppWidget(widgetId, remoteViews)
```

### 6.2 向后兼容机制

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    向后兼容机制                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Android 16+                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   RemoteCompose 二进制协议                                           │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   RemoteViews.DrawInstructions.Builder(bytes)                       │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   系统原生渲染 (高性能、丰富交互)                                     │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Android 15 及以下                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   RemoteCompose 二进制协议                                           │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   GlanceTemplate (模板转换)                                          │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   传统 RemoteViews (功能受限但兼容)                                   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 与 Glance 的集成点

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Glance ↔ RemoteCompose 集成点                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Glance 公共 API (开发者使用)                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  GlanceText, GlanceButton, GlanceImage...                           │   │
│  │  GlanceModifier.clickable, GlanceModifier.background...             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  Glance 内部实现                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  判断系统版本:                                                       │   │
│  │  - Android 16+: 使用 RemoteCompose 后端                             │   │
│  │  - Android 15-: 使用 GlanceTemplate 后端                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  RemoteCompose 后端 (Android 16+)                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │   │
│  │  │GlanceText ->│  │ RemoteText  │  │ 序列化为    │                 │   │
│  │  │GlanceButton->│  │ RemoteButton│  │ DRAW_TEXT   │                 │   │
│  │  │GlanceImage ->│  │ RemoteImage │  │ 操作码      │                 │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                 │   │
│  │                                                                     │   │
│  │  GlanceModifier -> RemoteModifier 映射                             │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 附录

### 7.1 关键代码文件索引

| 文件 | 路径 | 职责 |
|------|------|------|
| RemoteComposeWidget | `remote-creation-compose/.../widgets/RemoteComposeWidget.kt` | AppWidgetProvider 基类 |
| RCWidget | `remote-creation-compose/.../widgets/RCWidget.kt` | Compose 风格 Widget 实现 |
| ProceduralRCWidget | `remote-creation-compose/.../widgets/ProceduralRCWidget.kt` | 程序化 Widget 实现 |
| AbstractRCWidget | `remote-creation-compose/.../widgets/AbstractRCWidget.kt` | Widget 抽象基类 |
| WidgetLambdaAction | `remote-creation-compose/.../widgets/WidgetLambdaAction.kt` | 点击交互 Lambda 管理 |
| CaptureRemoteDocument | `remote-creation-compose/.../capture/CaptureRemoteDocument.kt` | 文档捕获 |
| GEMINI.md | `remote-creation-compose/GEMINI.md` | AI API 规范 |

### 7.2 相关文档索引

| 文档 | 路径 | 内容 |
|------|------|------|
| 粒子系统指南 | `remote-creation/doc/guides/PARTICLE_SYSTEM_GUIDE.md` | 粒子效果使用 |
| 触摸交互指南 | `remote-creation/doc/guides/TOUCH_GUIDE.md` | 触摸交互 API |
| Compose 组件指南 | `remote-creation/doc/guides/COMPOSE_COMPONENTS_GUIDE.md` | Compose-like API |
| 程序化组件指南 | `remote-creation/doc/guides/PROCEDURAL_COMPONENTS_GUIDE.md` | Procedural API |

### 7.3 版本要求

| 组件 | 最低 Android 版本 |
|------|------------------|
| RemoteCompose Widget (完整功能) | Android 16 (Vanilla Ice Cream) |
| RemoteCompose Widget (基本功能) | Android 15 (Baklava) |
| GlanceTemplate 降级 | Android 14 及以下 |

---

> **文档结束**
>
> 本文档分析了 RemoteCompose 与 Google 官方宣布的 Glance/Widget 增强功能之间的关系，揭示了 RemoteCompose 作为底层框架如何为 Android 微件系统带来革命性的交互和表现力提升。
