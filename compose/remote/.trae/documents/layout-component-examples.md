# 计划：Remote Compose 布局组件使用示例文档

## Summary

在项目根目录创建 `LAYOUT_COMPONENT_EXAMPLES.md`，列举项目中所有使用 `remote-creation-compose/.../layout/` 目录下布局组件的示例，按组件分类，标明文件路径。

## Current State Analysis

- 搜索覆盖了 integration-tests/demos、integration-tests/player-view-demos、remote-creation-compose/samples、remote-creation-compose/src/test、remote-creation-compose/src/androidTest、remote-player-compose/src/androidTest、remote-testing 等目录
- 共找到 12 个布局组件的 200+ 处使用
- 使用最广泛：RemoteText（50+处）、RemoteBox（40+处）、RemoteColumn（25+处）
- 使用较少：RemoteSpacer（6处）、FitBox（6处）
- DemoWeather.kt 是最全面的综合示例，使用了 9 种布局组件

## Proposed Changes

### 文件：`/home/meizu/Documents/my_android_projects/androidx/compose/remote/LAYOUT_COMPONENT_EXAMPLES.md`

创建新文档，按组件分类，每个组件下按来源目录分组列出所有使用示例：

#### 1. 概述
- 12 个布局组件使用频次统计表

#### 2-13. 每个组件一个章节
- RemoteBox（~40处）
- RemoteColumn（~25处）
- RemoteRow（~20处）
- RemoteText（~50处）
- RemoteImage（~9处）
- RemoteCanvas（~18处）
- RemoteSpacer（~6处）
- RemoteStateLayout（~7处）
- RemoteCollapsibleColumn（~10处）
- RemoteCollapsibleRow（~10处）
- RemoteFlowRow（~13处）
- FitBox（~6处）

每条记录包含：文件路径、函数/类名、使用上下文代码片段

#### 14. 综合示例
- DemoWeather.kt 详细分析（使用了 9 种组件）

#### 15. 专用测试文件索引
- 每个组件对应的专门测试类

## Assumptions & Decisions

1. **文档语言**：中文
2. **文档位置**：项目根目录，命名 `LAYOUT_COMPONENT_EXAMPLES.md`
3. **代码片段**：提取关键使用上下文（1-3行）
4. **按组件分类**：而非按文件分类，便于查找某组件的所有用法
5. **不修改现有文档**

## Verification Steps

1. 确认 12 个组件均有使用示例
2. 确认每条记录有文件路径
3. 确认 DemoWeather.kt 综合分析完整
4. 确认专用测试文件索引完整
