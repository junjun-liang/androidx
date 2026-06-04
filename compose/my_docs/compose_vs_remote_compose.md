# Jetpack Compose 与 Remote Compose 差异详解

> 基于 `/home/meizu/Documents/my_android_projects/androidx/compose` 源码分析
> 生成日期：2026-05-19

---

## 目录

1. [概述：核心设计哲学差异](#1-概述核心设计哲学差异)
2. [架构对比](#2-架构对比)
3. [状态管理对比](#3-状态管理对比)
4. [渲染管线对比](#4-渲染管线对比)
5. [组件模型对比](#5-组件模型对比)
6. [Modifier 系统对比](#6-modifier-系统对比)
7. [编译器依赖对比](#7-编译器依赖对比)
8. [适用场景分析](#8-适用场景分析)
9. [技术选型指南](#9-技术选型指南)
10. [附录](#10-附录)

---

## 1. 概述：核心设计哲学差异

### 1.1 一句话总结

**Jetpack Compose** 通过**重新执行代码**（重组）响应变化；**Remote Compose** 通过**重新计算值**（表达式引擎）响应变化。

### 1.2 核心隐喻

| 维度 | Jetpack Compose | Remote Compose |
|------|----------------|----------------|
| **核心隐喻** | 可执行的函数树 | 可传输的操作流 |
| **状态哲学** | 状态是对象，变化触发重组 | 状态是 ID→值映射，变化触发求值 |
| **更新机制** | 重新执行代码 | 重新计算值 |
| **通信模型** | 无通信（本地进程内） | 一次传输 + 本地求值 |
| **时间模型** | 帧驱动（Choreographer） | 时间是一等公民变量 |
| **执行环境** | 需要完整运行时 | 仅需操作码解释器 |

### 1.3 设计目标差异

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    设计目标对比                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Jetpack Compose                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  目标：构建高性能的本地声明式 UI                                      │   │
│  │                                                                     │   │
│  │  - 完整的 UI 能力（任意组件、任意交互）                              │   │
│  │  - 最小化重组范围（智能跳过）                                        │   │
│  │  - 跨平台统一 API                                                   │   │
│  │  - 开发者体验优先                                                   │   │
│  │  - 无安全限制                                                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Remote Compose                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  目标：跨进程/跨设备传输可交互的 UI                                  │   │
│  │                                                                     │   │
│  │  - 紧凑的二进制格式（传输效率）                                      │   │
│  │  - 播放端本地动画/交互（无需持续通信）                               │   │
│  │  - 安全的执行沙箱（仅预定义操作码）                                  │   │
│  │  - 向后兼容（优雅降级）                                             │   │
│  │  - 省电（最小化 IPC 和更新频率）                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 架构对比

### 2.1 并排架构图

```
  Jetpack Compose                              Remote Compose
  ═════════════════                             ═════════════════

  ┌───────────────────┐                        ┌───────────────────┐
  │   Material/M3     │                        │  Glance (上层API)  │
  │   设计系统组件     │                        │  微件/表盘/车载    │
  └─────────┬─────────┘                        └─────────┬─────────┘
            │                                            │
  ┌─────────▼─────────┐                        ┌─────────▼─────────┐
  │   Foundation      │                        │  remote-creation-  │
  │   基础组件        │                        │  compose           │
  │   (LazyColumn等)  │                        │  @RemoteComposable │
  └─────────┬─────────┘                        │  RemoteModifier    │
            │                                  └─────────┬─────────┘
  ┌─────────▼─────────┐                                  │
  │   UI Core         │                        ┌─────────▼─────────┐
  │   LayoutNode      │                        │  remote-creation   │
  │   Modifier.Node   │                        │  RemoteComposeWriter│
  │   MeasurePolicy   │                        │  RecordingModifier │
  └─────────┬─────────┘                        └─────────┬─────────┘
            │                                            │
  ┌─────────▼─────────┐                        ┌─────────▼─────────┐
  │   Runtime         │                        │  remote-core       │
  │   Composer        │                        │  CoreDocument      │
  │   Recomposer      │                        │  WireBuffer        │
  │   SlotTable       │                        │  Operations        │
  │   Snapshot        │                        │  RemoteContext     │
  └─────────┬─────────┘                        │  Expression Engine │
            │                                  └─────────┬─────────┘
  ┌─────────▼─────────┐                                  │
  │   Compiler Plugin │                        ┌─────────▼─────────┐
  │   (Kotlin 仓库)   │                        │  remote-player-*   │
  │   代码转换        │                        │  View/Compose 播放器│
  │   startGroup等    │                        │  AndroidCanvas渲染  │
  └───────────────────┘                        └───────────────────┘
```

### 2.2 关键架构差异

| 维度 | Jetpack Compose | Remote Compose |
|------|----------------|----------------|
| 分层模型 | Runtime → UI → Foundation → Material | Creation → Core → Player |
| 数据流方向 | 单向（状态→UI），本地闭环 | 双向分离（创建端→字节流→播放端） |
| 核心数据结构 | Slot Table（Gap Buffer） | WireBuffer（字节数组） |
| 节点模型 | LayoutNode 树 | Operation 列表 + 组件树 |
| 平台耦合 | commonMain 抽象，平台特定实现 | remote-core 纯 Java，平台无关 |
| 语言 | 全 Kotlin | remote-core Java，其余 Kotlin |

---

## 3. 状态管理对比

### 3.1 Jetpack Compose：Snapshot + Recomposition

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Jetpack Compose 状态管理流程                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   mutableStateOf(0)                                                         │
│        │                                                                    │
│        ▼                                                                    │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │  Snapshot    │ --> │  Invalidation│ --> │  Recomposer │                  │
│   │  (MVCC)     │     │  (标记无效)  │     │  (调度重组)  │                  │
│   └─────────────┘     └─────────────┘     └─────────────┘                  │
│                                                   │                         │
│                                                   ▼                         │
│   ┌─────────────────────────────────────────────────────────────────┐      │
│   │  Recomposition (重新执行 @Composable 函数)                       │      │
│   │                                                                  │      │
│   │  1. Composer.startRestartGroup()                                 │      │
│   │  2. 检查参数是否变化 (changed())                                  │      │
│   │  3. 如果变化 → 重新执行函数体                                     │      │
│   │  4. 如果未变化 → 跳过 (Skipping)                                 │      │
│   │  5. Composer.endRestartGroup()                                   │      │
│   │                                                                  │      │
│   │  位置记忆化 (Positional Memoization):                            │      │
│   │  - remember 的结果与 Slot Table 中的位置绑定                     │      │
│   │  - 重组时相同位置返回缓存值                                       │      │
│   │                                                                  │      │
│   └─────────────────────────────────────────────────────────────────┘      │
│                                                                             │
│   关键源码:                                                                 │
│   - runtime/Composer.kt — 组合器核心                                       │
│   - runtime/Recomposer.kt — 重组调度器                                     │
│   - runtime/SnapshotState.kt — Snapshot 状态系统                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Remote Compose：ID 变量 + RPN 表达式

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Remote Compose 状态管理流程                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   创建端: 定义变量和表达式                                                    │
│   ┌─────────────────────────────────────────────────────────────────┐      │
│   │  val x = mutableFloat(0f)  // 注册 ID=42, 初始值=0.0           │      │
│   │  val y = mutableFloat(0f)  // 注册 ID=43, 初始值=0.0           │      │
│   │  val result = x + y * 2   // 注册 ID=44, 表达式=[43, 2.0, MUL,│      │
│   │                              //            42, ADD]             │      │
│   │                                                                  │      │
│   │  序列化为 ANIMATED_FLOAT 操作码 → WireBuffer                     │      │
│   └─────────────────────────────────────────────────────────────────┘      │
│                                                                             │
│   播放端: 本地求值                                                           │
│   ┌─────────────────────────────────────────────────────────────────┐      │
│   │                                                                  │      │
│   │  变量更新 (触摸/时间/传感器):                                     │      │
│   │    RemoteContext.setFloat(42, 5.0)  // x = 5.0                  │      │
│   │         │                                                        │      │
│   │         ▼                                                        │      │
│   │    脏追踪: ID=44 依赖 ID=42 → 标记为脏                           │      │
│   │         │                                                        │      │
│   │         ▼                                                        │      │
│   │    RPN 栈求值:                                                   │      │
│   │    [43, 2.0, MUL, 42, ADD]                                      │      │
│   │    → 压栈 43(y) → 压栈 2.0 → MUL(y*2) → 压栈 42(x) → ADD       │      │
│   │    → result = x + y*2 = 5.0 + 0*2 = 5.0                        │      │
│   │                                                                  │      │
│   │    无需重新执行创建端代码！                                       │      │
│   │                                                                  │      │
│   └─────────────────────────────────────────────────────────────────┘      │
│                                                                             │
│   关键源码:                                                                 │
│   - remote-core/RemoteContext.java — 运行时上下文和变量管理                 │
│   - remote-core/Operations.java — 操作码和表达式定义                       │
│   - remote-core/CoreDocument.java — 文档状态管理                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 核心差异表

| 维度 | Jetpack Compose | Remote Compose |
|------|----------------|----------------|
| 状态标识 | 对象引用 + Slot Table 位置 | 整数 ID |
| 变化传播 | Snapshot → Invalidation → Recomposition | 变量更新 → 脏标记 → 栈求值 |
| 计算位置 | 本地进程内 | 播放端本地（表达式引擎） |
| 通信开销 | 无（同进程） | 仅初始传输，后续无需通信 |
| 时间变量 | 无内置概念 | `ANIMATION_TIME`、`CONTINUOUS_SEC` |
| 传感器变量 | 需自行订阅 | `ACCELEROMETER_X/Y/Z` 内置 |
| 重组/求值 | 重新执行整个 Composable 函数 | 仅重新计算受影响的表达式 |

---

## 4. 渲染管线对比

### 4.1 Jetpack Compose：同步本地管线

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Jetpack Compose 渲染管线（单帧内同步完成）                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   状态变化                                                                  │
│      │                                                                      │
│      ▼                                                                      │
│   ┌─────────────┐                                                           │
│   │ Composition │  @Composable 函数执行 → LayoutNode 树构建/更新             │
│   │  (组合)     │  Composer 写入 Slot Table                                  │
│   └──────┬──────┘                                                           │
│          │                                                                  │
│          ▼                                                                  │
│   ┌─────────────┐                                                           │
│   │   Layout    │  自顶向下传递 Constraints                                  │
│   │  (布局)     │  自底向上测量 + 放置                                       │
│   └──────┬──────┘  MeasurePolicy.measure() → Placeable                      │
│          │                                                                  │
│          ▼                                                                  │
│   ┌─────────────┐                                                           │
│   │    Draw     │  LayoutNode 遍历绘制                                       │
│   │  (绘制)     │  NodeCoordinator 调用 Modifier.DrawModifierNode           │
│   └──────┬──────┘  Canvas 绘制到硬件加速的 RenderNode                       │
│          │                                                                  │
│          ▼                                                                  │
│   ┌─────────────┐                                                           │
│   │   Submit    │  提交渲染指令到 GPU                                        │
│   │  (提交)     │  Choreographer 同步 VSync                                  │
│   └─────────────┘                                                           │
│                                                                             │
│   特点: 全部在同一帧内完成，无跨进程通信                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Remote Compose：异步远程管线

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Remote Compose 渲染管线（跨进程/跨设备）                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─── 创建端 (Host Process) ───────────────────────────────────────────┐  │
│   │                                                                     │  │
│   │  @RemoteComposable 函数执行                                         │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │  RemoteComposeWriter 发射操作到 WireBuffer                          │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │  byte[] (二进制文档)                                                │  │
│   │                                                                     │  │
│   └──────────────────────────┬──────────────────────────────────────────┘  │
│                              │ 传输 (IPC/网络/文件)                         │
│                              ▼                                              │
│   ┌─── 播放端 (Player Process) ─────────────────────────────────────────┐  │
│   │                                                                     │  │
│   │  CoreDocument 读取 WireBuffer                                       │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │  Operations.read() → 重建操作列表 + 组件树 + 变量注册               │  │
│   │                                                                     │  │
│   │  Playback Loop (VSync 驱动):                                        │  │
│   │  ┌─────────────────────────────────────────────────────────────┐   │  │
│   │  │ 1. RemoteClock 更新时间变量                                   │   │  │
│   │  │ 2. RemoteContext 评估脏表达式 (RPN 求值)                     │   │  │
│   │  │ 3. LayoutCompute 计算组件位置和大小                           │   │  │
│   │  │ 4. 遍历操作列表 → Operation.apply(RemoteContext)             │   │  │
│   │  │ 5. 绘制操作 → Canvas 绘制                                    │   │  │
│   │  │ 6. 触摸事件处理 → 本地变量更新 / Host 回调                   │   │  │
│   │  └─────────────────────────────────────────────────────────────┘   │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   特点: 创建和播放完全解耦，动画/交互在播放端本地完成                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 核心差异表

| 维度 | Jetpack Compose | Remote Compose |
|------|----------------|----------------|
| 管线类型 | 同步本地 | 异步远程 |
| 帧驱动 | Recomposer + Choreographer | Player Playback Loop + VSync |
| 数据结构 | LayoutNode 树（内存对象） | Operation 列表（解码后对象） |
| 跨帧更新 | 重组（重新执行 Composable） | 变量更新 + 表达式求值 |
| 序列化 | 无 | WireBuffer 二进制协议 |
| 通信频率 | 无（同进程） | 一次传输，后续无需通信 |

---

## 5. 组件模型对比

### 5.1 Jetpack Compose：Composable 函数树

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Jetpack Compose 组件模型                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  源码:                                                                      │
│  @Composable                                                                │
│  fun MyScreen() {                                                           │
│      Column {                                                               │
│          Text("Hello")                                                      │
│          Button(onClick = { ... }) { Text("Click") }                        │
│      }                                                                      │
│  }                                                                          │
│                                                                             │
│  编译后 (Compiler Plugin 转换):                                              │
│  fun MyScreen($composer: Composer, $changed: Int) {                         │
│      $composer.startRestartGroup(key)                                       │
│      Column($composer, ...) {                                               │
│          $composer.startReplaceableGroup(key)                               │
│          Text("Hello", $composer, ...)                                      │
│          $composer.endReplaceableGroup()                                    │
│          $composer.startReplaceableGroup(key)                               │
│          Button(onClick, $composer, ...) { ... }                            │
│          $composer.endReplaceableGroup()                                    │
│      }                                                                      │
│      $composer.endRestartGroup()?.updateScope { ... }                       │
│  }                                                                          │
│                                                                             │
│  运行时结构:                                                                 │
│  Slot Table (Gap Buffer)          LayoutNode 树                             │
│  ┌──────────────────┐            ┌──────────────────┐                       │
│  │ Group: MyScreen  │            │ LayoutNode:Column│                       │
│  │  Group: Column   │ ────────> │  ├─ LayoutNode:Text│                     │
│  │   Group: Text    │            │  └─ LayoutNode:Btn│                      │
│  │   Group: Button  │            └──────────────────┘                       │
│  └──────────────────┘                                                       │
│                                                                             │
│  组件身份: 调用位置 (Slot Table 中的 Group Key)                               │
│  组件执行: 运行时直接执行 Composable 函数                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Remote Compose：操作码序列化

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Remote Compose 组件模型                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  源码 (Compose-like API):                                                   │
│  @RemoteComposable                                                          │
│  @Composable                                                                │
│  fun MyWidget() {                                                           │
│      RemoteColumn {                                                         │
│          RemoteText("Hello")                                                │
│          RemoteBox(modifier = RemoteModifier.clickable { ... })             │
│      }                                                                      │
│  }                                                                          │
│                                                                             │
│  创建端执行 → 序列化为操作码:                                                │
│  WireBuffer:                                                                │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ HEADER(version, width, height, ...)                                 │  │
│  │ DATA_TEXT(1, "Hello")                                               │  │
│  │ COMPONENT_START(id=1, type=COLUMN, modifiers=[])                    │  │
│  │   COMPONENT_START(id=2, type=TEXT, modifiers=[])                    │  │
│  │     DRAW_TEXT_RUN(textId=1, ...)                                    │  │
│  │   CONTAINER_END                                                     │  │
│  │   COMPONENT_START(id=3, type=BOX, modifiers=[clickable])            │  │
│  │     CLICK_AREA(actionId=1001)                                       │  │
│  │   CONTAINER_END                                                     │  │
│  │ CONTAINER_END                                                       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  播放端解码 → 重建结构:                                                      │
│  Operation 列表                 组件树                                       │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │ HEADER           │         │ Column           │                         │
│  │ DATA_TEXT        │ ------> │  ├─ Text          │                         │
│  │ COMPONENT_START  │         │  └─ Box [click]   │                         │
│  │ DRAW_TEXT_RUN    │         └──────────────────┘                         │
│  │ CONTAINER_END    │                                                      │
│  │ ...              │                                                      │
│  └──────────────────┘                                                      │
│                                                                             │
│  组件身份: 操作码 + 组件 ID                                                  │
│  组件执行: 创建端执行后序列化，播放端回放                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 核心差异表

| 维度 | Jetpack Compose | Remote Compose |
|------|----------------|----------------|
| 组件定义 | `@Composable` 函数 | `Operation` 子类 + `@RemoteComposable` |
| 组件身份 | 调用位置（Slot Table Group Key） | 操作码 + 组件 ID |
| 组件执行 | 运行时直接执行 | 创建端执行后序列化，播放端回放 |
| 层级构建 | `ComposeNode` → `LayoutNode` 树 | `COMPONENT_START`/`CONTAINER_END` 对 |
| 可用组件 | 完整 Material/M3 组件库 | RemoteBox/Column/Row/Text/Canvas/Image 子集 |
| 自定义组件 | 任意 `@Composable` 函数 | 需实现 `Operation` 子类并注册 OpCode |

---

## 6. Modifier 系统对比

### 6.1 Jetpack Compose：Modifier.Node 链

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Jetpack Compose Modifier 系统                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Modifier 链:                                                               │
│  Modifier.padding(16.dp).background(Color.Blue).clickable { ... }          │
│                                                                             │
│  运行时节点链:                                                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │ PaddingNode  │ -> │BackgroundNode│ -> │ ClickableNode│ -> LayoutNode   │
│  │ (LayoutModifier)│  │ (DrawModifier)│  │(PointerInput)│                  │
│  └──────────────┘    └──────────────┘    └──────────────┘                  │
│                                                                             │
│  特点:                                                                      │
│  - Modifier.Node 是长生命周期对象，跨重组持久存在                            │
│  - 通过 diff 机制更新（attach/detach/update）                               │
│  - 可实现多个接口：LayoutModifierNode, DrawModifierNode,                    │
│    SemanticsModifierNode, PointerInputModifierNode 等                       │
│  - 可访问任意 Kotlin API，无安全限制                                        │
│  - 完整的修饰符库（数十种）                                                 │
│                                                                             │
│  关键源码: ui/Modifier.kt, ui/node/ModifierNode.kt                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Remote Compose：RecordingModifier 序列化

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Remote Compose Modifier 系统                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  RemoteModifier 链:                                                         │
│  RemoteModifier.padding(16.rdp).background(Color.Blue).clickable { ... }   │
│                                                                             │
│  序列化过程:                                                                │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │ PaddingElement│ -> │BackgroundElem│ -> │ ClickableElem│                  │
│  │ (RemoteModifier│    │(RemoteModifier│    │(RemoteModifier│                │
│  │  .Element)    │    │  .Element)   │    │  .Element)   │                  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                  │
│         │ toRecordingModifier()                     │                        │
│         ▼                   ▼                       ▼                        │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │PaddingModOp  │    │BackgroundModOp│   │ ClickModOp   │                  │
│  │(ModifierOper-│    │(ModifierOper- │    │(ModifierOper-│                  │
│  │ ation)       │    │ ation)        │    │ ation)       │                  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                  │
│         │ 序列化到 WireBuffer                      │                        │
│         ▼                                                          ▼        │
│  COMPONENT_START(modifiers=[PaddingOp, BackgroundOp, ClickOp])             │
│                                                                             │
│  播放端:                                                                    │
│  ComponentModifiers 持有 ArrayList<ModifierOperation>                       │
│  apply() 时遍历列表逐个应用                                                 │
│                                                                             │
│  特点:                                                                      │
│  - 无 diff 机制，每次完整序列化                                             │
│  - 参数可以是动态表达式 ID（如 visibility(intState)）                       │
│  - 有限的修饰符集（约 15 种核心修饰符）                                     │
│  - 自定义修饰符需实现 ModifierOperation 并注册 OpCode                       │
│                                                                             │
│  关键源码:                                                                  │
│  - remote-creation-compose/.../modifier/RemoteModifier.kt                  │
│  - remote-core/.../operations/layout/modifiers/ComponentModifiers.java     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 核心差异表

| 维度 | Jetpack Compose Modifier | Remote Compose RemoteModifier |
|------|-------------------------|-------------------------------|
| 接口 | `Modifier` (commonMain) | `RemoteModifier` (Android-specific) |
| 节点模型 | `Modifier.Node`（长生命周期，跨重组 diff） | `ModifierOperation`（序列化操作，无 diff） |
| 执行方式 | 本地直接执行 | 序列化为操作码，播放端回放 |
| 动态值 | `mutableStateOf` + 重组 | 表达式 ID + RPN 求值 |
| 修饰符丰富度 | 完整（数十种） | 有限（约 15 种核心修饰符） |
| 自定义能力 | 任意 `Modifier.Node` 子类 | 需实现 `ModifierOperation` 并注册 OpCode |

---

## 7. 编译器依赖对比

### 7.1 Jetpack Compose：重度依赖 Compiler Plugin

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Compose Compiler Plugin 转换                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  源码:                                                                      │
│  @Composable fun Greeting(name: String) { Text("Hi $name") }               │
│                                                                             │
│  编译后:                                                                    │
│  fun Greeting(name: String, $composer: Composer, $changed: Int) {          │
│      $composer.startRestartGroup(1234)                                      │
│      val changed = $composer.changed(name)                                  │
│      if (changed || !$composer.skipping) {                                  │
│          Text("Hi $name", $composer, 0)                                     │
│      } else {                                                               │
│          $composer.skipToEndGroup()  // 跳过未变化的 Composable             │
│      }                                                                      │
│      $composer.endRestartGroup()?.updateScope {                             │
│          Greeting(name, it, 0b111)  // 重组时重新调用                       │
│      }                                                                      │
│  }                                                                          │
│                                                                             │
│  Plugin 注入的关键调用:                                                      │
│  - startRestartGroup / endRestartGroup  → 重组作用域                        │
│  - startReplaceableGroup / endReplaceableGroup → 不可重启作用域              │
│  - changed() → 参数变化检测                                                 │
│  - skipToEndGroup() → 跳过优化                                              │
│  - updateScope {} → 注册重组回调                                            │
│                                                                             │
│  位置: Compose Compiler Plugin (已迁移至 Kotlin 仓库)                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Remote Compose：仅标记注解

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  @RemoteComposable 注解                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  @RemoteComposable 是一个标记注解（Marker Annotation），                    │
│  不触发任何编译器转换。                                                      │
│                                                                             │
│  它的作用:                                                                  │
│  1. 文档标记 — 表示此 Composable 函数用于创建远程文档                        │
│  2. API 约束 — 提醒开发者只能使用 Remote* 类型的组件和修饰符                │
│  3. captureSingleRemoteDocument 的内容参数类型标记                          │
│                                                                             │
│  无编译器插件意味着:                                                         │
│  - 没有 startGroup/endGroup 注入                                            │
│  - 没有 changed() 检测                                                      │
│  - 没有 Skipping/Restarting 逻辑                                            │
│  - 函数在创建端正常执行，结果被序列化                                       │
│                                                                             │
│  关键源码: remote-creation-compose/.../layout/RemoteComposable.kt           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.3 核心差异表

| 维度 | Jetpack Compose | Remote Compose |
|------|----------------|----------------|
| 编译器插件 | 重度依赖 | 无 |
| 注解作用 | 触发代码转换 | 仅标记 |
| 参数注入 | `$composer`, `$changed` | 无额外参数 |
| 代码转换 | startGroup/endGroup/changed | 无转换 |
| Skipping 优化 | 编译器生成跳过逻辑 | 无（创建端只执行一次） |
| 重组支持 | 编译器生成 updateScope | 无重组概念 |

---

## 8. 适用场景分析

### 8.1 Jetpack Compose 适用场景

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Jetpack Compose 最佳场景                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ✅ 全功能应用 UI                                                           │
│     - 复杂的屏幕布局和导航                                                  │
│     - 完整的 Material Design 组件                                           │
│     - 自定义视图和动画                                                      │
│                                                                             │
│  ✅ 需要完整平台能力的场景                                                   │
│     - 网络请求、数据库访问                                                  │
│     - 文件系统操作                                                          │
│     - 平台 API 调用                                                         │
│                                                                             │
│  ✅ 高交互性应用                                                            │
│     - 手势密集的界面（拖拽、缩放、滑动）                                    │
│     - 实时数据更新                                                          │
│     - 复杂的状态管理                                                        │
│                                                                             │
│  ✅ 跨平台应用                                                              │
│     - Android + iOS + Desktop + Web                                         │
│     - 共享业务逻辑和 UI                                                     │
│                                                                             │
│  ❌ 不适用场景                                                              │
│     - 跨进程 UI 渲染（如 Widget）                                           │
│     - 需要序列化传输的 UI                                                   │
│     - 安全沙箱中的 UI 渲染                                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Remote Compose 适用场景

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Remote Compose 最佳场景                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ✅ Android 主屏幕微件 (AppWidget)                                          │
│     - 通过 RemoteViews.DrawInstructions 传输                               │
│     - Android 16+ 原生支持，旧版优雅降级                                   │
│     - 支持 SnapScroll、粒子效果、富有表现力的按钮                           │
│                                                                             │
│  ✅ Wear OS 表盘和微件                                                      │
│     - 圆形/方形表盘自适应                                                   │
│     - 省电的本地动画                                                        │
│                                                                             │
│  ✅ Android Auto 车载微件                                                   │
│     - 2.5 亿辆兼容汽车的新机会                                              │
│     - 安全的 UI 渲染（仅预定义操作码）                                      │
│                                                                             │
│  ✅ AI 生成微件 ("帮我制作微件")                                            │
│     - Gemini 生成 @RemoteComposable 代码                                    │
│     - 自动序列化和安装                                                      │
│     - 自适应不同设备尺寸                                                    │
│                                                                             │
│  ✅ 跨进程/跨设备 UI                                                        │
│     - 通知栏自定义布局                                                      │
│     - 服务端驱动的 UI                                                       │
│     - 蓝牙/网络传输的 UI                                                    │
│                                                                             │
│  ❌ 不适用场景                                                              │
│     - 复杂的应用界面（组件有限）                                            │
│     - 需要完整平台 API 的场景                                               │
│     - 需要自定义 View 的场景                                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. 技术选型指南

### 9.1 决策树

```
需要构建 UI？
    │
    ├── UI 在本地进程内渲染？
    │       │
    │       ├── 是 → Jetpack Compose
    │       │
    │       └── 否 → UI 需要跨进程/跨设备传输？
    │               │
    │               ├── 是 → Remote Compose
    │               │
    │               └── 否 → Jetpack Compose
    │
    ├── 是 Android 主屏幕微件？
    │       │
    │       ├── 是 → Glance (底层使用 Remote Compose)
    │       │
    │       └── 否 → 继续
    │
    ├── 是 Wear OS 表盘？
    │       │
    │       ├── 是 → Remote Compose
    │       │
    │       └── 否 → 继续
    │
    ├── 需要 AI 生成 UI？
    │       │
    │       ├── 是 → Remote Compose (@RemoteComposable)
    │       │
    │       └── 否 → 继续
    │
    ├── 需要完整组件库和平台能力？
    │       │
    │       ├── 是 → Jetpack Compose
    │       │
    │       └── 否 → Remote Compose
    │
    └── 需要省电的本地动画？
            │
            ├── 是 → Remote Compose (RPN 表达式引擎)
            │
            └── 否 → Jetpack Compose
```

### 9.2 组合使用场景

| 场景 | Jetpack Compose | Remote Compose | 组合方式 |
|------|----------------|----------------|---------|
| 应用 + 微件 | 应用主体 | 微件部分 | Glance 桥接 |
| 应用 + Wear OS | 手机端 | 手表端 | RemoteCompose 传输 |
| 应用 + 车载 | 手机端 | 车载端 | Android Auto 框架 |
| AI 生成 + 应用 | 预览/编辑 | 生成/部署 | captureSingleRemoteDocument |

### 9.3 能力边界总结

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          能力边界                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  能力                          Jetpack Compose    Remote Compose            │
│  ─────────────────────────────  ───────────────   ──────────────            │
│  完整组件库                     ✅                 ❌ (子集)                 │
│  自定义组件                     ✅ 任意             ⚠️ 需注册 OpCode          │
│  完整 Modifier                  ✅ 数十种           ⚠️ 约15种核心            │
│  跨进程传输                     ❌                  ✅                       │
│  省电本地动画                   ⚠️ 需持续重组       ✅ 表达式引擎             │
│  安全沙箱执行                   ❌                  ✅ 仅预定义操作码         │
│  向后兼容                       N/A                ✅ GlanceTemplate 降级    │
│  AI 代码生成                    ❌                  ✅ GEMINI.md 规范         │
│  时间作为一等公民               ❌                  ✅ ANIMATION_TIME         │
│  传感器变量                     ⚠️ 需自行订阅       ✅ 内置支持               │
│  粒子系统                       ❌                  ✅ 内置支持               │
│  触摸交互(本地)                 ✅                  ✅ addTouch 系统          │
│  跨平台(Android/iOS/Desktop)   ✅                  ❌ 仅 Android 目标        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. 附录

### 10.1 关键源码索引

| 模块 | 文件 | 职责 |
|------|------|------|
| **Jetpack Compose** | | |
| Runtime | `runtime/Composer.kt` | 组合器核心 |
| Runtime | `runtime/Recomposer.kt` | 重组调度器 |
| Runtime | `runtime/SnapshotState.kt` | Snapshot 状态系统 |
| Runtime | `runtime/design/how-compose-works.md` | Compose 工作原理设计文档 |
| UI | `ui/Modifier.kt` | Modifier 接口定义 |
| UI | `ui/node/LayoutNode.kt` | 布局节点核心 |
| **Remote Compose** | | |
| Core | `remote/remote-core/CoreDocument.java` | 文档核心类 |
| Core | `remote/remote-core/WireBuffer.java` | 二进制缓冲区 |
| Core | `remote/remote-core/Operations.java` | 操作码定义 |
| Core | `remote/remote-core/RemoteContext.java` | 运行时上下文 |
| Core | `remote/remote-core/doc/REMOTE_COMPOSE_ARCHITECTURE.md` | 系统架构文档 |
| Core | `remote/remote-core/doc/DATA_FLOW.md` | 数据流文档 |
| Creation | `remote/remote-creation-compose/.../RemoteModifier.kt` | 远程修饰符 |
| Creation | `remote/remote-creation-compose/.../RemoteComposeWidget.kt` | Widget 基类 |
| Creation | `remote/remote-creation-compose/.../RCWidget.kt` | Compose Widget 实现 |
| Creation | `remote/remote-creation-compose/GEMINI.md` | AI API 规范 |

### 10.2 版本要求

| 组件 | Jetpack Compose | Remote Compose |
|------|----------------|----------------|
| 最低 Android 版本 | API 21+ | API 35+ (Vanilla Ice Cream) 完整功能 |
| 向后兼容 | N/A | API 34- 通过 GlanceTemplate 降级 |
| Kotlin 版本 | 1.9+ | 1.9+ |
| Compose Compiler | 与 Kotlin 版本匹配 | 不需要 |

### 10.3 相关文档

| 文档 | 路径 |
|------|------|
| Jetpack Compose 架构 | `my_docs/jetpack_compose_architecture.md` |
| Remote Compose 架构 | `my_docs/remote_compose_architecture.md` |
| Remote Compose 与 Glance/Widget | `my_docs/remote_compose_glance_widget.md` |

---

> **文档结束**
>
> 本文档基于对 Jetpack Compose 和 Remote Compose 源码的深入对比分析，从设计哲学、状态管理、渲染管线、组件模型、Modifier 系统、编译器依赖、适用场景等七个维度全面阐述了两者的差异。
