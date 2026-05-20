# GenWidget - LLM 生成 Wire Format 编译器

本项目提供了一套完整的工具，用于将 LLM 生成的 JSON 规格转换为 Remote Compose Wire Format 二进制数据。

## 项目结构

```
compose/gen-widget/
├── build.gradle.kts                          # 构建配置
├── README.md                                 # 本文档
└── src/main/java/androidx/compose/genwidget/
    ├── model/                                # 数据模型
    │   ├── WidgetSpec.kt                     # 微件规格定义
    │   └── Interaction.kt                    # 交互相关数据类
    ├── compiler/                             # 编译器
    │   └── WidgetCompiler.kt                 # 核心编译器
    └── sample/                               # 示例
        └── WeatherWidgetSample.kt            # 天气微件示例
```

## 功能特性

### 1. 数据模型

- **WidgetSpec**: 微件规格说明，映射 LLM 输出的 JSON
- **Component**: 组件定义（Column、Row、Box、Text、Image 等）
- **Modifier**: 修饰器（尺寸、间距、背景等）
- **ClickAction**: 点击动作定义
- **Interaction**: 交互行为定义

### 2. 编译器功能

- **compileFromJson()**: 从 JSON 字符串编译为 Wire Format
- **compile()**: 从 WidgetSpec 编译为 Wire Format
- 支持的操作码：
  - `HEADER`: 文档头部
  - `DATA_TEXT`: 文本资源
  - `DATA_BITMAP`: 位图资源
  - `COMPONENT_START`: 组件开始
  - `DRAW_TEXT_RUN`: 绘制文本
  - `DRAW_BITMAP`: 绘制图片
  - `MODIFIER_CLICK`: 点击修饰器
  - `HOST_ACTION`: 主机动作
  - `VALUE_INTEGER_CHANGE_ACTION`: 值变更动作

### 3. 交互支持

支持两种交互类型：

#### host_action 类型
触发主机回调（打开 Activity、发送广播等）：
```json
"clickAction": {
  "type": "host_action",
  "actionId": 1,
  "metadata": "open_detail"
}
```

#### value_change 类型
更改变量值（播放器自动执行）：
```json
"clickAction": {
  "type": "value_change",
  "targetValueId": 100,
  "newValue": 1
}
```

## 使用示例

### 1. 创建天气微件

```kotlin
import androidx.compose.genwidget.compiler.WidgetCompiler
import androidx.compose.genwidget.sample.WeatherWidgetSample

// 方法 1: 使用预定义示例
val wireFormat = WeatherWidgetSample.compileWeatherWidget()

// 方法 2: 自定义 JSON
val json = """
    {
      "widget": {
        "type": "weather",
        "width": 400,
        "height": 600,
        "components": [...],
        "resources": {...}
      }
    }
""".trimIndent()

val wireFormat = WidgetCompiler.compileFromJson(json)
```

### 2. 应用到 RemoteViews

```kotlin
fun createWeatherWidget(context: Context, appWidgetId: Int) {
    // 1. 编译为 Wire Format
    val wireFormat = WeatherWidgetSample.compileWeatherWidget()
    
    // 2. 创建 RemoteViews
    val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
    val remoteViews = RemoteViews(instructions)
    
    // 3. 注册动作回调（在 Player 中）
    remoteComposePlayer.addIdActionListener { actionId, metadata ->
        when (actionId) {
            1 -> {
                // 打开天气详情页面
                val intent = Intent("android.intent.action.VIEW").apply {
                    data = Uri.parse("weather://detail")
                }
                context.startActivity(intent)
            }
        }
    }
    
    // 4. 更新微件
    AppWidgetManager.getInstance(context)
        .updateAppWidget(appWidgetId, remoteViews)
}
```

## Wire Format 二进制结构

### 带交互的 Text 组件

```
┌──────────────────────────────────────┐
│ DRAW_TEXT_RUN (OP_CODE: 43)          │
├──────────────────────────────────────┤
│ textResourceId: 1 (int)              │
├──────────────────────────────────────┤
│ fontSize: 48.0 (float)               │
├──────────────────────────────────────┤
│ fontWeight: 500 (int)                │
├──────────────────────────────────────┤
│ color: -0x1000000 (int)              │
├──────────────────────────────────────┤
│ MODIFIER_CLICK (OP_CODE: 59)         │  ← 点击修饰器开始
├──────────────────────────────────────┤
│ HOST_ACTION (OP_CODE: 209)           │  ← 主机动作
├──────────────────────────────────────┤
│ actionId: 1 (int)                    │  ← 动作 ID
└──────────────────────────────────────┘
```

## 依赖

```kotlin
dependencies {
    implementation project(':compose:remote:remote-core')
    implementation libs.gson
}
```

## 参考资料

- [LLM Wire Format 实现指南](../../../my_docs/llm_wire_format_implementation.md)
- [LLM Wire Format 交互实现](../../../my_docs/llm_wire_format_interaction_implementation.md)
- [LLM Wire Format 完整实现 v2](../../../my_docs/llm_wire_format_complete_implementation_v2.md)

## 许可证

Apache License 2.0
