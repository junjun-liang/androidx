# Plan: remote-core 模块架构文档整理

## Summary

整理 `remote-core` 模块的软件设计架构、设计模式和使用方法，生成包含架构图和流程图的文档，输出到 `my_docs` 目录。

## Current State Analysis

- 模块位于 `/Users/liangyingjie/Documents/my_android_projects/androidx/compose/remote/remote-core/`
- 这是整个项目的最底层核心模块，纯 Java 库，无 Android 依赖
- 约 150+ 个源文件，分布在 8 个包中
- 所有其他模块都直接或间接依赖此模块

## Proposed Changes

### 1. 创建架构文档 `my_docs/remote-core-architecture.md`

文档内容包含以下章节：

1. 模块概览：定位、依赖关系、构建配置
2. 分层架构图（Mermaid）
3. 包结构与职责
4. 核心类详解：
   - 核心基础设施：CoreDocument、RemoteComposeBuffer、WireBuffer、Operations
   - 状态管理：RemoteComposeState、RemoteContext、RemoteClock
   - 操作体系：Operation 基类、Companion 工厂模式
   - 布局系统：Component、LayoutComponent、RootLayoutComponent
   - 绘制系统：DrawBase 系列、PaintBundle
   - 表达式引擎：AnimatedFloatExpression、IntegerExpressionEvaluator
   - 宏展开系统：LoomManager、PatternDefine、PatternInflation
   - 路径系统、矩阵系统、文本系统、颜色系统
   - 语义系统、序列化系统、文档生成系统
5. 设计模式总结
6. 接口与实现对应关系表
7. 类关系图（Mermaid classDiagram）
8. 数据流与生命周期流程图（Mermaid）
9. 二进制协议概览
10. 使用方法

## Assumptions & Decisions

- 使用 Mermaid 语法绘制图表
- 文档语言为中文
- 所有内容基于实际代码分析
- 由于文件数量巨大（150+），按包分组概述，重点突出核心架构
- 与已有文档保持一致风格

## Verification Steps

1. 确认文档文件已生成且内容完整
2. 确认 Mermaid 图表语法正确
3. 确认所有类和方法引用与实际代码一致
