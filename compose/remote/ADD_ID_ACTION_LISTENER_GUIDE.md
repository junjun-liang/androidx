# addIdActionListener 使用指南

## 一、概述

`addIdActionListener` 是 Remote Compose 中**ID 动作回调**的注册方法。当用户在 Remote Compose 文档中点击绑定了 `HostAction(id)` 的组件或 `ClickArea` 区域时，播放端通过此回调接收交互事件。

**核心特征：**
- 交互逻辑由宿主 App（播放端）处理，不回传给文档创建端
- 回调参数为 `(id: Int, metadata: String?)`，其中 `id` 由文档创建端定义
- 支持注册多个监听器
- 存在三层 API 封装，适配不同使用场景

---

## 二、API 签名速查

| 层级 | 注册方法 | 回调签名 | metadata 可空性 |
|------|---------|---------|----------------|
| **Core** (CoreDocument) | `addIdActionListener(IdActionCallback)` | `onAction(id: Int, metadata: String?)` | `@Nullable` |
| **View** (RemoteComposePlayer) | `addIdActionListener(IdActionCallbacks)` | `onAction(id: Int, metadata: String?)` | `@Nullable` |
| **Compose** (RemoteDocumentPlayer) | `onAction` 参数 | `onAction(actionId: Int, value: String?)` | Kotlin `String?` |

---

## 三、三层 API 详解

### 3.1 底层：CoreDocument（remote-core）

[CoreDocument.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/src/main/java/androidx/compose/remote/core/CoreDocument.java)

```java
// 回调接口定义
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IdActionCallback {
    void onAction(int id, @Nullable String metadata);
}

// 注册监听器
public void addIdActionListener(@NonNull IdActionCallback callback) {
    mIdActionListeners.add(callback);
}

// 移除所有监听器
public void clearActionCallbacks() {
    mActionListeners.clear();
    mIdActionListeners.clear();
}
```

**特点：**
- 回调存储在 `HashSet<IdActionCallback>` 中，支持多个监听器
- `id` 为创建端定义的整数标识
- `metadata` 可为 null（如 `HostAction(id)` 不带 metadata 时传空字符串）
- `clearActionCallbacks()` 会同时清除 `IdActionCallback` 和 `ActionCallback`（命名动作回调）

### 3.2 View 层：RemoteComposePlayer（remote-player-view）

[RemoteComposePlayer.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-player-view/src/main/java/androidx/compose/remote/player/view/RemoteComposePlayer.java)

```java
// 回调接口定义
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IdActionCallbacks {
    void onAction(int id, @Nullable String metadata);
}

// 注册监听器（委托给内部 RemoteComposeView）
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public void addIdActionListener(@NonNull IdActionCallbacks callback) {
    mInner.addIdActionListener((id, metadata) -> callback.onAction(id, metadata));
}
```

**调用链：** `RemoteComposePlayer.addIdActionListener` → `RemoteComposeView.addIdActionListener` → `CoreDocument.addIdActionListener`

**使用场景：** 在传统 Android View 体系中使用 RemoteComposePlayer 时注册回调。

### 3.3 Compose 层：RemoteDocumentPlayer（remote-player-compose）

[RemoteDocumentPlayer.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-player-compose/src/main/java/androidx/compose/remote/player/compose/RemoteDocumentPlayer.kt)

```kotlin
@Composable
public fun RemoteDocumentPlayer(
    document: CoreDocument,
    documentWidth: Int,
    documentHeight: Int,
    modifier: Modifier = Modifier,
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },  // ID 动作回调
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
) {
    AndroidView(
        // ...
        update = { remoteComposePlayer ->
            remoteComposePlayer.document.document.clearActionCallbacks()
            remoteComposePlayer.document.document.addIdActionListener { id, value ->
                onAction.invoke(id, value)
            }
            // ...
        }
    )
}
```

**特点：**
- 以 Composable 参数形式提供，符合 Compose 声明式风格
- 默认为空操作 `{ _, _ -> }`
- 每次 recompose 会先 `clearActionCallbacks()` 再重新注册

---

## 四、创建端对应 API

在文档创建端，以下 API 定义的动作会触发 `addIdActionListener` 的回调：

### 4.1 HostAction(int id) — 无 metadata

[HostAction.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-creation-core/src/main/java/androidx/compose/remote/creation/actions/HostAction.java)

```java
// Java 过程式 API
Action action = new HostAction(567);
rc.box(modifier.onClick(action), ...);
```

触发时回调：`onAction(567, "")`

### 4.2 HostAction(int id, int metadataId) — 带 metadata

```java
// Java 过程式 API
int metadataTextId = rc.addText("refresh");
Action action = new HostAction(567, metadataTextId);
rc.box(modifier.onTouchDown(action), ...);
```

触发时回调：`onAction(567, "refresh")`

### 4.3 addClickArea — 画布级点击区域

[RemoteComposeWriter.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-creation-core/src/main/java/androidx/compose/remote/creation/RemoteComposeWriter.java)

```java
// Java 过程式 API
rc.addClickArea(567, "This click area", 0f, 0f, 300f, 300f, "bob");
```

```kotlin
// Kotlin DSL API
addClickArea(567, "clock face", 0f, 0f, 600f, 600f, "1234")
```

参数说明：
- `id`: 点击区域标识，回调时返回
- `contentDescription`: 无障碍描述
- `left/top/right/bottom`: 点击区域矩形坐标
- `metadata`: 附加元数据字符串，回调时返回

触发时回调：`onAction(567, "bob")`

### 4.4 clickable + HostAction — 组件级点击修饰符

[ClickableModifier.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-creation-compose/src/main/java/androidx/compose/remote/creation/compose/modifier/ClickableModifier.kt)

```kotlin
// Compose 声明式 API
RemoteBox(
    modifier = RemoteModifier.clickable(HostAction("home_action".rs, payload.rs))
) { ... }
```

> **注意：** Compose 风格的 `HostAction(name, value)` 走的是**命名动作通道**（`onNamedAction`），而非 ID 动作通道。要使用 ID 动作通道，需使用 Java 过程式 `new HostAction(id)` 或内部类 `HostAction` 的 `id` 参数。

### 4.5 三种触发源对比

| 触发源 | API 层级 | 回调参数 | 适用场景 |
|--------|---------|---------|---------|
| `HostAction(id)` | 组件修饰符 | `(id, "")` | 组件点击，无附加信息 |
| `HostAction(id, metadataId)` | 组件修饰符 | `(id, "metadata文本")` | 组件点击，需传递附加信息 |
| `addClickArea(id, ..., metadata)` | 画布级 | `(id, "metadata")` | 画布上的矩形热区 |

---

## 五、完整事件流

```
┌──────────────────────────────────────────────────────────────┐
│  创建端 (Document Creation)                                   │
│                                                              │
│  HostAction(567)           ─写入─>  HostActionOperation      │
│  HostAction(567, metaId)   ─写入─>  HostActionMetadataOperation│
│  addClickArea(567,...,"bob")─写入─> ClickArea opcode          │
└──────────────────────────┬───────────────────────────────────┘
                           │ byte[] 文档数据
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  播放端 (Document Playback)                                   │
│                                                              │
│  ┌─ 协议解码 ─────────────────────────────────────────────┐ │
│  │ ClickArea.apply(context)  → context.addClickArea()     │ │
│  │                           → CoreDocument.mClickAreas   │ │
│  │ HostActionOperation 作为修饰符附加到 Component          │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─ 用户点击触发 ─────────────────────────────────────────┐ │
│  │                                                        │ │
│  │ 路径 A: ClickArea 点击                                  │ │
│  │   RemoteComposeView.performClick()                     │ │
│  │     → CoreDocument.onClick(context, x, y)              │ │
│  │       → 遍历 mClickAreas，找到包含(x,y)的区域           │ │
│  │         → warnClickListeners(clickArea)                │ │
│  │           → notifyOfException(id, metadata)            │ │
│  │                                                        │ │
│  │ 路径 B: 组件修饰符上的 HostAction 点击                   │ │
│  │   Component.onClick() 递归处理                          │ │
│  │     → ClickHandler.onClick()                           │ │
│  │       → ActionOperation.runAction()                    │ │
│  │         → HostActionOperation: context.runAction(id,"")│ │
│  │         → HostActionMetadataOperation:                 │ │
│  │             context.runAction(id, metadataText)        │ │
│  │           → AndroidRemoteContext.runAction()            │ │
│  │             → CoreDocument.performClick(context,id,meta)│ │
│  │               → notifyOfException(id, metadata)        │ │
│  │                                                        │ │
│  │ 路径 C: ClickAreaView 点击 (USE_VIEW_AREA_CLICK 模式)   │ │
│  │   viewArea.setOnClickListener()                        │ │
│  │     → CoreDocument.performClick(context, id, metadata) │ │
│  │       → notifyOfException(id, metadata)                │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─ 回调分发 ─────────────────────────────────────────────┐ │
│  │ CoreDocument.notifyOfException(id, metadata)           │ │
│  │   → 遍历 mIdActionListeners                            │ │
│  │     → IdActionCallback.onAction(id, metadata)          │ │
│  │       → View 层: IdActionCallbacks.onAction()          │ │
│  │       → Compose 层: onAction(actionId, value)          │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

---

## 六、代码示例

### 6.1 Java View 层：TickerActivity

[TickerActivity.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/TickerActivity.java)

**播放端注册回调：**

```java
RemoteComposePlayer player = new RemoteComposePlayer(this);
player.addIdActionListener((id, metadata) -> {
    Utils.log(" \"" + metadata + "\"");
    setup(buffer);  // 点击后刷新 Widget
});
```

**对应的创建端定义：**

[RcTicker.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/examples/RcTicker.kt)

```kotlin
val action = HostAction(567, textCreateId("refresh"))
canvas(Modifier.size(size).onTouchDown(action)) { ... }
```

当用户触摸画布时，播放端收到 `onAction(567, "refresh")`，然后重新加载文档。

### 6.2 Kotlin Compose 层：RemoteDocumentPlayer

```kotlin
RemoteDocumentPlayer(
    document = coreDocument,
    documentWidth = 400,
    documentHeight = 800,
    onAction = { actionId, value ->
        when (actionId) {
            567 -> handleRefresh(value)
            568 -> handleNavigation(value)
            else -> Log.d("RemoteDoc", "Unknown action: $actionId, metadata: $value")
        }
    }
)
```

### 6.3 创建端 + 播放端配对示例

[SimpleClick.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/examples/old/SimpleClick.java)

**创建端：**

```java
// 定义两个 ID 动作
Action action1 = new HostAction(567);   // 触摸动作
Action action2 = new HostAction(568);   // 点击动作

// 绑定到组件
rc.box(
    new RecordingModifier().background(Color.YELLOW).onTouchDown(action1),
    0, 0, 200, 200
);
rc.box(
    new RecordingModifier().background(Color.RED).onClick(action2),
    200, 0, 200, 200
);

// 也可以用 addClickArea 定义画布级热区
rc.addClickArea(567, "This click area", x, y, x + w, y + h, "bob");
```

**播放端：**

```java
player.addIdActionListener((id, metadata) -> {
    switch (id) {
        case 567:
            Log.d("Click", "触摸了黄色区域，metadata: " + metadata);
            break;
        case 568:
            Log.d("Click", "点击了红色区域，metadata: " + metadata);
            break;
    }
});
```

### 6.4 ExperimentActivity 中的状态更新用法

[ExperimentActivity.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/ExperimentActivity.kt)

```kotlin
var clickId by remember { mutableStateOf(-1) }
var clickMetadata by remember { mutableStateOf("") }

AndroidView(
    factory = {
        val player = RemoteComposePlayer(it)
        player.addIdActionListener { _id, _metadata ->
            clickId = _id
            clickMetadata = _metadata ?: "empty"
        }
        player
    },
    update = {
        if (currentDocument.value != null) {
            it.setDocument(RemoteDocument(currentDocument.value!!))
        }
    }
)

// 在 UI 中显示点击信息
Text("Last click: id=$clickId, metadata=$clickMetadata")
```

---

## 七、注意事项

### 7.1 多监听器

`addIdActionListener` 支持注册多个监听器（底层使用 `HashSet`），每次点击事件会通知所有已注册的监听器：

```java
// 可以注册多个监听器，都会被调用
player.addIdActionListener((id, meta) -> Log.d("Logger", "id=" + id));
player.addIdActionListener((id, meta) -> handleAction(id, meta));
```

### 7.2 metadata 可空性差异

| 层级 | metadata 类型 | 空值行为 |
|------|-------------|---------|
| CoreDocument.IdActionCallback | `@Nullable String` | 可能为 null |
| RemoteComposePlayer.IdActionCallbacks | `@Nullable String` | 可能为 null |
| RemoteComposeView.ClickCallbacks | `@NonNull String` | 不会为 null（空字符串） |
| RemoteDocumentPlayer.onAction | `String?` (Kotlin) | 可能为 null |

实际运行时，`HostAction(id)` 不带 metadata 时传空字符串 `""`，`ClickArea` 无 metadata 时传 null。

### 7.3 clearActionCallbacks 的副作用

`CoreDocument.clearActionCallbacks()` 会**同时清除** `IdActionCallback` 和 `ActionCallback`（命名动作回调）：

```java
public void clearActionCallbacks() {
    mActionListeners.clear();      // 清除命名动作回调
    mIdActionListeners.clear();    // 清除 ID 动作回调
}
```

`RemoteDocumentPlayer` 在每次 `update` 时会调用此方法再重新注册，因此不要在 `RemoteDocumentPlayer` 外部单独注册 `addIdActionListener`，否则会被清除。

### 7.4 ID 的选择建议

- ID 是 `int` 类型，由创建端自由定义
- 建议使用有意义的常量而非魔法数字：`static final int ACTION_REFRESH = 567;`
- 不同组件可以使用相同 ID（表示同一类动作），也可以使用不同 ID 区分

### 7.5 与命名动作（onNamedAction）的对比

| 特性 | addIdActionListener (ID 动作) | onNamedAction (命名动作) |
|------|------------------------------|------------------------|
| 回调参数 | `(id: Int, metadata: String?)` | `(name: String, value: Any?, stateUpdater: StateUpdater)` |
| 创建端 API | `new HostAction(id)` | `hostAction("name")` / `HostAction("name")` |
| 能否回写文档状态 | 不能 | 可以通过 `stateUpdater` 修改变量 |
| 值类型 | 仅字符串 metadata | Float / Int / String / FloatArray |
| 适用场景 | 简单点击通知 | 需要传递结构化数据或回写状态 |

---

## 八、关键文件索引

| 文件 | 作用 |
|------|------|
| [CoreDocument.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/src/main/java/androidx/compose/remote/core/CoreDocument.java) | IdActionCallback 接口定义、addIdActionListener、notifyOfException |
| [RemoteComposePlayer.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-player-view/src/main/java/androidx/compose/remote/player/view/RemoteComposePlayer.java) | View 层 IdActionCallbacks 封装 |
| [RemoteComposeView.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-player-view/src/main/java/androidx/compose/remote/player/view/platform/RemoteComposeView.java) | 触摸事件入口、ClickCallbacks 封装 |
| [RemoteDocumentPlayer.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-player-compose/src/main/java/androidx/compose/remote/player/compose/RemoteDocumentPlayer.kt) | Compose 层 onAction 参数 |
| [HostAction.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-creation-core/src/main/java/androidx/compose/remote/creation/actions/HostAction.java) | Java 过程式 HostAction 创建 |
| [HostAction.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-creation-compose/src/main/java/androidx/compose/remote/creation/compose/action/HostAction.kt) | Compose 声明式 HostAction 创建 |
| [HostActionOperation.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/src/main/java/androidx/compose/remote/core/operations/layout/modifiers/HostActionOperation.java) | ID 动作协议操作（无 metadata） |
| [HostActionMetadataOperation.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/src/main/java/androidx/compose/remote/core/operations/layout/modifiers/HostActionMetadataOperation.java) | ID 动作协议操作（带 metadata） |
| [ClickArea.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-core/src/main/java/androidx/compose/remote/core/operations/ClickArea.java) | 画布级点击区域协议操作 |
| [AndroidRemoteContext.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/remote-player-core/src/main/java/androidx/compose/remote/player/core/platform/AndroidRemoteContext.java) | runAction 桥接到 CoreDocument |
| [SimpleClick.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/examples/old/SimpleClick.java) | 创建端完整示例 |
| [TickerActivity.java](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/TickerActivity.java) | 播放端 View 层示例 |
| [ExperimentActivity.kt](file:///home/meizu/Documents/my_android_projects/androidx/compose/remote/integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/ExperimentActivity.kt) | 播放端 Compose 层示例 |
