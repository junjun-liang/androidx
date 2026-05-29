# Glance AppWidget: Compose View 转换为 Remote Compose 完整手册

本文档详细描述 Glance AppWidget 模块中，Compose View 如何转换为 Remote Compose 字节流并最终生成 RemoteViews 的完整流程。

---

## 1. 整体架构概览

### 1.1 转换流程总览

```
Glance Composable (@Composable)
    ↓ compose 运行时
Emittable 树 (Glance 中间表示)
    ↓ normalizeCompositionTree()
Backend 选择 (RemoteCompose / Traditional)
    ↓ Backend.RemoteCompose
GlanceRemoteComposeTranslator.translateCompositionUsingRemoteCompose()
    ↓ 两步转换
Step 1: Emittable → RcElement 树 (translateEmittable)
Step 2: RcElement → WireBuffer (writeComponent)
    ↓
DrawInstructionRemoteViews.create()
    ↓
RemoteViews (包含 DrawInstructions)
    ↓
AppWidgetManager.updateAppWidget()
```

### 1.2 核心模块结构

```
glance/glance-appwidget/src/main/java/androidx/glance/appwidget/
├── GlanceAppWidget.kt              # 入口：用户定义 Composable
├── AppWidgetSession.kt             # 会话管理：触发转换
├── NormalizeCompositionTree.kt     # 树规范化 + Backend 选择
├── remotecompose/
│   ├── GlanceRemoteComposeTranslator.kt  # 核心转换器
│   ├── DrawInstructionRemoteViews.kt     # RemoteViews 生成
│   ├── TranslationModel.kt               # 数据模型
│   ├── ApplyModifiersToRemoteCompose.kt  # Modifier 转换
│   ├── ResourceResolvers.kt              # 资源解析
│   ├── RemoteComposeConstants.kt         # 常量定义
│   ├── RemoteComposeAlignments.kt        # 对齐方式映射
│   └── components/
│       ├── RcElement.kt              # RC 元素基类
│       ├── RcText.kt                 # 文本组件
│       ├── RcColumn.kt               # 列布局
│       ├── RcRow.kt                  # 行布局
│       ├── RcBox.kt                  # 盒子布局
│       ├── RcImage.kt                # 图片组件
│       ├── RcButton.kt               # 按钮组件
│       ├── RcSpacer.kt               # 间距组件
│       ├── RcLazyColumn.kt           # 懒加载列表
│       ├── RcMerge.kt                # 合并组件
│       ├── RcMaterial3IconButton.kt  # M3 图标按钮
│       └── RcMaterial3TextButton.kt  # M3 文本按钮
└── translators/                      # 传统 RemoteViews 转换器
    ├── TextTranslator.kt
    ├── ImageTranslator.kt
    └── ...
```

---

## 2. 详细转换流程

### 2.1 入口：GlanceAppWidget

**文件**: [GlanceAppWidget.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/GlanceAppWidget.kt#L58-L88)

用户通过继承 `GlanceAppWidget` 并实现 `provideGlance()` 来定义 Widget UI：

```kotlin
// GlanceAppWidget.kt:58
public abstract class GlanceAppWidget(
    @LayoutRes internal open val errorUiLayout: Int = R.layout.glance_error_layout
) {
    // L88: 用户重写此方法提供 Composable
    public abstract suspend fun provideGlance(context: Context, id: GlanceId)
}
```

### 2.2 会话管理：AppWidgetSession

**文件**: [AppWidgetSession.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/AppWidgetSession.kt#L170-L227)

```kotlin
// AppWidgetSession.kt:185-201
val backend =
    normalizeCompositionTree(root, backendOverrideRequest = getBackendOverride(options))
lambdas = root.updateLambdaActionKeys()

val translateWithRemoteCompose = backend == Backend.RemoteCompose

val remoteViews: RemoteViews =
    if (translateWithRemoteCompose) {
        // L194: 使用 Remote Compose 路径
        translateCompositionUsingRemoteCompose(
            remoteViewsRoot = root,
            context = context,
            appWidgetId = id.appWidgetId,
            glanceComponents =
                widget.getComponents(context) ?: GlanceComponents.getDefault(context),
            actionBroadcastReceiver = receiver,
        )
    } else {
        // L203: 使用传统 RemoteViews 路径
        translateComposition(context, id.appWidgetId, root, ...)
    }
```

**关键决策点 (L189)**: `backend == Backend.RemoteCompose` 决定走哪条转换路径。

### 2.3 核心转换器：GlanceRemoteComposeTranslator

**文件**: [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L77-L98)

```kotlin
// L77: 核心转换器对象
internal object GlanceRemoteComposeTranslator {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun translateCompositionUsingRemoteCompose(
        remoteViewsRoot: RemoteViewsRoot,
        context: Context,
        appWidgetId: Int,
        glanceComponents: GlanceComponents,
        actionBroadcastReceiver: ComponentName,
    ): RemoteViews {
        // L87-88: Step 1: 将 Glance 树转换为 Remote Compose 文档
        val translation =
            translateEmittableTreeToRemoteCompose(
                context = context,
                remoteViewRoot = remoteViewsRoot,
                appWidgetId = appWidgetId,
                glanceComponents = glanceComponents,
                actionBroadcastReceiver = actionBroadcastReceiver,
            )

        // L98: Step 2: 从 Remote Compose 文档创建 RemoteViews
        return DrawInstructionRemoteViews.create(translation)
    }
}
```

### 2.4 Step 1: Emittable 树 → RcElement 树

**文件**: [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L102-L267)

#### 2.4.1 顶层分发 (L102-147)

```kotlin
internal fun translateEmittableTreeToRemoteCompose(
    context: Context,
    remoteViewRoot: RemoteViewsRoot,
    ...
): GlanceToRemoteComposeTranslation {
    val topLevelChildren = remoteViewRoot.children

    val result =
        if (topLevelChildren.any { it is EmittableSizeBox }) {
            // L118: 多尺寸模式 (SizeMode.Exact)
            val documentsBySize = translateEmittableSizeBoxesToRemoteCompose(...)
            GlanceToRemoteComposeTranslation.SizeMap(documentsBySize)
        } else if (topLevelChildren.size == 1) {
            // L129: 单根模式
            val (doc, map) = translateSingleRootEmittableTreeToRemoteCompose(...)
            GlanceToRemoteComposeTranslation.Single(doc, actionMap = map)
        } else {
            throw IllegalStateException("Unexpected emittable tree")
        }
    return result
}
```

#### 2.4.2 单根转换 (L214-267)

```kotlin
private fun translateSingleRootEmittableTreeToRemoteCompose(
    context: Context, rootEmittable: Emittable, layoutSize: DpSize, ...
): Pair<RemoteComposeContext, TranslationActionMap> {
    val actionMap: TranslationActionMap = mutableListOf()
    val rcContext =
        RemoteComposeContext(
            creationDisplayInfo = CreationDisplayInfo(widthPx, heightPx, density),
            contentDescription = description,
            profile = GlanceRemoteComposeProfile,
        ) {
            val translationContext = TranslationContext(
                context = context,
                remoteComposeContext = this,
                appWidgetId = appWidgetId,
                actionMap = actionMap,
                ...
            )

            // L249-251: Step 1: 递归翻译 Emittable → RcElement
            val translatedRoot: RcElement =
                translateEmittable(
                    emittable = rootEmittable,
                    translationContext = translationContext,
                )

            // L259: Step 2: 将 RcElement 树写入 RemoteComposeContext
            root { translatedRoot.writeComponent(translationContext) }
        }
    return rcContext to actionMap
}
```

#### 2.4.3 Emittable 类型分发 (L149-179)

```kotlin
internal fun translateEmittable(
    emittable: Emittable,
    translationContext: TranslationContext,
): RcElement {
    val root: RcElement =
        when (emittable) {
            is EmittableSpacer      -> translateEmittableSpacer(emittable, translationContext)
            is EmittableBox         -> translateBox(emittable, translationContext)
            is EmittableColumn      -> translateColumn(emittable, translationContext)
            is EmittableLazyColumn  -> translateLazyColumn(emittable, translationContext)
            is EmittableRow         -> translateRow(emittable, translationContext)
            is EmittableText        -> translateText(emittable, translationContext)
            is EmittableImage       -> translateImage(emittable, translationContext)
            is EmittableButton      -> translateButton(emittable, translationContext)
            is EmittableSizeBox     -> translateEmittableSizeBox(emittable, translationContext)
            is EmittableM3IconButton -> translateEmittableM3IconButton(emittable, translationContext)
            is EmittableM3TextButton -> translateRcMaterial3Button(emittable, translationContext)
            else -> {
                Log.w(TAG, "Emittable $emittable not supported")
                translateUnknownElementToSpacer(emittable, translationContext)
            }
        }
    return root
}
```

### 2.5 Step 2: RcElement 树 → WireBuffer

每个 RcElement 子类实现 `writeComponent()` 方法，将自身写入 RemoteComposeContext。

#### 2.5.1 RcElement 基类

**文件**: [RcElement.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcElement.kt)

```kotlin
internal abstract class RcElement(translationContext: TranslationContext) {
    abstract val outputModifier: RecordingModifier
    abstract fun writeComponent(translationContext: TranslationContext)
}
```

#### 2.5.2 RcText - 文本组件

**文件**: [RcText.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcText.kt)

```kotlin
internal class RcText private constructor(
    emittable: EmittableText,
    translationContext: TranslationContext,
) : RcElement(translationContext) {

    override val outputModifier: RecordingModifier

    init {
        outputModifier = convertGlanceModifierToRemoteComposeModifier(
            modifiers = emittable.modifier,
            translationContext = translationContext,
        )
    }

    override fun writeComponent(translationContext: TranslationContext) {
        translationContext.remoteComposeContext.text(
            text = emittable.text,
            modifier = outputModifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            maxLines = maxLines,
        )
    }

    companion object {
        fun create(emittable: EmittableText, translationContext: TranslationContext): RcText
    }
}
```

#### 2.5.3 RcColumn - 列布局

**文件**: [RcColumn.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcColumn.kt)

```kotlin
internal class RcColumn(
    emittable: EmittableColumn,
    translationContext: TranslationContext,
) : RcElement(translationContext) {

    private val children = mutableListOf<RcElement>()

    init {
        for (child in emittable.children) {
            children.add(
                GlanceRemoteComposeTranslator.translateEmittable(child, translationContext)
            )
        }
    }

    override fun writeComponent(translationContext: TranslationContext) {
        translationContext.remoteComposeContext.column(
            modifier = outputModifier,
            horizontalAlignment = horizontalAlign,
        ) {
            for (child in children) {
                child.writeComponent(translationContext)
            }
        }
    }
}
```

#### 2.5.4 RcRow - 行布局

**文件**: [RcRow.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcRow.kt)

```kotlin
internal class RcRow(
    emittable: EmittableRow,
    translationContext: TranslationContext,
    modifierOverride: RecordingModifier? = null,
) : RcElement(translationContext) {

    override fun writeComponent(translationContext: TranslationContext) {
        translationContext.remoteComposeContext.row(
            modifier = outputModifier,
            verticalAlignment = verticalAlign,
        ) {
            for (child in children) {
                child.writeComponent(translationContext)
            }
        }
    }
}
```

#### 2.5.5 RcBox - 盒子布局

**文件**: [RcBox.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcBox.kt)

```kotlin
internal class RcBox(
    emittable: EmittableBox,
    translationContext: TranslationContext,
    modifierOverride: RecordingModifier? = null,
) : RcElement(translationContext) {

    override fun writeComponent(translationContext: TranslationContext) {
        translationContext.remoteComposeContext.box(
            modifier = outputModifier,
        ) {
            for (child in children) {
                child.writeComponent(translationContext)
            }
        }
    }
}
```

#### 2.5.6 RcSpacer - 间距

**文件**: [RcSpacer.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcSpacer.kt#L26-L46)

```kotlin
internal class RcSpacer(emittable: Emittable, translationContext: TranslationContext) :
    RcElement(translationContext) {

    override val outputModifier: RecordingModifier

    init {
        outputModifier = convertGlanceModifierToRemoteComposeModifier(
            modifiers = emittable.modifier,
            translationContext = translationContext,
        )
    }

    override fun writeComponent(translationContext: TranslationContext) {
        // L45: Spacer 写入为空 box
        translationContext.remoteComposeContext.box(outputModifier)
    }
}
```

#### 2.5.7 RcLazyColumn - 懒加载列表

**文件**: [RcLazyColumn.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcLazyColumn.kt#L45-L181)

```kotlin
internal class RcLazyColumn(
    emittable: EmittableLazyColumn,
    translationContext: TranslationContext,
) : RcElement(translationContext) {

    override fun writeComponent(translationContext: TranslationContext) {
        val rcContext = translationContext.remoteComposeContext

        rcContext.column(
            outputModifier.then(DrawWithContentModifier()),
            horizontalAlign,
        ) {
            val writer = translationContext.remoteComposeContext.writer
                as RemoteComposeWriterAndroid
            writer.startCanvasOperations()
            val scrollColumnDynamicHeightId = writer.addComponentHeightValue()
            val computedHeight = writer.floatExpression(
                scrollColumnDynamicHeightId, 1f, Rc.FloatExpression.MUL
            )

            // 更新滚动视图高度
            val action = ValueFloatExpressionChange(
                Utils.idFromNan(heightVariableId),
                Utils.idFromNan(computedHeight),
            )
            writer.startRunActions()
            writer.addAction(action)
            writer.endRunActions()
            writer.drawComponentContent()

            // 绘制分页点
            if (verticalScrollMode !is VerticalScrollMode.Normal) {
                drawDots(computedHeight, mainColor, fadedColor)
            }
            writer.endCanvasOperations()

            // 写入子组件
            for (child in children) {
                child.writeComponent(translationContext)
            }
        }
    }
}
```

### 2.6 Step 3: Remote Compose → RemoteViews

**文件**: [DrawInstructionRemoteViews.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/DrawInstructionRemoteViews.kt)

```kotlin
internal object DrawInstructionRemoteViews {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun create(
        translation: GlanceToRemoteComposeTranslation,
    ): RemoteViews {
        return when (translation) {
            is GlanceToRemoteComposeTranslation.Single -> createSingle(translation)
            is GlanceToRemoteComposeTranslation.SizeMap -> createSized(translation)
        }
    }

    private fun createSingle(translation: Single): RemoteViews {
        val rcContext = translation.remoteComposeContext
        val buffer = rcContext.writer.buffer
        val bytes = buffer.getBuffer().cloneBytes()
        val drawInstructions =
            RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
        return RemoteViews(drawInstructions)
    }

    private fun createSized(translation: SizeMap): RemoteViews {
        val sizeMap = translation.sizeToTranslationMap.map { (size, single) ->
            val bytes = single.remoteComposeContext.writer.buffer.getBuffer().cloneBytes()
            size to RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
        }
        return RemoteViews(sizeMap)
    }
}
```

---

## 3. Modifier 转换

### 3.1 Modifier 转换入口

**文件**: [ApplyModifiersToRemoteCompose.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/ApplyModifiersToRemoteCompose.kt)

```kotlin
internal fun convertGlanceModifierToRemoteComposeModifier(
    modifiers: GlanceModifier,
    translationContext: TranslationContext,
): RecordingModifier {
    // 遍历 GlanceModifier 中的每个元素，转换为 RecordingModifier
}
```

### 3.2 Modifier 映射表

| Glance Modifier | Remote Compose Modifier | 说明 |
|----------------|------------------------|------|
| `padding()` | `padding()` | 内边距 |
| `fillMaxWidth()` | `fillMaxWidth()` | 填充最大宽度 |
| `fillMaxHeight()` | `fillMaxHeight()` | 填充最大高度 |
| `fillMaxSize()` | `fillMaxSize()` | 填充最大尺寸 |
| `width()` | `width()` | 固定宽度 |
| `height()` | `height()` | 固定高度 |
| `background()` | `backgroundId()` | 背景色 |
| `clickable()` | `clickable()` | 点击事件 |
| `cornerRadius()` | `cornerRadius()` | 圆角 |
| `visibility()` | `visibility()` | 可见性 |
| `alpha()` | `alpha()` | 透明度 |
| `semantics()` | `contentDescription()` | 语义信息 |

---

## 4. Emittable → RcElement 映射表

| Glance Emittable | RC Component | 转换方法 | 文件 |
|-----------------|-------------|---------|------|
| `EmittableText` | `RcText` | `translateText()` | [RcText.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcText.kt) |
| `EmittableColumn` | `RcColumn` | `translateColumn()` | [RcColumn.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcColumn.kt) |
| `EmittableRow` | `RcRow` | `translateRow()` | [RcRow.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcRow.kt) |
| `EmittableBox` | `RcBox` | `translateBox()` | [RcBox.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcBox.kt) |
| `EmittableImage` | `RcImage` | `translateImage()` | [RcImage.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcImage.kt) |
| `EmittableButton` | `RcButton` | `translateButton()` | [RcButton.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcButton.kt) |
| `EmittableSpacer` | `RcSpacer` | `translateEmittableSpacer()` | [RcSpacer.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcSpacer.kt) |
| `EmittableLazyColumn` | `RcLazyColumn` | `translateLazyColumn()` | [RcLazyColumn.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcLazyColumn.kt) |
| `EmittableSizeBox` | 直接透传 | `translateEmittableSizeBox()` | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L272-L291) |
| `EmittableM3IconButton` | `RcMaterial3IconButton` | `translateEmittableM3IconButton()` | [RcMaterial3IconButton.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcMaterial3IconButton.kt) |
| `EmittableM3TextButton` | `RcMaterial3TextButton` | `translateRcMaterial3Button()` | [RcMaterial3TextButton.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/components/RcMaterial3TextButton.kt) |
| 其他 | `RcSpacer` | `translateUnknownElementToSpacer()` | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L315-L320) |

---

## 5. 完整流程图

### 5.1 端到端转换流程

```
用户代码
  |
  v
@Composable fun MyWidget() {          <-- GlanceAppWidget.kt:88
    Column {
        Text("Hello")
        Button("Click") { ... }
    }
}
  |
  v Compose 运行时
Emittable 树                          <-- Glance 内部表示
  | RemoteViewsRoot
  +-- EmittableColumn
  |   +-- EmittableText(text="Hello")
  |   +-- EmittableButton(text="Click")
  |
  v normalizeCompositionTree()         <-- NormalizeCompositionTree.kt
Backend 选择
  | backend == Backend.RemoteCompose ?
  |
  +-- YES --> Remote Compose 路径
  |     |
  |     v translateCompositionUsingRemoteCompose()  <-- GlanceRemoteComposeTranslator.kt:79
  |     |
  |     +-- Step 1: translateEmittableTreeToRemoteCompose()  <-- L103
  |     |     |
  |     |     v translateSingleRootEmittableTreeToRemoteCompose()  <-- L214
  |     |           |
  |     |           +-- 创建 RemoteComposeContext  <-- L227-237
  |     |           |
  |     |           +-- translateEmittable()  <-- L149 (递归)
  |     |           |     |
  |     |           |     +-- EmittableColumn --> RcColumn  <-- L157
  |     |           |     |     +-- EmittableText --> RcText  <-- L164
  |     |           |     |     +-- EmittableButton --> RcButton  <-- L166
  |     |           |     +-- ...
  |     |           |
  |     |           +-- root { translatedRoot.writeComponent() }  <-- L259
  |     |                 |
  |     |                 v RcElement.writeComponent() (递归)
  |     |                   +-- RcColumn.writeComponent()
  |     |                   |     +-- RcText.writeComponent() --> rcContext.text()
  |     |                   |     +-- RcButton.writeComponent() --> rcContext.button()
  |     |                   +-- ...
  |     |
  |     +-- Step 2: DrawInstructionRemoteViews.create()  <-- L98
  |           |
  |           v createSingle() / createSized()
  |                 |
  |                 +-- rcContext.writer.buffer
  |                 +-- buffer.getBuffer().cloneBytes()
  |                 +-- RemoteViews.DrawInstructions.Builder(bytes)
  |                 +-- RemoteViews(drawInstructions)
  |
  +-- NO --> 传统 RemoteViews 路径
        |
        v translateComposition()  <-- RemoteViewsTranslator.kt
              |
              +-- 传统 XML 布局方式
  |
  v
AppWidgetManager.updateAppWidget()     <-- AppWidgetSession.kt:215
```

### 5.2 RcElement 写入流程

```
RcElement.writeComponent(translationContext)
  |
  +-- RcText
  |     +-- rcContext.text(text, modifier, color, fontSize, ...)
  |           +-- RemoteComposeWriter.textComponent()
  |                 +-- WireBuffer.writeInt() / writeFloat() / writeUTF8()
  |
  +-- RcColumn
  |     +-- rcContext.column(modifier, alignment) { children.writeComponent() }
  |           +-- RemoteComposeWriter.startColumn() / endColumn()
  |
  +-- RcRow
  |     +-- rcContext.row(modifier, alignment) { children.writeComponent() }
  |           +-- RemoteComposeWriter.startRow() / endRow()
  |
  +-- RcBox
  |     +-- rcContext.box(modifier) { children.writeComponent() }
  |           +-- RemoteComposeWriter.startBox() / endBox()
  |
  +-- RcImage
  |     +-- rcContext.image(bitmap, modifier)
  |           +-- RemoteComposeWriter.imageComponent()
  |
  +-- RcButton
  |     +-- rcContext.button(text, modifier, action)
  |           +-- RemoteComposeWriter.buttonComponent()
  |
  +-- RcSpacer
  |     +-- rcContext.box(modifier)  // 空盒子
  |
  +-- RcLazyColumn
        +-- rcContext.column(modifier) {
              writer.startCanvasOperations()
              // ... 滚动逻辑 + 分页点绘制
              children.writeComponent()
            }
```

---

## 6. 关键代码行号索引

| 步骤 | 文件 | 行号 | 说明 |
|------|------|------|------|
| 用户入口 | [GlanceAppWidget.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/GlanceAppWidget.kt#L88) | L88 | `provideGlance()` 定义 |
| Backend 选择 | [AppWidgetSession.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/AppWidgetSession.kt#L189) | L189 | `backend == Backend.RemoteCompose` |
| RC 转换入口 | [AppWidgetSession.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/AppWidgetSession.kt#L194) | L194 | `translateCompositionUsingRemoteCompose()` |
| 核心转换器 | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L79) | L79 | `translateCompositionUsingRemoteCompose()` |
| Step 1 入口 | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L103) | L103 | `translateEmittableTreeToRemoteCompose()` |
| Emittable 分发 | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L149) | L149 | `translateEmittable()` when 分发 |
| 单根转换 | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L214) | L214 | `translateSingleRootEmittableTreeToRemoteCompose()` |
| RC Context 创建 | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L227) | L227 | `RemoteComposeContext(...)` |
| writeComponent | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L259) | L259 | `root { translatedRoot.writeComponent() }` |
| RemoteViews 创建 | [GlanceRemoteComposeTranslator.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/remotecompose/GlanceRemoteComposeTranslator.kt#L98) | L98 | `DrawInstructionRemoteViews.create()` |
| Widget 更新 | [AppWidgetSession.kt](file:///home/meizu/Documents/my_android_projects/androidx/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/AppWidgetSession.kt#L215) | L215 | `appWidgetManager.updateAppWidget()` |

---

## 7. 两步转换设计

### 7.1 为什么需要两步转换？

```
Step 1: Emittable --> RcElement 树
  - 目的：先收集所有元数据（图片、字符串、颜色等）
  - 优点：可以在写入前优化资源引用
  - 实现：translateEmittable() 递归构建 RcElement 树

Step 2: RcElement 树 --> WireBuffer
  - 目的：将 RcElement 树写入 RemoteComposeContext
  - 优点：确保资源先注册，组件后引用
  - 实现：writeComponent() 递归写入
```

### 7.2 两步转换流程

```kotlin
// Step 1: 构建 RcElement 树 (L249-256)
val translatedRoot: RcElement =
    translateEmittable(
        emittable = rootEmittable,
        translationContext = translationContext,
    )

// Step 2: 写入 WireBuffer (L259)
root { translatedRoot.writeComponent(translationContext) }
```

---

## 8. 总结

### 8.1 转换链路

```
Glance @Composable
  --> Emittable 树 (Compose 运行时)
  --> Backend 选择 (normalizeCompositionTree)
  --> RcElement 树 (translateEmittable, 递归)
  --> WireBuffer (writeComponent, 递归)
  --> byte[] (cloneBytes)
  --> DrawInstructions (Builder)
  --> RemoteViews
  --> AppWidgetManager.updateAppWidget()
```

### 8.2 核心设计模式

1. **两步转换**: 先构建中间树，再写入缓冲区
2. **递归分发**: `translateEmittable()` 使用 when 表达式分发
3. **Modifier 转换**: 独立的 Modifier 转换层
4. **Backend 抽象**: 支持 RemoteCompose 和传统两种路径

### 8.3 API 要求

- **compileSdk**: 35+ (Vanilla Ice Cream)
- **运行时**: API 35+ 才能使用 Remote Compose 路径
- **降级**: API < 35 自动回退到传统 RemoteViews 路径

---

**文档生成时间**: 2026-05-22
**基于代码版本**: AndroidX Glance AppWidget (API 35+)
