# Remote Compose Player 图像资源流程文档 Spec

## Why
需要清晰记录 Remote Compose Player 从 ByteArray 二进制流中获取并显示图像资源的完整流程，帮助开发者理解图像的编码、传输、解码、缓存和渲染机制。

## What Changes
- 在项目根目录创建 `IMAGE_RESOURCE_FLOW.md` 文档
- 文档包含完整的图像资源流程说明，涵盖：编码写入、协议解析、解码缓存、渲染显示
- 文档包含 Mermaid 流程图，可视化展示各阶段的数据流转
- 文档包含关键类和文件的索引引用

## Impact
- Affected code: 无代码变更，仅新增文档
- Affected specs: 无

## ADDED Requirements

### Requirement: 图像资源流程文档
系统 SHALL 在项目根目录提供 `IMAGE_RESOURCE_FLOW.md` 文档，完整描述 Remote Compose Player 从 ByteArray 获取图像资源并显示的流程。

#### Scenario: 开发者阅读文档
- **WHEN** 开发者打开 `IMAGE_RESOURCE_FLOW.md`
- **THEN** 能通过 Mermaid 流程图直观理解图像从创建端编码到播放端渲染的完整链路
- **AND** 能通过文字说明了解每个阶段涉及的关键类、方法和数据结构
- **AND** 能通过文件索引快速定位源码位置

### Requirement: 流程图
文档 SHALL 包含以下 Mermaid 流程图：
1. 总体数据流图：从创建端 Bitmap 对象到播放端 Canvas 绘制的端到端流程
2. 解码决策流程图：根据 encoding 和 type 组合选择不同解码路径的决策树
3. 缓存与渲染流程图：从缓存取图到最终绘制的流程

#### Scenario: 流程图渲染
- **WHEN** 在支持 Mermaid 的 Markdown 查看器中打开文档
- **THEN** 所有流程图能正确渲染并清晰展示数据流转
