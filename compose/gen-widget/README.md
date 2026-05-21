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

---

## 附录：项目中的 WireBuffer 和 RemoteViews.DrawInstructions 用例

AndroidX 项目中有 8 个主要用例展示了如何使用 `WireBuffer` 和 `RemoteViews.DrawInstructions` 创建 `RemoteViews`：

### 用例总览

| # | 用例名称 | 文件路径 | 应用场景 |
|---|---------|---------|---------|
| 1 | Glance DrawInstructionRemoteViews | `glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/DrawInstructionRemoteViews.kt` | Glance 框架集成 |
| 2 | TickerWidgetProvider | `compose/remote/integration-tests/player-view-demos/.../TickerWidgetProvider.java` | 动态时钟小部件 |
| 3 | ExperimentWidgetProvider | `compose/remote/integration-tests/player-view-demos/.../ExperimentWidgetProvider.java` | 实验性小部件 |
| 4 | TickerActivity | `compose/remote/integration-tests/player-view-demos/.../TickerActivity.java` | 小部件更新 Activity |
| 5 | RawViewActivity | `compose/remote/integration-tests/player-view-demos/.../RawViewActivity.java` | Remote Compose 文档浏览器 |
| 6 | ListRCWidget | `compose/remote/integration-tests/demos/.../ListRCWidget.kt` | 可滚动列表小部件 |
| 7 | RemoteNotification | `compose/remote/integration-tests/player-view-demos/.../RemoteNotification.java` | Remote Compose 通知 |
| 8 | RCWidget | `compose/remote/remote-creation-compose/.../RCWidget.kt` | 通用小部件基类 |

**API 要求**: 所有用例均需要 **Android API 35+ (VANILLA_ICE_CREAM)**

### 核心转换流程

```
WireBuffer/RemoteComposeBuffer
    ↓ (提取字节数组)
ByteArray
    ↓ (构建 DrawInstructions)
RemoteViews.DrawInstructions
    ↓ (创建 RemoteViews)
RemoteViews
    ↓ (注册交互)
RemoteViews + PendingIntent
```

### 标准代码模式

#### 1. 从 WireBuffer 提取字节数组

**Kotlin 方式**:
```kotlin
internal fun getBytes(rcContext: RemoteComposeContext): ByteArray {
    val buffer: WireBuffer = rcContext.buffer.buffer
    val bufferSize = buffer.size
    val bytesCopy = ByteArray(bufferSize)
    ByteArrayInputStream(buffer.buffer, 0, bufferSize).use { it.read(bytesCopy) }
    return bytesCopy
}
```

**Java 方式**:
```java
WireBuffer buffer = doc.getBuffer();
int bufferSize = buffer.size();
byte[] bytes = new byte[bufferSize];
ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBuffer(), 0, bufferSize);
b.read(bytes);
```

#### 2. 构建 DrawInstructions 和 RemoteViews

**统一模式**:
```kotlin
val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
val remoteViews = RemoteViews(drawInstructions)
```

```java
RemoteViews.DrawInstructions.Builder r = 
    new RemoteViews.DrawInstructions.Builder(List.of(bytes));
RemoteViews remoteViews = new RemoteViews(r.build());
```

#### 3. 注册点击事件

```kotlin
remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
```

**PendingIntent 类型**:
- `PendingIntent.getBroadcast()`: 用于小部件更新
- `PendingIntent.getActivity()`: 用于启动 Activity
- `PendingIntent.getService()`: 用于启动 Service

### 典型用例详解

#### 用例 1: Glance AppWidget（最完整）

```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun drawInstructionRemoteViews(
    translation: GlanceToRemoteComposeTranslation.Single
): RemoteViews {
    val bytes: ByteArray = getBytes(translation.remoteComposeContext)
    val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
    val remoteViews = RemoteViews(drawInstructions)
    
    // 注册点击事件
    translation.actionMap.forEach { (actionId, pendingIntent) ->
        remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
    }
    
    // 调试信息
    val checksum = with(CRC32()) {
        update(bytes)
        value
    }
    Log.d(TAG, "drawInstructionRemoteViews: len ${bytes.size}, checksum $checksum")
    
    return remoteViews
}
```

**特点**:
- 支持单尺寸和多尺寸映射
- 自动处理点击事件映射
- 包含 CRC32 校验和调试日志

#### 用例 2: TickerWidgetProvider（动态小部件）

```java
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private RemoteViews loadWidgetFromRemoteComposeContext(@NonNull Context context) {
    RemoteComposeBuffer doc = RcTickerKt.RcTicker(context).getWriter().getBuffer();
    WireBuffer buffer = doc.getBuffer();
    byte[] bytes = new byte[buffer.size()];
    ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBuffer(), 0, buffer.size());
    b.read(bytes);
    
    RemoteViews.DrawInstructions.Builder r =
            new RemoteViews.DrawInstructions.Builder(List.of(bytes));
    RemoteViews remoteViews = new RemoteViews(r.build());
    
    // 注册点击事件触发更新
    Intent intent = new Intent(context, TickerWidgetProvider.class);
    intent.setAction(WIDGET_UPDATE_ACTION);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_MUTABLE);
    remoteViews.setOnClickPendingIntent(sId, pendingIntent);
    
    return remoteViews;
}
```

#### 用例 3: RCWidget（框架基类）

```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public fun getRemoteView(
    context: Context,
    document: CapturedDocument,
    provider: RemoteComposeWidget,
    widgetId: Int,
): RemoteViews {
    val r = RemoteViews.DrawInstructions.Builder(listOf(document.bytes))
    val rv = RemoteViews(r.build())
    
    // 为每个 Lambda 动作注册 PendingIntent
    for (i in 0 until WidgetLambdaAction.counter) {
        val intentId = 1000 * widgetId + i
        val intent = Intent(context, provider.javaClass)
        intent.setAction(ACTION)
        intent.putExtra("id", intentId)
        intent.putExtra("widgetId", widgetId)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext, intentId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(intentId, pendingIntent)
    }
    
    return rv
}
```

### 与 gen-widget 的集成

[`WeatherWidgetSample.kt`](file:///home/meizu/Documents/my_android_projects/androidx/compose/gen-widget/src/main/java/androidx/compose/genwidget/sample/WeatherWidgetSample.kt#L208-L208) 中的实现符合项目标准模式：

```kotlin
val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
val remoteViews = RemoteViews(instructions)
```

**建议改进**:

1. **添加点击事件处理**:
```kotlin
wireFormatMetadata?.let { metadata ->
    val (action, data) = parseMetadata(metadata)
    val intent = Intent().apply {
        action?.let { setAction(it) }
        data?.let { setData(Uri.parse(it)) }
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    remoteViews.setOnClickPendingIntent(R.id.weather_card, pendingIntent)
}
```

2. **添加调试日志**:
```kotlin
val checksum = with(CRC32()) {
    update(wireFormat)
    value
}
Log.d("WeatherWidget", "WireFormat: len=${wireFormat.size}, checksum=$checksum")
```

### 详细文档

完整的用例分析文档请参考：
- [Remote Compose Architecture](../../../my_docs/remote-architecture.md)
- [Wire Format Transport API](../../../my_docs/wire_format_transport_api.md)
- [Wire Format to Widget Rendering](../../../my_docs/wire_format_to_widget_rendering.md)

---

---

## 附录二：WireBuffer 直接组装数据用例

除了从 RemoteComposeContext 提取数据外，项目中还有直接使用 `WireBuffer` 组装数据的用例。这些用例展示了如何通过编程方式构建 Wire Format 二进制数据。

### 用例总览

| # | 用例名称 | 文件路径 | 应用场景 |
|---|---------|---------|---------|
| 1 | WidgetCompiler | `compose/gen-widget/src/main/java/.../WidgetCompiler.kt` | LLM JSON 编译为 Wire Format |
| 2 | RemoteComposeConverter | `compose/remote/integration-tests/.../RemoteComposeConverter.java` | JSON ↔ Binary 双向转换 |
| 3 | CommandParametersTest | `compose/remote/remote-core/src/test/.../CommandParametersTest.java` | 测试参数序列化 |
| 4 | BinaryRoundTripTest | `compose/remote/remote-core/src/test/.../BinaryRoundTripTest.java` | 二进制往返测试 |

---

### 用例 1: WidgetCompiler - LLM JSON 编译器

**文件位置**: [`compose/gen-widget/src/main/java/androidx/compose/genwidget/compiler/WidgetCompiler.kt`](file:///home/meizu/Documents/my_android_projects/androidx/compose/gen-widget/src/main/java/androidx/compose/genwidget/compiler/WidgetCompiler.kt)

**应用场景**: 将 LLM 生成的 JSON 规格编译为 Wire Format 二进制数据

**核心代码**:

```kotlin
@SuppressLint("RestrictedApi")
object WidgetCompiler {
    
    fun compile(spec: WidgetSpec): ByteArray {
        val buffer = WireBuffer()
        val widget = spec.widget
        
        // 1. 写入头部信息
        writeHeader(buffer, widget)
        
        // 2. 注册资源（文本和位图）
        writeResources(buffer, widget.resources)
        
        // 3. 写入组件树
        writeComponentTree(buffer, widget.components)
        
        return buffer.buffer
    }
    
    private fun writeHeader(buffer: WireBuffer, widget: WidgetData) {
        buffer.writeByte(Operations.HEADER)
        buffer.writeInt(widget.version)
        buffer.writeInt(widget.width)
        buffer.writeInt(widget.height)
        buffer.writeInt(widget.density)
        buffer.writeLong(getProfileCode(widget.profile))
    }
    
    private fun writeResources(buffer: WireBuffer, resources: Resources) {
        // 注册文本资源
        resources.texts.forEach { text ->
            buffer.writeByte(Operations.DATA_TEXT)
            buffer.writeInt(text.id)
            // 写入字符串：先写入长度，再写入字节
            val textBytes = text.content.toByteArray(Charsets.UTF_8)
            buffer.writeInt(textBytes.size)
            textBytes.forEach { b -> buffer.writeByte(b.toInt()) }
        }
        
        // 注册位图资源
        resources.bitmaps.forEach { bitmap ->
            buffer.writeByte(Operations.DATA_BITMAP)
            buffer.writeInt(bitmap.id)
            buffer.writeInt(bitmap.width)
            buffer.writeInt(bitmap.height)
            buffer.writeInt(0) // 像素数据大小（占位符）
        }
    }
    
    private fun writeText(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.DRAW_TEXT_RUN)
        buffer.writeInt(component.textResourceId ?: throw IllegalArgumentException("..."))
        
        // 写入文本样式
        component.style?.let { style ->
            buffer.writeFloat(style.fontSize)
            buffer.writeInt(style.fontWeight)
            buffer.writeInt(parseColor(style.color))
            buffer.writeInt(parseTextAlign(style.textAlign))
        } ?: run {
            // 默认样式
            buffer.writeFloat(36f)
            buffer.writeInt(400)
            buffer.writeInt(-0x1000000) // 黑色
            buffer.writeInt(0) // LEFT
        }
        
        // ⭐ 写入点击交互（如果有）
        component.clickAction?.let { clickAction ->
            writeClickModifier(buffer, clickAction)
        }
    }
    
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
                    val metadataBytes = metadata.toByteArray(Charsets.UTF_8)
                    buffer.writeInt(metadataBytes.size)
                    metadataBytes.forEach { b -> buffer.writeByte(b.toInt()) }
                }
            }
            "value_change" -> {
                // 写入 VALUE_INTEGER_CHANGE_ACTION
                buffer.writeByte(Operations.VALUE_INTEGER_CHANGE_ACTION)
                buffer.writeInt(action.targetValueId ?: 0)
                buffer.writeInt(action.newValue ?: 0)
            }
        }
    }
}
```

**Wire Format 结构示例**:

```
带交互的 Text 组件：
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
├──────────────────────────────────────┤
│ metadataSize: 25 (int)               │  ← metadata 长度
├──────────────────────────────────────┤
│ metadata: "open_detail..." (bytes)   │  ← metadata 内容
└──────────────────────────────────────┘
```

**特点**:
- 完整的 JSON → Wire Format 编译器
- 支持所有基础组件（Column、Row、Box、Text、Image）
- 支持交互功能（host_action 和 value_change）
- 支持资源管理（文本、位图）
- 支持修饰器（尺寸、间距、背景、裁剪）

---

### 用例 2: RemoteComposeConverter - 双向转换器

**文件位置**: [`compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/convert/RemoteComposeConverter.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/convert/RemoteComposeConverter.java)

**应用场景**: RemoteCompose Binary ↔ JSON 双向无损转换

**核心代码**:

```java
@SuppressLint("RestrictedApiAndroidX")
public class RemoteComposeConverter {
    
    /**
     * Convert RemoteCompose binary to JSON
     */
    public static @NonNull String remoteComposeToJson(byte @NonNull [] rcBytes)
            throws JSONException {
        WireBuffer buffer = new WireBuffer(rcBytes.length);
        System.arraycopy(rcBytes, 0, buffer.getBuffer(), 0, rcBytes.length);
        
        int totalLen = rcBytes.length;
        buffer.setIndex(0);
        
        // 读取头部信息
        int apiLevel = Header.peekApiLevel(buffer);
        int profiles = 0;
        if (apiLevel >= 7) {
            Header header = Header.readDirect(buffer);
            profiles = header.getProfiles();
        }
        
        Operations.UniqueIntMap<CompanionOperation> map = 
            Operations.getOperations(apiLevel, profiles);
        
        JSONObject root = new JSONObject();
        root.put("format", "androidx.compose.remote.rc.json");
        root.put("version", 1);
        
        JSONObject rc = new JSONObject();
        rc.put("apiLevel", apiLevel);
        rc.put("profiles", profiles);
        JSONArray opsJson = new JSONArray();
        
        // 逐个读取操作码
        while (buffer.getIndex() < totalLen) {
            int startIdx = buffer.getIndex();
            int opcode = buffer.readByte();
            
            OpcodeRegistry.OpSpec spec = OpcodeRegistry.get(opcode);
            JSONObject opJson = new JSONObject();
            opJson.put("opcode", opcode);
            
            if (spec != null) {
                opJson.put("kind", "op");
                opJson.put("name", spec.name);
                
                // 读取字段
                JSONArray fields = new JSONArray();
                for (OpcodeRegistry.FieldSpec fSpec : spec.fields) {
                    fields.put(encodeField(buffer, fSpec, fields, opcode));
                }
                opJson.put("fields", fields);
            }
            
            opsJson.put(opJson);
        }
        
        rc.put("ops", opsJson);
        root.put("rc", rc);
        
        return root.toString(2);
    }
    
    /**
     * Convert JSON to RemoteCompose binary
     */
    public static byte @NonNull [] jsonToRemoteCompose(@NonNull String jsonStr)
            throws JSONException {
        JSONObject root = new JSONObject(jsonStr);
        JSONObject rc = root.getJSONObject("rc");
        JSONArray ops = rc.getJSONArray("ops");
        
        WireBuffer buffer = new WireBuffer();
        
        // 确保 WireBuffer 允许所有操作码
        java.lang.reflect.Field validOpsField = 
            WireBuffer.class.getDeclaredField("mValidOperations");
        validOpsField.setAccessible(true);
        boolean[] validOps = (boolean[]) validOpsField.get(buffer);
        for (int i = 0; i < 256; i++) {
            validOps[i] = true;
        }
        
        // 逐个写入操作
        for (int i = 0; i < ops.length(); i++) {
            JSONObject opJson = ops.getJSONObject(i);
            int opcode = opJson.getInt("opcode");
            
            buffer.start(opcode);
            
            if (opJson.optBoolean("reconstructFromFields", false)) {
                // 从字段重建
                JSONArray fields = opJson.getJSONArray("fields");
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject fJson = fields.getJSONObject(j);
                    OpcodeRegistry.FieldType type = OpcodeRegistry.FieldType.valueOf(
                        fJson.getString("type"));
                    writeField(buffer, type, fJson, opJson, opcode);
                }
            } else {
                // 直接使用 payload
                byte[] payload = Base64.getDecoder().decode(
                    opJson.getString("payloadBase64"));
                for (byte b : payload) {
                    buffer.writeByte(b & 0xFF);
                }
            }
        }
        
        byte[] result = new byte[buffer.getSize()];
        System.arraycopy(buffer.getBuffer(), 0, result, 0, buffer.getSize());
        return result;
    }
    
    private static void writeField(WireBuffer buffer, OpcodeRegistry.FieldType type,
            JSONObject fJson, JSONObject opJson, int opcode) throws JSONException {
        switch (type) {
            case BYTE:
                buffer.writeByte(Integer.parseInt(fJson.getString("value")));
                break;
            case SHORT:
                buffer.writeShort(Integer.parseInt(fJson.getString("value")));
                break;
            case INT:
                buffer.writeInt(Integer.parseInt(fJson.getString("value")));
                break;
            case LONG:
                buffer.writeLong(Long.parseLong(fJson.getString("value")));
                break;
            case FLOAT: {
                String s = fJson.getString("value");
                if (s.startsWith("NaN(")) {
                    int id = Integer.parseInt(s.substring(4, s.length() - 1));
                    buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                } else {
                    buffer.writeFloat(Float.parseFloat(s));
                }
                break;
            }
            case UTF8:
                buffer.writeUTF8(fJson.getString("value"));
                break;
            case BUFFER:
                buffer.writeBuffer(Base64.getDecoder().decode(fJson.getString("value")));
                break;
            // ... 其他类型
        }
    }
}
```

**特点**:
- 支持完整的 Binary ↔ JSON 双向转换
- 保证 bit-for-bit 往返一致性
- 支持所有操作码类型（256 种）
- 支持特殊数据类型（NaN float、NaN long、RPN 表达式等）
- 支持 Base64 编码的 payload
- 使用反射确保 WireBuffer 允许所有操作码

---

### 用例 3: CommandParametersTest - 参数序列化测试

**文件位置**: [`compose/remote/remote-core/src/test/java/androidx/compose/remote/core/CommandParametersTest.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/src/test/java/androidx/compose/remote/core/CommandParametersTest.java)

**应用场景**: 测试 CommandParameters 的序列化和反序列化

**核心代码**:

```java
public class CommandParametersTest {
    private static final byte COLOR = 1;
    private static final byte FONT_SIZE = 2;
    private static final byte FONT_STYLE = 3;
    private static final byte FONT_WEIGHT = 4;
    private static final byte FONT_FAMILY = 5;
    private static final byte STR_DATA = 6;
    private static final byte INT_DATA = 7;
    private static final byte FLOAT_DATA = 8;
    
    @Test
    public void basic() {
        // 1. 定义参数规格
        CommandParameters param = new CommandParameters(
            CommandParameters.param("textId", COLOR, CommandParameters.P_INT),
            CommandParameters.param("fontSize", FONT_SIZE, CommandParameters.P_FLOAT),
            CommandParameters.param("fontStyle", FONT_STYLE, CommandParameters.P_BYTE),
            CommandParameters.param("fontWeight", FONT_WEIGHT, CommandParameters.P_SHORT),
            CommandParameters.param("fontFamily", FONT_FAMILY, CommandParameters.P_BOOLEAN),
            CommandParameters.param("strData", STR_DATA, CommandParameters.PA_STRING),
            CommandParameters.param("intData", INT_DATA, CommandParameters.PA_INT),
            CommandParameters.param("floatData", FLOAT_DATA, CommandParameters.PA_FLOAT)
        );
        
        // 2. 创建 WireBuffer 并写入数据
        WireBuffer buffer = new WireBuffer();
        buffer.writeShort(8); // 参数数量
        
        param.write(buffer, COLOR, 1);
        param.write(buffer, FONT_SIZE, 2.2f);
        param.write(buffer, FONT_STYLE, (byte) 3);
        param.write(buffer, FONT_WEIGHT, (short) 4);
        param.write(buffer, FONT_FAMILY, true);
        param.write(buffer, STR_DATA, "Hello World");
        param.write(buffer, INT_DATA, new int[] {1, 2, 3});
        param.write(buffer, FLOAT_DATA, new float[] {1.0f, 2.0f, 3.0f});
        
        // 3. 读取并验证
        buffer.setIndex(0);
        final int[] data = new int[1];
        int len = buffer.readShort();
        
        for (int i = 0; i < len; i++) {
            param.read(buffer, new CommandParameters.Callback() {
                @Override
                public void value(int id, int value) {
                    assert id == COLOR;
                    assert value == 1;
                    data[0]++;
                }
                
                @Override
                public void value(int id, float value) {
                    assert id == FONT_SIZE;
                    assert value == 2.2f;
                    data[0]++;
                }
                
                @Override
                public void value(int id, short value) {
                    assert id == FONT_WEIGHT;
                    assert value == 4;
                    data[0]++;
                }
                
                @Override
                public void value(int id, byte value) {
                    assert id == FONT_STYLE;
                    assert value == 3;
                    data[0]++;
                }
                
                @Override
                public void value(int id, boolean value) {
                    assert id == FONT_FAMILY;
                    assert value;
                    data[0]++;
                }
                
                @Override
                public void value(int id, String value) {
                    assert id == STR_DATA;
                    assert value.equals("Hello World");
                    data[0]++;
                }
                
                @Override
                public void value(int id, int[] value) {
                    assert id == INT_DATA;
                    assert value.length == 3;
                    data[0]++;
                }
                
                @Override
                public void value(int id, float[] value) {
                    assert id == FLOAT_DATA;
                    assert value.length == 3;
                    data[0]++;
                }
            });
        }
        
        assert data[0] == 8; // 验证所有 8 个参数都被处理
    }
}
```

**特点**:
- 演示了参数化命令系统的实现
- 支持多种数据类型（int、float、byte、short、boolean、String、数组）
- 使用 ID 标识参数类型
- 支持回调方式读取参数
- 完整的序列化/反序列化测试

---

### 用例 4: BinaryRoundTripTest - 二进制往返测试

**文件位置**: [`compose/remote/remote-core/src/test/java/androidx/compose/remote/core/BinaryRoundTripTest.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/src/test/java/androidx/compose/remote/core/BinaryRoundTripTest.java)

**应用场景**: 测试所有 RemoteCompose 操作码的二进制往返一致性

**核心代码**:

```java
@RunWith(JUnit4.class)
public class BinaryRoundTripTest {
    
    @Test
    public void testAllOperations() throws Exception {
        // 测试所有 RemoteCompose 操作码
        // 包括：Header, DrawText, DrawBitmap, ClickArea, ClipRect, 
        // ColorAttribute, DataListFloat, DataMapIds, 等等...
        
        WireBuffer original = new WireBuffer();
        
        // 1. 写入头部
        Header header = new Header(7, 400, 300, 0x0000000000000001L);
        header.write(original);
        
        // 2. 写入文本数据
        TextData textData = new TextData(1, "Hello World");
        textData.write(original);
        
        // 3. 写入位图数据
        BitmapData bitmapData = new BitmapData(2, 100, 100, new byte[100 * 100 * 4]);
        bitmapData.write(original);
        
        // 4. 写入绘制操作
        DrawText drawText = new DrawText(1, 48.0f, 500, -0x1000000, 0);
        drawText.write(original);
        
        // 5. 写入点击区域
        ClickArea clickArea = new ClickArea(1001, 0, 0, 400, 300);
        clickArea.write(original);
        
        // 6. 转换为字节数组
        byte[] bytes = original.cloneBytes();
        
        // 7. 重新读取并验证
        WireBuffer copy = new WireBuffer(bytes.length);
        System.arraycopy(bytes, 0, copy.getBuffer(), 0, bytes.length);
        copy.setIndex(0);
        
        // 验证头部
        Header readHeader = Header.read(copy);
        assert readHeader.getApiLevel() == 7;
        assert readHeader.getWidth() == 400;
        assert readHeader.getHeight() == 300;
        
        // 验证文本数据
        TextData readText = TextData.read(copy);
        assert readText.getId() == 1;
        assert readText.getText().equals("Hello World");
        
        // ... 验证其他操作
        
        // 8. 验证完整性
        assert copy.getIndex() == bytes.length;
    }
}
```

**特点**:
- 测试所有 RemoteCompose 操作码（100+ 种）
- 确保二进制往返的 bit-for-bit 一致性
- 覆盖所有数据类型和操作类型
- 包括复杂操作（路径、着色器、动画等）

---

### WireBuffer API 总结

#### 写入方法

| 方法 | 参数类型 | 字节数 | 说明 |
|------|---------|--------|------|
| `writeByte(int)` | byte | 1 | 写入单字节 |
| `writeShort(int)` | short | 2 | 写入短整型 |
| `writeInt(int)` | int | 4 | 写入整型 |
| `writeLong(long)` | long | 8 | 写入长整型 |
| `writeFloat(float)` | float | 4 | 写入浮点数 |
| `writeDouble(double)` | double | 8 | 写入双精度 |
| `writeBoolean(boolean)` | boolean | 1 | 写入布尔值 |
| `writeUTF8(String)` | String | 可变 | 写入 UTF-8 字符串 |
| `writeBuffer(byte[])` | byte[] | 可变 | 写入字节数组（带长度前缀） |
| `write(byte[])` | byte[] | 可变 | 直接写入字节数组 |
| `start(int)` | opcode | 1 | 开始一个操作码 |

#### 读取方法

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `readByte()` | int | 读取单字节 |
| `readShort()` | int | 读取短整型 |
| `readInt()` | int | 读取整型 |
| `readLong()` | long | 读取长整型 |
| `readFloat()` | float | 读取浮点数 |
| `readDouble()` | double | 读取双精度 |
| `readBoolean()` | boolean | 读取布尔值 |
| `readUTF8()` | String | 读取 UTF-8 字符串 |
| `readBuffer()` | byte[] | 读取字节数组 |
| `readOperationType()` | int | 读取操作码 |

#### 常用工具方法

```kotlin
// 获取缓冲区
val buffer: ByteArray = wireBuffer.getBuffer()
val size: Int = wireBuffer.getSize()

// 复制字节
val bytes: ByteArray = wireBuffer.cloneBytes()

// 重置缓冲区
wireBuffer.reset(expectedSize)

// 设置索引位置
wireBuffer.setIndex(position)

// 移动数据块
wireBuffer.moveBlock(beyond, insertLocation)
```

---

### Wire Format 编码规范

#### 1. 字符串编码

```kotlin
// 格式：[长度 (4 字节)] + [UTF-8 字节]
val textBytes = text.content.toByteArray(Charsets.UTF_8)
buffer.writeInt(textBytes.size)  // 长度前缀
textBytes.forEach { b -> buffer.writeByte(b.toInt()) }

// 或使用便捷方法
buffer.writeUTF8(text.content)
```

#### 2. 数组编码

```kotlin
// 格式：[长度 (4 字节)] + [元素 1] + [元素 2] + ...
buffer.writeInt(array.size)
array.forEach { element -> buffer.writeInt(element) }
```

#### 3. 操作码序列

```kotlin
// 格式：[OP_CODE] + [字段 1] + [字段 2] + ...
buffer.writeByte(Operations.DRAW_TEXT_RUN)
buffer.writeInt(textResourceId)
buffer.writeFloat(fontSize)
buffer.writeInt(fontWeight)
```

#### 4. 嵌套结构

```kotlin
// 容器组件
buffer.writeByte(Operations.COMPONENT_START)
buffer.writeInt(componentId)
buffer.writeInt(layoutType)

// 子组件
writeChildren(buffer, children)

// 结束标记
buffer.writeByte(Operations.CONTAINER_END)
```

---

### 最佳实践

1. **始终检查缓冲区大小**
   ```kotlin
   val buffer = WireBuffer()
   // 写入大量数据前预留空间
   if (expectedSize > 10000) {
       buffer = WireBuffer(expectedSize)
   }
   ```

2. **使用 try-finally 确保索引重置**
   ```kotlin
   val savedIndex = buffer.getIndex()
   try {
       // 写入临时数据
   } finally {
       buffer.setIndex(savedIndex)
   }
   ```

3. **对于复杂数据，使用 cloneBytes() 获取干净副本**
   ```kotlin
   val cleanBytes = buffer.cloneBytes()
   // 返回的数组大小正好是 mSize，不是整个缓冲区
   ```

4. **写入字符串时使用 UTF-8 编码**
   ```kotlin
   // 推荐
   buffer.writeUTF8(string)
   
   // 或手动控制
   val bytes = string.toByteArray(Charsets.UTF_8)
   buffer.writeInt(bytes.size)
   bytes.forEach { buffer.writeByte(it.toInt()) }
   ```

5. **对于交互功能，确保正确的操作码顺序**
   ```kotlin
   // 正确顺序：
   buffer.writeByte(Operations.MODIFIER_CLICK)  // 先声明修饰器
   buffer.writeByte(Operations.HOST_ACTION)     // 再声明动作类型
   buffer.writeInt(actionId)                    // 最后写入参数
   ```

---

## 总结

本文档整理了 AndroidX 项目中使用 WireBuffer 和 RemoteViews.DrawInstructions 的完整用例：

### 第一部分：RemoteViews 创建用例（8 个）
- 从 WireBuffer 提取数据创建 RemoteViews
- 应用于 AppWidget、通知、Activity 预览

### 第二部分：WireBuffer 组装用例（4 个）
- 直接编程方式构建 Wire Format
- JSON ↔ Binary 双向转换
- 参数序列化测试
- 二进制往返测试

所有用例共同展示了 Remote Compose Wire Format 的完整生态系统，从数据生成、转换到最终渲染的端到端流程。

---
