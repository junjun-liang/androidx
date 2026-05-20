# 构建 RemoteCompose 过程式组件化示例指南

本指南介绍如何使用 **过程式组件和修饰器** 构建示例——即基于布局的方法，你可以使用 `RemoteComposeContextAndroid` 将 `box`、`column`、`row`、`text`、`canvas` 和 `flow` 元素与修饰器链组合。

## API 风格：过程式 (`RemoteComposeContextAndroid`)

函数返回 `RemoteComposeWriter` 或 `RemoteComposeContext`。用于在 `DemosCreation.java` 中注册的演示。

```kotlin
fun myDemo(): RemoteComposeWriter {
    val rc = RemoteComposeContextAndroid(
        platform = AndroidxRcPlatformServices(),
        apiLevel = 7,
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "My Demo"),
    ) {
        root {
            column(Modifier.fillMaxSize().background(Color.WHITE).padding(16)) {
                text("Hello World", fontSize = 64f)
            }
        }
    }
    return rc.writer
}
```

- 使用 `Modifier` 属性（返回 `RecordingModifier()`）
- 直接使用 `RecordingModifier()` 进行显式构造
- 修饰器值为原始 `Int`、`Float` 等
- 在 `DemosCreation.java` 中通过 `getp("path", MyDemoKt::myDemo)` 注册
- 通过 `RemoteDocPreview(myDemo())` 预览

---

## 文档设置

```kotlin
fun myDemo(): RemoteComposeWriter {
    val rc = RemoteComposeContextAndroid(
        platform = AndroidxRcPlatformServices(),
        apiLevel = 7,
        // 头部标签（可选，按需使用）
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "description"),
    ) {
        root {
            // 组件树放在这里
        }
    }
    return rc.writer
}
```

替代构造函数：
```kotlin
// 宽度/高度/描述构造函数
RemoteComposeContextAndroid(500, 500, "Demo") { ... }

// 宽度/高度/描述/apiLevel/profiles 构造函数
RemoteComposeContextAndroid(500, 500, "Demo", 7,
    RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
    AndroidxRcPlatformServices()) { ... }
```

如果你返回 `RemoteComposeContext` 而不是 `RemoteComposeWriter`，直接返回 `rc`（而不是 `rc.writer`）。

## 布局组件

所有组件都接受可选的 `modifier` 和一个用于子元素的尾部 lambda：

### `column`

垂直布局。子元素从上到下堆叠。

```kotlin
column(
    modifier = Modifier.fillMaxSize().background(Color.YELLOW),
    horizontal = ColumnLayout.CENTER,   // 子元素水平对齐
    vertical = ColumnLayout.TOP,        // 默认值
) {
    text("Item 1")
    text("Item 2")
}
```

对齐常量：`ColumnLayout.START`、`CENTER`、`END`、`TOP`、`BOTTOM`、`SPACE_EVENLY`、`SPACE_BETWEEN`。

### `row`

水平布局。子元素从左到右流动。

```kotlin
row(
    modifier = Modifier.fillMaxWidth().padding(8),
    horizontal = RowLayout.SPACE_EVENLY,
    vertical = RowLayout.CENTER,
) {
    text("Left")
    text("Right")
}
```

对齐常量：`RowLayout.START`、`CENTER`、`END`、`TOP`、`BOTTOM`、`SPACE_EVENLY`、`SPACE_BETWEEN`。

### `box`

堆叠布局。子元素彼此层叠。

```kotlin
box(
    Modifier.fillMaxSize().background(Color.YELLOW),
    horizontal = BoxLayout.CENTER,
    vertical = BoxLayout.CENTER,
) {
    text("Centered")
}
```

对齐常量：`BoxLayout.START`、`CENTER`、`END`、`TOP`、`BOTTOM`。

### `flow`

换行 Row 布局。当子元素超出宽度时换到下一行。

```kotlin
flow(
    Modifier.fillMaxWidth().background(Color.DKGRAY),
    RowLayout.SPACE_EVENLY,
    RowLayout.CENTER,
) {
    box(Modifier.background(Color.RED).size(200))
    box(Modifier.background(Color.GREEN).size(200))
    box(Modifier.background(Color.BLUE).size(200))
}
```

### `canvas`

绘图表面，你可以在其中使用 `painter`、`drawLine`、`drawRect` 等。也可以包含嵌套组件。

```kotlin
canvas(Modifier.fillMaxWidth().height(200).background(Color.BLUE)) {
    val w = ComponentWidth()
    val h = ComponentHeight()
    painter.setColor(Color.RED).setStrokeWidth(4f).commit()
    drawLine(0f, 0f, w, h)

    // 组件可以嵌入在 canvas 内部
    box(Modifier.size(100, 50).background(Color.CYAN).computePosition {
        x = w / 2f - RFloat(width.toFloat()) / 2f
        y = h / 2f - RFloat(height.toFloat()) / 2f
    }) {
        text("Inside Canvas", autosize = true, textAlign = CoreText.TEXT_ALIGN_CENTER)
    }
}
```

### `text`

具有丰富格式选项的文本显示组件。

```kotlin
// 简单字符串
text("Hello World")

// 完整选项
text(
    "Arsenal vs Bayern Munich",
    modifier = Modifier.fillMaxWidth().background(Color.LTGRAY),
    color = Color.WHITE,             // 静态 ARGB 颜色
    colorId = dynamicColorId,        // 或动态颜色 ID（互斥）
    fontSize = 64f,                  // 像素为单位的字体大小
    fontWeight = 700f,               // 100-900
    fontStyle = 1,                   // 0=normal, 1=italic
    fontFamily = "sans-serif",       // "default", "sans-serif", "serif", "monospace", 或字体文件名
    textAlign = CoreText.TEXT_ALIGN_CENTER,    // LEFT, CENTER, RIGHT, JUSTIFY
    overflow = CoreText.OVERFLOW_ELLIPSIS,     // CLIP, ELLIPSIS, MIDDLE_ELLIPSIS, START_ELLIPSIS
    maxLines = 2,
    autosize = true,                 // 自动适应文本到容器
    minFontSize = 48f,               // autosize=true 时的最小大小
    maxFontSize = 200f,              // autosize=true 时的最大大小
    underline = true,
    strikethrough = false,
    letterSpacing = 0.1f,
    lineHeightMultiplier = 1.2f,
    lineBreakStrategy = CoreText.BREAK_STRATEGY_HIGH_QUALITY,
    hyphenationFrequency = CoreText.HYPHENATION_FREQUENCY_FULL,
    justificationMode = CoreText.JUSTIFICATION_MODE_INTER_CHARACTER,
    fontAxis = listOf("wght" to 600f),  // 可变字体轴
)

// 使用动态文本 ID（来自 createTextFromFloat、textMerge 等）
val textId = createTextFromFloat(someValue, 3, 1, Rc.TextFromFloat.PAD_PRE_NONE)
text(textId, fontSize = 48f, colorId = myColorId)
```

### `image`

显示位图图像。

```kotlin
val imageId = addBitmap(myBitmap)
image(Modifier.size(100), imageId, ImageScaling.SCALE_INSIDE, 1f)
```

### `space`（辅助模式）

没有内置的 `space()` 组件。创建一个扩展函数：

```kotlin
fun RemoteComposeContextAndroid.space() {
    box(Modifier.horizontalWeight(1f))
}
```

## 修饰器参考 (RecordingModifier)

在 `RemoteComposeContextAndroid` 块内通过 `Modifier` 属性访问，或使用 `RecordingModifier()` 构造。

### 尺寸

```kotlin
Modifier.fillMaxSize()           // 填充两个维度
Modifier.fillMaxWidth()          // 填充宽度
Modifier.fillMaxHeight()         // 填充高度
Modifier.size(200)               // 正方形 200x200
Modifier.size(300, 200)          // width=300, height=200
Modifier.width(200)              // 固定宽度（float 或 int）
Modifier.height(100)             // 固定高度（float 或 int）
Modifier.widthIn(100f, 400f)     // 最小/最大宽度约束
Modifier.heightIn(50f, 300f)     // 最小/最大高度约束
Modifier.wrapContentSize()       // 包裹内容
Modifier.wrapContentWidth()
Modifier.wrapContentHeight()
```

### 间距和权重

```kotlin
Modifier.padding(16)                 // 所有边
Modifier.padding(8, 16, 8, 16)      // start, top, end, bottom
Modifier.padding(8f, 0f, 8f, 0f)    // float 变体
Modifier.horizontalWeight(1f)       // row 中的弹性权重
Modifier.verticalWeight(1f)         // column 中的弹性权重
Modifier.spacedBy(8f)               // 子元素之间的间隙
```

### 外观

```kotlin
Modifier.background(Color.YELLOW)            // ARGB int 颜色
Modifier.background(0xFF007700L.toInt())     // 十六进制颜色
Modifier.backgroundId(dynamicColorId)        // 动态颜色表达式 ID（Int 或 Short）
Modifier.border(2f, 0f, Color.BLUE, 0)      // width, roundedCorner, color, shape
Modifier.dynamicBorder(10f, 30f, colorId, 0) // 动态颜色边框
Modifier.clip(RoundedRectShape(s, s, s, s))  // 裁剪为圆角矩形
Modifier.clip(RectShape(8f, 8f, 8f, 8f))    // 裁剪为带圆角的矩形
```

### 滚动

```kotlin
Modifier.verticalScroll()                    // 启用垂直滚动
Modifier.verticalScroll(position.toFloat())  // 带位置跟踪
Modifier.horizontalScroll()                  // 启用水平滚动
```

### 交互

```kotlin
Modifier.onClick(action)           // 点击处理
Modifier.onTouchDown(action)       // 触摸按下处理
Modifier.onTouchUp(action)         // 触摸抬起处理
```

### 高级

```kotlin
Modifier.componentId(4343)          // 分配组件 ID 以供引用
Modifier.visibility(intId)          // 动态可见性
Modifier.alignByBaseline()          // 按文本基线对齐 row 中的兄弟元素
Modifier.computeMeasure { height = width }  // 动态测量
Modifier.computePosition {                  // 动态定位
    x = (parentWidth - width) / 2f
    y = (parentHeight - height) / 2f
}
```

### 链式调用

修饰器从左到右链式调用。顺序对视觉效果很重要：

```kotlin
// 红色背景，内部黄色内边距
Modifier.background(Color.RED).padding(32).background(Color.YELLOW)

// 带裁剪的双边框效果
Modifier.padding(8)
    .clip(RoundedRectShape(s, s, s, s))
    .backgroundId(outerColor)
    .padding(4)
    .clip(RoundedRectShape(s, s, s, s))
    .backgroundId(innerColor)
    .padding(16)
```

## 构建可重用组件

将重复的 UI 模式提取为 `RemoteComposeContextAndroid` 上的扩展函数：

```kotlin
fun RemoteComposeContextAndroid.stockCard(name: String, price: Float) {
    val s = 48f
    row(
        Modifier.padding(32, 0, 32, 28)
            .clip(RoundedRectShape(s, s, s, s))
            .backgroundId(panelColor)
            .horizontalWeight(1f)
            .widthIn(160f, Float.MAX_VALUE)
            .padding(24)
    ) {
        column {
            text(name, colorId = nameColorId)
            val priceText = createTextFromFloat(price, 8, 0, flags)
            text(priceText, colorId = priceColorId, fontSize = fontSize)
        }
    }
}
```

然后在你的布局中使用它：

```kotlin
root {
    column(Modifier.fillMaxWidth()) {
        stockCard("S&P 500", 6846.51f)
        stockCard("Nasdaq", 23545.9f)
    }
}
```

## 主题/动态颜色

使用 `addThemedColor` 支持亮/暗模式，使用 `addColorExpression` 支持动画/数据驱动颜色：

```kotlin
// 主题颜色（全局部分）
beginGlobal()
val bgColor = writer.addThemedColor(
    Rc.AndroidColors.GROUP,
    Rc.AndroidColors.SYSTEM_ACCENT2_50,
    Rc.AndroidColors.SYSTEM_ACCENT2_800,
    0xFFFFFFFF.toInt(),  // 浅色回退
    0xFF000000.toInt(),  // 深色回退
)
endGlobal()

// 与 backgroundId 一起使用
box(RecordingModifier().backgroundId(bgColor).fillMaxSize()) { ... }

// 命名颜色（从系统解析）
val namedColor = addNamedColor("color.system_accent2_800", 0xFFFF0000.toInt())

// 两种颜色之间的动画补间
val bounce = pingPong(1, ContinuousSec()).toFloat()
val animColor = addColorExpression(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), bounce)
box(RecordingModifier().backgroundId(animColor).height(120)) {}
```

## 在组件中混合 Canvas 绘图

组件可以包含 `canvas` 块，canvas 块也可以包含组件：

```kotlin
column(Modifier.fillMaxSize().padding(16)) {
    text("Header", fontSize = 48f)

    // column 内的 Canvas
    canvas(RecordingModifier().fillMaxWidth().height(260)) {
        val w = ComponentWidth()
        val h = ComponentHeight()
        painter.setColor(Color.RED).setStrokeWidth(6f).commit()
        drawLine(0f, 0f, w, h)
    }

    // canvas 后的更多组件
    row(Modifier.fillMaxWidth()) {
        text("Footer Left")
        box(Modifier.horizontalWeight(1f))  // 间隔
        text("Footer Right")
    }
}
```

## 滚动内容

```kotlin
// 带位置跟踪的垂直滚动
val position = rf(0f)
column(
    Modifier.fillMaxSize().padding(32).verticalScroll(position.toFloat()),
    horizontal = ColumnLayout.CENTER,
) {
    for (i in 0..100) {
        text(" $i ", fontSize = 64f, textAlign = CoreText.TEXT_ALIGN_CENTER)
    }
}

// 两个并排的滚动列
row {
    column(Modifier.horizontalWeight(1f).fillMaxHeight().verticalScroll()) {
        for (i in 0..100) { text("$i") }
    }
    column(Modifier.horizontalWeight(1f).fillMaxHeight().verticalScroll()) {
        for (i in 0..100) {
            box(Modifier.size(200, 60).background(Color.RED)
                .border(2f, 0f, Color.BLUE, 0)
                .clip(RectShape(8f, 8f, 8f, 8f)),
                horizontal = BoxLayout.CENTER,
                vertical = BoxLayout.CENTER,
            ) { text("$i") }
        }
    }
}
```

## 预览

为 Android Studio 添加预览函数：

```kotlin
@Preview @Composable private fun MyDemoPreview() = RemoteDocPreview(myDemo())
```

对于返回 `RemoteComposeContext` 的函数：
```kotlin
@Preview @Composable private fun MyDemoPreview() = RemoteDocPreview(MyDemo())
```

## 注册演示

在 `DemosCreation.java` 中：

```java
import androidx.compose.remote.integration.view.demos.examples.MyDemoKt;

// 在 getDemos() 中：
getp("Category/DemoName", MyDemoKt::myDemoFunction),    // 返回 RemoteComposeWriter
getpc("Category/DemoName", MyDemoKt::myDemoFunction),   // 返回 RemoteComposeContext
```

## 常见模式

### 圆角卡片

```kotlin
val s = 48f
row(
    Modifier.padding(32, 0, 32, 28)
        .clip(RoundedRectShape(s, s, s, s))
        .backgroundId(panelColorId)
        .padding(24)
) { ... }
```

### 加权 Row（分割布局）

```kotlin
row(Modifier.fillMaxWidth()) {
    column(Modifier.horizontalWeight(1f).background(Color.YELLOW)) {
        text("Left side")
    }
    column(Modifier.size(130).background(Color.CYAN)) {
        text("Fixed right")
    }
}
```

### 弹性间隔

```kotlin
// 创建扩展函数
fun RemoteComposeContextAndroid.space() {
    box(Modifier.horizontalWeight(1f))
}

// 然后使用：
row {
    text("Left")
    space()
    text("Right")
}
```

### 徽章/胶囊按钮

```kotlin
val s = 60f
box(
    Modifier.size(120).padding(16)
        .clip(RoundedRectShape(s, s, s, s))
        .backgroundId(colorId),
    horizontal = BoxLayout.CENTER,
    vertical = BoxLayout.CENTER,
) {
    text("Label", fontSize = 48f, colorId = textColorId,
         textAlign = TextLayout.TEXT_ALIGN_CENTER)
}
```

### 动态字体大小

```kotlin
// 按系统字体大小缩放字体
val fontScale = rf(Rc.System.FONT_SIZE)
val headlineSize = (42f / 37f * fontScale).toFloat()
text("Title", fontSize = headlineSize)
```

### 动画文本属性

```kotlin
// 脉动字体粗细
val tween = (sin(ContinuousSec() % 3600f) + 1f) * 500f
text("Animated", fontWeight = tween.toFloat())

// 动画字体大小
val fontSize = (sin(ContinuousSec() % 3600f) + 1f) * 100f + 16f
text("Growing", fontSize = fontSize.toFloat())
```

## 示例文件参考

| 文件 | 关键模式 |
|------|-------------|
| `Text.kt` | 文本格式、基线对齐、自动调整大小、溢出、字体轴 |
| `LayoutModifiersDemo.kt` | computeMeasure、computePosition、动态定位 |
| `RcTicker.kt` | 完整应用：主题颜色、滚动、扩展函数、布局中的 canvas |
| `RcFlow.kt` | 带权重和尺寸约束的 Flow 布局 |
| `CanvasComponents.kt` | 带嵌入组件的 Canvas、滚动、水平滚动 |
| `ColorThemeCheck.kt` | 主题颜色、verticalScroll、RecordingModifier |
| `DemoColor.kt` | 动画颜色、动态边框、布局中的 canvas |
