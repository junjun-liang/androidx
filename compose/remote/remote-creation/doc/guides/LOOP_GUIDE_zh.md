# RemoteCompose 循环指南

RemoteCompose 中的 `loop` 机制允许你在播放器端重复执行一系列操作。这对于绘制重复元素（如时钟刻度、网格线或多个粒子）非常高效，无需通过线路发送每个单独的命令。

## 核心原则

- **播放器端执行**：循环逻辑在播放器上运行。你定义一次循环，播放器遍历操作。
- **动态索引**：每个循环都有一个索引变量，每次迭代都会更新。此变量可用于表达式中以改变输出（例如旋转、位置、颜色）。
- **RPN 兼容性**：`from`、`step` 和 `until` 参数可以是动态表达式（远程 ID）。

## Kotlin (DSL) 中的用法

`RemoteComposeContextAndroid` 提供了一个方便的 `loop` 扩展。

```kotlin
loop(from = 0f, step = 1f, until = 12f) { index ->
    // 'index' 是一个 RFloat，表示当前迭代值
    save {
        rotate(index * 30f, centerX, centerY)
        drawLine(centerX, centerY - 100f, centerX, centerY - 120f)
    }
}
```

- **`from`**：索引的起始值。
- **`step`**：每次迭代后添加到索引的增量。
- **`until`**：只要 `index < until`，循环就继续。
- **`index`**：闭包将索引作为 `RFloat` 提供，你可以在数学表达式中使用它。

## Java（过程式）中的用法

在 Java 中，你直接使用 `RemoteComposeWriter`。

```java
float indexVar = rc.startLoopVar(0f, 1f, 12f);
// 使用 indexVar 的绘图操作
rc.save();
rc.rotate(rc.floatExpression(indexVar, 30f, MUL), cx, cy);
rc.drawLine(cx, cy - 100, cx, cy - 120);
rc.restore();
rc.endLoop();
```

或者，使用函数式接口：
```java
rc.loop(indexId, 0f, 1f, 12f, () -> {
    // 操作
});
```

## 内部架构

### 1. 创建阶段 (`RemoteComposeWriter.java`)
当你调用 `startLoop` 时，writer 将 `LOOP_START` 操作码连同以下内容一起发射到 `WireBuffer`：
- `indexId`：将存储当前索引的变量 ID。
- `from`、`step`、`until`：循环范围参数。

`LOOP_START` 之后的操作被缓冲为循环的子操作，直到遇到 `endLoop`（关闭容器）。

### 2. 播放器阶段 (`LoopOperation.java`)
在播放器端，`LoopOperation` 充当其子操作的容器。
在 `paint` 阶段：
1. 它解析 `from`、`step` 和 `until` 的当前值。
2. 它进入标准的 `for` 循环。
3. 在每次迭代中：
   - 它使用 `loadFloat(mIndexVariableId, i)` 在 `RemoteContext` 中更新索引变量。
   - 它遍历所有子操作并对它们调用 `apply()`。
   - 如果子操作依赖于索引变量，它会被标记为脏，并在该特定迭代中重新评估其变量。

## 实际示例

### 绘制时钟表盘
```kotlin
// 绘制 60 个分钟刻度
loop(0f, 1f, 60f) { i ->
    val angle = i * 6f // 360 / 60
    save {
        rotate(angle, cx, cy)
        val length = ifElse(i % 5f, 10f, 20f) // 每 5 分钟一个长刻度
        drawLine(cx, cy - radius, cx, cy - radius + length)
    }
}
```

### 动态网格
```kotlin
val spacing = 50f
loop(0f, spacing, width) { x ->
    drawLine(x, 0f, x, height)
}
loop(0f, spacing, height) { y ->
    drawLine(0f, y, width, y)
}
```

## 性能考虑
- **迭代次数**：注意迭代次数。数千次迭代配合复杂的绘图操作可能会影响播放器的帧率。
- **变量依赖**：循环内部依赖于循环索引的操作必须在每次迭代中重新评估。保持循环内的表达式尽可能简单。
- **嵌套循环**：循环可以嵌套，但复杂度呈指数增长。谨慎使用。
