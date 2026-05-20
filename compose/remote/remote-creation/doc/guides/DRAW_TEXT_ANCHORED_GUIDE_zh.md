# RemoteCompose DrawTextAnchored 指南

`drawTextAnchored` 操作提供了一种灵活的方式，相对于特定坐标（"锚点"）定位和居中文本。与通常使用基线和左边缘的标准 `drawText` 不同，`drawTextAnchored` 使用 "平移" 因子来在其自身的边界框内相对于锚点调整文本位置。

## 核心原则

- **基于锚点的定位**：你指定画布上的一个固定 (x, y) 点。
- **边界框平移**：不使用简单的对齐（LEFT、CENTER、RIGHT），而是使用 `panX` 和 `panY` 浮点数来相对于锚点定位文本的边界框。
- **动态测量**：播放器在运行时计算文本的边界框并应用平移，即使文本内容或字体大小动态变化，也能确保正确的对齐。

## Kotlin (DSL) 中的用法

```kotlin
canvas {
    val cx = ComponentWidth() / 2f
    val cy = ComponentHeight() / 2f

    // 将文本完美居中于 (cx, cy)
    drawTextAnchored(
        str = "Hello World",
        x = cx,
        y = cy,
        panX = 0f, // 0 = 水平居中
        panY = 0f, // 0 = 垂直居中
        flags = 0
    )
}
```

## 参数详情

### `panX`（水平平移）
- **`-1.0f`**：文本右边缘位于锚点（文本在左侧）。
- **`0.0f`**：文本在锚点上水平居中。
- **`1.0f`**：文本左边缘位于锚点（文本在右侧）。

### `panY`（垂直平移）
- **`-1.0f`**：文本底部边缘位于锚点（文本在上方）。
- **`0.0f`**：文本在锚点上垂直居中。
- **`1.0f`**：文本顶部边缘位于锚点（文本在下方）。
- **`NaN`**：特殊值；锚点的 `y` 坐标用作文本的**基线**。

### `flags`
- **`ANCHOR_TEXT_RTL` (1)**：以从右到左方向渲染文本。
- **`ANCHOR_MONOSPACE_MEASURE` (2)**：强制使用等宽字符宽度进行测量。
- **`MEASURE_EVERY_TIME` (4)**：每帧强制重新测量（如果字体或样式正在动画，这很有用）。
- **`BASELINE_RELATIVE` (8)**：调整垂直居中逻辑，使其相对于基线而非几何中心。

## 内部架构

### 1. 创建阶段 (`RemoteComposeWriter.java`)
Writer 将 `DRAW_TEXT_ANCHOR` 操作码与文本 ID、坐标、平移因子和标志一起序列化。如果传递了原始 `String`，它首先会被添加到文档的文本数据池中。

### 2. 播放器阶段 (`DrawTextAnchored.java`)
在播放器端：
1. **测量**：播放器检索字符串并使用当前的 `Paint` 状态获取其几何边界。
2. **偏移计算**：
   - `horizontalOffset`：基于文本宽度和 `panX` 计算。
   - `verticalOffset`：基于文本高度和 `panY`（如果 `panY` 是 NaN 则为基线）计算。
3. **渲染**：最终坐标为 `(x + horizontalOffset, y + verticalOffset)`，文本使用 `drawTextRun` 绘制。

## 实际示例

### 角落对齐
```kotlin
// 将文本定位在组件的右上角
drawTextAnchored("Top Right", width, 0f, panX = -1f, panY = 1f, flags = 0)
```

### 标注数据点
在图表中，你可能希望标签显示在点的正上方：
```kotlin
drawTextAnchored(
    str = valueText,
    x = pointX,
    y = pointY - 5f, // 5px 间距
    panX = 0f,  // 在点上水平居中
    panY = -1f, // 文本在点上方
    flags = 0
)
```

### 基线对齐
```kotlin
// 将多个文本元素对齐到同一基线，无论其高度如何
drawTextAnchored("Big Text", 100f, 200f, 0f, Float.NaN, 0)
drawTextAnchored("small", 300f, 200f, 0f, Float.NaN, 0)
```

## 与标准 DrawText 的比较
虽然 `drawText` 更简单，但 `drawTextAnchored` 是 RemoteCompose 中处理动态文本的首选方式，因为它抽象了开发者在服务器端手动计算文本宽度和高度的需求（这可能与播放器的字体渲染不完全匹配）。
