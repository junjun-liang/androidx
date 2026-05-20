# LLM 生成 Wire Format 完整实现示例

本文档提供完整的 LLM 输出 JSON 示例和将 JSON 转换为 Wire Format 的实现代码。

---

## 步骤 2: LLM 生成结构化数据（JSON 示例）

### 完整 JSON 输出示例

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
        "layout": {
          "horizontalAlignment": "CENTER",
          "verticalArrangement": "TOP"
        },
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
            "style": {
              "fontSize": 48,
              "fontWeight": 500,
              "color": "#000000",
              "textAlign": "CENTER"
            },
            "modifier": {
              "padding": {
                "bottom": 16
              }
            }
          },
          {
            "type": "Row",
            "id": 3,
            "layout": {
              "horizontalArrangement": "SPACE_BETWEEN",
              "verticalAlignment": "CENTER"
            },
            "modifier": {
              "fillMaxWidth": true,
              "padding": {
                "top": 16,
                "bottom": 32
              }
            },
            "children": [
              {
                "type": "Text",
                "id": 4,
                "textResourceId": 2,
                "style": {
                  "fontSize": 128,
                  "fontWeight": 700,
                  "color": "#000000"
                }
              },
              {
                "type": "Image",
                "id": 5,
                "bitmapResourceId": 1,
                "size": {
                  "width": 192,
                  "height": 192
                }
              }
            ]
          },
          {
            "type": "Text",
            "id": 6,
            "textResourceId": 3,
            "style": {
              "fontSize": 36,
              "fontWeight": 400,
              "color": "#666666",
              "textAlign": "CENTER"
            }
          }
        ]
      }
    ],
    "resources": {
      "texts": [
        {
          "id": 1,
          "content": "Beijing",
          "description": "City name"
        },
        {
          "id": 2,
          "content": "24°C",
          "description": "Temperature"
        },
        {
          "id": 3,
          "content": "Sunny",
          "description": "Weather condition"
        }
      ],
      "bitmaps": [
        {
          "id": 1,
          "name": "sunny_icon",
          "width": 192,
          "height": 192,
          "format": "PNG",
          "description": "Sunny weather icon"
        }
      ]
    },
    "interactions": [
      {
        "componentId": 1,
        "type": "click",
        "action": {
          "type": "open_activity",
          "intent": {
            "action": "android.intent.action.VIEW",
            "data": "weather://detail"
          }
        }
      }
    ]
  }
}
```

---

## 步骤 3: 转换为操作码序列（Kotlin 实现）

### 完整实现代码

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
    @SerializedName("interactions") val interactions: List<Interaction>? = null
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
    @SerializedName("size") val size: Size? = null
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
// 2. 编译器实现
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
        
        // 3. 写入组件树
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
        
        component.style?.let { style ->
            buffer.writeFloat(style.fontSize)
            buffer.writeInt(style.fontWeight)
            buffer.writeInt(parseColor(style.color))
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
    
    private fun parseColor(colorHex: String): Int {
        return colorHex.removePrefix("#").toLong(16).toInt() or -0x1000000
    }
}

// ============================================================================
// 3. 使用示例
// ============================================================================

fun createWeatherWidget(context: Context, appWidgetId: Int) {
    val json = """
        {
          "widget": {
            "type": "weather",
            "width": 400,
            "height": 600,
            "components": [
              {
                "type": "Column",
                "id": 1,
                "modifier": {
                  "fillMaxSize": true,
                  "background": "#FFFFFF"
                },
                "children": [
                  {
                    "type": "Text",
                    "id": 2,
                    "textResourceId": 1,
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
            }
          }
        }
    """.trimIndent()
    
    // 编译为 Wire Format
    val wireFormat = WidgetCompiler.compileFromJson(json)
    
    // 创建 RemoteViews
    val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
    val remoteViews = RemoteViews(instructions)
    
    // 更新微件
    AppWidgetManager.getInstance(context)
        .updateAppWidget(appWidgetId, remoteViews)
}
```

---

## 完整工作流程

```
用户输入
  ↓
LLM 生成 JSON
  ↓
WidgetCompiler.compileFromJson(json)
  ↓
WireBuffer 序列化
  ↓
byte[] wireFormat
  ↓
RemoteViews.DrawInstructions
  ↓
AppWidgetManager.updateAppWidget()
  ↓
桌面微件更新完成
```

---

## 总结

本文档提供了：

1. **完整的 LLM JSON 输出示例** - 包含所有字段和嵌套结构
2. **Kotlin 实现代码** - 完整的 JSON 到 Wire Format 编译器
3. **使用示例** - 从 JSON 到 RemoteViews 的完整流程

这个实现展示了如何自定义中间转换层，将 LLM 的结构化输出转换为 Remote Compose 可以执行的 Wire Format 二进制数据。
