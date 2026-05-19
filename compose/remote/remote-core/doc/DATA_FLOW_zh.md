# RemoteCompose 数据流与生命周期

本文档描述 RemoteCompose 文档从初始创建到最终在设备上渲染的整个过程。

## 1. 创建（服务端/主机）
开发者使用 `RemoteComposeWriter`（Java）或 `RemoteComposeContext`（Kotlin DSL）发射操作。
- **缓冲**：操作被序列化到 `WireBuffer`（可增长的字节数组）中。
- **ID 管理**：`RemoteComposeState` 跟踪字符串、位图、路径和动态变量的唯一 ID。
- **Painter 状态**：设置绘制属性（颜色、描边）会累积在 `PaintBundle` 中，只有在调用 `commit()` 时才会发射。

## 2. 传输
`WireBuffer` 的内容作为简单的字节数组（`byte[]`）通过网络或 IPC 发送。

## 3. 解码（客户端/播放器）
播放器接收字节并初始化 `CoreDocument`。
- **解析**：`Operations.read()` 遍历缓冲区，在 `DefaultVersionMap` 中查找操作码并实例化对应的 `Operation` 类。
- **布局树**：组件操作（`COMPONENT_START`、`CONTAINER_END`）重建 UI 层级结构。
- **变量注册**：依赖动态值的操作在 `RemoteContext` 中注册为监听器。

## 4. 回放循环
播放器运行一个持续循环（通常由 vsync 驱动）：
1.  **时间更新**：`RemoteClock` 更新全局时间变量（`ANIMATION_TIME`、`CONTINUOUS_SEC`）。
2.  **变量解析**：`RemoteContext` 评估依赖已发生变化的动态表达式（RPN）。
3.  **布局阶段**：如果组件或变量有脏标记，`LayoutCompute` 计算新的几何信息。
4.  **绘制阶段**：播放器遍历 `Operation` 列表。
    - 每个操作调用 `apply(RemoteContext)`。
    - 绘制操作使用已解析的坐标和当前 `Paint` 状态。
5.  **交互**：触摸事件由 `RemoteComposeView` 捕获，映射回组件，可能触发主机端回调或本地变量更新。
