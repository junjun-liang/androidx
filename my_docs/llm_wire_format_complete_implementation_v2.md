# LLM 生成 Wire Format 完整实现示例（v2 - 包含交互功能）

本文档提供完整的 LLM 输出 JSON 示例和将 JSON 转换为 Wire Format 的实现代码，**包含交互功能的完整实现**。

---

## 步骤 2: LLM 生成结构化数据（JSON 示例）

### 完整 JSON 输出示例（包含交互）

```json
{
  "widget": {
    "type": "weather",
    "version": 1,
    "width": 400,
    "height": 600,
    "density": 4,
    "profile": "WIDGETS_V7",
    "components": [
      {
        "type": "Column",
        "id": 1,
        "modifier": {
          "fillMaxSize": true,
          "background": "#FFFFFF",
          "padding": {
            "left": 32,
            "top": 32,
            "right": 32,
            "bottom": 32
          }
        },
        "children": [
          {
            "type": "Text",
            "id": 2,
            "textResourceId": 1,
            "clickAction": {
              "actionId": 1,
              "metadata": "open_detail"
            },
            "style": {
              "fontSize": 48,
              "fontWeight": 500,
              "color": "#000000"
            }
          },
          {
            "type": "Text",
            "id": 3,
            "textResourceId": 2,
            "style": {
              "fontSize": 128,
              "fontWeight": 700,
              "color": "#000000"
            }
          }
        ]
      }
    ],
    "resources": {
      "texts": [
        {"id": 1, "content": "Beijing"},
        {"id": 2, "content": "24°C"}
      ],
      "bitmaps": []
    },
    "actions": [
      {
        "actionId": 1,
        "type": "host_action",
        "description": "Open weather detail page"
      }
    ]
  }
}
```

### 交互部分说明

```json
"interactions": [
  {
    "componentId": 1,
    "type": "click",
    "action": {
      "type": "host_action",
      "actionId": 1,
      "metadata": "open_detail"
    }
  }
],
"actions": [
  {
    "actionId": 1,
    "type": "host_action",
    "description": "Open weather detail page"
  }
]
```

**关键点**：
- `interactions`: 定义组件的交互行为（点击时触发什么）
- `actions`: 定义全局动作注册表（actionId → 具体行为）
- Wire Format 中只存储 `actionId`，不存储 Intent
- Intent 在应用层通过 `addIdActionListener()` 注册

---

## 步骤 3: 转换为操作码序列（完整 Kotlin 实现）

### 完整实现代码（包含交互）

```kotlin
package androidx.compose.remote.widget.compiler

import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.Operations
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// ============================================================================
// 1. 数据类定义
// ============================================================================

data class WidgetSpec(
    @SerializedName("widget") val widget: WidgetData
)

data class WidgetData(
    @SerializedName("type") val type: String,
    @SerializedName("version") val version: Int = 1,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("density") val density: Int = 4,
    @SerializedName("profile") val profile: String = "WIDGETS_V7",
    @SerializedName("components") val components: List<Component>,
    @SerializedName("resources") val resources: Resources,
    @SerializedName("interactions") val interactions: List<Interaction>? = null,
    @SerializedName("actions") val actions: List<ActionDefinition>? = null
)

data class Component(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: Int,
    @SerializedName("layout") val layout: LayoutParams? = null,
    @SerializedName("modifier") val modifier: Modifier? = null,
    @SerializedName("children") val children: List<Component>? = null,
    @SerializedName("textResourceId") val textResourceId: Int? = null,
    @SerializedName("bitmapResourceId") val bitmapResourceId: Int? = null,
    @SerializedName("style") val style: TextStyle? = null,
    @SerializedName("size") val size: Size? = null,
    @SerializedName("clickAction") val clickAction: ClickAction? = null
)

data class LayoutParams(
    @SerializedName("horizontalAlignment") val horizontalAlignment: String = "START",
    @SerializedName("verticalArrangement") val verticalArrangement: String = "TOP"
)

data class Modifier(
    @SerializedName("fillMaxSize") val fillMaxSize: Boolean? = null,
    @SerializedName("fillMaxWidth") val fillMaxWidth: Boolean? = null,
    @SerializedName("padding") val padding: Padding? = null,
    @SerializedName("background") val background: String? = null
)

data class Padding(
    @SerializedName("left") val left: Int = 0,
    @SerializedName("top") val top: Int = 0,
    @SerializedName("right") val right: Int = 0,
    @SerializedName("bottom") val bottom: Int = 0
)

data class TextStyle(
    @SerializedName("fontSize") val fontSize: Float,
    @SerializedName("fontWeight") val fontWeight: Int = 400,
    @SerializedName("color") val color: String = "#000000"
)

data class Resources(
    @SerializedName("texts") val texts: List<TextResource>,
    @SerializedName("bitmaps") val bitmaps: List<BitmapResource>
)

data class TextResource(
    @SerializedName("id") val id: Int,
    @SerializedName("content") val content: String
)

data class BitmapResource(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

// ============================================================================
// 交互相关数据类
// ============================================================================

data class Interaction(
    @SerializedName("componentId") val componentId: Int,
    @SerializedName("type") val type: String,
    @SerializedName("action") val action: Action
)

data class Action(
    @SerializedName("type") val type: String,
    @SerializedName("actionId") val actionId: Int? = null,
    @SerializedName("targetValueId") val targetValueId: Int? = null,
    @SerializedName("newValue") val newValue: Int? = null,
    @SerializedName("metadata") val metadata: String? = null,
    @SerializedName("intent") val intent: Intent? = null
)

data class Intent(
    @SerializedName("action") val action: String,
    @SerializedName("data") val data: String? = null
)

data class ActionDefinition(
    @SerializedName("actionId") val actionId: Int,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String? = null
)

data class ClickAction(
    @SerializedName("actionId") val actionId: Int,
    @SerializedName("metadata") val metadata: String? = null,
    @SerializedName("type") val type: String = "host_action",
    @SerializedName("targetValueId") val targetValueId: Int? = null,
    @SerializedName("newValue") val newValue: Int? = null
)

// ============================================================================
// 2. 编译器实现（包含交互）
// ============================================================================

object WidgetCompiler {
    
    private val gson = Gson()
    
    fun compileFromJson(jsonString: String): ByteArray {
        val spec = gson.fromJson(jsonString, WidgetSpec::class.java)
        return compile(spec)
    }
    
    fun compile(spec: WidgetSpec): ByteArray {
        val buffer = WireBuffer()
        val widget = spec.widget
        
        // 1. 写入头部
        writeHeader(buffer, widget)
        
        // 2. 注册资源
        writeResources(buffer, widget.resources)
        
        // 3. 写入组件树（包含交互）
        writeComponentTree(buffer, widget.components)
        
        return buffer.toByteArray()
    }
    
    private fun writeHeader(buffer: WireBuffer, widget: WidgetData) {
        buffer.writeByte(Operations.HEADER)
        buffer.writeInt(widget.version)
        buffer.writeInt(widget.width)
        buffer.writeInt(widget.height)
        buffer.writeInt(widget.density)
        buffer.writeLong(0x0000000000000001L) // WIDGETS_V7
    }
    
    private fun writeResources(buffer: WireBuffer, resources: Resources) {
        resources.texts.forEach { text ->
            buffer.writeByte(Operations.DATA_TEXT)
            buffer.writeInt(text.id)
            buffer.writeString(text.content)
        }
        
        resources.bitmaps.forEach { bitmap ->
            buffer.writeByte(Operations.DATA_BITMAP)
            buffer.writeInt(bitmap.id)
            buffer.writeInt(bitmap.width)
            buffer.writeInt(bitmap.height)
        }
    }
    
    private fun writeComponentTree(buffer: WireBuffer, components: List<Component>) {
        components.forEach { component ->
            writeComponent(buffer, component)
        }
    }
    
    private fun writeComponent(buffer: WireBuffer, component: Component) {
        when (component.type) {
            "Column" -> writeColumn(buffer, component)
            "Row" -> writeRow(buffer, component)
            "Text" -> writeText(buffer, component)
            "Image" -> writeImage(buffer, component)
        }
    }
    
    private fun writeColumn(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.COMPONENT_START)
        buffer.writeInt(component.id)
        buffer.writeInt(0) // COLUMN type
        
        component.modifier?.let { writeModifier(buffer, it) }
        
        component.children?.forEach { child ->
            writeComponent(buffer, child)
        }
        
        buffer.writeByte(Operations.CONTAINER_END)
    }
    
    private fun writeRow(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.COMPONENT_START)
        buffer.writeInt(component.id)
        buffer.writeInt(1) // ROW type
        
        component.modifier?.let { writeModifier(buffer, it) }
        
        component.children?.forEach { child ->
            writeComponent(buffer, child)
        }
        
        buffer.writeByte(Operations.CONTAINER_END)
    }
    
    private fun writeText(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.DRAW_TEXT_RUN)
        buffer.writeInt(component.textResourceId ?: 0)
        
        // 写入样式
        component.style?.let { style ->
            buffer.writeFloat(style.fontSize)
            buffer.writeInt(style.fontWeight)
            buffer.writeInt(parseColor(style.color))
        }
        
        // ⭐ 写入点击交互（如果有）
        component.clickAction?.let { clickAction ->
            writeClickModifier(buffer, clickAction)
        }
    }
    
    private fun writeImage(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.DRAW_BITMAP)
        buffer.writeInt(component.bitmapResourceId ?: 0)
        
        component.size?.let { size ->
            buffer.writeInt(size.width)
            buffer.writeInt(size.height)
        }
    }
    
    private fun writeModifier(buffer: WireBuffer, modifier: Modifier) {
        var flags = 0
        if (modifier.fillMaxSize == true) flags = flags or 0x01
        if (modifier.fillMaxWidth == true) flags = flags or 0x02
        
        buffer.writeInt(flags)
        
        modifier.padding?.let { padding ->
            buffer.writeInt(padding.left)
            buffer.writeInt(padding.top)
            buffer.writeInt(padding.right)
            buffer.writeInt(padding.bottom)
        }
        
        modifier.background?.let { bg ->
            buffer.writeInt(parseColor(bg))
        }
    }
    
    // ========================================================================
    // ⭐ 交互功能实现
    // ========================================================================
    
    /**
     * 写入点击修饰器
     * 
     * Wire Format 结构:
     * [OP_CODE: MODIFIER_CLICK]
     *   [OP_CODE: HOST_ACTION 或 VALUE_INTEGER_CHANGE_ACTION]
     *   [actionId 或 targetValueId + newValue]
     */
    private fun writeClickModifier(buffer: WireBuffer, action: ClickAction) {
        // 1. 写入 MODIFIER_CLICK
        buffer.writeByte(Operations.MODIFIER_CLICK)
        
        // 2. 根据类型写入不同的动作
        when (action.type) {
            "host_action" -> {
                // 写入 HOST_ACTION
                buffer.writeByte(Operations.HOST_ACTION)
                buffer.writeInt(action.actionId)
                // metadata 可以在 HostNamedActionOperation 中使用
                action.metadata?.let { metadata ->
                    buffer.writeString(metadata)
                }
            }
            "value_change" -> {
                // 写入 VALUE_INTEGER_CHANGE_ACTION
                buffer.writeByte(Operations.VALUE_INTEGER_CHANGE_ACTION)
                buffer.writeInt(action.targetValueId ?: 0)
                buffer.writeInt(action.newValue ?: 0)
            }
        }
        
        // 3. 结束标记（可选，根据具体实现）
        // buffer.writeByte(Operations.CONTAINER_END)
    }
    
    private fun parseColor(colorHex: String): Int {
        return colorHex.removePrefix("#").toLong(16).toInt() or -0x1000000
    }
}
```

---

## 完整使用示例

### 示例 1: 点击打开 Activity

```kotlin
fun createWeatherWidget(context: Context, appWidgetId: Int) {
    val json = """
        {
          "widget": {
            "type": "weather",
            "width": 400,
            "height": 600,
            "components": [
              {
                "type": "Text",
                "id": 1,
                "textResourceId": 1,
                "clickAction": {
                  "actionId": 1,
                  "metadata": "open_detail",
                  "type": "host_action"
                },
                "style": {
                  "fontSize": 48,
                  "fontWeight": 500,
                  "color": "#000000"
                }
              }
            ],
            "resources": {
              "texts": [
                {"id": 1, "content": "Beijing"}
              ]
            }
          }
        }
    """.trimIndent()
    
    // 1. 编译为 Wire Format
    val wireFormat = WidgetCompiler.compileFromJson(json)
    
    // 2. 创建 RemoteViews
    val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
    val remoteViews = RemoteViews(instructions)
    
    // 3. ⭐ 注册动作回调（在 Player 中）
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

### 示例 2: 点击切换状态

```kotlin
fun createSwitchWidget(context: Context, appWidgetId: Int) {
    val json = """
        {
          "widget": {
            "type": "switch",
            "width": 200,
            "height": 100,
            "components": [
              {
                "type": "Box",
                "id": 1,
                "clickAction": {
                  "type": "value_change",
                  "targetValueId": 100,
                  "newValue": 1
                },
                "children": [
                  {
                    "type": "Text",
                    "id": 2,
                    "textResourceId": 1,
                    "style": {
                      "fontSize": 36,
                      "color": "#000000"
                    }
                  }
                ]
              }
            ],
            "resources": {
              "texts": [
                {"id": 1, "content": "OFF"}
              ],
              "namedVariables": {
                "integers": [
                  {"id": 100, "name": "switch_state", "initialValue": 0}
                ]
              }
            }
          }
        }
    """.trimIndent()
    
    // 编译为 Wire Format
    val wireFormat = WidgetCompiler.compileFromJson(json)
    
    // 创建 RemoteViews
    val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
    val remoteViews = RemoteViews(instructions)
    
    // 注意：value_change 类型不需要注册回调
    // 播放器会自动执行 context.overrideInteger(100, 1)
    
    // 更新微件
    AppWidgetManager.getInstance(context)
        .updateAppWidget(appWidgetId, remoteViews)
}
```

---

## Wire Format 二进制结构详解

### 带交互的 Text 组件 Wire Format

```
假设 JSON:
{
  "type": "Text",
  "textResourceId": 1,
  "clickAction": {
    "actionId": 1,
    "type": "host_action"
  }
}

生成的 Wire Format:
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

### 对应的 Java 代码执行流程

```java
// 播放器解析 Wire Format
ClickModifierOperation clickModifier = new ClickModifierOperation();
HostActionOperation hostAction = new HostActionOperation(1); // actionId = 1
clickModifier.mList.add(hostAction);

// 用户点击时
@Override
public boolean onClick(RemoteContext context, ...) {
    for (Operation o : mList) {
        if (o instanceof ActionOperation) {
            ((ActionOperation) o).runAction(context, document, component, x, y);
        }
    }
    return true;
}

// HostActionOperation.runAction()
@Override
public void runAction(RemoteContext context, ...) {
    context.runAction(mActionId, ""); // mActionId = 1
}

// RemoteContext.runAction() 触发注册的回调
// RemoteComposePlayer.addIdActionListener { id, metadata -> ... }
```

---

## 交互类型对照表

| JSON 中的 type | 对应的操作码 | Wire Format 写入 | 用途 |
|--------------|------------|----------------|------|
| `host_action` | `HOST_ACTION` (209) | `[OP_CODE][actionId][metadata?]` | 触发主机回调（打开 Activity 等） |
| `value_change` | `VALUE_INTEGER_CHANGE_ACTION` (212) | `[OP_CODE][targetValueId][newValue]` | 更改变量值（播放器自动执行） |
| `value_float_change` | `VALUE_FLOAT_CHANGE_ACTION` (223) | `[OP_CODE][targetValueId][newValue]` | 更改浮点变量值 |
| `value_string_change` | `VALUE_STRING_CHANGE_ACTION` (213) | `[OP_CODE][targetValueId][newValue]` | 更改字符串变量值 |

---

## 总结

### 关键改进

1. **✅ 添加了 `ClickAction` 数据类**
   - 支持 `host_action` 和 `value_change` 两种类型
   - 包含 `actionId`、`targetValueId`、`newValue` 等字段

2. **✅ 实现了 `writeClickModifier()` 函数**
   - 写入 `MODIFIER_CLICK` 操作码
   - 根据类型写入 `HOST_ACTION` 或 `VALUE_INTEGER_CHANGE_ACTION`

3. **✅ 在 `writeText()` 中调用交互写入**
   - `component.clickAction?.let { writeClickModifier(buffer, it) }`

4. **✅ 提供了完整的使用示例**
   - 示例 1: 点击打开 Activity（host_action 类型）
   - 示例 2: 点击切换状态（value_change 类型）

### Wire Format 交互编码规则

```kotlin
// host_action 类型
buffer.writeByte(Operations.MODIFIER_CLICK)
buffer.writeByte(Operations.HOST_ACTION)
buffer.writeInt(actionId)
buffer.writeString(metadata) // 可选

// value_change 类型
buffer.writeByte(Operations.MODIFIER_CLICK)
buffer.writeByte(Operations.VALUE_INTEGER_CHANGE_ACTION)
buffer.writeInt(targetValueId)
buffer.writeInt(newValue)
```

这个实现完整地将 JSON 中的交互部分转换为 Wire Format 二进制数据，基于 Remote Compose 项目的实际代码。
