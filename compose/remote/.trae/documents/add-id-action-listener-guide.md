# 计划：整理 addIdActionListener 使用方法文档

## Summary
整理 Remote Compose 中 `addIdActionListener` 的完整使用方法，包括 API 签名、三层封装（Core/View/Compose）、创建端对应 API、完整事件流、代码示例，输出为一份 Markdown 文档。

## Current State Analysis
- `addIdActionListener` 是 Remote Compose 中 ID 动作回调的核心注册方法
- 存在三层 API 封装：CoreDocument（底层）、RemoteComposePlayer（View 层）、RemoteDocumentPlayer（Compose 层）
- 创建端通过 `HostAction(id)` 和 `addClickArea()` 定义触发源
- 代码库中有多个使用示例（TickerActivity、ExperimentActivity 等）
- 目前没有专门的文档整理这些信息

## Proposed Changes

### 输出文件
- `/home/meizu/Documents/my_android_projects/androidx/compose/remote/ADD_ID_ACTION_LISTENER_GUIDE.md`

### 文档结构

1. **概述** — addIdActionListener 的作用和定位
2. **API 签名速查** — 三层 API 的签名对照表
3. **三层 API 详解**
   - CoreDocument.IdActionCallback + addIdActionListener（底层）
   - RemoteComposePlayer.IdActionCallbacks + addIdActionListener（View 层）
   - RemoteDocumentPlayer.onAction 参数（Compose 层）
4. **创建端对应 API** — 如何在文档中定义触发 IdActionCallback 的动作
   - HostAction(int id) — 无 metadata
   - HostAction(int id, int metadataId) — 带 metadata
   - addClickArea(id, ..., metadata) — 画布级点击区域
   - clickable(HostAction) — 组件级点击修饰符
5. **完整事件流** — 从用户点击到回调触发的完整链路图
6. **代码示例**
   - Java View 层使用示例（TickerActivity）
   - Kotlin Compose 层使用示例（RemoteDocumentPlayer）
   - 创建端 + 播放端配对示例
7. **注意事项** — 多监听器、metadata 可空性差异、clearActionCallbacks 等

### 内容来源（已探索确认）
- CoreDocument.java: IdActionCallback 接口、addIdActionListener、notifyOfException、performClick、onClick
- RemoteComposePlayer.java: IdActionCallbacks 接口、addIdActionListener 封装
- RemoteComposeView.java: ClickCallbacks 接口、addIdActionListener 封装、performClick
- RemoteDocumentPlayer.kt: onAction 参数、内部桥接实现
- HostAction.java (creation-core): 四个构造函数、write 方法
- HostAction.kt (creation-compose): 顶层工厂函数、内部类
- HostActionOperation.java: runAction 实现
- HostActionMetadataOperation.java: runAction 实现
- ClickArea.java: apply 实现
- AndroidRemoteContext.java: runAction 桥接
- SimpleClick.java: 创建端示例
- TickerActivity.java: 播放端示例
- ExperimentActivity.kt: 播放端示例

## Assumptions & Decisions
- 文档语言：中文
- 输出路径：项目根目录
- 文档包含流程图（使用 ASCII art）
- 代码示例引用实际源码文件路径

## Verification Steps
- 确认文档中所有 API 签名与源码一致
- 确认文件路径引用正确
- 确认事件流描述与代码逻辑匹配
