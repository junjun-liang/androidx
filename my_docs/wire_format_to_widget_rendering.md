# Wire Format 到桌面微件渲染完整流程文档

> 本文档详细阐述从 Wire Format 二进制数据生成到桌面微件渲染的完整流程，包含系统架构图、数据流图和关键源码分析。

---

## 目录

- [1. 概述](#1-概述)
- [2. 总体架构](#2-总体架构)
- [3. 完整流程详解](#3-完整流程详解)
  - [3.1 阶段一：Wire Format 生成](#31-阶段一wire-format-生成)
  - [3.2 阶段二：RemoteViews 创建](#32-阶段二remoteviews-创建)
  - [3.3 阶段三：AppWidgetManager 更新](#33-阶段三appwidgetmanager-更新)
  - [3.4 阶段四：系统渲染](#34-阶段四系统渲染)
- [4. 关键源码分析](#4-关键源码分析)
- [5. 完整流程图](#5-完整流程图)
- [6. 数据流时序图](#6-数据流时序图)
- [7. 总结](#7-总结)

---

## 1. 概述

从 Wire Format 二进制数据到桌面微件显示，整个流程涉及以下关键组件：

| 组件 | 职责 | 所在进程 |
|------|------|---------|
| **WireBuffer** | 存储 Wire Format 二进制数据 | 系统服务进程 |
| **RemoteViews.DrawInstructions** | 封装二进制数据为可渲染指令 | 系统服务进程 |
| **RemoteViews** | 携带绘制指令和交互事件 | 系统服务进程 → Launcher |
| **AppWidgetManager** | 管理所有微件的状态和更新 | SystemServer |
| **AppWidgetHost** | 在 Launcher 中托管微件 | Launcher 进程 |
| **RemoteViewsRenderer** | 解析 Wire Buffer 并渲染到 Canvas | Launcher 进程 |

**关键特性：**
- **跨进程通信**：SystemServer → Launcher 通过 Binder 传输 RemoteViews
- **零编译**：Wire Format 是二进制协议，无需编译直接解析
- **高效渲染**：Android 16+ 使用 `RemoteViews.DrawInstructions` API

---

## 2. 总体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                Wire Format 到桌面微件渲染架构                             │
├─────────────────────────────────────────────────────────────────────────┤

  阶段一：Wire Format 生成（系统服务进程）
  ┌─────────────────────────────────────────────────────────────────┐
  │  LLM / 系统 API                                                  │
  │     │                                                            │
  │     ▼                                                            │
  │  WireBuffer                                                      │
  │  - writeByte(HEADER)                                             │
  │  - writeInt(Version, Width, Height)                              │
  │  - writeByte(DATA_TEXT, DATA_BITMAP)                             │
  │  - writeByte(COMPONENT_START, DRAW_TEXT_RUN)                     │
  │  - writeByte(CONTAINER_END)                                      │
  │     │                                                            │
  │     ▼                                                            │
  │  byte[] bytes = buffer.toByteArray()                             │
  └─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
  阶段二：RemoteViews 创建（系统服务进程）
  ┌─────────────────────────────────────────────────────────────────┐
  │  RemoteViews.DrawInstructions.Builder(listOf(bytes))            │
  │     │                                                            │
  │     ▼                                                            │
  │  .build() → DrawInstructions                                     │
  │     │                                                            │
  │     ▼                                                            │
  │  RemoteViews(drawInstructions)                                   │
  │     │                                                            │
  │     ▼                                                            │
  │  setOnClickPendingIntent(actionId, pendingIntent)                │
  └─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
  阶段三：AppWidgetManager 更新（SystemServer 进程）
  ┌─────────────────────────────────────────────────────────────────┐
  │  AppWidgetManager.getInstance(context)                           │
  │     │                                                            │
  │     ▼                                                            │
  │  updateAppWidget(appWidgetId, remoteViews)                       │
  │     │                                                            │
  │     ▼                                                            │
  │  IAppWidgetService (Binder)                                      │
  │     │                                                            │
  │     ▼                                                            │
  │  存储 RemoteViews 到 AppWidgetImpl                                │
  │     │                                                            │
  │     ▼                                                            │
  │  通知 AppWidgetHost (Launcher)                                   │
  └─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
  阶段四：系统渲染（Launcher 进程）
  ┌─────────────────────────────────────────────────────────────────┐
  │  AppWidgetHostView                                               │
  │     │                                                            │
  │     ▼                                                            │
  │  applyRemoteViews(remoteViews)                                   │
  │     │                                                            │
  │     ▼                                                            │
  │  RemoteViewsRenderer (Android 16+)                               │
  │     │                                                            │
  │     ▼                                                            │
  │  解析 DrawInstructions                                            │
  │     │                                                            │
  │     ▼                                                            │
  │  解析 Wire Buffer → 执行操作码                                    │
  │     │                                                            │
  │     ▼                                                            │
  │  Canvas 绘制                                                      │
  │     │                                                            │
  │     ▼                                                            │
  │  显示到桌面                                                       │
  └─────────────────────────────────────────────────────────────────┘
```

---

## 3. 完整流程详解

### 3.1 阶段一：Wire Format 生成

**输入：** 用户自然语言描述（如"创建一个显示北京天气的微件"）

**输出：** `byte[]` Wire Format 二进制数据

**流程：**

```kotlin
// 1. LLM 解析用户描述，生成操作码序列
fun generateWidgetFromDescription(description: String): ByteArray {
    val buffer = WireBuffer()
    
    // 2. 写入文档头部
    buffer.writeByte(Operations.HEADER)           // OpCode 0
    buffer.writeInt(7)                            // Version 7
    buffer.writeInt(400)                          // Width
    buffer.writeInt(600)                          // Height
    buffer.writeInt(4)                            // Density
    buffer.writeLong(RcPlatformProfiles.WIDGETS_V7) // Profile
    
    // 3. 注册文本资源
    buffer.writeByte(Operations.DATA_TEXT)        // OpCode 102
    buffer.writeInt(1)                            // TextID
    buffer.writeString("Beijing")                 // UTF-8
    
    buffer.writeByte(Operations.DATA_TEXT)
    buffer.writeInt(2)
    buffer.writeString("24°C")
    
    // 4. 注册图片资源
    buffer.writeByte(Operations.DATA_BITMAP)      // OpCode 101
    buffer.writeInt(1)                            // BitmapID
    buffer.writeInt(48)                           // Width
    buffer.writeInt(48)                           // Height
    buffer.writeBytes(bitmapPixels)               // ARGB_8888
    
    // 5. 组件树
    buffer.writeByte(Operations.COMPONENT_START)  // OpCode 201
    buffer.writeInt(1)                            // ComponentID
    buffer.writeInt(LayoutType.COLUMN)            // LayoutType
    writeModifier(buffer, Modifier.fillMaxSize())
    
    // 6. 绘制操作
    buffer.writeByte(Operations.DRAW_TEXT_RUN)    // OpCode 43
    buffer.writeInt(1)                            // TextID
    buffer.writeInt(0)                            // start
    buffer.writeInt(7)                            // end
    buffer.writeFloat(0f)                         // x (由布局计算)
    buffer.writeFloat(0f)                         // y (由布局计算)
    buffer.writeByte(0)                           // RTL false
    
    // 7. 关闭容器
    buffer.writeByte(Operations.CONTAINER_END)    // OpCode 230
    
    return buffer.toByteArray()
}
```

**关键点：**
- WireBuffer 是可增长的字节数组
- 所有数据按操作码顺序写入
- 文本、图片等资源先注册 ID，后引用
- 组件树结构通过 COMPONENT_START/CONTAINER_END 嵌套

---

### 3.2 阶段二：RemoteViews 创建

**输入：** `byte[]` Wire Format 二进制数据

**输出：** `RemoteViews` 对象（含 DrawInstructions 和 PendingIntent 映射）

**流程：**

```kotlin
// DrawInstructionRemoteViews.kt
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun drawInstructionRemoteViews(
    translation: GlanceToRemoteComposeTranslation.Single
): RemoteViews {
    // 1. 获取 Wire Buffer 字节
    val bytes: ByteArray = getBytes(translation.remoteComposeContext)
    
    // 2. 构建 DrawInstructions
    // API 设计为 List<ByteArray>，但目前只使用第一个数组
    val drawInstructions = RemoteViews.DrawInstructions
        .Builder(listOf(bytes))
        .build()
    
    // 3. 创建 RemoteViews
    val remoteViews = RemoteViews(drawInstructions)
    
    // 4. 绑定点击事件
    translation.actionMap.forEach { (actionId, pendingIntent) ->
        remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
    }
    
    // 5. (可选) 计算 CRC32 校验和用于调试
    val checksum = CRC32().apply { update(bytes) }.value
    Log.d(TAG, "drawInstructionRemoteViews: len ${bytes.size}, checksum $checksum")
    
    return remoteViews
}
```

**关键点：**
- `RemoteViews.DrawInstructions` 是 Android 16+ 新 API
- 专门用于 Remote Compose 渲染
- 支持多个字节数组（为未来扩展预留）
- PendingIntent 通过 `setOnClickPendingIntent()` 绑定

---

### 3.3 阶段三：AppWidgetManager 更新

**输入：** `RemoteViews` 对象 + `appWidgetId`

**输出：** 微件状态更新到 SystemServer，通知 Launcher

**流程：**

```kotlin
// GlanceAppWidget.kt
public open fun onCompositionError(
    context: Context,
    glanceId: GlanceId,
    appWidgetId: Int,
    throwable: Throwable,
) {
    if (errorUiLayout == 0) {
        throw throwable
    } else {
        // 创建错误状态的 RemoteViews
        val rv = RemoteViews(context.packageName, errorUiLayout)
        
        // 调用 AppWidgetManager 更新微件
        AppWidgetManager.getInstance(context)
            .updateAppWidget(appWidgetId, rv)
    }
}
```

**底层实现（SystemServer）：**

```java
// AppWidgetManager.java (Framework)
public void updateAppWidget(int appWidgetId, RemoteViews views) {
    getService().updateAppWidget(appWidgetId, views, null);
}

// IAppWidgetService.java (Binder Interface)
void updateAppWidget(int appWidgetId, RemoteViews views, int[] viewIds);

// AppWidgetService.java (SystemServer)
public void updateAppWidget(int appWidgetId, RemoteViews views, int[] viewIds) {
    synchronized (mLock) {
        AppWidgetImpl widget = getAppWidgetImpl(appWidgetId);
        if (widget != null) {
            widget.updateAppWidget(views);
            
            // 通知 AppWidgetHost (Launcher)
            notifyAppWidgetViewDataChanged(appWidgetId, null);
        }
    }
}
```

**关键点：**
- 通过 Binder IPC 从应用进程到 SystemServer
- `AppWidgetImpl` 存储 RemoteViews 状态
- 通过 `AppWidgetHostCallback` 通知 Launcher 更新

---

### 3.4 阶段四：系统渲染

**输入：** `RemoteViews`（含 DrawInstructions）

**输出：** 微件渲染到桌面 Canvas

**流程：**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Launcher 进程渲染流程                                 │
├─────────────────────────────────────────────────────────────────────────┤

  AppWidgetHostView
       │
       ▼
  applyRemoteViews(remoteViews)
       │
       ▼
  RemoteViewsRenderer (Android 16+)
       │
       ▼
  1. 获取 DrawInstructions
       │
       ▼
  2. 解析 Wire Buffer
       │
       ├──────────────────────────────────────────────────────┐
       │  while (buffer.hasMoreBytes()) {                     │
       │    byte opCode = buffer.readByte();                  │
       │    Operation op = Operations.create(opCode);         │
       │    op.apply(remoteContext);                          │
       │  }                                                   │
       └──────────────────────────────────────────────────────┘
       │
       ▼
  3. 执行操作码
       │
       ├── HEADER: 读取文档元数据
       ├── DATA_TEXT: 注册文本到 StringPool
       ├── DATA_BITMAP: 解码图片到 BitmapPool
       ├── COMPONENT_START: 创建布局节点
       ├── DRAW_TEXT_RUN: 绘制文本
       ├── DRAW_BITMAP: 绘制图片
       ├── DRAW_RECT: 绘制矩形
       └── CONTAINER_END: 关闭布局节点
       │
       ▼
  4. Canvas 绘制
       │
       ├── canvas.save()
       ├── layout.measure()  // 布局计算
       ├── layout.layout()   // 位置分配
       ├── draw()            // 递归绘制子节点
       └── canvas.restore()
       │
       ▼
  5. 显示到桌面 Surface
```

**关键点：**
- RemoteViewsRenderer 是 Android 16+ 新增的渲染器
- 专门用于解析 Remote Compose Wire Format
- 布局计算和绘制都在 Launcher 进程完成
- 支持动态表达式求值（动画、触摸交互）

---

## 4. 关键源码分析

### 4.1 DrawInstructionRemoteViews.kt

文件路径：`glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/DrawInstructionRemoteViews.kt`

核心代码：

```kotlin
internal object DrawInstructionRemoteViews {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun create(translation: GlanceToRemoteComposeTranslation): RemoteViews {
        return when (translation) {
            is GlanceToRemoteComposeTranslation.Single -> {
                drawInstructionRemoteViews(translation)
            }
            is GlanceToRemoteComposeTranslation.SizeMap -> {
                val map = mutableMapOf<SizeF, RemoteViews>()
                translation.results.forEach { (size, translation) ->
                    val sizeF = size.toSizeF()
                    val remoteView = drawInstructionRemoteViews(translation)
                    map.put(sizeF, remoteView)
                }
                RemoteViews(map)  // 多尺寸适配
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun drawInstructionRemoteViews(
    translation: GlanceToRemoteComposeTranslation.Single
): RemoteViews {
    // 1. 从 RemoteComposeContext 获取 Wire Buffer
    val bytes: ByteArray = getBytes(translation.remoteComposeContext)

    // 2. 构建 DrawInstructions
    val drawInstructions = RemoteViews.DrawInstructions
        .Builder(listOf(bytes))
        .build()
    
    // 3. 创建 RemoteViews
    val remoteViews = RemoteViews(drawInstructions)

    // 4. 绑定点击事件
    translation.actionMap.forEach { (actionId, pendingIntent) ->
        remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
    }

    return remoteViews
}
```

### 4.2 GlanceAppWidget.kt

文件路径：`glance/glance-appwidget/src/main/java/androidx/glance/appwidget/GlanceAppWidget.kt`

核心代码：

```kotlin
public abstract class GlanceAppWidget {
    public abstract suspend fun provideGlance(context: Context, id: GlanceId)

    public open fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        if (errorUiLayout == 0) {
            throw throwable
        } else {
            val rv = RemoteViews(context.packageName, errorUiLayout)
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, rv)
        }
    }

    public suspend fun provideContent(
        content: @Composable @GlanceComposable () -> Unit
    ): Nothing {
        coroutineContext[ContentReceiver]?.provideContent(content)
            ?: error("provideContent requires a ContentReceiver")
    }
}
```

---

## 5. 完整流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                Wire Format → 桌面微件完整流程                             │
├─────────────────────────────────────────────────────────────────────────┤

  [用户] "创建一个显示北京天气的微件"
       │
       ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │  阶段一：Wire Format 生成（系统服务进程）                         │
  │                                                                  │
  │  Gemini/LLM                                                      │
  │     │                                                            │
  │     ▼                                                            │
  │  解析意图：                                                      │
  │  - 城市：北京                                                    │
  │  - 组件：Text, Image, Column                                     │
  │  - 布局：垂直排列                                                │
  │  - 样式：圆角卡片、浅蓝色背景                                     │
  │     │                                                            │
  │     ▼                                                            │
  │  生成操作码序列：                                                │
  │  [HEADER, DATA_TEXT, DATA_BITMAP, COMPONENT_START, ...]         │
  │     │                                                            │
  │     ▼                                                            │
  │  WireBuffer                                                      │
  │  - writeByte(HEADER)                                             │
  │  - writeInt(Version, Width, Height)                              │
  │  - writeByte(DATA_TEXT, "Beijing")                               │
  │  - writeByte(DATA_BITMAP, icon)                                  │
  │  - writeByte(COMPONENT_START, COLUMN)                            │
  │  - writeByte(DRAW_TEXT_RUN, textId)                              │
  │  - writeByte(CONTAINER_END)                                      │
  │     │                                                            │
  │     ▼                                                            │
  │  byte[] bytes = buffer.toByteArray()                             │
  └─────────────────────────────────────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │  阶段二：RemoteViews 创建（系统服务进程）                         │
  │                                                                  │
  │  RemoteViews.DrawInstructions.Builder(listOf(bytes))            │
  │     │                                                            │
  │     ▼                                                            │
  │  .build() → DrawInstructions                                     │
  │     │                                                            │
  │     ▼                                                            │
  │  RemoteViews(drawInstructions)                                   │
  │     │                                                            │
  │     ▼                                                            │
  │  setOnClickPendingIntent(actionId, pendingIntent)                │
  └─────────────────────────────────────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │  阶段三：AppWidgetManager 更新（SystemServer 进程）              │
  │                                                                  │
  │  AppWidgetManager.getInstance(context)                           │
  │     │                                                            │
  │     ▼                                                            │
  │  IAppWidgetService.updateAppWidget(appWidgetId, remoteViews)    │
  │     │                                                            │
  │     ▼                                                            │
  │  [SystemServer]                                                  │
  │  AppWidgetImpl.updateAppWidget(views)                            │
  │     │                                                            │
  │     ▼                                                            │
  │  存储 RemoteViews 状态                                           │
  │     │                                                            │
  │     ▼                                                            │
  │  notifyAppWidgetViewDataChanged(appWidgetId)                     │
  │     │                                                            │
  │     ▼                                                            │
  │  [Binder IPC]                                                    │
  │  AppWidgetHostCallback.onAppWidgetViewDataChanged()             │
  └─────────────────────────────────────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │  阶段四：系统渲染（Launcher 进程）                                │
  │                                                                  │
  │  [Launcher]                                                      │
  │  AppWidgetHostView                                               │
  │     │                                                            │
  │     ▼                                                            │
  │  applyRemoteViews(remoteViews)                                   │
  │     │                                                            │
  │     ▼                                                            │
  │  RemoteViewsRenderer (Android 16+)                               │
  │     │                                                            │
  │     ▼                                                            │
  │  解析 DrawInstructions                                            │
  │     │                                                            │
  │     ▼                                                            │
  │  解析 Wire Buffer:                                               │
  │  while (buffer.hasMoreBytes()) {                                 │
  │    opCode = buffer.readByte()                                    │
  │    op = Operations.create(opCode)                                │
  │    op.apply(remoteContext)                                       │
  │  }                                                               │
  │     │                                                            │
  │     ▼                                                            │
  │  布局计算：                                                      │
  │  - measure()  // 测量组件尺寸                                    │
  │  - layout()   // 分配位置                                        │
  │     │                                                            │
  │     ▼                                                            │
  │  Canvas 绘制：                                                   │
  │  - canvas.save()                                                 │
  │  - draw()      // 递归绘制子节点                                 │
  │  - canvas.restore()                                              │
  │     │                                                            │
  │     ▼                                                            │
  │  SurfaceFlinger 合成                                             │
  │     │                                                            │
  │     ▼                                                            │
  │  显示到桌面                                                       │
  └─────────────────────────────────────────────────────────────────┘
```

---

## 6. 数据流时序图

```
┌─────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  用户   │      │ LLM/系统 API  │      │ 系统服务进程  │      │ SystemServer │      │  Launcher    │
│         │      │              │      │              │      │              │      │              │
│ 输入描述│      │              │      │              │      │              │      │              │
│────────>│      │              │      │              │      │              │      │              │
│         │      │              │      │              │      │              │      │              │
│         │ 解析意图            │      │              │      │              │      │              │
│         │──────────>│      │              │      │              │      │              │
│         │              │      │              │      │              │      │              │
│         │ 生成操作码序列    │      │              │      │              │      │              │
│         │<──────────│      │              │      │              │      │              │
│         │              │      │              │      │              │      │              │
│         │              │ WireBuffer 写入       │      │              │      │              │
│         │              │────────>│      │              │      │              │
│         │              │      │              │      │              │      │              │
│         │              │      │ WireBuffer         │      │              │      │              │
│         │              │      │ .toByteArray()     │      │              │      │              │
│         │              │      │────────>│      │              │      │              │
│         │              │      │              │      │              │      │              │
│         │              │      │ DrawInstructions   │      │              │      │              │
│         │              │      │ .Builder(bytes)    │      │              │      │              │
│         │              │      │────────>│      │              │      │              │
│         │              │      │              │      │              │      │              │
│         │              │      │ RemoteViews        │      │              │      │              │
│         │              │      │ (drawInstructions) │      │              │      │              │
│         │              │      │<────────│      │              │      │              │
│         │              │      │              │      │              │      │              │
│         │              │      │ setOnClickPendingIntent           │      │              │      │
│         │              │      │────────>│      │              │      │              │
│         │              │      │              │      │              │      │              │
│         │              │      │ RemoteViews        │      │              │      │              │
│         │              │      │─────────────────────────>│      │              │
│         │              │      │              │      │              │      │              │
│         │              │      │              │ updateAppWidget(   │              │      │
│         │              │      │              │ appWidgetId, views)│              │      │
│         │              │      │              │────────>│      │              │
│         │              │      │              │      │              │      │              │
│         │              │      │              │      │ 存储 RemoteViews             │      │
│         │              │      │              │      │────────>│              │      │
│         │              │      │              │      │              │      │              │
│         │              │      │              │      │ notifyViewDataChanged        │      │
│         │              │      │              │      │ (Binder IPC) │              │      │
│         │              │      │              │      │─────────────────────────>│      │
│         │              │      │              │      │              │      │              │
│         │              │      │              │      │              │ applyRemoteViews│
│         │              │      │              │      │              │<──────────────│      │
│         │              │      │              │      │              │              │      │
│         │              │      │              │      │              │ 解析 Wire Buffer│
│         │              │      │              │      │              │              │      │
│         │              │      │              │      │              │ 布局计算      │      │
│         │              │      │              │      │              │              │      │
│         │              │      │              │      │              │ Canvas 绘制   │      │
│         │              │      │              │      │              │              │      │
│         │              │      │              │      │              │ 显示到桌面    │      │
│         │              │      │              │      │              │              │      │
│ 微件显示│      │              │      │              │      │              │
│<────────│      │              │      │              │      │              │      │              │
│         │              │      │              │      │              │      │              │
└─────────┘      └──────────────┘      └──────────────┘      └──────────────┘      └──────────────┘
```

---

## 7. 总结

### 流程关键点

| 阶段 | 关键操作 | 耗时 | 是否需编译 |
|------|---------|------|-----------|
| **阶段一** | Wire Format 生成 | 毫秒级 | ❌ 不需要 |
| **阶段二** | RemoteViews 创建 | 毫秒级 | ❌ 不需要 |
| **阶段三** | AppWidgetManager 更新 | 10-50ms | ❌ 不需要 |
| **阶段四** | 系统渲染 | 16-60ms (1-3 帧) | ❌ 不需要 |

### 技术优势

1. **零编译**：Wire Format 是二进制协议，无需编译直接解析
2. **跨进程高效**：通过 Binder IPC 传输 RemoteViews，数据量小
3. **动态更新**：支持运行时修改变量值，无需重新生成文档
4. **向后兼容**：Android 16+ 使用 DrawInstructions，旧版本降级到传统 RemoteViews

### 适用场景

- **"帮我制作微件"**：LLM 生成 Wire Format，系统即时渲染
- **动态微件**：根据实时数据更新微件内容
- **自定义微件**：开发者使用 Remote Compose API 创建复杂效果

### 核心 API

```kotlin
// 1. 生成 Wire Format
val bytes = generateWidgetBytes()

// 2. 创建 RemoteViews
val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
val remoteViews = RemoteViews(drawInstructions)

// 3. 更新微件
AppWidgetManager.getInstance(context)
    .updateAppWidget(appWidgetId, remoteViews)
```

---

## 附录：参考文档

- [Remote Compose 架构文档](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/REMOTE_COMPOSE_ARCHITECTURE_zh.md)
- [协议规范](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/PROTOCOL_SPEC_zh.md)
- [数据流与生命周期](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/doc/DATA_FLOW_zh.md)
- [GlanceRemoteComposeTranslator 设计文档](file:///home/meizu/Documents/my_android_projects/androidx/my_docs/glance_remote_compose_translator_design.md)
- [Remote Compose + Glance 架构](file:///home/meizu/Documents/my_android_projects/androidx/my_docs/remote_compose_glance_widget.md)
