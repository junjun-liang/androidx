# "帮我制作微件" 入门手册

> 基于 Glance + RemoteCompose 构建 Android 自适应微件的完整指南

---

## 目录

1. [技术概览](#1-技术概览)
2. [架构设计](#2-架构设计)
3. [环境准备](#3-环境准备)
4. [方式一：Glance API（推荐，自动向后兼容）](#4-方式一glance-api推荐自动向后兼容)
5. [方式二：RemoteCompose API（API 35+，更丰富交互）](#5-方式二remotecompose-apiapi-35更丰富交互)
6. [方式三：RemoteComposeWidget 抽象基类](#6-方式三remotecomposewidget-抽象基类)
7. [组件参考](#7-组件参考)
8. [Modifier 参考](#8-modifier-参考)
9. [动画与粒子效果](#9-动画与粒子效果)
10. [触摸交互](#10-触摸交互)
11. [状态管理](#11-状态管理)
12. [测试](#12-测试)
13. [完整 Demo 代码](#13-完整-demo-代码)
14. [常见问题](#14-常见问题)

---

## 1. 技术概览

### 1.1 什么是 Glance + RemoteCompose

**Glance** 是 AndroidX 提供的声明式 App Widget 框架，使用 Compose 风格的 API 构建微件。

**RemoteCompose** 是 Glance 背后的新底层框架，提供：
- **深度自适应性**：微件可根据尺寸自动调整布局
- **省电性**：动画和交互逻辑在 Player 端（系统进程）运行，无需持续唤醒应用
- **丰富交互**：支持 snap scroll、富有表现力的按钮、粒子效果等
- **内置向后兼容**：新功能在 Android 16+ 开箱即用，旧版本优雅降级

### 1.2 核心优势

| 特性 | 传统 RemoteViews | Glance | Glance + RemoteCompose |
|------|-----------------|--------|----------------------|
| API 风格 | XML + Java | Compose-like | Compose-like |
| 布局能力 | 有限 | 中等 | 丰富（自适应/折叠/流式） |
| 动画 | 不支持 | 不支持 | ✅ 支持 |
| 粒子效果 | 不支持 | 不支持 | ✅ 支持 |
| 触摸交互 | 基础点击 | 基础点击 | 高级（滚动/拖拽/惯性） |
| 向后兼容 | N/A | ✅ | ✅ 自动降级 |
| 省电 | ✅ | ✅ | ✅ Player 端运行 |

### 1.3 "帮我制作微件" 功能

RemoteCompose 是"帮我制作微件"功能背后的引擎。用户可以询问 Gemini 构建完全自适应的自定义微件，这些微件可以：
- 无缝调整大小
- 针对用户主屏幕或 Wear OS 手表优化
- 自动适配不同尺寸和方向

---

## 2. 架构设计

### 2.1 整体架构

```
┌──────────────────────────────────────────────────────────────────┐
│                     Glance + RemoteCompose 架构                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  开发者 API 层                                             │  │
│  │  ┌──────────────────┐  ┌───────────────────────────────┐  │  │
│  │  │  Glance API      │  │  RemoteCompose API             │  │  │
│  │  │  (GlanceAppWidget)│  │  (@RemoteComposable)          │  │  │
│  │  └────────┬─────────┘  └───────────┬───────────────────┘  │  │
│  └───────────┼─────────────────────────┼──────────────────────┘  │
│              │                         │                         │
│  ┌───────────┼─────────────────────────┼──────────────────────┐  │
│  │  翻译层   │                         │                      │  │
│  │  ┌────────▼─────────┐  ┌───────────▼───────────────────┐  │  │
│  │  │  Glance →         │  │  captureSingleRemoteDocument  │  │  │
│  │  │  Emittable 树     │  │  → RemoteComposeNode 树       │  │  │
│  │  └────────┬─────────┘  └───────────┬───────────────────┘  │  │
│  │           │                         │                      │  │
│  │  ┌────────▼─────────────────────────▼──────────────────┐  │  │
│  │  │  RemoteCompose 文档 (二进制字节码)                    │  │  │
│  │  │  WireBuffer → 操作码序列 → RPN 表达式                │  │  │
│  │  └────────┬────────────────────────────────────────────┘  │  │
│  └───────────┼───────────────────────────────────────────────┘  │
│              │                                                   │
│  ┌───────────▼───────────────────────────────────────────────┐  │
│  │  传输层                                                    │  │
│  │  RemoteViews.DrawInstructions → IPC → 系统进程            │  │
│  └───────────┬───────────────────────────────────────────────┘  │
│              │                                                   │
│  ┌───────────▼───────────────────────────────────────────────┐  │
│  │  Player 层 (系统进程)                                      │  │
│  │  CoreDocument 解码 → 布局计算 → 绘制 → 动画 → 交互       │  │
│  │  (无需唤醒应用进程)                                        │  │
│  └───────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 三种开发方式对比

| 方式 | 入口类 | API 风格 | 最低 API | 向后兼容 | 交互能力 |
|------|--------|---------|---------|---------|---------|
| **Glance API** | `GlanceAppWidget` | Glance Composable | API 21 | ✅ 自动降级 | 基础（API<35）/ 丰富（API35+） |
| **RemoteCompose API** | `AppWidgetProvider` | `@RemoteComposable` | API 35 | ❌ | 最丰富 |
| **RemoteComposeWidget** | `RemoteComposeWidget` | `@Composable` | API 35 | ❌ | 最丰富 |

---

## 3. 环境准备

### 3.1 依赖配置

```kotlin
// build.gradle.kts (模块级)
android {
    compileSdk = 36  // RemoteCompose 需要 API 35+
    defaultConfig {
        minSdk = 21   // Glance 支持到 API 21
        targetSdk = 36
    }
}

dependencies {
    // Glance 核心（必须）
    implementation("androidx.glance:glance:1.1.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")

    // Glance Material 主题（可选）
    implementation("androidx.glance:glance-material3:1.1.0")

    // RemoteCompose（如需直接使用，API 35+）
    implementation("androidx.compose.remote:remote-core:1.0.0")
    implementation("androidx.compose.remote:remote-creation:1.0.0")
    implementation("androidx.compose.remote:remote-creation-core:1.0.0")
    implementation("androidx.compose.remote:remote-creation-compose:1.0.0")

    // DataStore（状态持久化）
    implementation("androidx.datastore:datastore-preferences:1.1.0")
}
```

### 3.2 AndroidManifest.xml 配置

```xml
<application>
    <!-- Widget Receiver -->
    <receiver
        android:name=".widget.MyWidgetReceiver"
        android:enabled="@bool/glance_appwidget_available"
        android:exported="false"
        android:label="My Widget">
        <intent-filter>
            <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        </intent-filter>
        <meta-data
            android:name="android.appwidget.provider"
            android:resource="@xml/my_widget_info" />
    </receiver>
</application>
```

### 3.3 Widget 信息配置 (res/xml/my_widget_info.xml)

```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="180dp"
    android:minResizeWidth="180dp"
    android:minResizeHeight="110dp"
    android:maxResizeWidth="600dp"
    android:maxResizeHeight="450dp"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="3600000"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:widgetCategory="home_screen"
    android:targetCellWidth="3"
    android:targetCellHeight="2" />
```

---

## 4. 方式一：Glance API（推荐，自动向后兼容）

### 4.1 最简 Widget

```kotlin
class HelloWorldWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Text(text = "Hello, World!")
        }
    }
}

class HelloWorldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HelloWorldWidget()
}
```

### 4.2 带状态的 Widget

```kotlin
class CounterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataStore = context.counterDataStore
        val initial = dataStore.data.first()

        provideContent {
            val prefs by dataStore.data.collectAsState(initial)
            val count = prefs[COUNTER_KEY] ?: 0

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.White).padding(16.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Text(text = "Count: $count", style = TextStyle(fontSize = 24.sp))
                Spacer(modifier = GlanceModifier.height(8.dp))
                Button(
                    text = "Increment",
                    onClick = actionRunCallback<IncrementAction>(),
                )
            }
        }
    }

    companion object {
        val COUNTER_KEY = intPreferencesKey("counter")
    }
}

class IncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val dataStore = context.counterDataStore
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                val current = this[CounterWidget.COUNTER_KEY] ?: 0
                this[CounterWidget.COUNTER_KEY] = current + 1
            }
        }
    }
}
```

### 4.3 响应式布局 Widget

```kotlin
class ResponsiveWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 180.dp), DpSize(350.dp, 250.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            when {
                size.width < 200.dp -> CompactLayout()
                size.width < 300.dp -> MediumLayout()
                else -> ExpandedLayout()
            }
        }
    }
}

@Composable
fun CompactLayout() {
    Box(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
        Text("Compact", modifier = GlanceModifier.fillMaxSize())
    }
}

@Composable
fun MediumLayout() {
    Row(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Text("Medium", modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
fun ExpandedLayout() {
    Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
        Text("Expanded", modifier = GlanceModifier.fillMaxWidth())
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text("Left", modifier = GlanceModifier.defaultWeight())
            Text("Right", modifier = GlanceModifier.defaultWeight())
        }
    }
}
```

### 4.4 可滚动列表 Widget（自动使用 RemoteCompose）

当使用 `VerticalScrollMode.Normal` 时，Glance 在 API 35+ 上自动使用 RemoteCompose 后端：

```kotlin
class ScrollableWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize().background(Color.White),
                verticalScrollMode = VerticalScrollMode.Normal,
            ) {
                items(20) { index ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Text("Item $index", modifier = GlanceModifier.defaultWeight())
                        Button("Action", onClick = actionRunCallback<ItemAction>())
                    }
                }
            }
        }
    }
}
```

### 4.5 M3 风格 Widget

```kotlin
class M3StyleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val colors = GlanceTheme.colors
            Scaffold(
                titleBar = TitleBar("My App"),
                backgroundColor = colors.surface,
            ) {
                Column(modifier = GlanceModifier.padding(16.dp)) {
                    Text(
                        "Hello M3",
                        style = TextStyle(color = colors.onSurface, fontSize = 20.sp),
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    FilledButton("Primary Action", onClick = actionRunCallback<PrimaryAction>())
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    OutlineButton("Secondary", onClick = actionRunCallback<SecondaryAction>())
                }
            }
        }
    }
}
```

---

## 5. 方式二：RemoteCompose API（API 35+，更丰富交互）

### 5.1 基础 RemoteCompose Widget

```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class MyRCWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, wm: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { widgetId ->
            goAsync {
                val bytes = recordWidget(context)
                val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
                val remoteViews = RemoteViews(drawInstructions)
                wm.updateAppWidget(widgetId, remoteViews)
            }
        }
    }

    private suspend fun recordWidget(context: Context): ByteArray {
        return captureSingleRemoteDocument(
            context = context.applicationContext,
            profile = RcPlatformProfiles.WIDGETS_V7,
        ) {
            MyWidgetContent()
        }.bytes
    }
}

@Composable
@RemoteComposable
fun MyWidgetContent() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize().background(RemoteColor(Color.White))) {
        RemoteText("Hello RemoteCompose!", color = Color.Black.rc)
        RemoteBox(
            modifier = RemoteModifier
                .size(width = 200.rdp, height = 100.rdp)
                .background(RemoteColor(Color.Blue))
                .padding(RemoteDp(16.dp)),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText("Tap me!", color = Color.White.rc)
        }
    }
}
```

### 5.2 带点击交互的 RemoteCompose Widget

```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class InteractiveRCWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, wm: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { widgetId ->
            goAsync {
                val bytes = recordWidget(context)
                val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
                val remoteViews = RemoteViews(drawInstructions)
                wm.updateAppWidget(widgetId, remoteViews)
            }
        }
    }

    private suspend fun recordWidget(context: Context): ByteArray {
        return captureSingleRemoteDocument(
            context = context.applicationContext,
            profile = RcPlatformProfiles.WIDGETS_V7,
        ) {
            InteractiveContent()
        }.bytes
    }
}

@Composable
@RemoteComposable
fun InteractiveContent() {
    val clickCount = rememberMutableRemoteInt(0)
    val onClickAction = ValueChange(clickCount, clickCount + 1)

    RemoteColumn(modifier = RemoteModifier.fillMaxSize().padding(RemoteDp(16.dp))) {
        RemoteText(
            text = "Clicks: ".rs + clickCount.toRemoteString(),
            color = Color.Black.rc,
        )
        RemoteBox(
            modifier = RemoteModifier
                .size(width = 200.rdp, height = 80.rdp)
                .background(RemoteColor(Color.LightGray))
                .clickable(onClickAction)
                .padding(RemoteDp(12.dp)),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText("Tap me!", color = Color.Black.rc)
        }
    }
}
```

---

## 6. 方式三：RemoteComposeWidget 抽象基类

### 6.1 基础用法

```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class MyRemoteWidget : RemoteComposeWidget(useCompose = true) {

    @Composable
    override fun Content(context: Context, widgetId: Int) {
        RemoteColumn(modifier = RemoteModifier.fillMaxSize().background(RemoteColor(Color.White))) {
            RemoteText("Widget ID: $widgetId", color = Color.Black.rc)
            RemoteBox(
                modifier = RemoteModifier
                    .fillMaxWidth()
                    .height(60.rdp)
                    .background(RemoteColor(Color.Blue))
                    .onClick { /* 处理点击 */ }
                    .padding(RemoteDp(16.dp)),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Click me!", color = Color.White.rc)
            }
        }
    }
}
```

### 6.2 过程式模式

```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class ProceduralWidget : RemoteComposeWidget(useCompose = false) {

    override fun ProceduralContent(context: Context, widgetId: Int): RemoteComposeContext? {
        return RemoteComposeContextAndroid(
            creationDisplayInfo = createCreationDisplayInfo(context),
            contentDescription = "Procedural Widget",
            profile = RcPlatformProfiles.WIDGETS_V7,
        ) {
            column(RemoteModifier.fillMaxSize().background(Color.White)) {
                text("Widget ID: $widgetId", textStyle = TextStyle(color = Color.Black))
                box(
                    modifier = RemoteModifier.fillMaxWidth().height(60f)
                        .background(Color.Blue).padding(16f),
                    horizontalAlign = BoxLayout.CENTER,
                    verticalAlign = BoxLayout.CENTER,
                ) {
                    text("Click me!", textStyle = TextStyle(color = Color.White))
                }
            }
        }
    }
}
```

---

## 7. 组件参考

### 7.1 Glance 组件

| 组件 | 说明 | 示例 |
|------|------|------|
| `Text` | 文本 | `Text("Hello", style = TextStyle(fontSize = 16.sp))` |
| `Button` | 按钮 | `Button("Click", onClick = actionRunCallback<MyAction>())` |
| `Image` | 图片 | `Image(provider = ImageProvider(R.drawable.icon), contentDescription = "icon")` |
| `Box` | 叠加布局 | `Box(modifier = GlanceModifier.fillMaxSize()) { ... }` |
| `Row` | 水平布局 | `Row { Text("A"); Text("B") }` |
| `Column` | 垂直布局 | `Column { Text("A"); Text("B") }` |
| `Spacer` | 间隔 | `Spacer(modifier = GlanceModifier.height(8.dp))` |
| `LazyColumn` | 滚动列表 | `LazyColumn(verticalScrollMode = VerticalScrollMode.Normal) { items(10) { ... } }` |
| `CheckBox` | 复选框 | `CheckBox(checked = true, onCheckedChange = actionRunCallback<...>())` |
| `Switch` | 开关 | `Switch(checked = false, onCheckedChange = actionRunCallback<...>())` |
| `RadioButton` | 单选按钮 | `RadioButton(selected = true, onClick = actionRunCallback<...>())` |
| `LinearProgressIndicator` | 线性进度条 | `LinearProgressIndicator(progress = 0.5f)` |
| `FilledButton` | M3 填充按钮 | `FilledButton("Action", onClick = ...)` |
| `OutlineButton` | M3 描边按钮 | `OutlineButton("Action", onClick = ...)` |
| `Scaffold` | 脚手架 | `Scaffold(titleBar = TitleBar("App")) { ... }` |

### 7.2 RemoteCompose 组件

| 组件 | 说明 | 示例 |
|------|------|------|
| `RemoteText` | 文本 | `RemoteText("Hello", color = Color.Black.rc)` |
| `RemoteBox` | 叠加布局 | `RemoteBox(modifier = RemoteModifier.fillMaxSize()) { ... }` |
| `RemoteColumn` | 垂直布局 | `RemoteColumn { RemoteText("A"); RemoteText("B") }` |
| `RemoteRow` | 水平布局 | `RemoteRow { RemoteText("A"); RemoteText("B") }` |
| `RemoteImage` | 图片 | `RemoteImage(bitmap, contentDescription = "img")` |
| `RemoteCanvas` | 绘图画布 | `RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) { ... }` |
| `RemoteSpacer` | 间隔 | `RemoteSpacer(modifier = RemoteModifier.height(8.rdp))` |
| `FitBox` | 适配 Box | `FitBox { RemoteImage(bitmap) }` |
| `RemoteCollapsibleColumn` | 可折叠列 | `RemoteCollapsibleColumn(priority = 1) { ... }` |
| `RemoteCollapsibleRow` | 可折叠行 | `RemoteCollapsibleRow(priority = 1) { ... }` |
| `StateLayout` | 状态切换 | `StateLayout(state = currentState, states = intArrayOf(0, 1, 2)) { ... }` |

---

## 8. Modifier 参考

### 8.1 GlanceModifier

| Modifier | 说明 |
|----------|------|
| `.fillMaxSize()` | 填满父容器 |
| `.fillMaxWidth()` | 填满宽度 |
| `.fillMaxHeight()` | 填满高度 |
| `.width(100.dp)` | 固定宽度 |
| `.height(50.dp)` | 固定高度 |
| `.padding(16.dp)` | 内边距 |
| `.background(Color.White)` | 背景色 |
| `.clickable("key") { ... }` | 点击事件 |
| `.cornerRadius(12.dp)` | 圆角（API 31+） |
| `.visibility(Visibility.Visible)` | 可见性 |
| `.defaultWeight()` | 权重（Row/Column） |

### 8.2 RemoteModifier

| Modifier | 说明 |
|----------|------|
| `.fillMaxSize()` | 填满父容器 |
| `.size(100.rdp)` | 固定尺寸 |
| `.padding(RemoteDp(16.dp))` | 内边距 |
| `.background(RemoteColor(Color.Red))` | 背景色 |
| `.clip(RoundedCornerShape(12.dp))` | 裁剪形状 |
| `.clickable(action)` | 点击事件 |
| `.onClick { ... }` | 点击回调（Widget 专用） |
| `.visibility(intState)` | 条件可见性 |
| `.scale(2f.rf)` | 缩放 |
| `.alpha(0.5f.rf)` | 透明度 |
| `.rotate(45f.rf)` | 旋转 |
| `.offset(x = 10.rdp)` | 偏移 |
| `.graphicsLayer { ... }` | 图形变换 |
| `.verticalScroll(scrollState)` | 垂直滚动 |
| `.horizontalScroll(scrollState)` | 水平滚动 |
| `.weight(1f)` | 权重 |
| `.border(2.rdp, RemoteColor(Color.Black))` | 边框 |
| `.marquee()` | 跑马灯效果 |

---

## 9. 动画与粒子效果

### 9.1 内置动画变量

RemoteCompose Player 端提供以下时间变量，无需应用进程参与：

| 变量 | 说明 |
|------|------|
| `ANIMATION_TIME` | 动画时间（秒） |
| `CONTINUOUS_SEC` | 从午夜开始的秒数 |
| `TIME_IN_SEC/MIN/HR` | 量化时间 |
| `CALENDAR_MONTH` | 月份 1-12 |
| `WEEK_DAY` | 星期 1-7 |
| `DAY_OF_MONTH` | 日期 1-31 |
| `ANIMATION_DELTA_TIME` | 帧间时间差 |

### 9.2 动画示例

```kotlin
@Composable
@RemoteComposable
fun AnimatedPulseContent() {
    val animTime = rememberAnimatedFloat()

    RemoteBox(
        modifier = RemoteModifier
            .fillMaxSize()
            .background(RemoteColor(Color.White)),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteBox(
            modifier = RemoteModifier
                .size(100.rdp)
                .scale(1.0f.rf + 0.2f.rf * sin(animTime * 2.0f.rf))
                .alpha(0.8f.rf + 0.2f.rf * cos(animTime * 3.0f.rf))
                .background(RemoteColor(Color.Blue)),
        )
    }
}
```

### 9.3 粒子效果

```kotlin
@Composable
@RemoteComposable
fun ParticleRainContent() {
    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
        val cx = rememberMutableRemoteFloat(0f)
        val cy = rememberMutableRemoteFloat(0f)
        val dx = rememberMutableRemoteFloat(0f)
        val dy = rememberMutableRemoteFloat(0f)

        val variables = arrayOf(cx, cy, dx, dy)
        val psId = createParticles(
            variables = variables,
            initial = arrayOf(
                rand() * width,
                0f.rf,
                (rand() - 0.5f.rf) * 2f.rf,
                rand() * 3f.rf + 1f.rf,
            ),
            particleCount = 100,
        )

        particlesLoop(
            id = psId,
            restartCondition = cy gt height,
            updateEquations = arrayOf(
                cx + dx * dt,
                cy + dy * dt,
                dx,
                dy + 9.8f.rf * dt,
            ),
        ) {
            drawCircle(px = cx.toFloat(), py = cy.toFloat(), radius = 3f, paint = bluePaint)
        }
    }
}
```

---

## 10. 触摸交互

### 10.1 滚动

```kotlin
@Composable
@RemoteComposable
fun ScrollableContent() {
    val scrollState = rememberScrollState()

    RemoteColumn(modifier = RemoteModifier.fillMaxSize().verticalScroll(scrollState)) {
        repeat(50) { index ->
            RemoteRow(modifier = RemoteModifier.fillMaxWidth().height(48.rdp).padding(RemoteDp(8.dp))) {
                RemoteText("Item $index", color = Color.Black.rc)
            }
        }
    }
}
```

### 10.2 触摸拖拽

```kotlin
@Composable
@RemoteComposable
fun TouchDragContent() {
    val posX = rememberMutableRemoteFloat(150f)
    val posY = rememberMutableRemoteFloat(150f)

    addTouch(
        variableX = posX,
        variableY = posY,
        startX = 0f.rf,
        startY = 0f.rf,
        stopMode = STOP_ABSOLUTE_POS,
    )

    RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
        RemoteBox(
            modifier = RemoteModifier
                .size(60.rdp)
                .offset(x = posX.rdp, y = posY.rdp)
                .background(RemoteColor(Color.Red))
                .clip(RoundedCornerShape(30.dp)),
        )
    }
}
```

---

## 11. 状态管理

### 11.1 Glance 状态管理（DataStore）

```kotlin
val Context.widgetDataStore by preferencesDataStore("widget_prefs")

class StatefulWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = context.widgetDataStore
        val initial = store.data.first()

        provideContent {
            val prefs by store.data.collectAsState(initial)
            val title = prefs[TITLE_KEY] ?: "Default"
            Column {
                Text(title)
                Button("Update", onClick = actionRunCallback<UpdateAction>())
            }
        }
    }

    companion object {
        val TITLE_KEY = stringPreferencesKey("title")
    }
}
```

### 11.2 RemoteCompose 状态管理

```kotlin
@Composable
@RemoteComposable
fun StatefulRCContent() {
    val counter = rememberMutableRemoteInt(0)
    val isVisible = rememberMutableRemoteInt(1)
    val scale = rememberMutableRemoteFloat(1f)

    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteText(text = "Count: ".rs + counter.toRemoteString())
        RemoteBox(
            modifier = RemoteModifier
                .size(100.rdp)
                .scale(scale.rf)
                .clickable(ValueChange(counter, counter + 1))
                .background(RemoteColor(Color.Blue)),
        )
    }
}
```

---

## 12. 测试

### 12.1 Glance 单元测试

```kotlin
@Test
fun testWidgetContent() = runGlanceAppWidgetUnitTest {
    setAppWidgetSize(DpSize(300.dp, 200.dp))
    provideComposable {
        MyWidgetContent()
    }
    onNode(hasText("Hello")).assertIsDisplayed()
    onNode(hasClickAction<ActionCallback>()).assertHasRunCallbackClickAction<MyAction>()
}
```

### 12.2 RemoteCompose 截图测试

```kotlin
@Test
fun testRemoteComposeContent() {
    val document = runBlocking {
        captureSingleRemoteDocument(
            context = context,
            profile = RcPlatformProfiles.WIDGETS_V7,
        ) {
            MyWidgetContent()
        }
    }
    assertNotNull(document.bytes)
}
```

---

## 13. 完整 Demo 代码

以下是一个完整的可运行 Demo 项目，展示 Glance + RemoteCompose 的三种开发方式。

### 13.1 项目结构

```
app/
├── src/main/
│   ├── java/com/example/widgetdemo/
│   │   ├── widget/
│   │   │   ├── glance/                    # 方式一：Glance API
│   │   │   │   ├── WeatherWidget.kt
│   │   │   │   └── WeatherWidgetReceiver.kt
│   │   │   ├── remote/                    # 方式二：RemoteCompose API
│   │   │   │   ├── AnimatedWidget.kt
│   │   │   │   └── AnimatedWidgetReceiver.kt
│   │   │   └── base/                      # 方式三：RemoteComposeWidget
│   │   │       └── ParticleWidget.kt
│   │   └── MainActivity.kt
│   ├── res/
│   │   ├── xml/
│   │   │   ├── weather_widget_info.xml
│   │   │   ├── animated_widget_info.xml
│   │   │   └── particle_widget_info.xml
│   │   ├── layout/
│   │   │   └── activity_main.xml
│   │   └── values/
│   │       └── strings.xml
│   └── AndroidManifest.xml
├── build.gradle.kts
└── settings.gradle.kts
```

### 13.2 build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.widgetdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.widgetdemo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Glance
    implementation("androidx.glance:glance:1.1.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // RemoteCompose
    implementation("androidx.compose.remote:remote-core:1.0.0-alpha01")
    implementation("androidx.compose.remote:remote-creation:1.0.0-alpha01")
    implementation("androidx.compose.remote:remote-creation-core:1.0.0-alpha01")
    implementation("androidx.compose.remote:remote-creation-compose:1.0.0-alpha01")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // Core
    implementation("androidx.core:core-ktx:1.13.0")
}
```

### 13.3 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="Widget Demo"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- 方式一：Glance Widget -->
        <receiver
            android:name=".widget.glance.WeatherWidgetReceiver"
            android:enabled="@bool/glance_appwidget_available"
            android:exported="false"
            android:label="Weather Widget">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/weather_widget_info" />
        </receiver>

        <!-- 方式二：RemoteCompose Widget -->
        <receiver
            android:name=".widget.remote.AnimatedWidgetReceiver"
            android:enabled="@bool/glance_appwidget_available"
            android:exported="false"
            android:label="Animated Widget">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/animated_widget_info" />
        </receiver>

        <!-- 方式三：Particle Widget -->
        <receiver
            android:name=".widget.base.ParticleWidget"
            android:enabled="@bool/glance_appwidget_available"
            android:exported="false"
            android:label="Particle Widget">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/particle_widget_info" />
        </receiver>

    </application>
</manifest>
```

### 13.4 Widget 信息 XML

**res/xml/weather_widget_info.xml**：
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="180dp"
    android:minResizeWidth="180dp"
    android:minResizeHeight="110dp"
    android:maxResizeWidth="600dp"
    android:maxResizeHeight="450dp"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:widgetCategory="home_screen"
    android:targetCellWidth="3"
    android:targetCellHeight="2"
    android:description="@string/weather_widget_desc" />
```

**res/xml/animated_widget_info.xml**：
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="250dp"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="86400000"
    android:widgetCategory="home_screen" />
```

**res/xml/particle_widget_info.xml**：
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="250dp"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="86400000"
    android:widgetCategory="home_screen" />
```

### 13.5 方式一：Glance 天气 Widget

**widget/glance/WeatherWidget.kt**：

```kotlin
package com.example.widgetdemo.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionCallback
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.weatherDataStore by preferencesDataStore("weather_prefs")

class WeatherWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 180.dp),
            DpSize(350.dp, 280.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = context.weatherDataStore
        val initial = store.data.first()

        provideContent {
            val prefs by androidx.compose.runtime.collectAsState(initial, store.data)
            val temp = prefs[TEMP_KEY] ?: 22
            val city = prefs[CITY_KEY] ?: "Beijing"
            val condition = prefs[CONDITION_KEY] ?: "Sunny"

            val size = LocalSize.current

            Scaffold(
                titleBar = TitleBar("Weather"),
                backgroundColor = GlanceTheme.colors.surface,
            ) {
                when {
                    size.width < 200.dp -> CompactWeather(city, temp, condition)
                    size.width < 300.dp -> MediumWeather(city, temp, condition)
                    else -> ExpandedWeather(city, temp, condition)
                }
            }
        }
    }

    companion object {
        val TEMP_KEY = intPreferencesKey("temperature")
        val CITY_KEY = stringPreferencesKey("city")
        val CONDITION_KEY = stringPreferencesKey("condition")
    }
}

@Composable
private fun CompactWeather(city: String, temp: Int, condition: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Text(city, style = TextStyle(fontSize = 16.sp))
            Text("$temp°C", style = TextStyle(fontSize = 28.sp))
            Text(condition, style = TextStyle(fontSize = 12.sp))
        }
    }
}

@Composable
private fun MediumWeather(city: String, temp: Int, condition: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(city, style = TextStyle(fontSize = 18.sp))
                Text(condition, style = TextStyle(fontSize = 14.sp))
            }
            Text("$temp°C", style = TextStyle(fontSize = 36.sp))
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text("H: ${temp + 3}°", style = TextStyle(fontSize = 14.sp))
            Spacer(modifier = GlanceModifier.width(16.dp))
            Text("L: ${temp - 5}°", style = TextStyle(fontSize = 14.sp))
        }
    }
}

@Composable
private fun ExpandedWeather(city: String, temp: Int, condition: String) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(city, style = TextStyle(fontSize = 20.sp))
                Text(condition, style = TextStyle(fontSize = 16.sp))
            }
            Text("$temp°C", style = TextStyle(fontSize = 48.sp))
        }
        Spacer(modifier = GlanceModifier.height(16.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text("H: ${temp + 3}°", style = TextStyle(fontSize = 14.sp))
            Spacer(modifier = GlanceModifier.width(16.dp))
            Text("L: ${temp - 5}°", style = TextStyle(fontSize = 14.sp))
            Spacer(modifier = GlanceModifier.width(16.dp))
            Text("Humidity: 65%", style = TextStyle(fontSize = 14.sp))
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        LazyColumn(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalScrollMode = VerticalScrollMode.Normal,
        ) {
            items(5) { hour ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(40.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text("${12 + hour}:00", modifier = GlanceModifier.defaultWeight())
                    Text("${temp + hour - 2}°")
                }
            }
        }
    }
}

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        context.weatherDataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                val current = this[WeatherWidget.TEMP_KEY] ?: 22
                this[WeatherWidget.TEMP_KEY] = current + (0..5).random() - 2
            }
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}
```

### 13.6 方式二：RemoteCompose 动画时钟 Widget

**widget/remote/AnimatedWidget.kt**：

```kotlin
package com.example.widgetdemo.widget.remote

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.remote.creation.compose.RemoteComposable
import androidx.compose.remote.creation.compose.RemoteModifier
import androidx.compose.remote.creation.compose.RemoteColor
import androidx.compose.remote.creation.compose.RemoteAlignment
import androidx.compose.remote.creation.compose.RemoteColumn
import androidx.compose.remote.creation.compose.RemoteBox
import androidx.compose.remote.creation.compose.RemoteText
import androidx.compose.remote.creation.compose.RemoteCanvas
import androidx.compose.remote.creation.compose.RemoteRow
import androidx.compose.remote.creation.compose.RemoteDp
import androidx.compose.remote.creation.compose.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.widget.RemoteViews

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class AnimatedWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, wm: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { widgetId ->
            goAsync {
                val bytes = recordAnimatedWidget(context)
                val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
                val remoteViews = RemoteViews(drawInstructions)
                wm.updateAppWidget(widgetId, remoteViews)
            }
        }
    }

    private suspend fun recordAnimatedWidget(context: Context): ByteArray {
        return captureSingleRemoteDocument(
            context = context.applicationContext,
            profile = RcPlatformProfiles.WIDGETS_V7,
        ) {
            AnimatedClockContent()
        }.bytes
    }
}

@Composable
@RemoteComposable
fun AnimatedClockContent() {
    val animTime = rememberAnimatedFloat()

    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(RemoteColor(Color(0xFF1A1A2E))),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteColumn(
            modifier = RemoteModifier.padding(RemoteDp(24.dp)),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            RemoteText(
                text = "Animated Clock",
                color = Color(0xFFE0E0E0).rc,
                fontSize = 18f.rf,
            )
            RemoteSpacer(modifier = RemoteModifier.height(16.rdp))
            RemoteCanvas(modifier = RemoteModifier.size(200.rdp)) {
                val cx = width / 2f.rf
                val cy = height / 2f.rf
                val radius = 90f.rf

                drawCircle(
                    cx = cx, cy = cy, radius = radius,
                    paint = createPaint().apply { color = Color(0xFF16213E) }
                )
                drawCircle(
                    cx = cx, cy = cy, radius = radius - 4f.rf,
                    paint = createPaint().apply { color = Color(0xFF0F3460) }
                )

                val sec = animTime % 60f.rf
                val min = (animTime / 60f.rf) % 60f.rf
                val hr = (animTime / 3600f.rf) % 12f.rf

                val secAngle = sec * 6f.rf
                val minAngle = min * 6f.rf
                val hrAngle = hr * 30f.rf

                drawLine(
                    x0 = cx, y0 = cy,
                    x1 = cx + 60f.rf * cos((hrAngle - 90f.rf) * 3.14159f.rf / 180f.rf),
                    y1 = cy + 60f.rf * sin((hrAngle - 90f.rf) * 3.14159f.rf / 180f.rf),
                    paint = createPaint().apply { color = Color.White; strokeWidth = 4f }
                )
                drawLine(
                    x0 = cx, y0 = cy,
                    x1 = cx + 75f.rf * cos((minAngle - 90f.rf) * 3.14159f.rf / 180f.rf),
                    y1 = cy + 75f.rf * sin((minAngle - 90f.rf) * 3.14159f.rf / 180f.rf),
                    paint = createPaint().apply { color = Color(0xFFE0E0E0); strokeWidth = 2f }
                )
                drawLine(
                    x0 = cx, y0 = cy,
                    x1 = cx + 80f.rf * cos((secAngle - 90f.rf) * 3.14159f.rf / 180f.rf),
                    y1 = cy + 80f.rf * sin((secAngle - 90f.rf) * 3.14159f.rf / 180f.rf),
                    paint = createPaint().apply { color = Color(0xFFE94560); strokeWidth = 1f }
                )

                drawCircle(
                    cx = cx, cy = cy, radius = 4f.rf,
                    paint = createPaint().apply { color = Color(0xFFE94560) }
                )
            }
            RemoteSpacer(modifier = RemoteModifier.height(12.rdp))
            RemoteRow {
                RemoteText(
                    text = "Powered by RemoteCompose",
                    color = Color(0xFF888888).rc,
                    fontSize = 10f.rf,
                )
            }
        }
    }
}
```

### 13.7 方式三：粒子效果 Widget

**widget/base/ParticleWidget.kt**：

```kotlin
package com.example.widgetdemo.widget.base

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.remote.creation.compose.RemoteComposable
import androidx.compose.remote.creation.compose.RemoteModifier
import androidx.compose.remote.creation.compose.RemoteColor
import androidx.compose.remote.creation.compose.RemoteAlignment
import androidx.compose.remote.creation.compose.RemoteBox
import androidx.compose.remote.creation.compose.RemoteText
import androidx.compose.remote.creation.compose.RemoteCanvas
import androidx.compose.remote.creation.compose.RemoteDp
import androidx.compose.remote.creation.compose.RemoteColumn
import androidx.compose.remote.creation.compose.RemoteSpacer
import androidx.compose.remote.creation.compose.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.widget.RemoteViews

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class ParticleWidget : RemoteComposeWidget(useCompose = true) {

    @Composable
    override fun Content(context: Context, widgetId: Int) {
        ParticleWidgetContent()
    }
}

@Composable
@RemoteComposable
fun ParticleWidgetContent() {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(RemoteColor(Color(0xFF0D1117))),
    ) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val px = rememberMutableRemoteFloat(0f)
            val py = rememberMutableRemoteFloat(0f)
            val dx = rememberMutableRemoteFloat(0f)
            val dy = rememberMutableRemoteFloat(0f)
            val life = rememberMutableRemoteFloat(0f)

            val variables = arrayOf(px, py, dx, dy, life)
            val psId = createParticles(
                variables = variables,
                initial = arrayOf(
                    width / 2f.rf,
                    height.rf,
                    (rand() - 0.5f.rf) * 4f.rf,
                    -(rand() * 5f.rf + 3f.rf),
                    rand() * 3f.rf + 1f.rf,
                ),
                particleCount = 80,
            )

            val sparkPaint = createPaint().apply {
                color = Color(0xFF58A6FF)
            }

            particlesLoop(
                id = psId,
                restartCondition = life lt 0f.rf,
                updateEquations = arrayOf(
                    px + dx * dt,
                    py + dy * dt,
                    dx,
                    dy + 2f.rf * dt,
                    life - dt,
                ),
            ) {
                val alpha = life / 3f.rf
                sparkPaint.setAlpha(alpha.toFloat())
                drawCircle(
                    cx = px.toFloat(), cy = py.toFloat(),
                    radius = 3f,
                    paint = sparkPaint,
                )
            }
        }

        RemoteColumn(
            modifier = RemoteModifier.padding(RemoteDp(16.dp)),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            RemoteSpacer(modifier = RemoteModifier.height(32.rdp))
            RemoteText(
                text = "Particle Widget",
                color = Color.White.rc,
                fontSize = 20f.rf,
            )
            RemoteText(
                text = "Powered by RemoteCompose",
                color = Color(0xFF888888).rc,
                fontSize = 12f.rf,
            )
        }
    }
}
```

### 13.8 MainActivity

**MainActivity.kt**：

```kotlin
package com.example.widgetdemo

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Widget Demo", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add widgets to your home screen",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                            context.startActivity(intent)
                        }) {
                            Text("Add Widget")
                        }
                    }
                }
            }
        }
    }
}
```

---

## 14. 常见问题

### Q1: RemoteCompose Widget 在旧设备上会怎样？

**A**: 使用 Glance API 构建的 Widget 会自动向后兼容。在 API 35+ 设备上，Glance 自动使用 RemoteCompose 后端渲染；在旧设备上，自动降级为传统 RemoteViews 渲染。直接使用 `@RemoteComposable` 或 `RemoteComposeWidget` 的 Widget 仅在 API 35+ 可用。

### Q2: 什么时候用 Glance API，什么时候用 RemoteCompose API？

**A**:
- **需要向后兼容** → 使用 Glance API
- **需要动画/粒子/高级交互** → 使用 RemoteCompose API（仅 API 35+）
- **两者兼顾** → 使用 Glance API，在 API 35+ 上自动获得 RemoteCompose 增强

### Q3: Widget 更新频率有限制吗？

**A**: 系统限制 `updatePeriodMillis` 最小为 30 分钟。对于实时数据，建议使用 `WorkManager` 或 `AlarmManager` 触发更新，或使用 `GlanceAppWidget.update()` 手动触发。

### Q4: 如何调试 Widget？

**A**:
1. 使用 `GlanceAppWidgetUnitTest` 进行单元测试
2. 使用 Android Studio 的 Widget Preview 功能
3. 使用 `adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE` 手动触发更新
4. 查看 Logcat 中 `GlanceAppWidget` 标签的日志

### Q5: 粒子效果会影响电池吗？

**A**: 不会。RemoteCompose 的粒子系统在 Player 端（系统进程）运行，使用高效的 RPN 表达式引擎，无需唤醒应用进程。系统会自动管理渲染频率以节省电量。

### Q6: 如何处理 Widget 的点击事件？

**A**:
- **Glance**: 使用 `actionRunCallback<T>()`、`actionStartActivity()`、`actionSendBroadcast()` 等
- **RemoteCompose**: 使用 `clickable(ValueChange(...))` 修改状态变量，或使用 `onClick { }` 回调（Widget 专用）
- 点击事件通过 `PendingIntent` 和广播机制实现，因为 Widget 运行在系统进程中

### Q7: RemoteCompose 的 Profile 是什么？

**A**: Profile 定义了不同平台支持的操作码子集：
- `WIDGETS_V7`：Android 16+ Widget 支持的完整操作集
- `WIDGETS_V6`：早期 Widget 操作集
- `ANDROIDX`：AndroidX Player 支持的完整操作集（Activity 中使用）
- `WEAR_WIDGETS`：Wear OS Widget 的操作子集

选择正确的 Profile 确保文档在目标平台可用。
