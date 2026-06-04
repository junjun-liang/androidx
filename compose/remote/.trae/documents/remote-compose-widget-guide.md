# Plan: Remote Compose Widget 开发使用指南

## Summary

整理一份 Remote Compose 使用指南，重点讲解如何使用 layout 包中的组件进行手机 Widget 开发，输出文档到 `my_docs` 目录。

## Current State Analysis

- Layout 包含 28 个文件，涵盖 11 个布局/内容组件、4 个可折叠/流式布局、以及对齐/排列/内边距等辅助类型
- Modifier 体系提供约 34 个修饰符，支持背景、边框、内边距、点击、裁剪、滚动、动画等
- State 体系提供 16 种远程状态类型，支持动态表达式和播放端动画
- Widget Profile（V6/V7/Wear）有特定限制（禁止自定义字体、验证操作码等）
- 与 AppWidget 原生开发模式有明确映射关系

## Proposed Changes

### 1. 创建文档 `my_docs/remote-compose-widget-guide.md`

文档内容包含以下章节：

1. 概述：Remote Compose vs AppWidget 原生
2. 快速开始：最小 Widget 示例
3. 布局组件详解（含代码示例）
   - RemoteBox / RemoteColumn / RemoteRow
   - RemoteFlowRow / RemoteCollapsibleColumn / RemoteCollapsibleRow
   - FitBox / RemoteStateLayout
   - RemoteText / RemoteImage / RemoteSpacer
   - RemoteCanvas（自定义绘制）
4. 修饰符系统
   - 常用修饰符速查表
   - 动态值修饰符
5. 状态与交互
   - 远程状态类型
   - Action 体系
   - RemoteStateLayout 状态切换
6. 动画与时间
   - 播放端动画
   - 时间驱动（时钟 Widget）
7. Widget Profile 约束
8. 完整示例：天气 Widget / 时钟 Widget
9. 最佳实践与注意事项

## Assumptions & Decisions

- 使用 Mermaid 语法绘制架构图
- 文档语言为中文
- 所有内容基于实际代码分析
- 重点面向 Widget 开发场景，提供可运行的代码示例
- 与 AppWidget 原生开发模式对比，帮助理解

## Verification Steps

1. 确认文档文件已生成且内容完整
2. 确认代码示例与实际 API 签名一致
3. 确认 Mermaid 图表语法正确
