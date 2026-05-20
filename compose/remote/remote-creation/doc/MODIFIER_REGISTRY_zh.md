# RemoteCompose 修饰器注册表

本文档列出了 `remote-creation` API 中可用的布局修饰器及其如何映射到底层操作。

| DSL 方法 | 操作 / 修饰器类 | 用途 |
| :--- | :--- | :--- |
| `.fillMaxSize()` | `DimensionModifierOperation.Type.FILL` | 占据整个父空间 |
| `.width(w)` / `.height(h)` | `DimensionModifierOperation.Type.EXACT` | 固定尺寸 |
| `.widthIn(min, max)` | `WidthInModifierOperation` | 宽度约束 |
| `.padding(p)` | `PaddingModifierOperation` | 外部间距 |
| `.background(color)` | `BackgroundModifierOperation` | 纯色层 |
| `.backgroundId(id)` | `DynamicBackgroundModifier` | 主题感知颜色层 |
| `.border(w, r, c)` | `BorderModifierOperation` | 边界描边 |
| `.clip(shape)` | `ClipRectModifierOperation` | 将内容限制为形状 |
| `.offset(x, y)` | `OffsetModifierOperation` | 位置平移 |
| `.zIndex(z)` | `ZIndexModifierOperation` | 层级顺序 |
| `.graphicsLayer(...)` | `GraphicsLayerModifierOperation`| 不透明度、旋转、缩放 |
| `.clickable(actions)` | `ClickModifierOperation` | 处理点击事件 |
| `.verticalScroll()` | `ScrollModifierOperation` | 启用垂直平移 |
| `.alignByBaseline()` | `AlignByModifierOperation` | 按文本基线对齐兄弟元素 |
| `.visibility(id)` | `ComponentVisibilityOperation` | 动态显示/隐藏 |

## 形状
与 `.clip()` 或 `.border()` 一起使用：
- `RectShape`
- `RoundedRectShape`
- `CircleShape`
