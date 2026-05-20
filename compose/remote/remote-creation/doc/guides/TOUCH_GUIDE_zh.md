# RemoteCompose 中的触摸交互指南

RemoteCompose 提供了一个强大的 `addTouch` 系统，允许你创建完全在播放器端运行的交互式组件（如滑块、旋钮和操纵杆）。这确保了低延迟交互，无需与服务器持续通信。

## 核心概念

`addTouch` 函数创建一个**远程变量**，该变量响应触摸事件而变化。然后你可以在其他绘图或布局操作中使用此变量（例如作为旋转角度、偏移或缩放因子）。

### 基本用法

```kotlin
val touchPos = rc.addTouch(
    defValue = 0f,            // 初始值
    min = 0f,                 // 最小值
    max = 100f,               // 最大值
    touchMode = STOP_GENTLY,  // 触摸释放时的行为
    velocityId = 0f,          // 保留供将来使用
    touchEffects = 4,         // 触觉反馈（例如 CLOCK_TICK）
    touchSpec = null,         // 停止模式的额外参数
    easingSpec = null,        // 自定义减速（maxTime、acceleration 等）
    // 映射表达式：触摸移动如何影响值
    RemoteContext.FLOAT_TOUCH_POS_X 
)
```

## 参数说明

### 1. 限制和默认值
- `defValue`：起始值。可以是常量或远程 ID。
- `min` / `max`：边界约束。如果 `min` 是值为 0 的 NaN 编码 ID，则启用**环绕模式**（值从 `max` 环绕回 `min`）。

### 2. 触摸模式 (`stopMode`)
这定义了用户抬起手指时会发生什么：
- `STOP_INSTANTLY` (1)：运动在手指释放的确切位置停止。
- `STOP_GENTLY` (0)：值继续以惯性移动并减速。
- `STOP_ENDS` (2)：吸附到 `min` 或 `max` 边界。
- `STOP_NOTCHES_EVEN` (3)：吸附到等间距的刻度。`touchSpec[0]` 定义刻度数量。
- `STOP_NOTCHES_PERCENTS` (4)：吸附到 `touchSpec` 中定义为范围百分比（0..1）的特定位置。
- `STOP_NOTCHES_ABSOLUTE` (5)：吸附到 `touchSpec` 中定义的特定绝对值。
- `STOP_ABSOLUTE_POS` (6)：直接跳转到触摸位置（典型的"点击跳转"）。

### 3. 触摸映射表达式
尾部 `vararg exp: Float`（或 Kotlin 中的 `RFloat`）是一个逆波兰表示法（RPN）表达式，将触摸坐标映射到变量的值。
- **直接映射**：`RemoteContext.FLOAT_TOUCH_POS_X` 水平映射。
- **角度映射**：使用 `ATAN2` 创建圆形交互（如旋钮）。
  ```kotlin
  // 示例：将触摸映射为围绕 (cx, cy) 的旋转角度
  tx, cx, SUB, ty, cy, SUB, ATAN2, RAD_TO_DEG, ADD_OFFSET
  ```

### 4. 触摸效果
主要用于触觉反馈。来自 `Rc.Haptic` 的常用值：
- `CLOCK_TICK` (4)：穿过刻度时的微妙滴答感。
- `VIRTUAL_KEY` (2)：标准按压感。

## 与组件集成

要将触摸检测限制到特定区域，请将 `addTouch` 调用放在 `canvas` 或 `box` 组件内部。RemoteCompose 自动将触摸监听器限定到定义它们的组件的边界框。

### 示例：水平滑块

```kotlin
canvas(RecordingModifier().size(200f, 40f)) {
    val w = ComponentWidth()
    val sliderPos = addTouch(
        100f, 0f, w, STOP_GENTLY, 0f, 4, null, null,
        RemoteContext.FLOAT_TOUCH_POS_X
    )
    
    // 绘制轨道
    painter.setColor(Color.GRAY).commit()
    drawLine(0f, 20f, w, 20f)
    
    // 绘制滑块
    painter.setColor(Color.RED).commit()
    drawCircle(sliderPos, 20f, 15f)
}
```

## 高级：自定义减速
你可以向 `easingSpec` 传递 `FloatArray` 来控制惯性的"手感"：
```kotlin
// [0f (type), maxTime, maxAcceleration, maxVelocity]
val myEasing = floatArrayOf(0f, 1.5f, 10f, 20f)
```

## 成功技巧
1. **RPN 辅助函数**：在 Kotlin 中，你可以使用 `RFloat` 运算符（`+`、`-`、`*`、`/`）在传递给 `addTouch` 之前更自然地构建映射表达式。
2. **调试**：使用 `addDebugMessage("label", touchVar)` 在播放器中实时查看值的变化。
3. **增量 vs 绝对**：默认情况下，`addTouch` 计算从触摸开始位置的增量。对于"跳转到位置"行为，使用 `STOP_ABSOLUTE_POS`。
