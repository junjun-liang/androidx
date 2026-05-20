# LLM 生成 Wire Format 交互功能实现指南

本文档详细说明如何在 Wire Format 中实现交互功能（点击、动作等），基于 Remote Compose 项目的实际代码实现。

---

## 核心概念

### Wire Format 交互机制

Wire Format 中的交互**不是直接存储 Intent 或复杂对象**，而是通过**动作 ID（actionId）**引用预注册的回调函数。

**工作流程**：

```
1. 应用层注册动作回调
   remotePlayer.addIdActionListener { id, metadata -> ... }

2. Wire Format 存储 actionId
   buffer.writeByte(Operations.HOST_ACTION)
   buffer.writeInt(actionId)

3. 用户点击触发
   ClickModifierOperation.onClick()
     → ActionOperation.runAction()
       → context.runAction(actionId, metadata)
         → 回调注册的监听器
```

---

## 系统提供的交互操作码

### 1. MODIFIER_CLICK (点击修饰器)

**文件**: `ClickModifierOperation.java`
**操作码**: `Operations.MODIFIER_CLICK`

```java
public class ClickModifierOperation extends PaintOperation {
    private static final int OP_CODE = Operations.MODIFIER_CLICK;
    
    @NonNull public ArrayList<Operation> mList = new ArrayList<>();
    
    @Override
    public boolean onClick(@NonNull RemoteContext context, ...) {
        // 执行所有动作
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).runAction(context, document, component, x, y);
            }
        }
        context.hapticEffect(3); // 触觉反馈
        return true;
    }
}
```

**Wire Format 编码**：
```
[OP_CODE: MODIFIER_CLICK]
[子操作列表]
  - ActionOperation 1
  - ActionOperation 2
  ...
```

### 2. HOST_ACTION (主机动作)

**文件**: `HostActionOperation.java`
**操作码**: `Operations.HOST_ACTION`

```java
public class HostActionOperation extends Operation implements ActionOperation {
    private static final int OP_CODE = Operations.HOST_ACTION;
    int mActionId = -1;
    
    @Override
    public void runAction(@NonNull RemoteContext context, ...) {
        context.runAction(mActionId, "");
    }
    
    public static void apply(@NonNull WireBuffer buffer, int actionId) {
        buffer.start(OP_CODE);
        buffer.writeInt(actionId);
    }
}
```

**Wire Format 编码**：
```
[OP_CODE: HOST_ACTION]
[actionId: int]
```

### 3. VALUE_INTEGER_CHANGE_ACTION (值变更动作)

**文件**: `ValueIntegerChangeActionOperation.java`
**操作码**: `Operations.VALUE_INTEGER_CHANGE_ACTION`

```java
public class ValueIntegerChangeActionOperation extends Operation implements ActionOperation {
    private static final int OP_CODE = Operations.VALUE_INTEGER_CHANGE_ACTION;
    int mTargetValueId = -1;
    int mValue = -1;
    
    @Override
    public void runAction(@NonNull RemoteContext context, ...) {
        context.overrideInteger(mTargetValueId, mValue);
    }
    
    public static void apply(@NonNull WireBuffer buffer, int valueId, int value) {
        buffer.start(OP_CODE);
        buffer.writeInt(valueId);
        buffer.writeInt(value);
    }
}
```

**Wire Format 编码**：
```
[OP_CODE: VALUE_INTEGER_CHANGE_ACTION]
[targetValueId: int]
[newValue: int]
```

---

## 完整的交互实现示例

### 示例 1: 点击打开 Activity

#### 步骤 1: 应用层注册动作回调

```kotlin
// 在应用中注册动作监听器
remoteComposePlayer.addIdActionListener { actionId, metadata ->
    when (actionId) {
        1 -> {
            // 打开天气详情页面
            val intent = Intent("android.intent.action.VIEW").apply {
                data = Uri.parse("weather://detail")
            }
            context.startActivity(intent)
        }
        2 -> {
            // 刷新天气数据
            val broadcastIntent = Intent("com.example.REFRESH_WEATHER")
            context.sendBroadcast(broadcastIntent)
        }
    }
}
```

#### 步骤 2: LLM 生成 JSON 规格

```json
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
            "clickAction": {
              "actionId": 1,
              "metadata": "open_detail"
            },
            "style": {
              "fontSize": 48,
              "fontWeight": 500,
              "color": "#000000"
            }
          }
        ]
      }
    ],
    "resources": {
      "texts": [
        {"id": 1, "content": "Beijing"}
      ]
    },
    "actions": [
      {
        "actionId": 1,
        "type": "open_activity",
        "description": "Open weather detail page"
      },
      {
        "actionId": 2,
        "type": "send_broadcast",
        "description": "Refresh weather data"
      }
    ]
  }
}
```

#### 步骤 3: 编译器实现

```kotlin
object WidgetCompiler {
    
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
    
    private fun writeComponent(buffer: WireBuffer, component: Component) {
        when (component.type) {
            "Column" -> writeColumn(buffer, component)
            "Text" -> writeText(buffer, component)
            // ...
        }
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
        
        // 写入点击修饰器（如果有）
        component.clickAction?.let { action ->
            writeClickModifier(buffer, action)
        }
    }
    
    private fun writeClickModifier(buffer: WireBuffer, action: ClickAction) {
        // 写入 MODIFIER_CLICK
        buffer.writeByte(Operations.MODIFIER_CLICK)
        
        // 写入 HOST_ACTION
        buffer.writeByte(Operations.HOST_ACTION)
        buffer.writeInt(action.actionId)
        
        // 写入结束标记（如果需要）
        buffer.writeByte(Operations.CONTAINER_END)
    }
}
```

#### 步骤 4: 使用示例

```kotlin
fun createWeatherWidget(context: Context, appWidgetId: Int) {
    val json = """
        {
          "widget": {
            "type": "weather",
            "width": 400,
            "height": 600,
            "components": [...],
            "resources": {...},
            "actions": [
              {"actionId": 1, "type": "open_activity"},
              {"actionId": 2, "type": "send_broadcast"}
            ]
          }
        }
    """.trimIndent()
    
    // 编译为 Wire Format
    val wireFormat = WidgetCompiler.compileFromJson(json)
    
    // 创建 RemoteViews
    val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
    val remoteViews = RemoteViews(instructions)
    
    // 注册动作回调（在 Player 中）
    remoteComposePlayer.addIdActionListener { actionId, metadata ->
        when (actionId) {
            1 -> context.startActivity(Intent("android.intent.action.VIEW")
                .setData(Uri.parse("weather://detail")))
            2 -> context.sendBroadcast(Intent("com.example.REFRESH_WEATHER"))
        }
    }
    
    // 更新微件
    AppWidgetManager.getInstance(context)
        .updateAppWidget(appWidgetId, remoteViews)
}
```

---

### 示例 2: 点击切换状态

这个示例展示如何使用 `VALUE_INTEGER_CHANGE_ACTION` 实现状态切换。

#### JSON 规格

```json
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
          "newValue": 1,
          "toggle": true
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
```

#### 编译器实现

```kotlin
private fun writeClickModifier(
    buffer: WireBuffer, 
    action: ClickAction
) {
    buffer.writeByte(Operations.MODIFIER_CLICK)
    
    when (action.type) {
        "host_action" -> {
            buffer.writeByte(Operations.HOST_ACTION)
            buffer.writeInt(action.actionId)
        }
        "value_change" -> {
            buffer.writeByte(Operations.VALUE_INTEGER_CHANGE_ACTION)
            buffer.writeInt(action.targetValueId)
            buffer.writeInt(action.newValue)
        }
    }
}
```

---

## 交互类型对照表

| JSON 中的 type | 对应的操作码 | 用途 | 参数 |
|--------------|------------|------|------|
| `host_action` | `HOST_ACTION` | 触发主机回调（打开 Activity、发送广播等） | `actionId`, `metadata` |
| `value_change` | `VALUE_INTEGER_CHANGE_ACTION` | 更改变量值 | `targetValueId`, `newValue` |
| `value_float_change` | `VALUE_FLOAT_CHANGE_ACTION` | 更改浮点变量值 | `targetValueId`, `newValue` |
| `value_string_change` | `VALUE_STRING_CHANGE_ACTION` | 更改字符串变量值 | `targetValueId`, `newValue` |

---

## 完整的 JSON Schema（包含交互）

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "WidgetSpec",
  "type": "object",
  "properties": {
    "widget": {
      "type": "object",
      "properties": {
        "type": {"type": "string"},
        "width": {"type": "integer"},
        "height": {"type": "integer"},
        "components": {
          "type": "array",
          "items": {"$ref": "#/definitions/Component"}
        },
        "resources": {"$ref": "#/definitions/Resources"},
        "actions": {
          "type": "array",
          "items": {"$ref": "#/definitions/ActionDefinition"}
        }
      }
    }
  },
  "definitions": {
    "Component": {
      "type": "object",
      "properties": {
        "type": {"type": "string"},
        "id": {"type": "integer"},
        "clickAction": {"$ref": "#/definitions/ClickAction"}
      }
    },
    "ClickAction": {
      "type": "object",
      "oneOf": [
        {
          "required": ["type", "actionId"],
          "properties": {
            "type": {"const": "host_action"},
            "actionId": {"type": "integer"},
            "metadata": {"type": "string"}
          }
        },
        {
          "required": ["type", "targetValueId", "newValue"],
          "properties": {
            "type": {"const": "value_change"},
            "targetValueId": {"type": "integer"},
            "newValue": {"type": "integer"},
            "toggle": {"type": "boolean"}
          }
        }
      ]
    },
    "ActionDefinition": {
      "type": "object",
      "required": ["actionId", "type"],
      "properties": {
        "actionId": {"type": "integer"},
        "type": {"type": "string"},
        "description": {"type": "string"}
      }
    },
    "Resources": {
      "type": "object",
      "properties": {
        "texts": {"type": "array"},
        "bitmaps": {"type": "array"},
        "namedVariables": {
          "type": "object",
          "properties": {
            "integers": {"type": "array"},
            "floats": {"type": "array"},
            "strings": {"type": "array"}
          }
        }
      }
    }
  }
}
```

---

## 总结

### 关键要点

1. **Wire Format 不直接存储 Intent**
   - 只存储 `actionId`（整数）
   - 复杂逻辑在应用层回调中实现

2. **交互在创建时注册**
   - 通过 `RemoteComposePlayer.addIdActionListener()` 注册
   - 播放器调用 `context.runAction(actionId, metadata)` 触发

3. **支持多种动作类型**
   - `HOST_ACTION`: 主机回调
   - `VALUE_INTEGER_CHANGE_ACTION`: 变量值变更
   - `VALUE_FLOAT_CHANGE_ACTION`: 浮点值变更
   - `VALUE_STRING_CHANGE_ACTION`: 字符串变更

4. **ClickModifierOperation 包含动作列表**
   - 一次点击可以触发多个动作
   - 动作按顺序执行

### 实现建议

**简单场景**（打开页面）：
```kotlin
// JSON
"clickAction": {"type": "host_action", "actionId": 1}

// 注册
player.addIdActionListener { id, _ ->
    if (id == 1) startActivity(...)
}
```

**复杂场景**（状态管理）：
```kotlin
// JSON
"clickAction": {
    "type": "value_change",
    "targetValueId": 100,
    "newValue": 1
}

// 播放器自动更新变量，无需回调
```

这个实现基于 Remote Compose 项目的实际代码，确保了准确性和可实现性。
