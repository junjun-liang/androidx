# RemoteCompose 系统架构

RemoteCompose 是一套将 UI 树和绘制命令序列化为二进制格式的系统，该格式可以在远程设备（"播放器"）上进行回放。它支持动态表达式、动画和交互元素，而无需对文档进行完整重新序列化。

## 核心模块

### 1. `remote-core`（协议与引擎）
这是系统的核心。它定义了二进制协议以及解码和维护远程文档状态的逻辑。
- **`Operations.java`**：定义操作码映射（例如 `DRAW_RECT`、`LAYOUT_BOX`）。
- **`CoreDocument.java`**：表示播放器端文档的状态。它保存已解码的操作、变量和布局树。
- **`RemoteComposeBuffer.java`**：用于写入/读取二进制流的底层缓冲区。
- **`RemoteContext.java`**：维护回放过程中的运行时状态，包括变量值、时钟时间和变换矩阵。
- **`operations/`**：每个操作的独立类（例如 `DrawRect.java`）。这些类处理各自的序列化和回放逻辑。

### 2. `remote-creation-core` 与 `remote-creation`（创建 API）
这些模块提供了面向开发者的 API 来构建远程文档。
- **`RemoteComposeWriter.java`**：用于向缓冲区发射命令的高级 API。
- **`RemoteComposeContext.kt`**：围绕 Writer 的 Kotlin DSL 封装，提供更符合语言习惯的方式来构建布局。
- **`RecordingModifier.java`**：收集布局修饰符（padding、size 等），以便与组件一起序列化。
- **`Painter.java`**：`android.graphics.Paint` 的桥接器，跟踪更改并将其序列化为 `PAINT_VALUES` 操作。

### 3. `remote-player-view`（渲染器）
用于回放远程文档的 Android 端具体实现。
- **`RemoteComposePlayer.java`**：编排回放流程，处理计时、输入事件和渲染到 `Canvas`。
- **`RemoteComposeView.java`**：托管 `RemoteComposePlayer` 的标准 Android `View`。
- **`accessibility/`**：将远程语义操作桥接到 Android 无障碍系统。

## 数据流与生命周期

1.  **创建**：开发者使用 `RemoteComposeContext`（DSL）或 `RemoteComposeWriter` 定义 UI。
2.  **序列化**：Writer 将二进制操作码和数据发射到 `WireBuffer` 中。
3.  **传输**：生成的字节数组被发送到播放器。
4.  **解码**：播放器的 `CoreDocument` 读取缓冲区并重建操作列表和布局树。
5.  **回放（持续）**：
    - **更新**：`RemoteContext` 更新基于时间的变量（`ANIMATION_TIME` 等）。
    - **布局**：如果需要，`LayoutCompute` 计算组件的位置和大小。
    - **绘制**：播放器遍历绘制操作，使用 `RemoteContext` 解析动态表达式（例如动画颜色或位置）。
    - **交互**：播放器捕获触摸事件，可能触发 `Actions`（如更新变量）或发送回主机。

## 核心概念

### 动态表达式（`RFloat`、`FloatExpression`）
许多操作接受指向表达式的"远程 ID"，而不是固定值。这些表达式可以是：
- **常量**：固定值。
- **动画**：依赖于时间或状态的值。
- **数学运算**：其他表达式的组合（ADD、MUL、SIN 等）。
- **系统变量**：由播放器提供的值（例如 `WINDOW_WIDTH`、`TOUCH_X`）。

### 布局树
RemoteCompose 不仅仅绘制；它还理解布局。
- **组件**：`Box`、`Row`、`Column`、`Text`、`Image`。
- **修饰符**：应用于组件以改变其行为或外观。
- **测量阶段**：播放器执行类似于 Compose 或标准 Android 视图的布局过程。

### Painter 与 Commit
`Painter`（或 `RemotePaint`）在创建期间是有状态的。你设置属性（颜色、描边）后必须调用 `commit()` 来发射 `PAINT_VALUES` 操作。所有后续的绘制操作将使用该绘制状态，直到下一次 commit 或 `save/restore` 块。

## 如何描述一个命令
在描述命令（操作）时：
1.  **识别操作码**：在 `Operations.java` 中找到它。
2.  **定位实现**：在 `remote-core/src/main/java/androidx/compose/remote/core/operations/` 中找到对应类。
3.  **检查 `write` 方法**：查看它是如何序列化的（哪些字段、什么顺序）。
4.  **检查 `paint` / `apply` 方法**：查看它是如何渲染的或如何影响 `RemoteContext` 的。
