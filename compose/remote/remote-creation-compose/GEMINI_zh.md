# RemoteCompose Creation API 约定

本文档概述了在 `remote-creation-compose` 中构建和使用公共 API 的约定和模式。

## 核心原则：Remote-First API

所有公共绘图 API 应优先使用 "Remote" 类型而非标准平台或 Compose 类型。这确保 API 与动态表达式和远程评估兼容。

### Remote 类型映射

| 标准类型 | Remote 类型 | 描述 |
| :--- | :--- | :--- |
| `Float` | `RemoteFloat` | 用于坐标、尺寸、透明度、描边宽度等。 |
| `String` | `RemoteString` | 用于文本内容。 |
| `Color` | `RemoteColor` | 用于纯色。 |
| `Brush` | `RemoteBrush` | 用于渐变和着色器。 |
| `Offset` | `RemoteOffset` | `RemoteFloat` 对（x, y）。 |
| `Size` | `RemoteSize` | `RemoteFloat` 对（width, height）。 |
| `ImageBitmap` | `RemoteBitmap` | 用于绘制图像。 |
| `Paint` | `RemotePaint` | `android.graphics.Paint` 的扩展，能够携带远程 ID。 |

## 关键模式和经验教训

### 1. 处理 `RemoteFloat`
`RemoteFloat` 可以表示常量值或动态表达式（由 ID 标识）。
- **切勿**使用 `RemoteFloat.floatId` 进行算术或逻辑运算。它返回编码为 NaN 的远程 ID，会破坏任何计算。
- **应当**在需要将值/ID 序列化为 `RecordingCanvas` 命令时使用 `RemoteStateScope`。如果需要这样做，请在 RecordingCanvas 中添加 TODO 以添加 Remote 重载。

### 2. `RemotePaint` 用法
`RemotePaint` 是一个关键桥梁。它允许标准的 `android.graphics.Paint` 属性与远程 ID 关联。
- `RecordingCanvas.usePaint` 拦截这些属性并通过 `PaintBundle` 序列化它们的 ID。

### 3. 桥接 Compose 类型
将标准 Compose 类型（如 `BlendMode` 或 `ColorFilter`）桥接到远程实现时：
- 优先使用已建立的转换方法，如 `asAndroidColorFilter()`。
- 如果转换是 `internal` 的（如 `BlendMode.toAndroidBlendMode()`），请在 UI 层实现一个可重用的本地辅助函数，以保持连接性同时尊重可见性。但首先检查 `compose/remote` 中是否已有现成的。

### 4. 增量式公共 API 设计
始终从下游消费者（如 `remote-material3`）所需的最小方法集开始。这减少了维护面和序列化开销。实现未使用的方法会引入大量传递性复杂性和未经测试的代码。

### 5. RecordingCanvas 中的 V1 兼容性
虽然 `RemoteCanvas2` 和 `RemoteDrawScope2` 经过精简并使用 remote-first 类型，但 `RecordingCanvas` 必须维护标准平台类型（`Float`、`Rect` 等）的重载，以在迁移期间支持现有的 V1 `RemoteDrawScope`。
- **注意**：向 V2 添加新功能时，请考虑是否需要在 `RecordingCanvas` 中添加 V1 桥接，以防止构建回归。

### 6. 专用绘图签名
某些绘图操作（如 `drawTextOnCircle`）在底层 `RemoteComposeWriter` 中具有复杂的签名。确保这些签名在 `RemoteCanvas2` 和 `RemoteDrawScope2` 中正确镜像，匹配 writer 对可选参数（如 warp radius、alignment 和 placement）的期望。
