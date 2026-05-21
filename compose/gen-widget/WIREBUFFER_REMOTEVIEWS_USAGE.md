# WireBuffer 与 RemoteViews.DrawInstructions 用例分析

## 概述

本文档整理了 AndroidX 项目中使用 `WireBuffer` 和 `RemoteViews.DrawInstructions` 创建 `RemoteViews` 的所有用例。这些用例展示了如何将 Remote Compose 生成的 Wire Format 二进制数据转换为可在 AppWidget、通知等场景中使用的 `RemoteViews` 对象。

**API 要求**: 所有用例均需要 **Android API 35+ (VANILLA_ICE_CREAM)**，这是 `RemoteViews.DrawInstructions` API 的最低要求。

---

## 核心转换流程

所有用例都遵循以下统一的转换流程：

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

---

## 用例列表

### 1. Glance AppWidget 集成

**文件位置**: 
[`glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/DrawInstructionRemoteViews.kt`](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/DrawInstructionRemoteViews.kt)

**应用场景**: Glance 框架将 Remote Compose 集成到 AppWidget 的核心转换逻辑

**核心代码**:
```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal fun create(translation: GlanceToRemoteComposeTranslation): RemoteViews {
    return when (translation) {
        is GlanceToRemoteComposeTranslation.Single -> {
            drawInstructionRemoteViews(translation)
        }
        is GlanceToRemoteComposeTranslation.SizeMap -> {
            // 支持多尺寸映射
            val map = mutableMapOf<SizeF, RemoteViews>()
            translation.results.forEach { (size, translation) ->
                val sizeF = size.toSizeF()
                val remoteView = drawInstructionRemoteViews(translation)
                map[sizeF] = remoteView
            }
            RemoteViews(map)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun drawInstructionRemoteViews(
    translation: GlanceToRemoteComposeTranslation.Single
): RemoteViews {
    val bytes: ByteArray = getBytes(translation.remoteComposeContext)
    
    // 构建 DrawInstructions
    val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
    val remoteViews = RemoteViews(drawInstructions)
    
    // 注册点击事件
    translation.actionMap.forEach { (actionId, pendingIntent) ->
        remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
    }
    
    return remoteViews
}

// 从 WireBuffer 提取字节数组
internal fun getBytes(rcContext: RemoteComposeContext): ByteArray {
    val buffer: WireBuffer = rcContext.buffer.buffer
    val bufferSize = buffer.size
    val bytesCopy = ByteArray(bufferSize)
    ByteArrayInputStream(buffer.buffer, 0, bufferSize).use { it.read(bytesCopy) }
    return bytesCopy
}
```

**关键特点**:
- 支持单个和多个尺寸的 RemoteViews 映射
- 自动处理点击事件映射
- 包含 CRC32 校验和调试日志
- Glance 框架与 Remote Compose 的桥梁

---

### 2. TickerWidgetProvider - 动态时钟小部件

**文件位置**: 
[`compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/TickerWidgetProvider.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/TickerWidgetProvider.java)

**应用场景**: 演示如何使用 Remote Compose 创建动态时钟小部件

**核心代码**:
```java
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private RemoteViews loadWidgetFromRemoteComposeContext(@NonNull Context context) {
    try {
        // 从 RemoteComposeBuffer 获取 WireBuffer
        RemoteComposeBuffer doc = RcTickerKt.RcTicker(context).getWriter().getBuffer();
        WireBuffer buffer = doc.getBuffer();
        int bufferSize = buffer.size();
        byte[] bytes = new byte[bufferSize];
        
        // 提取字节数组
        ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBuffer(), 0, bufferSize);
        b.read(bytes);
        
        // 构建 DrawInstructions 和 RemoteViews
        RemoteViews.DrawInstructions.Builder r =
                new RemoteViews.DrawInstructions.Builder(List.of(bytes));
        return new RemoteViews(r.build());
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

@Override
public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager,
        int @NonNull [] appWidgetIds) {
    RemoteViews widget = loadWidgetFromRemoteComposeContext(context);
    
    // 注册点击事件
    Intent intent1 = new Intent(context, TickerWidgetProvider.class);
    intent1.setAction(WIDGET_UPDATE_ACTION);
    PendingIntent pendingIntent1 = PendingIntent.getBroadcast(
        context, 0, intent1, PendingIntent.FLAG_MUTABLE);
    widget.setOnClickPendingIntent(sId, pendingIntent1);
    
    // 更新小部件
    for (int id : appWidgetIds) {
        appWidgetManager.updateAppWidget(id, widget);
    }
}
```

**关键特点**:
- 动态时钟演示
- 支持定时刷新
- 通过广播接收器处理点击事件

---

### 3. ExperimentWidgetProvider - 实验性小部件

**文件位置**: 
[`compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/ExperimentWidgetProvider.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/ExperimentWidgetProvider.java)

**应用场景**: 支持从多个数据源获取 WireBuffer 数据的实验性小部件

**核心代码**:
```java
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private RemoteViews loadWidgetFromRemoteComposeContext() {
    try {
        // 优先从 RawViewActivity 获取
        byte[] bytes = RawViewActivity.getCurrentDoc();
        
        // 如果为空，从 ExperimentRecyclerActivity 获取
        if (bytes == null || bytes.length == 0) {
            RemoteComposeBuffer doc = ExperimentRecyclerActivity.getCurrentDoc();
            WireBuffer buffer = doc.getBuffer();
            int bufferSize = buffer.size();
            bytes = new byte[bufferSize];
            ByteArrayInputStream b = new ByteArrayInputStream(
                buffer.getBuffer(), 0, bufferSize);
            b.read(bytes);
        }
        
        RemoteViews.DrawInstructions.Builder r =
                new RemoteViews.DrawInstructions.Builder(List.of(bytes));
        return new RemoteViews(r.build());
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
```

**关键特点**:
- 支持多个数据源（RawViewActivity / ExperimentRecyclerActivity）
- 灵活的数据获取策略
- 用于测试和演示不同场景

---

### 4. TickerActivity - 小部件更新活动

**文件位置**: 
[`compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/TickerActivity.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/TickerActivity.java)

**应用场景**: 在 Activity 中创建 RemoteViews 并更新小部件

**核心代码**:
```java
void setup(byte @NonNull [] bytes) {
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // 构建 DrawInstructions
        RemoteViews.DrawInstructions drawInstruction = 
            new RemoteViews.DrawInstructions.Builder(List.of(bytes)).build();
        RemoteViews remoteViews = new RemoteViews(drawInstruction);
        
        // 注册点击事件，触发小部件更新
        Intent intent = new Intent(this, TickerActivity.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_MUTABLE);
        
        int id = 567;
        remoteViews.setOnClickPendingIntent(id, pendingIntent);
        
        // 更新小部件
        appWidgetManager.updateAppWidget(TickerWidgetProvider.sAppWidgetIds, remoteViews);
    }
}
```

**关键特点**:
- Activity 直接控制小部件更新
- 用户交互触发小部件刷新
- 演示了 RemoteComposePlayer 与 RemoteViews 的集成

---

### 5. RawViewActivity - Remote Compose 文档浏览器

**文件位置**: 
[`compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/RawViewActivity.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/RawViewActivity.java)

**应用场景**: 完整的 Remote Compose 文档浏览器，支持预览、小部件创建和通知发送

**核心代码**:

**场景 1: RecyclerView 中展示 RemoteViews 预览**
```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    byte[] bytes = readRawResource(RawViewActivity.this, docItem.id);
    
    if (!USE_PLAYER) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            RemoteViews.DrawInstructions.Builder r =
                    new RemoteViews.DrawInstructions.Builder(List.of(bytes));
            RemoteViews remoteViews = new RemoteViews(r.build());
            
            // 将 RemoteViews 应用到容器中
            View previewView = remoteViews.apply(RawViewActivity.this, holder.mRcViewContainer);
            holder.mRcViewContainer.addView(previewView);
        }
    }
}
```

**场景 2: 点击按钮创建并固定小部件**
```java
@SuppressLint("RestrictedApiAndroidX")
public void click(int i) {
    try {
        sDoc = readRawResource(this, mFoundDocs[i].id);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            RemoteViews.DrawInstructions.Builder r =
                    new RemoteViews.DrawInstructions.Builder(List.of(sDoc));
            RemoteViews remoteViews = new RemoteViews(r.build());
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
            ComponentName myProvider = new ComponentName(this, ExperimentWidgetProvider.class);
            
            int[] ids = appWidgetManager.getAppWidgetIds(myProvider);
            if (ids != null && ids.length > 0) {
                // 更新现有小部件
                appWidgetManager.updateAppWidget(ids, remoteViews);
            } else if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                // 请求固定新小部件
                Bundle extras = new Bundle();
                extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, remoteViews);
                appWidgetManager.requestPinAppWidget(myProvider, extras, null);
            }
        }
    } catch (Exception e) {
        Log.e("RawViewActivity", "Click failed for " + mFoundDocs[i].name, e);
    }
}
```

**关键特点**:
- 完整的文档浏览器 UI
- 支持 RemoteComposePlayer 和 RemoteViews 两种渲染模式
- 支持小部件预览和固定
- 支持发送 Remote Compose 通知

---

### 6. ListRCWidget - 可滚动列表小部件

**文件位置**: 
[`compose/remote/integration-tests/demos/src/main/java/androidx/compose/remote/integration/demos/widget/ListRCWidget.kt`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/demos/src/main/java/androidx/compose/remote/integration/demos/widget/ListRCWidget.kt)

**应用场景**: 使用 Kotlin 协程创建包含可滚动列表的小部件

**核心代码**:
```kotlin
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Suppress("RestrictedApiAndroidX")
class ListRCWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, wm: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { widgetId ->
            goAsync {
                // 异步生成 Wire Format
                val bytes = listWidget(context.applicationContext, "ListRCWidget")
                
                // 创建 RemoteViews
                val widget = RemoteViews(DrawInstructions(bytes))
                
                // 更新小部件
                wm.updateAppWidget(widgetId, widget)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun DrawInstructions(bytes: ByteArray): RemoteViews.DrawInstructions {
    return RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
}

suspend fun listWidget(context: Context, name: String): ByteArray {
    return record(context.applicationContext) {
        ScrollableList(modifier = RemoteModifier.fillMaxSize(), name = name)
    }
}

@OptIn(ExperimentalRemoteCreationComposeApi::class, ExperimentalRemoteCreationApi::class)
suspend fun record(context: Context, content: @RemoteComposable @Composable () -> Unit): ByteArray =
    withContext(Dispatchers.Main) {
        captureSingleRemoteDocument(
            context = context,
            creationDisplayInfo = createCreationDisplayInfo(context),
            profile = RcPlatformProfiles.WIDGETS_V6,
            content = content,
        ).bytes
    }
```

**关键特点**:
- 使用 Kotlin 协程异步处理
- 展示可滚动列表组件
- 简洁的 Kotlin DSL 风格

---

### 7. RemoteNotification - Remote Compose 通知

**文件位置**: 
[`compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/notifications/RemoteNotification.java`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/notifications/RemoteNotification.java)

**应用场景**: 将 Remote Compose 文档用于通知的自定义视图

**核心代码**:
```java
@SuppressLint("RestrictedApiAndroidX")
public void send() {
    if (Build.VERSION.SDK_INT < 35) {
        Log.w(TAG, "Notification with DrawInstructions requires API 35 (Vanilla Ice Cream)");
        return;
    }
    
    if (mDocumentBytes == null) {
        Log.e(TAG, "Cannot send notification: document bytes are null");
        return;
    }
    
    try {
        createNotificationChannel();
        
        // 构建 DrawInstructions
        RemoteViews.DrawInstructions drawInstruction = 
            new RemoteViews.DrawInstructions.Builder(List.of(mDocumentBytes)).build();
        RemoteViews remoteViews = new RemoteViews(drawInstruction);
        
        // 设置默认 PendingIntent
        if (mPendingIntent == null) {
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(
                mContext.getPackageName());
            if (intent != null) {
                mPendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE);
            }
        }
        
        // 构建通知
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, mChannelId)
                .setSmallIcon(mSmallIcon)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteViews)
                .setPriority(mPriority)
                .setAutoCancel(mAutoCancel);
        
        if (mContentTitle != null) {
            builder.setContentTitle(mContentTitle);
        }
        if (mContentText != null) {
            builder.setContentText(mContentText);
        }
        if (mPendingIntent != null) {
            builder.setContentIntent(mPendingIntent);
        }
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        if (ActivityCompat.checkSelfPermission(mContext, 
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted for notifications");
            return;
        }
        
        int id = mNotificationId != -1 ? mNotificationId : sNotificationIdCounter++;
        notificationManager.notify(id, builder.build());
    } catch (Exception e) {
        Log.e(TAG, "Failed to send notification", e);
    }
}
```

**关键特点**:
- 支持 Remote Compose 通知
- Builder 模式构建通知
- 自动创建通知渠道
- 权限检查
- 支持自定义标题、文本、优先级等

---

### 8. RCWidget - 通用小部件基类

**文件位置**: 
[`compose/remote/remote-creation-compose/src/main/java/androidx/compose/remote/creation/compose/widgets/RCWidget.kt`](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-creation-compose/src/main/java/androidx/compose/remote/creation/compose/widgets/RCWidget.kt)

**应用场景**: Remote Compose 框架的核心小部件基类，支持 Lambda 动作和复杂交互

**核心代码**:
```kotlin
@SuppressLint("ResourceType")
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public fun getRemoteView(
    context: Context,
    document: CapturedDocument,
    provider: RemoteComposeWidget,
    widgetId: Int,
): RemoteViews {
    // 构建 DrawInstructions
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
            context.applicationContext,
            intentId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        
        rv.setOnClickPendingIntent(intentId, pendingIntent)
    }
    
    return rv
}
```

**关键特点**:
- Remote Compose 框架的核心基类
- 支持 Lambda 动作管理
- 自动为每个动作注册 PendingIntent
- 支持复杂的交互逻辑
- 与 `WidgetLambdaAction` 配合实现双向通信

---

## 技术要点总结

### 1. WireBuffer 数据提取

**方式一：使用 ByteArrayInputStream**
```kotlin
val buffer: WireBuffer = rcContext.buffer.buffer
val bufferSize = buffer.size
val bytesCopy = ByteArray(bufferSize)
ByteArrayInputStream(buffer.buffer, 0, bufferSize).use { it.read(bytesCopy) }
```

**方式二：直接复制**
```java
WireBuffer buffer = doc.getBuffer();
int bufferSize = buffer.size();
byte[] bytes = new byte[bufferSize];
ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBuffer(), 0, bufferSize);
b.read(bytes);
```

### 2. DrawInstructions 构建

所有用例都使用相同的构建模式：
```kotlin
// Kotlin
val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
val remoteViews = RemoteViews(drawInstructions)
```

```java
// Java
RemoteViews.DrawInstructions.Builder r = 
    new RemoteViews.DrawInstructions.Builder(List.of(bytes));
RemoteViews remoteViews = new RemoteViews(r.build());
```

### 3. 交互事件注册

**点击事件**:
```kotlin
remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
```

**PendingIntent 类型**:
- `PendingIntent.getBroadcast()`: 用于小部件更新
- `PendingIntent.getActivity()`: 用于启动 Activity
- `PendingIntent.getService()`: 用于启动 Service

### 4. 应用场景

| 场景 | 文件 | API 要求 |
|------|------|----------|
| AppWidget | Glance DrawInstructionRemoteViews, TickerWidgetProvider, ExperimentWidgetProvider, ListRCWidget, RCWidget | API 35+ |
| 通知 | RemoteNotification | API 35+ |
| Activity 预览 | RawViewActivity, TickerActivity | API 35+ |

### 5. 关键依赖

```kotlin
// Remote Compose Core
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.RemoteComposeBuffer

// Remote Compose Creation
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument

// Android Framework
import android.widget.RemoteViews
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
```

---

## 与 gen-widget 项目的集成建议

基于以上用例分析，[`WeatherWidgetSample.kt`](file:///home/meizu/Documents/my_android_projects/androidx/compose/gen-widget/src/main/java/androidx/compose/genwidget/sample/WeatherWidgetSample.kt) 中的实现与项目标准模式一致：

```kotlin
// 当前实现（第 208 行）
val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
val remoteViews = RemoteViews(instructions)
```

**建议改进**:

1. **添加点击事件处理**:
```kotlin
// 注册点击事件
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

3. **支持多尺寸**（可选）:
```kotlin
val sizeMap = mapOf(
    SizeF(180f, 180f) to smallRemoteViews,
    SizeF(360f, 180f) to mediumRemoteViews,
    SizeF(720f, 360f) to largeRemoteViews
)
val remoteViews = RemoteViews(sizeMap)
```

---

## 参考资料

- [Remote Compose Architecture](file:///home/meizu/Documents/my_android_projects/androidx/my_docs/remote-architecture.md)
- [Wire Format Transport API](file:///home/meizu/Documents/my_android_projects/androidx/my_docs/wire_format_transport_api.md)
- [Wire Format to Widget Rendering](file:///home/meizu/Documents/my_android_projects/androidx/my_docs/wire_format_to_widget_rendering.md)
- [LLM Wire Format Implementation](file:///home/meizu/Documents/my_android_projects/androidx/my_docs/llm_wire_format_implementation.md)

---

**文档生成时间**: 2026-05-21  
**AndroidX 版本**: 基于 API 35+ (VANILLA_ICE_CREAM)
