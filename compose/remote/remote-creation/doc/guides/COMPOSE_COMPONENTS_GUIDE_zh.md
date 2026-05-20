# 构建 RemoteCompose 组件化示例指南（Compose 风格）

本指南介绍如何使用 **Compose 风格的组件和修饰器** 构建示例——即使用 `@RemoteComposable` 注解的方式，你可以使用 `RemoteColumn`、`RemoteRow`、`RemoteText` 等组件以及 `RemoteModifier`。

## API 风格：Compose 风格 (`@RemoteComposable`)

使用 `@RemoteComposable` 注解的 `@Composable` 函数。用于在 `DemosCompose.kt` 中注册的演示。

```kotlin
@RemoteComposable @Composable
fun MyComposableDemo() {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().background(Color.White).padding(16.dp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteText("Hello World", fontSize = 38.sp, fontWeight = FontWeight.Bold)
    }
}
```

- 使用 `RemoteModifier` 扩展函数
- 单位使用 `Dp`、`sp`、`rdp`（remote dp）、`rf`（remote float）
- 组件包括 `RemoteBox`、`RemoteColumn`、`RemoteRow`、`RemoteText`、`RemoteCanvas`、`RemoteImage`
- 在 `DemosCompose.kt` 中通过 `getComposeDoc(context, "path") { MyComposableDemo() }` 注册
- 通过 `RemotePreview { MyComposableDemo() }` 预览

---

## 组件

| 组件 | 描述 |
|-----------|-------------|
| `RemoteBox` | 堆叠容器（类似 Compose `Box`） |
| `RemoteColumn` | 垂直布局（类似 Compose `Column`） |
| `RemoteRow` | 水平布局（类似 Compose `Row`） |
| `RemoteText` | 文本显示（类似 Compose `Text`） |
| `RemoteCanvas` | 绘图表面，支持 `drawCircle` 等 |
| `RemoteCanvas0` | 底层画布，支持 `drawAnchoredText` |
| `RemoteImage` | 图片显示 |
| `FitBox` | 自适应内容的 Box，支持多种布局变体 |
| `RemoteCollapsibleColumn` | 可折叠的 Column |
| `RemoteCollapsibleRow` | 可折叠的 Row |
| `StateLayout` | 在不同状态间切换的布局 |

## 布局参数

```kotlin
RemoteColumn(
    modifier = RemoteModifier.fillMaxWidth(),
    horizontalAlignment = RemoteAlignment.CenterHorizontally,  // Start, End, CenterHorizontally
    verticalArrangement = RemoteArrangement.SpaceBetween,       // Top, Bottom, Center, SpaceBetween, SpaceEvenly, SpaceAround
) { ... }

RemoteRow(
    modifier = RemoteModifier,
    horizontalArrangement = RemoteArrangement.SpaceBetween,
    verticalAlignment = RemoteAlignment.CenterVertically,
) { ... }

RemoteBox(
    modifier = RemoteModifier,
    horizontalAlignment = RemoteAlignment.CenterHorizontally,
    verticalArrangement = RemoteArrangement.Center,
) { ... }
```

## RemoteModifier 参考

```kotlin
// 尺寸
RemoteModifier.fillMaxSize()
RemoteModifier.fillMaxWidth()
RemoteModifier.fillMaxHeight()
RemoteModifier.size(48.rdp)
RemoteModifier.size(60.rdp, 36.rdp)
RemoteModifier.width(100.rdp)
RemoteModifier.height(120.rdp)
RemoteModifier.height(IntrinsicSize.Min)    // 固有尺寸
RemoteModifier.widthIn(min = 180.dp)
RemoteModifier.heightIn(min = 90.dp)
RemoteModifier.wrapContentSize()

// 间距
RemoteModifier.padding(8.dp)
RemoteModifier.padding(start = 8.dp, end = 8.dp)
RemoteModifier.padding(bottom = 24.dp)
RemoteModifier.weight(1f)                    // 弹性权重

// 外观
RemoteModifier.background(Color(219, 247, 239))
RemoteModifier.clip(RoundedCornerShape(24.dp))
RemoteModifier.clip(RectangleShape)

// 滚动
RemoteModifier.verticalScroll(scrollState)
RemoteModifier.horizontalScroll(scrollState)

// 交互
RemoteModifier.clickable(ValueChange(state, newValue))
RemoteModifier.visibility(intState)

// 变换
RemoteModifier.graphicsLayer(scaleX = scale, scaleY = scale, rotationX = rotation)
RemoteModifier.offset(x = 10.rdp, y = 20.rdp)
RemoteModifier.zIndex(zValue)
```

## 单位

```kotlin
8.dp    // 密度无关像素（标准 Compose）
48.rdp  // remote dp（RemoteDp - 用于尺寸/宽度/高度）
38.sp   // 缩放无关像素（用于文本）
1f.rf   // remote float（RemoteFloat - 用于表达式）
```

## 状态

```kotlin
val checked = rememberRemoteIntValue { 0 }       // 可变整数状态
val scale = rememberRemoteFloat { 0.8f.rf }       // 计算浮点数
val scrollState = rememberRemoteScrollState(evenNotches = 12)
val list = rememberRemoteStringList("OFF", "ON")  // 用于查找的字符串列表

// 在文本中使用状态
RemoteText(list[checked])

// 在修饰器中使用状态
RemoteModifier.visibility(checked)
RemoteModifier.clickable(ValueChange(checked, toggleExpression))
```

## Compose 中的 Canvas

```kotlin
RemoteCanvas(modifier = RemoteModifier.size(32.rdp)) {
    val paint = RemotePaint().apply { remoteColor = Color(255, 255, 255).rc }
    drawCircle(paint = paint, radius = 34f.rf)
}
```

## 注册演示

在 `DemosCompose.kt` 中：

```kotlin
getComposeDoc(context, "Compose/MyDemo") { MyComposableDemo() }
```

## 常见模式

### 圆角卡片

```kotlin
RemoteColumn(
    modifier = RemoteModifier
        .clip(RoundedCornerShape(24.dp))
        .background(Color(219, 247, 239))
        .padding(8.dp)
) { ... }
```

### 加权 Row（分割布局）

```kotlin
RemoteRow(modifier = RemoteModifier.fillMaxWidth()) {
    RemoteBox(RemoteModifier.weight(1f)) { RemoteText("Left") }
    RemoteBox(RemoteModifier.width(130.rdp)) { RemoteText("Right") }
}
```

### 弹性间隔

```kotlin
RemoteRow {
    RemoteText("Left")
    RemoteBox(RemoteModifier.weight(1f))
    RemoteText("Right")
}
```

## 示例文件参考

| 文件 | 关键模式 |
|------|-------------|
| `DemoWeather.kt` | 完整应用：可折叠布局、图片、响应式设计 |
| `SwitchWidget.kt` | 状态管理、点击、可见性、StateLayout |
| `ScrollView.kt` | 滚动状态、graphicsLayer 变换、RemoteCanvas0 |
