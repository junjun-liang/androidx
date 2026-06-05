# 计划：Remote Compose 布局组件使用方法文档

## Summary

在项目根目录创建 `LAYOUT_COMPONENTS_GUIDE.md`，详细介绍 `remote-creation-compose/layout/` 目录下每个布局组件的使用方法，包含完整的 API 签名、参数说明和代码示例。

## Current State Analysis

- `remote-creation-compose/src/main/java/.../layout/` 目录包含 28 个文件，其中 13 个是布局组件
- 现有文档 `COMPOSE_COMPONENTS_GUIDE.md` 只提供了简要概述，缺少完整 API 签名和详细代码示例
- 需要覆盖的组件：RemoteBox、RemoteColumn、RemoteRow、RemoteText、RemoteImage、RemoteCanvas、RemoteSpacer、RemoteStateLayout、RemoteCollapsibleColumn、RemoteCollapsibleRow、RemoteFlowRow、FitBox
- 支持类型也需要说明：RemoteAlignment、RemoteArrangement、RemoteDrawScope、RemoteSize、RemoteOffset、RemotePaddingValues

## Proposed Changes

### 文件：`/home/meizu/Documents/my_android_projects/androidx/compose/remote/LAYOUT_COMPONENTS_GUIDE.md`

创建新文档，包含以下章节：

#### 1. 概述
- 布局组件体系总览
- @RemoteComposable 注解说明
- RemoteModifier 基础

#### 2. 容器布局组件
- **RemoteBox** — 堆叠布局（2个重载，contentAlignment参数）
- **RemoteColumn** — 垂直布局（RemoteColumnScope.weight，verticalArrangement，horizontalAlignment）
- **RemoteRow** — 水平布局（RemoteRowScope.weight，horizontalArrangement，verticalAlignment）

#### 3. 内容组件
- **RemoteText** — 文本显示（4个重载，完整参数说明：color/fontSize/fontWeight/fontStyle/fontFamily/textAlign/overflow/maxLines/style/fontVariationSettings）
- **RemoteImage** — 图片显示（2个重载：ImageBitmap版和RemoteBitmap版，contentScale/alpha参数）

#### 4. 画布组件
- **RemoteCanvas** — 画布绘制（RemoteDrawScope绘制API，变换API，裁剪API）
- RemoteDrawScope 完整方法参考表

#### 5. 辅助布局组件
- **RemoteSpacer** — 间距占位
- **RemoteStateLayout** — 状态布局（3个重载：Enum/Boolean/Int）
- **FitBox** — 自适应盒子

#### 6. 可折叠/流式布局组件
- **RemoteCollapsibleColumn** — 可折叠垂直布局（priority修饰符）
- **RemoteCollapsibleRow** — 可折叠水平布局（priority修饰符）
- **RemoteFlowRow** — 流式换行布局（maxItemsInEachRow/maxLines）

#### 7. 对齐与排列参考
- RemoteAlignment 完整常量表（9个2D + 3个1D垂直 + 3个1D水平）
- RemoteArrangement 完整常量表 + spacedBy 工厂方法

#### 8. 辅助类型参考
- RemoteSize、RemoteOffset、RemotePaddingValues

每个组件包含：
- 完整函数签名
- 参数说明表
- 底层 OpCode 映射
- 2-3个代码示例（基础用法 + 进阶用法）

## Assumptions & Decisions

1. **文档语言**：中文
2. **文档位置**：项目根目录，命名 `LAYOUT_COMPONENTS_GUIDE.md`
3. **代码示例**：基于实际 API 签名编写，使用 @RemoteComposable 风格
4. **不修改现有文档**

## Verification Steps

1. 确认 12 个布局组件均有详细说明和代码示例
2. 确认 RemoteAlignment 和 RemoteArrangement 参考完整
3. 确认 RemoteDrawScope 方法参考完整
4. 确认代码示例语法正确
5. 确认参数说明与源码一致
