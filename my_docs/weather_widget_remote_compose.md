# 天气微件 Remote Compose 完整实现文档

> 本文档提供使用 Remote Compose 制作天气微件的完整实现方案，包含三种实现方式：Compose-like DSL 运行时构建、Procedural DSL 运行时构建、以及 Wire Format 二进制数据直接生成。

---

## 目录

- [1. 概述](#1-概述)
- [2. 方式一：Compose-like DSL 运行时构建](#2-方式一compose-like-dsl-运行时构建)
  - [2.1 完整代码](#21-完整代码)
  - [2.2 代码解析](#22-代码解析)
  - [2.3 预览方式](#23-预览方式)
- [3. 方式二：Procedural DSL 运行时构建](#3-方式二procedural-dsl-运行时构建)
  - [3.1 完整代码](#31-完整代码)
  - [3.2 代码解析](#32-代码解析)
- [4. 方式三：直接生成 Wire Format 二进制数据](#4-方式三直接生成-wire-format-二进制数据)
  - [4.1 操作码序列](#41-操作码序列)
  - [4.2 二进制数据结构](#42-二进制数据结构)
  - [4.3 伪代码实现](#43-伪代码实现)
- [5. 三种方式对比](#5-三种方式对比)
- [6. 在 "帮我制作微件" 中的应用](#6-在-帮我制作微件-中的应用)

---

## 1. 概述

天气微件是 "帮我制作微件" 功能最典型的应用场景之一。本文档基于 AndroidX Remote Compose 官方 Demo（[DemoWeather.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/examples/DemoWeather.kt)）整理，提供三种实现方式：

| 方式 | API 风格 | 适用场景 | 复杂度 |
|------|---------|---------|--------|
| **Compose-like DSL** | `@RemoteComposable` + `RemoteColumn/Row/Text` | 可视化开发、预览调试 | 低 |
| **Procedural DSL** | `RemoteComposeContextAndroid` + `column/row/text` | 程序化生成、动态构建 | 中 |
| **Wire Format 直接生成** | 操作码 + 二进制数据 | LLM 生成、系统内置 | 高 |

---

## 2. 方式一：Compose-like DSL 运行时构建

### 2.1 完整代码

```kotlin
import android.graphics.BitmapFactory
import androidx.compose.remote.creation.compose.layout.*
import androidx.compose.remote.creation.compose.modifier.*
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

/**
 * 天气微件主入口
 * 使用 @RemoteComposable 注解，可在 RemoteCompose 播放器中渲染
 */
@RemoteComposable
@Composable
fun WeatherWidget() {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize(),
        contentAlignment = RemoteAlignment.Center
    ) {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxWidth(),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            // 顶部：当前天气信息卡片
            WeatherCurrentCard()
            
            // 中部：小时天气预报
            WeatherHourlyForecast()
            
            // 底部：未来几天预报
            WeatherDailyForecast()
        }
    }
}

/**
 * 当前天气信息卡片
 * 显示城市、温度、天气图标、最高最低温
 */
@RemoteComposable
@Composable
fun WeatherCurrentCard() {
    val res = LocalResources.current
    val weatherIcon = remember {
        BitmapFactory.decodeResource(res, R.drawable.mostly_cloudy, BitmapFactory.Options())
            .asImageBitmap()
    }
    
    RemoteColumn(
        modifier = RemoteModifier
            .fillMaxWidth()
            .padding(16.rdp)
            .clip(RemoteRoundedCornerShape(24.rdp))
            .background(Color(219, 247, 239))
            .padding(16.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        // 城市名称
        RemoteText(
            "Rio de Janeiro",
            fontSize = 18.rsp,
            fontWeight = FontWeight.Medium
        )
        
        // 当前温度（大字体）
        RemoteText(
            "24°C",
            fontSize = 48.rsp,
            fontWeight = FontWeight.Bold
        )
        
        // 天气图标
        RemoteImage(
            weatherIcon,
            RemoteString(""),
            modifier = RemoteModifier.size(48.rdp)
        )
        
        // 天气描述
        RemoteText(
            "Mostly Cloudy",
            fontSize = 14.rsp,
            modifier = RemoteModifier.padding(top = 4.rdp)
        )
        
        // 最高/最低温度
        RemoteText(
            "H: 28°C  L: 19°C",
            fontSize = 12.rsp,
            modifier = RemoteModifier.padding(top = 4.rdp)
        )
    }
}

/**
 * 小时天气预报
 * 横向滚动显示未来几小时天气
 */
@RemoteComposable
@Composable
fun WeatherHourlyForecast() {
    RemoteRow(
        modifier = RemoteModifier
            .fillMaxWidth()
            .padding(horizontal = 16.rdp),
        horizontalArrangement = RemoteArrangement.SpaceBetween,
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        HourlyWeatherItem("Now", "24°C", R.drawable.mostly_cloudy)
        HourlyWeatherItem("1 PM", "26°C", R.drawable.partly_cloudy)
        HourlyWeatherItem("2 PM", "27°C", R.drawable.mostly_sunny)
        HourlyWeatherItem("3 PM", "25°C", R.drawable.partly_cloudy)
        HourlyWeatherItem("4 PM", "23°C", R.drawable.showers_rain)
        HourlyWeatherItem("5 PM", "22°C", R.drawable.windy_breezy)
    }
}

@RemoteComposable
@Composable
fun HourlyWeatherItem(time: String, temp: String, iconRes: Int) {
    val res = LocalResources.current
    val icon = remember {
        BitmapFactory.decodeResource(res, iconRes, BitmapFactory.Options()).asImageBitmap()
    }
    
    RemoteColumn(
        modifier = RemoteModifier.padding(8.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteText(time, fontSize = 12.rsp)
        RemoteImage(icon, RemoteString(""), RemoteModifier.size(24.rdp))
        RemoteText(temp, fontSize = 14.rsp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * 未来几天天气预报
 * 纵向列表显示未来7天天气
 */
@RemoteComposable
@Composable
fun WeatherDailyForecast() {
    RemoteColumn(
        modifier = RemoteModifier
            .fillMaxWidth()
            .padding(16.rdp)
            .clip(RemoteRoundedCornerShape(16.rdp))
            .background(Color(205, 232, 225))
            .padding(12.rdp),
        verticalArrangement = RemoteArrangement.SpaceEvenly,
    ) {
        DailyWeatherItem("Saturday", "70%", R.drawable.showers_rain, "22°C/18°C")
        DailyWeatherItem("Sunday", "", R.drawable.mostly_cloudy, "24°C/19°C")
        DailyWeatherItem("Monday", "", R.drawable.partly_cloudy, "26°C/20°C")
        DailyWeatherItem("Tuesday", "30%", R.drawable.windy_breezy, "23°C/17°C")
        DailyWeatherItem("Wednesday", "70%", R.drawable.showers_rain, "21°C/16°C")
    }
}

@RemoteComposable
@Composable
fun DailyWeatherItem(
    day: String,
    precipitation: String,
    iconRes: Int,
    temperature: String
) {
    val res = LocalResources.current
    val icon = remember {
        BitmapFactory.decodeResource(res, iconRes, BitmapFactory.Options()).asImageBitmap()
    }
    
    RemoteRow(
        modifier = RemoteModifier
            .fillMaxWidth()
            .padding(vertical = 4.rdp),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        // 星期
        RemoteBox(RemoteModifier.width(80.rdp)) {
            RemoteText(day, fontSize = 14.rsp)
        }
        
        // 降水概率
        if (precipitation.isNotEmpty()) {
            RemoteBox(RemoteModifier.width(40.rdp)) {
                RemoteText(precipitation, fontSize = 12.rsp)
            }
        } else {
            RemoteBox(RemoteModifier.width(40.rdp))
        }
        
        // 天气图标
        RemoteImage(icon, RemoteString(""), RemoteModifier.size(24.rdp))
        
        // 温度范围
        RemoteBox(
            RemoteModifier.weight(1f),
            contentAlignment = RemoteAlignment.CenterEnd
        ) {
            RemoteText(temperature, fontSize = 14.rsp)
        }
    }
}

// 预览函数
@Preview
@Composable
private fun WeatherWidgetPreview() = RemoteContentPreview { WeatherWidget() }
```

### 2.2 代码解析

#### 关键组件说明

| 组件 | 用途 | 对应原生 Compose |
|------|------|-----------------|
| `RemoteBox` | 堆叠容器，用于层叠布局 | `Box` |
| `RemoteColumn` | 垂直排列子组件 | `Column` |
| `RemoteRow` | 水平排列子组件 | `Row` |
| `RemoteText` | 文本显示 | `Text` |
| `RemoteImage` | 图片显示 | `Image` |

#### 关键 Modifier 说明

| Modifier | 用途 |
|----------|------|
| `RemoteModifier.fillMaxSize()` | 填充父容器 |
| `RemoteModifier.fillMaxWidth()` | 填充宽度 |
| `RemoteModifier.padding(16.rdp)` | 内边距（16 remote dp） |
| `RemoteModifier.clip(RemoteRoundedCornerShape(24.rdp))` | 圆角裁剪 |
| `RemoteModifier.background(Color(219, 247, 239))` | 背景色 |
| `RemoteModifier.size(48.rdp)` | 固定尺寸 |
| `RemoteModifier.weight(1f)` | 弹性权重 |

#### 单位说明

| 单位 | 含义 | 示例 |
|------|------|------|
| `rdp` | Remote DP（远程密度无关像素） | `16.rdp` |
| `rsp` | Remote SP（远程缩放无关像素，用于文字） | `14.rsp` |
| `rf` | Remote Float（远程浮点数，用于表达式） | `1f.rf` |

### 2.3 预览方式

```kotlin
// 在 Android Studio 中预览
@Preview
@Composable
private fun WeatherWidgetPreview() = RemoteContentPreview { WeatherWidget() }

// 在 DemosCompose.kt 中注册
getComposeDoc(context, "Weather/Widget") { WeatherWidget() }
```

---

## 3. 方式二：Procedural DSL 运行时构建

### 3.1 完整代码

```kotlin
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.CoreText

/**
 * 使用 Procedural API 构建天气微件
 * 返回 RemoteComposeWriter，可直接生成 Wire Buffer
 */
fun weatherWidgetProcedural(): RemoteComposeWriter {
    val rc = RemoteComposeContextAndroid(
        platform = AndroidxRcPlatformServices(),
        apiLevel = 7,
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 600),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Weather Widget"),
    ) {
        root {
            // 根容器：垂直排列
            column(
                modifier = Modifier.fillMaxSize().background(Color.WHITE),
                horizontal = ColumnLayout.CENTER,
                vertical = ColumnLayout.TOP,
            ) {
                // ===== 当前天气卡片 =====
                weatherCurrentCard()
                
                // ===== 小时预报 =====
                weatherHourlyForecast()
                
                // ===== 每日预报 =====
                weatherDailyForecast()
            }
        }
    }
    return rc.writer
}

/**
 * 当前天气卡片
 */
fun RemoteComposeContextAndroid.weatherCurrentCard() {
    column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32, 32, 32, 16)  // left, top, right, bottom (px)
            .clip(RoundedRectShape(96f, 96f, 96f, 96f))  // 24dp * 4 = 96px (density=4)
            .background(0xFFDBF7EF.toInt()),  // Color(219, 247, 239)
        horizontal = ColumnLayout.CENTER,
        vertical = ColumnLayout.CENTER,
    ) {
        // 城市
        text("Rio de Janeiro", fontSize = 72f)  // 18sp * 4 = 72px
        
        // 温度
        text("24°C", fontSize = 192f, fontWeight = 700f)  // 48sp * 4 = 192px
        
        // 天气图标（使用占位符 ID，实际需先 addBitmap）
        val iconId = addBitmap(weatherIconBitmap)  // 需预加载位图
        image(Modifier.size(192), iconId, ImageScaling.SCALE_INSIDE, 1f)
        
        // 天气描述
        text("Mostly Cloudy", fontSize = 56f)  // 14sp * 4 = 56px
        
        // 最高/最低温
        text("H: 28°C  L: 19°C", fontSize = 48f)  // 12sp * 4 = 48px
    }
}

/**
 * 小时天气预报
 */
fun RemoteComposeContextAndroid.weatherHourlyForecast() {
    row(
        modifier = Modifier.fillMaxWidth().padding(32, 16, 32, 16),
        horizontal = RowLayout.SPACE_BETWEEN,
        vertical = RowLayout.CENTER,
    ) {
        hourlyItem("Now", "24°C", iconId1)
        hourlyItem("1 PM", "26°C", iconId2)
        hourlyItem("2 PM", "27°C", iconId3)
        hourlyItem("3 PM", "25°C", iconId4)
        hourlyItem("4 PM", "23°C", iconId5)
        hourlyItem("5 PM", "22°C", iconId6)
    }
}

fun RemoteComposeContextAndroid.hourlyItem(time: String, temp: String, iconId: Int) {
    column(
        modifier = Modifier.padding(32, 32, 32, 32),
        horizontal = ColumnLayout.CENTER,
    ) {
        text(time, fontSize = 48f)  // 12sp
        image(Modifier.size(96), iconId, ImageScaling.SCALE_INSIDE, 1f)  // 24dp
        text(temp, fontSize = 56f, fontWeight = 600f)  // 14sp
    }
}

/**
 * 每日天气预报
 */
fun RemoteComposeContextAndroid.weatherDailyForecast() {
    column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32, 16, 32, 32)
            .clip(RoundedRectShape(64f, 64f, 64f, 64f))  // 16dp
            .background(0xFFCDE8E1.toInt())  // Color(205, 232, 225)
            .padding(48, 48, 48, 48),  // 12dp
        vertical = ColumnLayout.SPACE_EVENLY,
    ) {
        dailyItem("Saturday", "70%", iconRain, "22°C/18°C")
        dailyItem("Sunday", "", iconCloudy, "24°C/19°C")
        dailyItem("Monday", "", iconPartly, "26°C/20°C")
        dailyItem("Tuesday", "30%", iconWindy, "23°C/17°C")
        dailyItem("Wednesday", "70%", iconRain, "21°C/16°C")
    }
}

fun RemoteComposeContextAndroid.dailyItem(
    day: String,
    precipitation: String,
    iconId: Int,
    temperature: String
) {
    row(
        modifier = Modifier.fillMaxWidth().padding(0, 16, 0, 16),
        vertical = RowLayout.CENTER,
    ) {
        // 星期
        text(day, fontSize = 56f, modifier = Modifier.width(320))  // 80dp
        
        // 降水概率
        if (precipitation.isNotEmpty()) {
            text(precipitation, fontSize = 48f, modifier = Modifier.width(160))  // 40dp
        } else {
            box(Modifier.width(160))
        }
        
        // 图标
        image(Modifier.size(96), iconId, ImageScaling.SCALE_INSIDE, 1f)
        
        // 温度（靠右）
        box(Modifier.horizontalWeight(1f))
        text(temperature, fontSize = 56f)
    }
}

// 预览
@Preview @Composable private fun WeatherProceduralPreview() = RemoteDocPreview(weatherWidgetProcedural())
```

### 3.2 代码解析

#### Procedural API 特点

| 特点 | 说明 |
|------|------|
| `RemoteComposeContextAndroid` | Android 平台特定的创建上下文 |
| `Modifier` 属性 | 返回 `RecordingModifier()`，用于链式调用 |
| 原始单位 | 直接使用像素值（`px`），需手动计算密度 |
| `addBitmap()` | 预加载位图并获取 ID，后续通过 ID 引用 |

#### 与 Compose-like API 对比

| 维度 | Compose-like API | Procedural API |
|------|-----------------|----------------|
| 注解 | `@RemoteComposable` | 无注解，纯函数 |
| 组件名 | `RemoteColumn/RemoteRow/RemoteText` | `column/row/text` |
| Modifier | `RemoteModifier.xxx()` | `Modifier.xxx()` |
| 单位 | `dp/sp/rdp/rsp`（自动转换） | 原始像素值 |
| 适用场景 | 可视化开发 | 程序化/动态生成 |

---

## 4. 方式三：直接生成 Wire Format 二进制数据

### 4.1 操作码序列

基于 [PROTOCOL_SPEC.md](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/PROTOCOL_SPEC.md)，天气微件的操作码序列如下：

```
[HEADER]                    文档头部
├── OpCode: 0
├── Version: 7
├── Width: 400
├── Height: 600
├── Density: 4
└── Profile: WIDGETS_V7

[DATA_TEXT]                 注册文本资源
├── OpCode: 102
├── TextID: 1    -> "Rio de Janeiro"
├── TextID: 2    -> "24°C"
├── TextID: 3    -> "Mostly Cloudy"
├── TextID: 4    -> "H: 28°C  L: 19°C"
├── TextID: 5    -> "Now"
├── TextID: 6    -> "1 PM"
├── TextID: 7    -> "Saturday"
└── ...

[DATA_BITMAP]               注册图片资源
├── OpCode: 101
├── BitmapID: 1  -> 多云图标 (48x48)
├── BitmapID: 2  -> 晴间多云图标 (48x48)
└── ...

[PAINT_VALUES]              设置绘制状态
├── OpCode: 40
└── Color: 0xFFDBF7EF (背景色)

[COMPONENT_START]           根容器 (Column)
├── OpCode: 201
├── ComponentID: 1
├── LayoutType: COLUMN
└── Modifier: fillMaxSize, background(White)

├── [COMPONENT_START]       当前天气卡片 (Column)
│   ├── ComponentID: 2
│   ├── LayoutType: COLUMN
│   └── Modifier: padding(16dp), clip(round), bg(#DBF7EF), padding(16dp)
│
│   ├── [DRAW_TEXT_RUN]     城市名称
│   │   ├── TextID: 1
│   │   └── fontSize: 18sp
│   ├── [DRAW_TEXT_RUN]     温度
│   │   ├── TextID: 2
│   │   └── fontSize: 48sp, fontWeight: Bold
│   ├── [DRAW_BITMAP]       天气图标
│   │   └── BitmapID: 1
│   ├── [DRAW_TEXT_RUN]     天气描述
│   │   └── TextID: 3
│   └── [DRAW_TEXT_RUN]     最高/最低温
│       └── TextID: 4
│
├── [COMPONENT_START]       小时预报 (Row)
│   ├── LayoutType: ROW
│   └── Modifier: fillMaxWidth, padding(16dp), spaceBetween
│
│   ├── [COMPONENT_START]   小时项 1 (Column)
│   │   ├── [DRAW_TEXT_RUN]   "Now"
│   │   ├── [DRAW_BITMAP]     图标
│   │   └── [DRAW_TEXT_RUN]   "24°C"
│   ├── [COMPONENT_START]   小时项 2
│   └── ...
│
├── [COMPONENT_START]       每日预报 (Column)
│   ├── LayoutType: COLUMN
│   └── Modifier: padding(16dp), clip(round), bg(#CDE8E1), padding(12dp)
│
│   ├── [COMPONENT_START]   每日项 1 (Row)
│   │   ├── [DRAW_TEXT_RUN]   "Saturday"
│   │   ├── [DRAW_TEXT_RUN]   "70%"
│   │   ├── [DRAW_BITMAP]     雨天图标
│   │   └── [DRAW_TEXT_RUN]   "22°C/18°C"
│   └── ...
│
[CONTAINER_END]             关闭根容器
└── OpCode: 230
```

### 4.2 二进制数据结构

```
Offset    Size    Content
─────────────────────────────────────────────────────────────────

0x0000    1       OpCode: HEADER (0x00)
0x0001    4       Version: 7 (Int32 LE)
0x0005    4       Width: 400 (Int32 LE)
0x0009    4       Height: 600 (Int32 LE)
0x000D    4       Density: 4 (Int32 LE)
0x0011    8       Profile: WIDGETS_V7 (Int64 LE)

0x0019    1       OpCode: DATA_TEXT (0x66)
0x001A    4       TextID: 1 (Int32 LE)
0x001E    4       StringLength: 14 (Int32 LE)
0x0022    14      StringData: "Rio de Janeiro" (UTF-8)

0x0030    1       OpCode: DATA_TEXT (0x66)
0x0031    4       TextID: 2 (Int32 LE)
0x0035    4       StringLength: 4 (Int32 LE)
0x0039    4       StringData: "24°C" (UTF-8)

... (更多文本资源)

0x0100    1       OpCode: DATA_BITMAP (0x65)
0x0101    4       BitmapID: 1 (Int32 LE)
0x0105    4       Width: 48 (Int32 LE)
0x0109    4       Height: 48 (Int32 LE)
0x010D    N       PixelData: ARGB_8888 (48*48*4 bytes)

... (更多图片资源)

0x1000    1       OpCode: COMPONENT_START (0xC9)
0x1001    4       ComponentID: 1 (Int32 LE)
0x1005    4       LayoutType: COLUMN (Int32 LE)
0x1009    4       ModifierFlags (Int32 LE)
0x100D    4       ModifierDataLength (Int32 LE)
0x1011    N       ModifierData (Serialized RecordingModifier)

... (组件树嵌套)

0x2000    1       OpCode: CONTAINER_END (0xE6)
```

### 4.3 伪代码实现

```kotlin
/**
 * 直接生成天气微件的 Wire Format 二进制数据
 * 这是 "帮我制作微件" 功能最底层的实现方式
 */
fun generateWeatherWidgetBytes(): ByteArray {
    val buffer = WireBuffer()
    
    // ===== 1. 文档头部 =====
    buffer.writeByte(Operations.HEADER)           // OpCode 0
    buffer.writeInt(7)                            // Version 7
    buffer.writeInt(400)                          // Width
    buffer.writeInt(600)                          // Height
    buffer.writeInt(4)                            // Density
    buffer.writeLong(RcPlatformProfiles.WIDGETS_V7) // Profile
    
    // ===== 2. 注册文本资源 =====
    val texts = listOf(
        1 to "Rio de Janeiro",
        2 to "24°C",
        3 to "Mostly Cloudy",
        4 to "H: 28°C  L: 19°C",
        5 to "Now",
        6 to "1 PM",
        7 to "Saturday",
        8 to "Sunday",
        9 to "Monday",
        10 to "Tuesday",
        11 to "Wednesday",
        12 to "70%",
        13 to "30%",
        14 to "22°C/18°C",
        15 to "24°C/19°C",
        16 to "26°C/20°C",
        17 to "23°C/17°C",
        18 to "21°C/16°C",
    )
    
    for ((id, text) in texts) {
        buffer.writeByte(Operations.DATA_TEXT)    // OpCode 102
        buffer.writeInt(id)
        buffer.writeString(text)                  // UTF-8 with length prefix
    }
    
    // ===== 3. 注册图片资源 =====
    val bitmaps = listOf(
        1 to loadBitmap("mostly_cloudy.png"),
        2 to loadBitmap("partly_cloudy.png"),
        3 to loadBitmap("mostly_sunny.png"),
        4 to loadBitmap("showers_rain.png"),
        5 to loadBitmap("windy_breezy.png"),
    )
    
    for ((id, bitmap) in bitmaps) {
        buffer.writeByte(Operations.DATA_BITMAP)  // OpCode 101
        buffer.writeInt(id)
        buffer.writeInt(bitmap.width)
        buffer.writeInt(bitmap.height)
        buffer.writeBytes(bitmap.pixels)          // ARGB_8888 raw data
    }
    
    // ===== 4. 组件树 =====
    // 根容器 Column
    buffer.writeByte(Operations.COMPONENT_START)  // OpCode 201
    buffer.writeInt(1)                            // ComponentID
    buffer.writeInt(LayoutType.COLUMN)            // LayoutType
    writeModifier(buffer, Modifier.fillMaxSize().background(Color.WHITE))
    
    // 当前天气卡片 Column
    buffer.writeByte(Operations.COMPONENT_START)
    buffer.writeInt(2)
    buffer.writeInt(LayoutType.COLUMN)
    writeModifier(buffer, Modifier
        .fillMaxWidth()
        .padding(64, 64, 64, 32)                  // 16dp * 4
        .clip(RoundedRectShape(96f, 96f, 96f, 96f))
        .background(0xFFDBF7EF.toInt())
        .padding(64, 64, 64, 64)
    )
    
    // 城市名称
    buffer.writeByte(Operations.DRAW_TEXT_RUN)    // OpCode 43
    buffer.writeInt(1)                            // TextID
    buffer.writeInt(0)                            // start
    buffer.writeInt(14)                           // end
    buffer.writeFloat(0f)                         // x (由布局计算)
    buffer.writeFloat(0f)                         // y (由布局计算)
    buffer.writeByte(0)                           // RTL false
    
    // 温度
    buffer.writeByte(Operations.DRAW_TEXT_RUN)
    buffer.writeInt(2)
    buffer.writeInt(0)
    buffer.writeInt(4)
    buffer.writeFloat(0f)
    buffer.writeFloat(0f)
    buffer.writeByte(0)
    
    // 天气图标
    buffer.writeByte(Operations.DRAW_BITMAP)      // OpCode 44
    buffer.writeInt(1)                            // BitmapID
    buffer.writeFloat(0f)                         // L (由布局计算)
    buffer.writeFloat(0f)                         // T
    buffer.writeFloat(192f)                       // R (48dp * 4)
    buffer.writeFloat(192f)                       // B
    
    // ... 更多绘制操作
    
    // 关闭当前天气卡片
    buffer.writeByte(Operations.CONTAINER_END)    // OpCode 230
    
    // ... 小时预报和每日预报组件
    
    // 关闭根容器
    buffer.writeByte(Operations.CONTAINER_END)
    
    return buffer.toByteArray()
}
```

---

## 5. 三种方式对比

| 对比维度 | Compose-like DSL | Procedural DSL | Wire Format 直接生成 |
|---------|-----------------|----------------|---------------------|
| **代码风格** | 声明式，类似 Jetpack Compose | 命令式，函数调用 | 二进制操作码 |
| **学习曲线** | 低（熟悉 Compose 即可） | 中 | 高（需理解协议） |
| **开发效率** | 高（有预览、自动补全） | 中 | 低（需手动计算） |
| **运行时开销** | 低 | 低 | 极低（直接二进制） |
| **适用场景** | 可视化开发、调试 | 程序化生成 | LLM 生成、系统内置 |
| **单位系统** | `dp/sp`（自动转换） | 原始像素 | 原始像素 |
| **注解要求** | `@RemoteComposable` | 无 | 无 |
| **预览支持** | Android Studio 预览 | `RemoteDocPreview` | 需播放器 |

---

## 6. 在 "帮我制作微件" 中的应用

### 6.1 LLM 生成流程

```
用户: "创建一个显示北京天气的微件"
    |
    ▼
Gemini 解析意图:
├── 城市: 北京
├── 组件: 当前天气 + 小时预报 + 每日预报
├── 布局: Column 根容器
├── 样式: 圆角卡片、浅蓝色背景
└── 数据: 温度、天气图标、降水概率
    |
    ▼
生成 Remote Compose DSL 代码（方式一或二）
    |
    ▼
系统执行 DSL -> 生成 Wire Buffer -> 创建 RemoteViews
    |
    ▼
桌面立即显示天气微件
```

### 6.2 动态数据绑定

在实际应用中，天气数据是动态变化的。Remote Compose 支持通过表达式引擎实现数据绑定：

```kotlin
// 使用远程变量绑定动态数据
val temperature = rememberRemoteFloat { 24f.rf }
val city = rememberRemoteString("Beijing")

RemoteText(
    text = city.value,  // 远程字符串变量
    fontSize = 48.rsp
)

RemoteText(
    text = "${temperature.value}°C",  // 远程浮点数变量
    fontSize = 96.rsp,
    fontWeight = FontWeight.Bold
)

// 数据更新时，只需修改变量值，无需重新生成整个文档
temperature.value = 26f.rf  // 微件自动刷新
```

### 6.3 系统内置实现

在 Android 系统层面，"帮我制作微件"功能的实现可能如下：

```kotlin
class WidgetGenerationService {
    
    fun generateWidgetFromDescription(description: String): RemoteViews {
        // 1. LLM 解析描述，生成操作码序列
        val operations = llm.generateOperations(description)
        
        // 2. 直接构建 Wire Buffer
        val buffer = WireBuffer()
        for (op in operations) {
            buffer.writeByte(op.opcode)
            buffer.writeBytes(op.data)
        }
        
        // 3. 创建 RemoteViews
        val bytes = buffer.toByteArray()
        val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
        return RemoteViews(drawInstructions)
    }
}
```

---

## 附录：参考文档

- [Remote Compose 架构文档](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/REMOTE_COMPOSE_ARCHITECTURE_zh.md)
- [协议规范](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/PROTOCOL_SPEC_zh.md)
- [数据流与生命周期](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/DATA_FLOW_zh.md)
- [表达式引擎](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/EXPRESSION_ENGINE_zh.md)
- [官方天气 Demo](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/examples/DemoWeather.kt)
