# 计划：整理 Remote Compose 支持的组件文档

## Summary

在项目根目录创建 `REMOTE_COMPOSE_COMPONENTS.md`，系统性地整理 Remote Compose 支持的所有组件，包括布局组件、绘制操作、修饰符、动作事件等，并提供 OpCode → 核心实现 → 创建 API 的完整映射。

## Current State Analysis

- 项目已有组件相关文档，但分散在 `remote-creation/doc/guides/` 下的多个文件中，侧重于"如何使用"而非"完整清单"
- `COMPONENTS_GUIDE.md` 仅是索引页，指向过程式和 Compose 风格两份指南
- `COMPOSE_COMPONENTS_GUIDE.md` 和 `PROCEDURAL_COMPONENTS_GUIDE.md` 只覆盖创建 API 层面，不包含 OpCode、核心实现类等底层信息
- 缺少一份从协议层到 API 层的完整组件参考文档

## Proposed Changes

### 文件：`/home/meizu/Documents/my_android_projects/androidx/compose/remote/REMOTE_COMPOSE_COMPONENTS.md`

创建新文档，包含以下章节：

#### 1. 概述
- Remote Compose 组件体系的三层架构：协议层(OpCode) → 核心层(Core Operations) → 创建层(Creation API)
- 组件分类总览图（Mermaid）

#### 2. 布局组件（13 种）
- 表格列出：组件名、OpCode、实现类、关键属性、功能描述、Compose API 对应
- 涵盖：RootLayout、Content、Box、Row、Column、Canvas、CanvasContent、Text、State、CollapsibleRow、CollapsibleColumn、Image、FitBox、Flow

#### 3. 绘制操作（21 种）
- 表格列出：操作名、OpCode、实现类、功能描述
- 按类别分组：形状绘制、文本绘制、位图绘制、路径绘制、特殊绘制

#### 4. 修饰符操作（26 种）
- 表格列出：修饰符名、OpCode、实现类、关键参数、功能描述
- 按类别分组：尺寸约束、间距边框、裁剪、交互事件、可见性偏移、图形变换、滚动、其他

#### 5. 动作/事件操作（9 种）
- 表格列出：操作名、OpCode、实现类、功能描述

#### 6. 数据/资源声明操作（14 种）
- 表格列出：操作名、OpCode、实现类、功能描述

#### 7. 表达式/变量操作（20+ 种）
- 按类别分组：浮点表达式、颜色表达式、路径表达式、文本表达式、函数定义

#### 8. 创建 API 速查
- 过程式 API（RemoteComposeContext）方法 → OpCode 映射表
- Compose 风格 API（@RemoteComposable）组件 → OpCode 映射表

#### 9. 关键文件索引
- 按模块分组的源码路径表

## Assumptions & Decisions

1. **文档语言**：中文，与用户请求一致
2. **文档位置**：项目根目录，命名为 `REMOTE_COMPOSE_COMPONENTS.md`
3. **覆盖范围**：包含所有已注册的 OpCode（0-250），不遗漏非实验性功能，实验性功能标注"实验性"
4. **格式**：Markdown 表格 + Mermaid 概览图，便于查阅
5. **不修改现有文档**：仅新增文档，不改动 `remote-creation/doc/guides/` 下的现有文件

## Verification Steps

1. 确认文档包含所有 13 种布局组件
2. 确认文档包含所有 21 种绘制操作
3. 确认文档包含所有 26 种修饰符操作
4. 确认 Mermaid 概览图语法正确
5. 确认 OpCode 与 Operations.java 中的定义一致
6. 确认创建 API 映射与 RemoteComposeWriter/RemoteComposeContext 中的方法一致
