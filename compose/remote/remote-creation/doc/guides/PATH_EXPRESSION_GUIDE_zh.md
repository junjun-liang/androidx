# RemoteCompose 路径表达式指南

路径表达式允许你使用数学方程在播放器端算法生成矢量路径。这对于随时间变化的动态形状（如图表、波形或复杂装饰）非常高效。

## 核心操作

### 1. `addPathExpression`（笛卡尔坐标）
通过评估 X 和 Y 方程在范围内生成路径。

```kotlin
val pathId = addPathExpression(
    expressionX = VAR1, // X = t
    expressionY = sin(VAR1 * 2f * PI.toFloat()) * 100f, // Y = sin(t) * 100
    min = 0f,
    max = 100f,
    count = 64, // 采样点数
    flags = Rc.PathExpression.SPLINE_PATH
)
```

- **`VAR1`**：从 `min` 到 `max` 范围的独立变量。
- **`count`**：路径的分辨率。值越高意味着曲线越平滑，但计算量越大。
- **`flags`**：控制插值（见下文）。

### 2. `addPolarPathExpression`（极坐标）
使用半径和角度生成路径，这对于圆形或对称形状通常更容易。

```kotlin
val pathId = addPolarPathExpression(
    expressionRad = 100f + 20f * sin(VAR1 * 10f), // 半径随角度变化
    startAngle = 0f,
    endAngle = 2f * PI.toFloat(),
    count = 100,
    centerX = 250f,
    centerY = 250f,
    flags = Rc.PathExpression.LOOP_PATH or Rc.PathExpression.SPLINE_PATH
)
```

- **`VAR1`**：表示 `startAngle` 和 `endAngle` 之间的当前角度（弧度）。

## 插值模式

`flags` 参数控制采样点的连接方式：

- **`SPLINE_PATH` (0)**：使用标准三次样条。非常平滑但可能过冲。
- **`MONOTONIC_PATH` (2)**：使用单调三次样条。比线性更平滑，但保证不会过冲局部极值。适合数据图表。
- **`LINEAR_PATH` (4)**：用直线连接点。

### 其他标志
- **`LOOP_PATH` (1)**：自动将最后一个点连接回第一个点以闭合形状。

## 使用生成的路径

创建路径并获得其 `pathId` 后，你可以将其用于多个绘图操作：

### 1. 绘制路径
```kotlin
drawPath(pathId)
```

### 2. 路径上的文本
```kotlin
drawTextOnPath(textId, pathId, hOffset, vOffset)
```

### 3. 路径变形 (`drawTweenPath`)
如果两个路径表达式具有相同的 `count`，你可以平滑地在它们之间动画。

```kotlin
drawTweenPath(pathId1, pathId2, progress, start, stop)
```

## 实际示例：动画波浪

```kotlin
val time = ContinuousSec()
val pathId = addPathExpression(
    expressionX = VAR1,
    expressionY = cy + 50f * sin(VAR1 * 0.1f + time),
    min = 0f,
    max = width,
    count = 40,
    flags = Rc.PathExpression.SPLINE_PATH
)

painter.setColor(Color.Blue).setStyle(Style.STROKE).setStrokeWidth(5f).commit()
drawPath(pathId)
```

## 性能考虑
- **分辨率**：不要对路径的视觉尺寸使用不必要的高 `count`。
- **复杂度**：保持表达式相对简单。当依赖项（如 `time`）变化时，它们会在播放器上重新评估。
- **预计算**：如果路径不变，请使用静态 `RemotePath` 而不是表达式。
