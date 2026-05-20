# RemoteCompose 粒子系统指南

RemoteCompose 粒子系统允许使用过程式创建 API 对许多相似对象（粒子）进行高效、高性能的动画。它通过在播放器端直接运行粒子演化逻辑来最小化通信。

## 核心概念

该系统由两个主要操作组成：
1. **`ParticlesCreate`**：初始化一组粒子，为固定的一组变量设置起始值。
2. **`ParticlesLoop`**：遍历粒子，使用数学方程（基于 RPN）更新它们的变量，并根据条件可选地重新启动它们。

## 基本工作流程

### 1. 定义变量并创建系统
使用 `createParticles` 定义每个粒子将跟踪的变量及其初始状态。

```kotlin
val variables: Array<RFloat> = Array(4) { RFloat(this, 0f) }
val psId = createParticles(
    variables,
    arrayOf(
        cx,      // 初始 x
        cy,      // 初始 y
        rand() - 0.5f, // 初始 dx
        rand() - 0.5f  // 初始 dy
    ),
    pCount = 100
)
val (px, py, dx, dy) = variables
```

- **`variables`**：一个 `RFloat` 对象数组，表示循环期间单个粒子的状态。
- **`initialValues`**：一个值或表达式数组，用于初始化每个粒子。你可以使用 `index()` 来根据粒子索引变化初始值。
- **`pCount`**：系统中的粒子总数。

### 2. 更新和绘制粒子
使用 `particlesLoops` 定义粒子如何随时间演化以及如何渲染。

```kotlin
val dt = deltaTime() // 自上一帧以来的时间

particlesLoops(
    psId,
    restartCondition = null, // 可选表达式，如果 > 0 则重新启动粒子
    updateEquations = arrayOf(
        px + dx * dt, // 新 px
        py + dy * dt, // 新 py
        dx,           // dx 保持不变
        dy + 9.8f * dt // 将重力添加到 dy
    )
) {
    // 在此块内，'variables'（px、py 等）保存当前粒子的状态
    drawCircle(px.toFloat(), py.toFloat(), 5f)
}
```

- **`restartCondition`**：一个表达式，如果评估结果 > 0，则触发对该特定粒子的初始化方程的调用。
- **`updateEquations`**：`createParticles` 中定义的每个变量的新值。
- **`drawingBlock`**：此块中的操作对每一帧中的*每个*粒子执行。

## 关键 API 和辅助函数

### 脉冲控制
粒子通常在 `impulse` 块内使用，以管理它们的生命周期并确保它们仅在触发时运行。

```kotlin
val eventTime = ContinuousSec().toFloat()
impulse(durationMs = 2000f, eventTime) {
    // 系统设置
    impulseProcess() {
        // particlesLoops 和绘制
    }
}
```

### 随机性
- `rand()`：在播放器端返回 0 到 1 之间的随机浮点数。对于变化粒子行为至关重要。

### 粒子索引
- `index()`：返回当前正在初始化或更新的粒子的索引（0..pCount-1）。

### 碰撞和逻辑：`particlesComparison`

`particlesComparison` 操作（由 `ParticlesCompare.java` 实现）允许条件逻辑和粒子间交互。它可以以两种模式运行：

#### 1. 单粒子模式（边界/条件检查）
用于独立检查每个粒子的条件（例如撞到墙壁）。

```kotlin
particlesComparison(
    id = psId,
    min = -1f, max = -1f, // 处理所有粒子
    condition = px + ballRad - w, // 每个粒子评估
    then = arrayOf(
        dx * -elastic, // 更新 dx（反弹）
        dy,
        w - ballRad - 1f, // 将 x 吸附回内部
        py
    )
) {
    // 可选：满足条件时绘制某些内容或触发触觉反馈
    performHaptic(2)
}
```

#### 2. 双粒子模式（粒子间交互）
如果你同时提供 `then` 和 `else`（或两个结果块），系统可以对粒子对执行 $O(N^2)$ 比较。这对于粒子之间的碰撞或类似重力的效果很有用。

- **CMD1 / CMD2**：在双粒子模式下，特殊命令允许你引用对中第一个和第二个粒子的属性。

### 性能提示
- **保持简单**：绘制块和更新方程每帧对每个粒子执行。
- **N^2 注意**：双粒子比较计算成本高（$O(N^2)$）。谨慎使用，并且仅用于少量粒子。
- **使用 `restartCondition`**：不要创建新系统，而是在 `particlesLoops` 中使用重新启动条件来回收粒子（例如将火花重置到原点）。
