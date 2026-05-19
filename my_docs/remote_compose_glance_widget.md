# Remote Compose + Glance：自定义微件开发与 LLM 生成指南

> 本文档基于 AndroidX `glance` 和 `compose/remote` 模块源码分析，阐述两者关系、架构原理，以及如何通过 LLM（如 Gemini）生成自定义微件。

---

## 目录

- [1. 功能背景与关系概述](#1-功能背景与关系概述)
- [2. Glance 与 Remote Compose 架构关系](#2-glance-与-remote-compose-架构关系)
  - [2.1 依赖关系](#21-依赖关系)
  - [2.2 翻译层架构](#22-翻译层架构)
  - [2.3 渲染流程对比](#23-渲染流程对比)
- [3. Remote Compose 核心能力](#3-remote-compose-核心能力)
- [4. Glance → Remote Compose 翻译详解](#4-glance--remote-compose-翻译详解)
  - [4.1 组件映射表](#41-组件映射表)
  - [4.2 Modifier 转换](#42-modifier-转换)
  - [4.3 代码示例分析](#43-代码示例分析)
- [5. LLM 生成微件的技术路径](#5-llm-生成微件的技术路径)
  - [5.1 "帮我制作微件"功能原理](#51-帮我制作微件功能原理)
  - [5.2 LLM 生成 Remote Compose 代码](#52-llm-生成-remote-compose-代码)
  - [5.3 从自然语言到微件的完整链路](#53-从自然语言到微件的完整链路)
- [6. 自定义微件开发实战](#6-自定义微件开发实战)
  - [6.1 使用 Glance API 开发](#61-使用-glance-api-开发)
  - [6.2 使用 Remote Compose 直接开发](#62-使用-remote-compose-直接开发)
  - [6.3 混合开发模式](#63-混合开发模式)
- [7. 高级功能](#7-高级功能)
  - [7.1 SnapScroll 与分页](#71-snapscroll-与分页)
  - [7.2 粒子效果](#72-粒子效果)
  - [7.3 触摸交互](#73-触摸交互)
  - [7.4 动画与表达式](#74-动画与表达式)
- [8. 向后兼容性](#8-向后兼容性)
- [9. 项目目录结构](#9-项目目录结构)

---

## 1. 功能背景与关系概述

Google 在 Android 16 及更高版本中推出了 **"帮我制作微件"（Make My Widget）** 功能，用户可以通过 Gemini 用自然语言描述来生成完全自适应的自定义微件。这一功能的底层技术栈正是 **Remote Compose**。

**核心关系：**

| 层级 | 模块 | 作用 |
|------|------|------|
| 用户 API 层 | **Glance** | 提供 Compose 风格的声明式 API，开发者用 Kotlin 编写微件 |
| 翻译层 | **GlanceRemoteComposeTranslator** | 将 Glance 的 Emittable 树翻译为 Remote Compose 文档 |
| 运行时层 | **Remote Compose (remote-core)** | 解析 Wire Format，执行布局、渲染、动画、交互 |
| 输出层 | **RemoteViews.DrawInstructions** | Android 16+ 新 API，将二进制指令传递给系统渲染 |

**关键洞察：**
- Glance 是 Remote Compose 的**上层封装**，让开发者可以用熟悉的 Compose API 编写微件
- Remote Compose 是**底层引擎**，负责将 UI 描述序列化为紧凑二进制格式，在设备端高效渲染
- "帮我制作微件"功能让 LLM 直接生成 Remote Compose 代码（或 Glance 代码），实现自然语言到微件的转换

---

## 2. Glance 与 Remote Compose 架构关系

### 2.1 依赖关系

在 [glance-appwidget/build.gradle](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/build.gradle#L40-L42) 中明确声明了对 Remote Compose 模块的依赖：

```gradle
dependencies {
    implementation(project(":compose:remote:remote-core"))
    implementation(project(":compose:remote:remote-creation"))
    testImplementation(project(":compose:remote:remote-core"))
    testImplementation(project(":compose:remote:remote-creation"))
}
```

这表明 Glance AppWidget 模块深度集成了 Remote Compose 技术栈。

### 2.2 翻译层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Glance → Remote Compose 翻译架构                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐  │
│  │   Glance API    │     │  翻译层 (Kotlin)  │     │  Remote Compose │  │
│  │  (Compose-like) │────▶│                  │────▶│   Wire Format   │  │
│  │                 │     │                  │     │                 │  │
│  │  @Composable    │     │  GlanceRemote    │     │  Binary Buffer  │  │
│  │  Text(), Box()  │     │  ComposeTranslator│    │  (Operations)   │  │
│  │  Column(), Row()│     │                  │     │                 │  │
│  └─────────────────┘     └──────────────────┘     └─────────────────┘  │
│           │                       │                        │           │
│           ▼                       ▼                        ▼           │
│  ┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐  │
│  │  Emittable Tree │     │  RcElement Tree  │     │  RemoteViews    │  │
│  │                 │     │                  │     │  .DrawInstructions│  │
│  │  EmittableText  │────▶│  RcText          │────▶│                 │  │
│  │  EmittableBox   │────▶│  RcBox           │────▶│  Android 16+    │  │
│  │  EmittableColumn│────▶│  RcColumn        │────▶│  System Renderer│  │
│  │  EmittableRow   │────▶│  RcRow           │────▶│                 │  │
│  │  EmittableImage │────▶│  RcImage         │────▶│                 │  │
│  │  EmittableButton│────▶│  RcButton        │────▶│                 │  │
│  └─────────────────┘     └──────────────────┘     └─────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.3 渲染流程对比

**传统 Glance（Android 15 及以下）：**
```
Glance Composable → Emittable Tree → XML Layout Templates → RemoteViews → System Widget Host
```

**Remote Compose 模式（Android 16+）：**
```
Glance Composable → Emittable Tree → RemoteComposeContext → Wire Buffer → RemoteViews.DrawInstructions → System Renderer
```

关键差异：
- 传统模式使用预定义的 XML 布局模板（如 `glance_text.xml`、`glance_button.xml`）
- Remote Compose 模式直接生成二进制绘制指令，无需 XML 模板，支持更丰富的自定义效果

---

## 3. Remote Compose 核心能力

Remote Compose 为 Glance 微件带来了以下新能力：

| 能力 | 说明 | Glance 对应支持 |
|------|------|----------------|
| **SnapScroll** | 吸附滚动，分页效果 | `VerticalScrollMode.SnapScroll` |
| **富有表现力的按钮** | Material3 风格按钮，圆角、边框、背景 | `EmittableM3TextButton` |
| **粒子效果** | 高性能粒子系统 | 通过 Remote Compose 直接访问 |
| **触摸交互** | 低延迟触摸响应，无需服务端通信 | `addTouch` 系统 |
| **动画表达式** | RPN 数学表达式驱动动画 | `RFloat` 表达式系统 |
| **Canvas 绘图** | 自定义绘制：圆形、矩形、路径、文字 | `RemoteCanvas` |
| **主题颜色** | 自动适配系统主题（浅色/深色） | `addThemedColor` |
| **自适应布局** | Flow 布局、Collapsible 布局 | `RemoteCollapsibleColumn/Row` |

---

## 4. Glance → Remote Compose 翻译详解

### 4.1 组件映射表

| Glance 组件 | Remote Compose 组件 | 源文件 |
|-------------|---------------------|--------|
| `EmittableText` | `RcText` → `startTextComponent` | [RcText.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcText.kt) |
| `EmittableBox` | `RcBox` → `box()` | [RcBox.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcBox.kt) |
| `EmittableColumn` | `RcColumn` → `column()` | [RcColumn.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcColumn.kt) |
| `EmittableRow` | `RcRow` → `row()` | [RcRow.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcRow.kt) |
| `EmittableImage` | `RcImage` → `image()` | [RcImage.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcImage.kt) |
| `EmittableButton` | `RcButton` → `box() + text()` | [RcButton.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcButton.kt) |
| `EmittableM3TextButton` | `RcMaterial3TextButton` → `row() + text() + image()` | [RcMaterial3TextButton.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcMaterial3TextButton.kt) |
| `EmittableLazyColumn` | `RcLazyColumn` → `column() + CustomScrollModifier` | [RcLazyColumn.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcLazyColumn.kt) |
| `EmittableSpacer` | `RcSpacer` → `box()` | [RcSpacer.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcSpacer.kt) |

### 4.2 Modifier 转换

[ApplyModifiersToRemoteCompose.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/ApplyModifiersToRemoteCompose.kt) 负责将 Glance Modifier 转换为 Remote Compose 的 `RecordingModifier`：

```kotlin
internal fun convertGlanceModifierToRemoteComposeModifier(
    modifiers: GlanceModifier,
    translationContext: TranslationContext,
): RecordingModifier {
    val outputModifier = RecordingModifier()
    
    modifiers.foldIn(Unit) { _, modifier ->
        when (modifier) {
            is ActionModifier -> { /* 转换为 onClick */ }
            is WidthModifier -> { /* 转换为 width/fillMaxWidth/horizontalWeight */ }
            is HeightModifier -> { /* 转换为 height/fillMaxHeight/verticalWeight */ }
            is BackgroundModifier -> { /* 转换为 background() */ }
            is PaddingModifier -> { /* 转换为 padding() */ }
            is VisibilityModifier -> { /* 转换为 visibility() */ }
            is CornerRadiusModifier -> { /* 转换为 clip(RoundedRectShape) */ }
            // ...
        }
    }
    return outputModifier
}
```

**支持的 Modifier 映射：**

| Glance Modifier | Remote Compose Modifier |
|-----------------|------------------------|
| `width(Dp)` | `width(px)` |
| `height(Dp)` | `height(px)` |
| `fillMaxWidth()` | `fillMaxWidth()` |
| `fillMaxHeight()` | `fillMaxHeight()` |
| `padding()` | `padding(start, top, end, bottom)` |
| `background(color)` | `background(argb)` |
| `cornerRadius()` | `clip(RoundedRectShape)` |
| `clickable(action)` | `onClick(HostAction(id))` |
| `visibility()` | `visibility(intId)` |
| `horizontalWeight` | `horizontalWeight(weight)` |
| `verticalWeight` | `verticalWeight(weight)` |

### 4.3 代码示例分析

以 [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt) 为例，翻译流程分为两步：

**Step 1: 创建 Remote Compose 文档**
```kotlin
private fun translateSingleRootEmittableTreeToRemoteCompose(
    context: Context,
    rootEmittable: Emittable,
    // ...
): Pair<RemoteComposeContext, TranslationActionMap> {
    val rcContext = RemoteComposeContext(
        creationDisplayInfo = CreationDisplayInfo(widthPx, heightPx, density),
        contentDescription = description,
        profile = GlanceRemoteComposeProfile, // WIDGETS_V7
    ) {
        // Step 1a: 将 Glance Emittable 翻译为 RcElement 树
        val translatedRoot: RcElement = translateEmittable(rootEmittable, translationContext)
        
        // Step 1b: 将 RcElement 树写入 Remote Compose 文档
        root { translatedRoot.writeComponent(translationContext) }
    }
    return rcContext to actionMap
}
```

**Step 2: 创建 RemoteViews**
```kotlin
// DrawInstructionRemoteViews.kt
private fun drawInstructionRemoteViews(
    translation: GlanceToRemoteComposeTranslation.Single
): RemoteViews {
    val bytes: ByteArray = getBytes(translation.remoteComposeContext)
    val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
    val remoteViews = RemoteViews(drawInstructions)
    
    // 绑定点击事件
    translation.actionMap.forEach { (actionId, pendingIntent) ->
        remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
    }
    return remoteViews
}
```

---

## 5. LLM 生成微件的技术路径

### 5.1 "帮我制作微件"功能原理

Google 的 "帮我制作微件"（Make My Widget）功能允许用户通过自然语言描述让 Gemini 生成自定义微件。其技术原理基于以下架构：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    "帮我制作微件" 技术链路                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │   用户输入    │    │   Gemini/LLM  │    │  代码生成     │              │
│  │              │    │              │    │              │              │
│  │ "创建一个天气 │───▶│ 理解意图、   │───▶│ 生成 Glance  │              │
│  │  微件，显示   │    │ 提取参数     │    │ 或 Remote    │              │
│  │  温度和图标"  │    │              │    │ Compose 代码 │              │
│  └──────────────┘    └──────────────┘    └──────────────┘              │
│                                                   │                     │
│                                                   ▼                     │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │   微件渲染    │◀───│ 系统编译运行  │◀───│  项目集成     │              │
│  │              │    │              │    │              │              │
│  │ Android 16+  │    │ Gradle Build │    │ 插入到 App   │              │
│  │ 系统渲染器    │    │              │    │ 代码库中     │              │
│  └──────────────┘    └──────────────┘    └──────────────┘              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 LLM 生成 Remote Compose 代码

LLM 可以生成两种风格的代码：

**风格 A: Glance API（推荐）**
```kotlin
class WeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WeatherWidgetContent()
        }
    }
    
    @Composable
    fun WeatherWidgetContent() {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("24°C", style = TextStyle(fontSize = 48.sp))
            Text("Sunny", style = TextStyle(fontSize = 18.sp))
        }
    }
}
```

**风格 B: Remote Compose 直接 API（高级）**
```kotlin
fun weatherWidgetDoc(context: Context): RemoteComposeWriter {
    val rc = RemoteComposeContextAndroid(
        platform = AndroidxRcPlatformServices(),
        apiLevel = 7,
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
    ) {
        root {
            column(
                Modifier.fillMaxSize().padding(16).background(Color.WHITE),
                horizontal = ColumnLayout.CENTER,
            ) {
                text("24°C", fontSize = 64f, textAlign = CoreText.TEXT_ALIGN_CENTER)
                text("Sunny", fontSize = 24f, color = Color.GRAY)
            }
        }
    }
    return rc.writer
}
```

### 5.3 从自然语言到微件的完整链路

```
用户自然语言描述
        │
        ▼
┌───────────────────┐
│ 1. 意图理解 (LLM)  │
│ 提取: 组件类型、    │
│ 布局结构、样式、   │
│ 数据源、交互需求   │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ 2. 架构选择        │
│ - 简单微件 → Glance│
│ - 复杂动画/交互    │
│   → Remote Compose │
│   直接 API         │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ 3. 代码生成        │
│ 生成 Kotlin 代码   │
│ 包含:              │
│ - 布局组件         │
│ - Modifier 链      │
│ - 状态管理         │
│ - 点击交互         │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ 4. 编译与验证      │
│ - 语法检查         │
│ - 布局预览         │
│ - 尺寸适配         │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ 5. 部署运行        │
│ Android 16+:       │
│ Remote Compose 模式│
│ Android 15-:       │
│ 传统 XML 模板模式  │
└───────────────────┘
```

---

## 6. 自定义微件开发实战

### 6.1 使用 Glance API 开发

Glance 提供了最简洁的 API，适合大多数微件场景：

```kotlin
class MyCustomWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MyWidgetContent()
        }
    }
}

@Composable
fun MyWidgetContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Hello Widget",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.Black)
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Button(
                text = "Click Me",
                onClick = actionStartActivity<MainActivity>()
            )
        }
    }
}
```

### 6.2 使用 Remote Compose 直接开发

对于需要高级自定义效果（粒子、复杂动画、Canvas 绘图）的场景，可以直接使用 Remote Compose API：

```kotlin
@RemoteComposable @Composable
fun AdvancedWidget() {
    val scale = rememberRemoteFloat { 0.8f.rf }
    
    RemoteColumn(
        modifier = RemoteModifier
            .fillMaxSize()
            .background(Color(219, 247, 239))
            .padding(16.dp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteText("Advanced", fontSize = 38.sp, fontWeight = FontWeight.Bold)
        
        RemoteCanvas(modifier = RemoteModifier.size(200.rdp)) {
            val paint = RemotePaint().apply { 
                remoteColor = Color(255, 100, 100).rc 
            }
            // 绘制动态圆形
            drawCircle(paint = paint, radius = 50f.rf * scale)
        }
        
        RemoteButton(
            text = "Animate",
            onClick = { scale.value = 1.2f.rf }
        )
    }
}
```

### 6.3 混合开发模式

在实际项目中，可以结合两种模式：

```kotlin
// 使用 Glance 作为入口，内部调用 Remote Compose 能力
class HybridWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Glance API 提供基础布局
            GlanceContent()
        }
    }
}

@Composable
fun GlanceContent() {
    // 标准 Glance 组件
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text("Header")
        
        // 对于复杂区域，可以嵌入 Remote Compose 渲染的内容
        // 通过自定义 Emittable 和 RcElement 实现
        CustomRemoteComposeArea(
            modifier = GlanceModifier.fillMaxWidth().height(200.dp)
        )
    }
}
```

---

## 7. 高级功能

### 7.1 SnapScroll 与分页

[RcLazyColumn.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcLazyColumn.kt) 实现了 SnapScroll 功能：

```kotlin
// Glance API 中使用
LazyColumn(
    verticalScrollMode = VerticalScrollMode.SnapScroll(
        initialChildHeight = 100.dp
    )
) {
    items(data) { item ->
        // 每个 item 会吸附滚动
        ListItem(item)
    }
}
```

底层通过 `CustomScrollModifier` 和触摸变量实现：
```kotlin
val customScrollModifier = CustomScrollModifier(
    direction = CustomScrollModifier.VERTICAL,
    touchPositionVariable = touchPositionVariable,
    scrollPositionExpr = scrollPositionExpr,
    numItems = children.size - 1,
    snapScrolling = true,
)
```

### 7.2 粒子效果

Remote Compose 提供了高性能粒子系统：

```kotlin
val variables: Array<RFloat> = Array(4) { RFloat(this, 0f) }
val psId = createParticles(
    variables,
    arrayOf(cx, cy, rand() - 0.5f, rand() - 0.5f),
    pCount = 100
)
val (px, py, dx, dy) = variables

particlesLoops(psId, restartCondition = null,
    updateEquations = arrayOf(px + dx * dt, py + dy * dt, dx, dy + 9.8f * dt)
) {
    drawCircle(px.toFloat(), py.toFloat(), 5f)
}
```

### 7.3 触摸交互

```kotlin
val sliderPos = rc.addTouch(
    defValue = 0f,
    min = 0f,
    max = 100f,
    touchMode = STOP_GENTLY,
    touchEffects = Rc.Haptic.CLOCK_TICK,
    RemoteContext.FLOAT_TOUCH_POS_X
)

canvas(RecordingModifier().size(200f, 40f)) {
    // 绘制滑块轨道
    painter.setColor(Color.GRAY).commit()
    drawLine(0f, 20f, w, 20f)
    
    // 绘制滑块 thumb
    painter.setColor(Color.RED).commit()
    drawCircle(sliderPos, 20f, 15f)
}
```

### 7.4 动画与表达式

Remote Compose 使用 RPN（逆波兰表示法）表达式驱动动画：

```kotlin
// 使用 Kotlin DSL 的运算符重载
val bounce = pingPong(1, ContinuousSec()).toFloat()
val animColor = addColorExpression(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), bounce)

// 正弦波动画
val tween = (sin(ContinuousSec() % 3600f) + 1f) * 500f
text("Animated", fontWeight = tween.toFloat())
```

---

## 8. 向后兼容性

Remote Compose 功能在 **Android 16（Vanilla Ice Cream）及以上版本** 自动启用，通过 Glance API 可以保持完全向后兼容：

```kotlin
// GlanceRemoteComposeTranslator.kt
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal fun translateCompositionUsingRemoteCompose(...): RemoteViews {
    // 1. 创建 Remote Compose 文档
    val translation = translateEmittableTreeToRemoteCompose(...)
    // 2. 创建 RemoteViews.DrawInstructions
    return DrawInstructionRemoteViews.create(translation)
}
```

对于旧版本系统：
- Glance 会自动回退到传统 XML 模板模式
- 相同的 Glance API 代码在不同版本上表现一致
- 新功能在旧系统上优雅降级

---

## 9. 项目目录结构

### Glance 模块关键文件

```
glance/
├── glance-appwidget/                          # Glance AppWidget 核心模块
│   ├── build.gradle                           # 依赖 remote-core 和 remote-creation
│   └── src/main/java/androidx/glance/appwidget/
│       ├── remotecompose/                     # Remote Compose 翻译层
│       │   ├── GlanceRemoteComposeTranslator.kt   # 主翻译器
│       │   ├── ApplyModifiersToRemoteCompose.kt   # Modifier 转换
│       │   ├── DrawInstructionRemoteViews.kt      # RemoteViews 创建
│       │   ├── TranslationModel.kt                # 翻译上下文
│       │   └── components/                        # RcElement 组件实现
│       │       ├── RcElement.kt                   # 基类
│       │       ├── RcText.kt                      # 文本组件
│       │       ├── RcBox.kt                       # 盒子组件
│       │       ├── RcColumn.kt                    # 列组件
│       │       ├── RcRow.kt                       # 行组件
│       │       ├── RcImage.kt                     # 图片组件
│       │       ├── RcButton.kt                    # 按钮组件
│       │       ├── RcLazyColumn.kt                # 懒加载列（SnapScroll）
│       │       ├── RcMaterial3TextButton.kt       # Material3 按钮
│       │       └── RcSpacer.kt                    # 间隔组件
│       └── ...
├── glance-appwidget-proto/                    # Protobuf 定义
│   └── src/main/proto/layout.proto
└── ...

compose/remote/                                # Remote Compose 模块
├── remote-core/                               # 运行时核心
│   ├── doc/
│   │   ├── REMOTE_COMPOSE_ARCHITECTURE.md     # 架构文档
│   │   ├── PROTOCOL_SPEC.md                   # 协议规范
│   │   ├── DATA_FLOW.md                       # 数据流文档
│   │   └── EXPRESSION_ENGINE.md               # 表达式引擎
│   └── src/main/java/androidx/compose/remote/core/
│       ├── RemoteContext.java                 # 运行时上下文
│       ├── WireBuffer.java                    # 二进制缓冲区
│       ├── Operations.java                    # 操作码注册表
│       └── ...
├── remote-creation/                           # 创建 API
│   ├── doc/guides/
│   │   ├── COMPONENTS_GUIDE.md                # 组件指南
│   │   ├── COMPOSE_COMPONENTS_GUIDE.md        # Compose 风格 API
│   │   ├── PROCEDURAL_COMPONENTS_GUIDE.md     # 过程式 API
│   │   ├── TOUCH_GUIDE.md                     # 触摸交互指南
│   │   └── PARTICLE_SYSTEM_GUIDE.md           # 粒子系统指南
│   └── src/...
├── remote-creation-core/                      # 创建核心
└── remote-creation-compose/                   # Compose 集成层
```

---

## 总结

**Glance 与 Remote Compose 的关系：**

1. **Glance 是面向开发者的 API 层**：提供 Compose 风格的声明式 API，让开发者用熟悉的 Kotlin 代码编写微件
2. **Remote Compose 是底层渲染引擎**：负责将 UI 描述序列化为紧凑二进制格式，在设备端高效渲染
3. **翻译层是桥梁**：[GlanceRemoteComposeTranslator](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt) 将 Glance 的 Emittable 树转换为 Remote Compose 的 Wire Format
4. **"帮我制作微件"功能**利用 LLM 理解用户自然语言描述，生成 Glance 或 Remote Compose 代码，实现零代码创建自定义微件

**对于开发者的建议：**
- 大多数场景使用 **Glance API** 即可，保持代码简洁
- 需要高级效果（粒子、复杂动画、自定义 Canvas 绘图）时，直接使用 **Remote Compose API**
- 通过 **Glance + Remote Compose 混合模式**，在保持向后兼容的同时利用新功能
