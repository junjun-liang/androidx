# 计划：Remote Compose 客户端与桌面应用交互框架流程文档

## Summary

在项目根目录创建 `CLIENT_DESKTOP_INTERACTION_FLOW.md`，整理 Remote Compose 客户端（Android Player）与桌面应用端（Host/Creation）的交互框架流程，包含 Mermaid 流程图。

## Current State Analysis

- 项目已有 `REMOTE_COMPOSE_ARCHITECTURE.md` 和 `DATA_FLOW.md`，但偏重内部模块说明，未聚焦"客户端-桌面端交互"视角
- 核心发现：**Remote Compose 不包含独立的网络传输层**，文档以 `byte[]` 传递，传输方式由集成方决定
- 交互模式为：Host 创建文档 → 序列化为 byte[] → 传输（DrawInstructions/Intent/文件）→ Client 解码渲染
- 用户交互通过 HostAction 回调机制回传，由宿主应用决定处理方式
- 支持增量更新（DOC_DATA_UPDATE 标志）

## Proposed Changes

### 文件：`/home/meizu/Documents/my_android_projects/androidx/compose/remote/CLIENT_DESKTOP_INTERACTION_FLOW.md`

创建新文档，包含以下章节：

#### 1. 概述
- Remote Compose 交互架构的核心设计理念：单向文档传输 + 本地渲染 + 回调交互
- 关键特性：无内置传输层、byte[] 为核心数据载体、HostAction 回调机制

#### 2. 总体架构流程图（Mermaid）
- 展示 Host → byte[] → Transport → Client 的端到端流程
- 标注三种传输通道：RemoteViews.DrawInstructions、Intent、文件

#### 3. 文档创建与序列化流程（Mermaid）
- Host 端：RemoteComposeWriter / RemoteComposeContext → WireBuffer → byte[]
- Compose 方式：@RemoteComposable → captureSingleRemoteDocument → CapturedDocument.bytes

#### 4. 传输通道详解
- RemoteViews.DrawInstructions（AppWidget/通知场景）
- Intent 传递（应用间跳转场景）
- 文件 URI（ADB 调试场景）
- 直接内存传递（同一应用内 Compose 预览场景）

#### 5. 客户端解码与渲染初始化流程（Mermaid）
- RemoteDocument(byte[]) → CoreDocument.initFromBuffer() → 操作列表解析
- RemoteComposePlayer.setDocument() → RemoteComposeView 初始化
- 播放循环：时间更新 → 变量求值 → 布局 → 绘制

#### 6. 用户交互事件流程（Mermaid）
- 触摸事件：RemoteComposeView.onTouchEvent → CoreDocument.onClick → HostAction 触发
- 回调出口：onAction(id, metadata) / onNamedAction(name, value, stateUpdater)
- StateUpdater 变量修改 → 触发重绘

#### 7. 增量更新机制流程（Mermaid）
- Header DOC_DATA_UPDATE 标志
- CoreDocument.applyUpdate() 合并策略
- 支持更新的数据类型

#### 8. 动态状态/变量同步机制
- 命名变量覆盖（StateUpdater）
- 系统变量（WINDOW_WIDTH、ANIMATION_TIME、TOUCH_X 等）
- 表达式引擎本地求值

#### 9. 关键类与文件索引

## Assumptions & Decisions

1. **文档语言**：中文
2. **文档位置**：项目根目录，命名 `CLIENT_DESKTOP_INTERACTION_FLOW.md`
3. **流程图**：使用 Mermaid 语法，至少 5 个流程图（总体架构、文档创建、解码渲染、用户交互、增量更新）
4. **强调交互本质**：明确说明框架不包含传输层，传输由集成方实现
5. **不修改现有文档**

## Verification Steps

1. 确认文档包含 5 个以上 Mermaid 流程图
2. 确认三种传输通道均有说明
3. 确认 HostAction 回调机制流程完整
4. 确认增量更新流程描述准确
5. 确认关键类名和文件路径与源码一致
