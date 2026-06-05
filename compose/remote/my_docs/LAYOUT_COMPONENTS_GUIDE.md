# Remote Compose 布局组件使用指南

## 目录

- [1. 概述](#1-概述)
  - [@RemoteComposable 注解](#remotecomposable-注解)
  - [RemoteModifier 基础](#remotemodifier-基础)
- [2. 容器布局组件](#2-容器布局组件)
  - [2.1 RemoteBox — 堆叠布局](#21-remotebox--堆叠布局)
  - [2.2 RemoteColumn — 垂直布局](#22-remotecolumn--垂直布局)
  - [2.3 RemoteRow — 水平布局](#23-remoterow--水平布局)
- [3. 内容组件](#3-内容组件)
  - [3.1 RemoteText — 文本显示](#31-remotetext--文本显示)
  - [3.2 RemoteImage — 图片显示](#32-remoteimage--图片显示)
- [4. 画布组件](#4-画布组件)
  - [4.1 RemoteCanvas — 画布绘制](#41-remotecanvas--画布绘制)
- [5. 辅助布局组件](#5-辅助布局组件)
  - [5.1 RemoteSpacer — 间距占位](#51-remotespacer--间距占位)
  - [5.2 RemoteStateLayout — 状态布局](#52-remotestatelayut--状态布局)
  - [5.3 FitBox — 自适应盒子](#53-fitbox--自适应盒子)
- [6. 可折叠/流式布局组件](#6-可折叠流式布局组件)
  - [6.1 RemoteCollapsibleColumn — 可折叠垂直列](#61-remotecollapsiblecolumn--可折叠垂直列)
  - [6.2 RemoteCollapsibleRow — 可折叠水平行](#62-remotecollapsiblerow--可折叠水平行)
  - [6.3 RemoteFlowRow — 流式换行布局](#63-remoteflowrow--流式换行布局)
- [7. 对齐与排列参考](#7-对齐与排列参考)
  - [7.1 RemoteAlignment — 对齐方式](#71-remotealignment--对齐方式)
  - [7.2 RemoteArrangement — 排列方式](#72-remotearrangement--排列方式)
- [8. 辅助类型参考](#8-辅助类型参考)
  - [8.1 RemoteSize](#81-remotesize)
  - [8.2 RemoteOffset](#82-remoteoffset)
  - [8.3 RemotePaddingValues](#83-remotepaddingvalues)

---

## 1. 概述

Remote Compose 布局组件位于 `remote-creation-compose/src/main/java/.../layout/` 目录，提供与 Jetpack Compose 类似的声明式 UI API。所有布局组件都标注了 `@RemoteComposable` 注解。

### @RemoteComposable 注解

标记为 `@RemoteComposable` 的 Composable 函数会在录制阶段被捕获为 Remote Compose 文档，可传输到远端设备播放。

### RemoteModifier 基础

布局组件使用 `RemoteModifier` 而非 Compose 的 `Modifier`，支持链式调用：

```kotlin
RemoteModifier.fillMaxSize().background(RemoteColor(Color.White)).padding(16.rdp)
```

常用 RemoteModifier 方法：

| 类别 | 方法 | 说明 |
|------|------|------|
| 尺寸 | `size()` | 设置固定尺寸 |
| 尺寸 | `fillMaxSize()` | 填满父容器 |
| 尺寸 | `fillMaxWidth()` | 填满父容器宽度 |
| 尺寸 | `fillMaxHeight()` | 填满父容器高度 |
| 尺寸 | `width()` | 设置宽度 |
| 尺寸 | `height()` | 设置高度 |
| 间距 | `padding()` | 设置内边距 |
| 背景 | `background()` | 设置背景颜色 |
| 裁剪 | `clip()` | 裁剪形状 |
| 交互 | `clickable()` | 点击事件 |
| 交互 | `combinedClickable()` | 组合点击事件 |
| 交互 | `onTouchDown()` | 触摸按下事件 |
| 交互 | `onTouchUp()` | 触摸抬起事件 |
| 滚动 | `verticalScroll()` | 垂直滚动 |
| 滚动 | `horizontalScroll()` | 水平滚动 |
| 变换 | `graphicsLayer()` | 图形层变换 |
| 可见性 | `visibility()` | 控制可见性 |

---

## 2. 容器布局组件

### 2.1 RemoteBox — 堆叠布局

将子组件层叠排列，类似 Compose 的 `Box`。

**函数签名**：

```kotlin
// 带内容的 Box
@RemoteComposable
@Composable
fun RemoteBox(
    modifier: RemoteModifier = RemoteModifier,
    contentAlignment: RemoteAlignment = RemoteAlignment.TopStart,
    content: @Composable () -> Unit,
)

// 无内容的 Box（用作占位或背景）
@RemoteComposable
@Composable
fun RemoteBox(
    modifier: RemoteModifier = RemoteModifier,
)
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modifier | RemoteModifier | RemoteModifier | 应用于 Box 的修饰符 |
| contentAlignment | RemoteAlignment | TopStart | 子内容的对齐方式 |
| content | @Composable | - | Box 的内容 |

**底层 OpCode**: `startBox` / `endBox` (OpCode 202)

**代码示例**：

```kotlin
// 基础用法 — 居中堆叠
@RemoteComposable
@Composable
fun BoxExample() {
    RemoteBox(
        modifier = RemoteModifier.size(200.rdp).background(RemoteColor(Color.LightGray)),
        contentAlignment = RemoteAlignment.Center
    ) {
        RemoteText("Centered Text")
    }
}

// 进阶用法 — 多层叠加
@RemoteComposable
@Composable
fun StackedBoxExample() {
    RemoteBox(
        modifier = RemoteModifier.size(300.rdp, 200.rdp)
    ) {
        // 底层背景
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize()
                .background(RemoteColor(Color.Blue))
        )
        // 顶层内容，右下角对齐
        RemoteBox(
            modifier = RemoteModifier.align(RemoteAlignment.BottomEnd)
                .padding(16.rdp)
        ) {
            RemoteText("Overlay", color = RemoteColor(Color.White))
        }
    }
}
```

### 2.2 RemoteColumn — 垂直布局

将子组件从上到下垂直排列，类似 Compose 的 `Column`。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun RemoteColumn(
    modifier: RemoteModifier = RemoteModifier,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    content: @Composable RemoteColumnScope.() -> Unit,
)
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modifier | RemoteModifier | RemoteModifier | 应用于 Column 的修饰符 |
| verticalArrangement | RemoteArrangement.Vertical | Top | 子组件的垂直排列方式 |
| horizontalAlignment | RemoteAlignment.Horizontal | Start | 子组件的水平对齐方式 |
| content | @Composable RemoteColumnScope | - | Column 的内容 |

**RemoteColumnScope**：提供 `weight()` 修饰符，用于按比例分配剩余空间。

**底层 OpCode**: `startColumn` / `endColumn` (OpCode 204)

**代码示例**：

```kotlin
// 基础用法 — 简单垂直列表
@RemoteComposable
@Composable
fun ColumnExample() {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().padding(16.rdp),
        verticalArrangement = RemoteArrangement.spacedBy(8.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally
    ) {
        RemoteText("Item 1")
        RemoteText("Item 2")
        RemoteText("Item 3")
    }
}

// 进阶用法 — weight 分配空间
@RemoteComposable
@Composable
fun ColumnWeightExample() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteBox(modifier = RemoteModifier.weight(1f.rf).background(RemoteColor(Color.Red)))
        RemoteBox(modifier = RemoteModifier.weight(2f.rf).background(RemoteColor(Color.Green)))
        RemoteBox(modifier = RemoteModifier.weight(1f.rf).background(RemoteColor(Color.Blue)))
    }
}

// SpaceBetween 排列
@RemoteComposable
@Composable
fun ColumnSpaceBetweenExample() {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().padding(16.rdp),
        verticalArrangement = RemoteArrangement.SpaceBetween
    ) {
        RemoteText("Top")
        RemoteText("Center")
        RemoteText("Bottom")
    }
}
```

### 2.3 RemoteRow — 水平布局

将子组件从左到右水平排列，类似 Compose 的 `Row`。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun RemoteRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top,
    content: @Composable RemoteRowScope.() -> Unit,
)
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modifier | RemoteModifier | RemoteModifier | 应用于 Row 的修饰符 |
| horizontalArrangement | RemoteArrangement.Horizontal | Start | 子组件的水平排列方式 |
| verticalAlignment | RemoteAlignment.Vertical | Top | 子组件的垂直对齐方式 |
| content | @Composable RemoteRowScope | - | Row 的内容 |

**RemoteRowScope**：提供 `weight()` 修饰符。

**底层 OpCode**: `startRow` / `endRow` (OpCode 203)

**代码示例**：

```kotlin
// 基础用法 — 水平排列
@RemoteComposable
@Composable
fun RowExample() {
    RemoteRow(
        modifier = RemoteModifier.fillMaxWidth().padding(16.rdp),
        horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
        verticalAlignment = RemoteAlignment.CenterVertically
    ) {
        RemoteBox(modifier = RemoteModifier.size(40.rdp).background(RemoteColor(Color.Red)))
        RemoteText("Label")
        RemoteBox(modifier = RemoteModifier.size(40.rdp).background(RemoteColor(Color.Blue)))
    }
}

// 进阶用法 — weight 等分
@RemoteComposable
@Composable
fun RowWeightExample() {
    RemoteRow(modifier = RemoteModifier.fillMaxWidth()) {
        RemoteBox(modifier = RemoteModifier.weight(1f.rf).height(100.rdp).background(RemoteColor(Color.Cyan)))
        RemoteBox(modifier = RemoteModifier.weight(1f.rf).height(100.rdp).background(RemoteColor(Color.Magenta)))
        RemoteBox(modifier = RemoteModifier.weight(1f.rf).height(100.rdp).background(RemoteColor(Color.Yellow)))
    }
}
```

---

## 3. 内容组件

### 3.1 RemoteText — 文本显示

显示文本内容，类似 Compose 的 `Text`。

**主要函数签名**（公共版）：

```kotlin
@Composable
@RemoteComposable
fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    fontSize: RemoteTextUnit? = null,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: RemoteTextStyle = RemoteTextStyle.Default,
    fontVariationSettings: FontVariation.Settings? = null,
)
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| text | RemoteString | - | 要显示的文本（支持动态字符串） |
| modifier | RemoteModifier | RemoteModifier | 修饰符 |
| color | RemoteColor? | null | 文本颜色，null 时使用 style 中的颜色或 Color.Black |
| fontSize | RemoteTextUnit? | null | 字体大小（使用 sp 单位，如 16.rsp） |
| fontStyle | FontStyle? | null | 字体样式（Normal/Italic） |
| fontWeight | FontWeight? | null | 字体粗细（Normal/Bold/W100-W900） |
| fontFamily | RemoteFontFamily? | null | 字体族 |
| textAlign | TextAlign? | null | 文本对齐（Start/Center/End/Justify） |
| overflow | TextOverflow | Clip | 溢出处理（Clip/Ellipsis/Visible） |
| maxLines | Int | Int.MAX_VALUE | 最大行数 |
| style | RemoteTextStyle | Default | 文本样式（包含 letterSpacing、lineHeight 等） |
| fontVariationSettings | FontVariation.Settings? | null | 字体变体设置 |

**底层 OpCode**: `startTextComponent` / `endTextComponent` (OpCode 208)

**代码示例**：

```kotlin
// 基础用法 — 简单文本
@RemoteComposable
@Composable
fun TextExample() {
    RemoteText("Hello, Remote Compose!")
}

// 进阶用法 — 富文本样式
@RemoteComposable
@Composable
fun StyledTextExample() {
    RemoteColumn(
        verticalArrangement = RemoteArrangement.spacedBy(8.rdp)
    ) {
        // 粗体标题
        RemoteText(
            "Title",
            fontSize = 24.rsp,
            fontWeight = FontWeight.Bold,
            color = RemoteColor(Color.DarkGray)
        )
        // 居中正文
        RemoteText(
            "Centered body text with ellipsis overflow".rs,
            fontSize = 14.rsp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
        // 动态文本
        val counter = rememberMutableRemoteInt(0)
        RemoteText(
            "Count: ${counter}".rs,
            fontSize = 18.rsp,
            color = RemoteColor(Color.Blue)
        )
    }
}
```

### 3.2 RemoteImage — 图片显示

显示图片内容，类似 Compose 的 `Image`。

**函数签名**：

```kotlin
// RemoteBitmap 版（推荐）
@Composable
@RemoteComposable
fun RemoteImage(
    remoteBitmap: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = DefaultAlpha.rf,
)

// ImageBitmap 版
@Composable
@RemoteComposable
fun RemoteImage(
    bitmap: ImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = DefaultAlpha.rf,
)
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| remoteBitmap/bitmap | RemoteBitmap/ImageBitmap | - | 要显示的图片 |
| contentDescription | RemoteString? | - | 无障碍描述 |
| modifier | RemoteModifier | RemoteModifier | 修饰符 |
| contentScale | ContentScale | Fit | 缩放规则（Fit/Crop/Inside/FillBounds/FillWidth/FillHeight/None） |
| alpha | RemoteFloat | 1f.rf | 透明度（0.0-1.0） |

**底层 OpCode**: `image` (OpCode 234)

**代码示例**：

```kotlin
// 基础用法 — 显示图片
@RemoteComposable
@Composable
fun ImageExample() {
    val bitmap = rememberRemoteBitmap(R.drawable.my_image)
    RemoteImage(
        remoteBitmap = bitmap,
        contentDescription = "My image".rs,
        modifier = RemoteModifier.size(200.rdp)
    )
}

// 进阶用法 — 动态透明度 + 裁剪缩放
@RemoteComposable
@Composable
fun AdvancedImageExample() {
    val bitmap = rememberRemoteBitmap(R.drawable.photo)
    val alpha = rememberMutableRemoteFloat(1f)
    RemoteImage(
        remoteBitmap = bitmap,
        contentDescription = null,
        modifier = RemoteModifier.size(150.rdp)
            .clip(RemoteRoundedCornerShape(8.rdp)),
        contentScale = ContentScale.Crop,
        alpha = alpha
    )
}
```

---

## 4. 画布组件

### 4.1 RemoteCanvas — 画布绘制

提供自定义绘制能力，类似 Compose 的 `Canvas`。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun RemoteCanvas(
    modifier: RemoteModifier = RemoteModifier,
    content: RemoteDrawScope.() -> Unit,
)
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modifier | RemoteModifier | RemoteModifier | 修饰符 |
| content | RemoteDrawScope.() -> Unit | - | 绘制命令 |

**底层 OpCode**: `startCanvas` / `endCanvas` (OpCode 205)

**RemoteDrawScope 常用方法**：

| 方法 | 说明 |
|------|------|
| `drawRect(paint, topLeft, size)` | 绘制矩形 |
| `drawRoundRect(paint, topLeft, size, cornerRadius)` | 绘制圆角矩形 |
| `drawCircle(paint, center, radius)` | 绘制圆形 |
| `drawOval(paint, topLeft, size)` | 绘制椭圆 |
| `drawArc(paint, startAngle, sweepAngle, useCenter, topLeft, size)` | 绘制弧形 |
| `drawLine(paint, start, end)` | 绘制直线 |
| `drawPath(path, paint)` | 绘制路径 |
| `drawImage(image, topLeft, paint)` | 绘制图片 |
| `drawText(text, x, y, paint)` | 绘制文本 |
| `drawAnchoredText(text, anchorX, anchorY, panX, panY, flags, paint)` | 绘制锚点对齐文本 |
| `drawTextOnPath(text, path, hOffset, vOffset, paint)` | 沿路径绘制文本 |
| `drawTextOnCircle(text, centerX, centerY, radius, startAngle, warpRadiusOffset, paint)` | 沿圆形绘制文本 |
| `drawTweenPath(path1, path2, tween, start, stop, paint)` | 绘制补间路径 |
| `rotate(degrees, block)` | 旋转变换 |
| `translate(left, top, block)` | 平移变换 |
| `scale(scaleX, scaleY, pivot, block)` | 缩放变换 |
| `clipRect(left, top, right, bottom, block)` | 裁剪到矩形 |
| `clipPath(path, block)` | 裁剪到路径 |
| `loop(from, until, step, body)` | 循环绘制 |

**代码示例**：

```kotlin
// 基础用法 — 绘制简单图形
@RemoteComposable
@Composable
fun CanvasExample() {
    RemoteCanvas(modifier = RemoteModifier.size(200.rdp)) {
        val redPaint = RemotePaint().setColor(RemoteColor(Color.Red))
        val bluePaint = RemotePaint().setColor(RemoteColor(Color.Blue))

        drawRect(paint = redPaint, topLeft = RemoteOffset(10f.rf, 10f.rf),
            size = RemoteSize(80f.rf, 80f.rf))
        drawCircle(paint = bluePaint, center = RemoteOffset(150f.rf, 150f.rf),
            radius = 40f.rf)
    }
}

// 进阶用法 — 变换 + 裁剪 + 循环
@RemoteComposable
@Composable
fun AdvancedCanvasExample() {
    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
        val paint = RemotePaint().setColor(RemoteColor(Color.Cyan))

        // 旋转绘制
        rotate(degrees = 45f.rf) {
            drawRect(paint = paint, topLeft = RemoteOffset(50f.rf, 50f.rf),
                size = RemoteSize(100f.rf, 100f.rf))
        }

        // 裁剪绘制
        clipRect(left = 0f.rf, top = 0f.rf, right = 100f.rf, bottom = 100f.rf) {
            drawCircle(paint = paint, center = RemoteOffset(100f.rf, 100f.rf),
                radius = 80f.rf)
        }

        // 循环绘制
        loop(from = 0f.rf, until = 10f.rf, step = 1f.rf) { index ->
            translate(left = index * 20f.rf, top = 0f.rf) {
                drawCircle(paint = paint, radius = 8f.rf)
            }
        }
    }
}
```

---

## 5. 辅助布局组件

### 5.1 RemoteSpacer — 间距占位

占用指定大小的空间，不显示任何内容。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun RemoteSpacer(modifier: RemoteModifier = RemoteModifier)
```

**底层 OpCode**: 委托 `startBox` / `endBox` (OpCode 202)

**代码示例**：

```kotlin
@RemoteComposable
@Composable
fun SpacerExample() {
    RemoteColumn {
        RemoteText("Above")
        RemoteSpacer(modifier = RemoteModifier.height(24.rdp))
        RemoteText("Below — 24dp gap")
    }
}
```

### 5.2 RemoteStateLayout — 状态布局

根据状态值显示不同的内容，支持三种状态类型。

**函数签名**：

```kotlin
// Enum 状态
@RemoteComposable
@Composable
fun <T : Enum<T>> RemoteStateLayout(
    state: RemoteEnum<T>,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (T) -> Unit,
)

// Boolean 状态
@RemoteComposable
@Composable
fun RemoteStateLayout(
    state: RemoteBoolean,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (Boolean) -> Unit,
)

// Int 状态
@RemoteComposable
@Composable
fun RemoteStateLayout(
    state: RemoteInt,
    vararg states: Int,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (Int) -> Unit,
)
```

**底层 OpCode**: `startStateLayout` / `endStateLayout` (OpCode 217)

**代码示例**：

```kotlin
// Enum 状态切换
enum class SwitchState { Off, On }

@RemoteComposable
@Composable
fun StateLayoutEnumExample() {
    val state = rememberMutableRemoteEnum(SwitchState.Off)
    RemoteStateLayout(state = state) { currentState ->
        when (currentState) {
            SwitchState.Off -> RemoteBox(modifier = RemoteModifier.size(60.rdp, 36.rdp)
                .background(RemoteColor(Color.Gray)))
            SwitchState.On -> RemoteBox(modifier = RemoteModifier.size(60.rdp, 36.rdp)
                .background(RemoteColor(Color.Blue)))
        }
    }
}

// Int 状态切换（宿主端驱动）
@RemoteComposable
@Composable
fun StateLayoutIntExample() {
    val stateId = 100
    val state = rememberNamedRemoteInt(stateId, 0)
    RemoteStateLayout(state = state, states = intArrayOf(0, 1, 2)) { currentState ->
        val color = when (currentState) {
            0 -> Color.Red; 1 -> Color.Green; else -> Color.Blue
        }
        RemoteBox(modifier = RemoteModifier.size(100.rdp).background(RemoteColor(color)))
    }
}
```

### 5.3 FitBox — 自适应盒子

根据内容尺寸自动调整的盒子布局。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun FitBox(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.CenterHorizontally,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Center,
    content: @RemoteComposable @Composable () -> Unit = {},
)
```

**底层 OpCode**: `startFitBox` / `endFitBox` (OpCode 176)

**代码示例**：

```kotlin
@RemoteComposable
@Composable
fun FitBoxExample() {
    FitBox(
        modifier = RemoteModifier.size(300.rdp, 200.rdp)
            .background(RemoteColor(Color.LightGray)),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center
    ) {
        RemoteText("Fitted Content", fontSize = 20.rsp)
    }
}
```

---

## 6. 可折叠/流式布局组件

### 6.1 RemoteCollapsibleColumn — 可折叠垂直列

空间不足时按优先级折叠子项。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun RemoteCollapsibleColumn(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    content: @Composable RemoteCollapsibleColumnScope.() -> Unit,
)
```

**RemoteCollapsibleColumnScope**：额外提供 `priority()` 修饰符，数值越小越先被折叠。

**底层 OpCode**: `startCollapsibleColumn` / `endCollapsibleColumn` (OpCode 233)

**代码示例**：

```kotlin
@RemoteComposable
@Composable
fun CollapsibleColumnExample() {
    RemoteCollapsibleColumn(
        modifier = RemoteModifier.fillMaxWidth().height(100.rdp)
    ) {
        // 高优先级 — 最后被折叠
        RemoteText("Important", modifier = RemoteModifier.priority(0f))
        // 低优先级 — 最先被折叠
        RemoteText("Optional", modifier = RemoteModifier.priority(10f))
        // 中等优先级
        RemoteText("Normal", modifier = RemoteModifier.priority(5f))
    }
}
```

### 6.2 RemoteCollapsibleRow — 可折叠水平行

空间不足时按优先级折叠子项。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun RemoteCollapsibleRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top,
    content: @Composable RemoteCollapsibleRowScope.() -> Unit,
)
```

**RemoteCollapsibleRowScope**：额外提供 `priority()` 修饰符。

**底层 OpCode**: `startCollapsibleRow` / `endCollapsibleRow` (OpCode 230)

**代码示例**：

```kotlin
@RemoteComposable
@Composable
fun CollapsibleRowExample() {
    RemoteCollapsibleRow(
        modifier = RemoteModifier.fillMaxWidth()
    ) {
        RemoteText("Always", modifier = RemoteModifier.priority(0f))
        RemoteText("Maybe", modifier = RemoteModifier.priority(5f))
        RemoteText("Rarely", modifier = RemoteModifier.priority(10f))
    }
}
```

### 6.3 RemoteFlowRow — 流式换行布局

子组件水平排列，超出宽度时自动换行。

**函数签名**：

```kotlin
@RemoteComposable
@Composable
fun RemoteFlowRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit,
)
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| maxItemsInEachRow | Int | Int.MAX_VALUE | 每行最大项数 |
| maxLines | Int | Int.MAX_VALUE | 最大行数 |

**底层 OpCode**: `startFlow` / `endFlow` (OpCode 240, 实验性)

**代码示例**：

```kotlin
@RemoteComposable
@Composable
fun FlowRowExample() {
    RemoteFlowRow(
        modifier = RemoteModifier.fillMaxWidth().padding(16.rdp),
        horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
        verticalArrangement = RemoteArrangement.spacedBy(4.rdp)
    ) {
        val tags = listOf("Kotlin", "Compose", "Remote", "Android", "UI", "Widget")
        tags.forEach { tag ->
            RemoteBox(modifier = RemoteModifier
                .background(RemoteColor(Color.LightGray), RemoteRoundedCornerShape(4.rdp))
                .padding(horizontal = 8.rdp, vertical = 4.rdp)) {
                RemoteText(tag, fontSize = 12.rsp)
            }
        }
    }
}
```

---

## 7. 对齐与排列参考

### 7.1 RemoteAlignment — 对齐方式

**2D 对齐常量**（用于 RemoteBox 的 contentAlignment）：

| 常量 | 说明 |
|------|------|
| TopStart | 左上角 |
| TopCenter | 顶部居中 |
| TopEnd | 右上角 |
| CenterStart | 左侧居中 |
| Center | 正中心 |
| CenterEnd | 右侧居中 |
| BottomStart | 左下角 |
| BottomCenter | 底部居中 |
| BottomEnd | 右下角 |

**1D 垂直对齐**（用于 RemoteRow 的 verticalAlignment）：

| 常量 | 说明 |
|------|------|
| Top | 顶部对齐 |
| CenterVertically | 垂直居中 |
| Bottom | 底部对齐 |

**1D 水平对齐**（用于 RemoteColumn 的 horizontalAlignment）：

| 常量 | 说明 |
|------|------|
| Start | 起始对齐 |
| CenterHorizontally | 水平居中 |
| End | 末尾对齐 |

### 7.2 RemoteArrangement — 排列方式

**预定义排列常量**：

| 常量 | 类型 | 说明 |
|------|------|------|
| Start | Horizontal | 起始排列 |
| End | Horizontal | 末尾排列 |
| Top | Vertical | 顶部排列 |
| Bottom | Vertical | 底部排列 |
| Center | HorizontalOrVertical | 居中排列 |
| SpaceEvenly | HorizontalOrVertical | 均匀间距（含两端） |
| SpaceBetween | HorizontalOrVertical | 两端对齐，中间均匀间距 |
| SpaceAround | HorizontalOrVertical | 等额间距（含两端半间距） |

**spacedBy 工厂方法**：

```kotlin
// 基础间距
RemoteArrangement.spacedBy(8.rdp)
RemoteArrangement.spacedBy(8f.rf)

// 间距 + 对齐
RemoteArrangement.spacedBy(8.rdp, RemoteAlignment.CenterVertically)
RemoteArrangement.spacedBy(8.rdp, RemoteAlignment.CenterHorizontally)
```

---

## 8. 辅助类型参考

### 8.1 RemoteSize

```kotlin
class RemoteSize(width: RemoteFloat, height: RemoteFloat) {
    val width: RemoteFloat
    val height: RemoteFloat
    val minDimension: RemoteFloat
    val center: RemoteOffset

    companion object {
        val Zero: RemoteSize
    }
}
```

### 8.2 RemoteOffset

```kotlin
class RemoteOffset(x: RemoteFloat, y: RemoteFloat) {
    val x: RemoteFloat
    val y: RemoteFloat
    val minDimension: RemoteFloat

    companion object {
        val Zero: RemoteOffset
    }
}
```

### 8.3 RemotePaddingValues

```kotlin
class RemotePaddingValues(
    leftPadding: RemoteDp = 0.rdp,
    topPadding: RemoteDp = 0.rdp,
    rightPadding: RemoteDp = 0.rdp,
    bottomPadding: RemoteDp = 0.rdp,
) {
    // 四边相同
    constructor(all: RemoteDp)
    // 水平 + 垂直
    constructor(horizontal: RemoteDp = 0.rdp, vertical: RemoteDp = 0.rdp)
}
```
