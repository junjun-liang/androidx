# 计划：Jetpack Compose 与 Remote Compose 差异详解文档

## 概述

基于对项目源码的深入分析，编写一份详细的 Markdown 文档，全面介绍 Jetpack Compose 与 Remote Compose 的差异、适用场景和底层框架实现。

## 当前状态分析

已有的文档：
- `my_docs/jetpack_compose_architecture.md` — Jetpack Compose 整体架构
- `my_docs/remote_compose_architecture.md` — Remote Compose 整体架构
- `my_docs/remote_compose_glance_widget.md` — Remote Compose 与 Glance/Widget 生态关系

**缺失**：一份直接对比两者的差异文档，从底层实现到适用场景的全方位对比。

## 核心发现（来自源码分析）

### 1. 状态管理机制
- **Jetpack Compose**：基于 Snapshot（MVCC）+ Slot Table（Gap Buffer）+ Recomposer 的响应式系统。状态变化 → Invalidation → Recomposition（重新执行代码）。
  - 关键文件：`runtime/Composer.kt`、`runtime/Recomposer.kt`、`runtime/SnapshotState.kt`
- **Remote Compose**：基于 ID 寻址变量 + RPN 表达式引擎 + 脏追踪。状态变化 → 标记脏表达式 → 栈求值（重新计算值）。
  - 关键文件：`remote-core/RemoteContext.java`、`remote-core/Operations.java`、`remote-core/CoreDocument.java`

### 2. 渲染管线
- **Jetpack Compose**：Composition → LayoutNode 树 → Measure/Layout → Draw，单帧同步完成
  - 关键文件：`ui/node/LayoutNode.kt`、`ui/Modifier.kt`
- **Remote Compose**：Creation → Serialization → Transmission → Decoding → Playback Loop（异步管线）
  - 关键文件：`remote-core/WireBuffer.java`、`remote-core/doc/DATA_FLOW.md`

### 3. 组件模型
- **Jetpack Compose**：`@Composable` + Compiler Plugin → Slot Table → LayoutNode
- **Remote Compose**：`Operation` 子类 + OpCode → WireBuffer → Player 回放

### 4. Modifier 系统
- **Jetpack Compose**：`Modifier` → `Modifier.Node`（长生命周期，跨重组 diff）
- **Remote Compose**：`RemoteModifier` → `RecordingModifier` → `ModifierOperation`（序列化操作）

### 5. 编译器依赖
- **Jetpack Compose**：重度依赖 Compose Compiler Plugin（注入 startGroup/endGroup/changed 等）
- **Remote Compose**：`@RemoteComposable` 仅是标记注解，不依赖编译器插件转换

### 6. 时间模型
- **Jetpack Compose**：帧驱动（Choreographer），时间不是一等公民
- **Remote Compose**：时间作为一等公民变量（`ANIMATION_TIME`、`CONTINUOUS_SEC`），表达式引擎原生支持

## 输出计划

### 文件：`my_docs/compose_vs_remote_compose.md`

### 文档结构：

1. **概述** — 两者的定位和核心设计哲学差异
2. **架构对比** — 并排架构图，展示两者的分层差异
3. **状态管理对比** — Snapshot/Recomposition vs ID变量/RPN表达式
4. **渲染管线对比** — 同步本地管线 vs 异步远程管线
5. **组件模型对比** — Composable函数树 vs 操作码序列化
6. **Modifier系统对比** — Modifier.Node vs RecordingModifier
7. **编译器依赖对比** — Compiler Plugin vs 标记注解
8. **适用场景分析** — 各自的最佳使用场景
9. **技术选型指南** — 何时用哪个，何时组合使用
10. **附录** — 关键源码索引、版本要求

### 每个对比章节包含：
- ASCII 架构/流程对比图
- 核心差异表格
- 源码引用（具体文件路径和行号）
- 适用场景说明

## 实施步骤

1. 创建 `my_docs/compose_vs_remote_compose.md` 文件
2. 编写完整对比文档，包含架构图、流程图、差异表格、源码引用
3. 确保所有文件路径引用基于实际源码分析

## 假设与决策

- 文档语言：中文（与用户消息一致）
- 图表格式：ASCII art（与已有文档风格一致）
- 源码引用：使用实际文件路径，基于 Phase 1 探索结果
- 不修改任何现有文件，仅新增一个对比文档

## 验证步骤

- 检查文档中的文件路径是否真实存在
- 检查技术描述是否与源码一致
- 检查文档结构完整性（10个章节是否齐全）
