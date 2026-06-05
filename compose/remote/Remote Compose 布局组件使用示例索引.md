# Remote Compose 布局组件使用示例索引

## 目录

- [1. 概述](#1-概述)
- [2. RemoteBox](#2-remotebox)
- [3. RemoteColumn](#3-remotecolumn)
- [4. RemoteRow](#4-remoterow)
- [5. RemoteText](#5-remotetext)
- [6. RemoteImage](#6-remoteimage)
- [7. RemoteCanvas](#7-remotecanvas)
- [8. RemoteSpacer](#8-remotespacer)
- [9. RemoteStateLayout](#9-remotestatelayoutr)
- [10. RemoteCollapsibleColumn](#10-remotecollapsiblecolumn)
- [11. RemoteCollapsibleRow](#11-remotecollapsiblerow)
- [12. RemoteFlowRow](#12-remoteflowrow)
- [13. FitBox](#13-fitbox)
- [14. 综合示例：DemoWeather.kt](#14-综合示例demoweatherkt)
- [15. 专用测试文件索引](#15-专用测试文件索引)

---

## 1. 概述

本文档列举项目中所有使用 `remote-creation-compose/.../layout/` 目录下布局组件的示例，按组件分类，标明文件路径。

**使用频次统计**：

| 组件 | 使用处数 | 最常用场景 |
|------|---------|-----------|
| RemoteText | 50+ | 几乎所有 demo 和测试 |
| RemoteBox | 40+ | 容器、背景占位 |
| RemoteColumn | 25+ | 垂直布局 |
| RemoteRow | 20+ | 水平布局 |
| RemoteCanvas | 18+ | 自定义绘制 |
| RemoteFlowRow | 13+ | 流式换行 |
| RemoteCollapsibleColumn | 10+ | 可折叠垂直布局 |
| RemoteCollapsibleRow | 10+ | 可折叠水平布局 |
| RemoteImage | 9 | 图片显示 |
| RemoteStateLayout | 7 | 状态切换 |
| RemoteSpacer | 6 | 间距占位 |
| FitBox | 6 | 自适应盒子 |

---

## 2. RemoteBox

### integration-tests/demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../modifier/CombinedClickableDemo.kt` | `CombinedClickableDemo()` | `RemoteBox(modifier = RemoteModifier.size(200.rdp, 100.rdp).background(RemoteColor(Color.LightGray)))` |
| `integration-tests/demos/.../modifier/PaddingDemo.kt` | `PaddingDemo()` | `RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red))` |
| `integration-tests/demos/.../modifier/ClickableDemo.kt` | `ClickableDemo()` | `RemoteBox(modifier = RemoteModifier.size(200.rdp, 100.rdp).background(RemoteColor(Color.LightGray)))` |
| `integration-tests/demos/.../modifier/TouchActionDemo.kt` | `TouchActionDemo()` | `RemoteBox(modifier = RemoteModifier.size(200.rdp, 100.rdp).background(RemoteColor(Color.LightGray)))` |
| `integration-tests/demos/.../integration/GesturePropagationDemo.kt` | `GesturePropagationDemo()` | `RemoteBox(modifier = RemoteModifier.size(80.rdp).background(RemoteColor(Color.Red)))` |
| `integration-tests/demos/.../layout/RemoteBoxDemos.kt` | `RemoteBoxAlignmentsDemo()` | `RemoteBox` + `RemoteStateLayout` 组合使用 |

### integration-tests/player-view-demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../widgets/MyWidget.kt` | `Button()` | `RemoteBox(modifier.padding(16.rdp).clip(RemoteRoundedCornerShape(20.rdp)))` |
| `integration-tests/player-view-demos/.../examples/TestDrawContentDemo.kt` | `TestDrawContentDemo()` | `RemoteBox(modifier = RemoteModifier.height(rcFloat).width(rcFloat).background(Color.Red.rc))` |
| `integration-tests/player-view-demos/.../examples/SwitchWidget.kt` | `SwitchWidgetOnState()` | `RemoteBox(modifier = modifier.clip(RemoteRoundedCornerShape(20.rdp)))` |
| `integration-tests/player-view-demos/.../examples/SwitchWidget.kt` | `SwitchWidget()` | `RemoteBox { ... when(state) ... }` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `Temp()` | `RemoteBox(modifier = RemoteModifier.width(60.rdp), contentAlignment = RemoteAlignment.Center)` |

### remote-creation-compose/samples/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/samples/.../samples/ScaleSample.kt` | `ScaleSample()` | `RemoteBox(RemoteModifier.scale(2f.rf).size(100.rdp).background(Color.Red))` |
| `remote-creation-compose/samples/.../samples/RotateSample.kt` | `RotateSample()` | `RemoteBox(RemoteModifier.rotate(45f.rf).size(100.rdp).background(Color.Red))` |
| `remote-creation-compose/samples/.../samples/AlphaSample.kt` | `AlphaSample()` | `RemoteBox(RemoteModifier.size(100.rdp).alpha(0.5f.rf).background(Color.Red))` |
| `remote-creation-compose/samples/.../previews/ScalePreview.kt` | `ScalePreview()` | `RemoteBox(RemoteModifier.size(100.rdp).scale(...).background(Color.Red))` |
| `remote-creation-compose/samples/.../previews/RotatePreview.kt` | `RotatePreview()` | `RemoteBox(RemoteModifier.size(100.rdp).rotate(45f.rf).background(Color.Red))` |
| `remote-creation-compose/samples/.../previews/AlphaPreview.kt` | `AlphaPreview()` | `RemoteBox(RemoteModifier.size(100.rdp).alpha(0.5f.rf).background(Color.Red))` |

### remote-creation-compose/src/test/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testBasicDocument()` | `RemoteBox { RemoteText(text = "Hello V2".rs) }` |
| `remote-creation-compose/src/test/.../state/RemoteStateCreationTest.kt` | `testNamedRemoteInt()` | `RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedInt.toRemoteFloat())))` |
| `remote-creation-compose/src/test/.../state/RemoteStateCreationTest.kt` | `testNamedRemoteBoolean()` | `RemoteBox(modifier = RemoteModifier.size(100.rdp).background(namedBoolean.select(...)))` |
| `remote-creation-compose/src/test/.../capture/CaptureRemoteDocumentTest.kt` | `testCapture()` | `RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red))` |

### remote-creation-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/androidTest/.../v2/CaptureRemoteDocumentV2Test.kt` | 多个测试 | `RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.DarkGray.rc).border(2.rdp, Color.Blue.rc))` |
| `remote-creation-compose/src/androidTest/.../vector/RemoteVectorPainterTest.kt` | 多个测试 | `RemoteBox(modifier) { RemoteCanvas(...) }` |
| `remote-creation-compose/src/androidTest/.../layout/RemoteBoxTest.kt` | `RemoteBoxTest` | 专门测试 RemoteBox 的各种 alignment 组合 |

### remote-player-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-player-compose/src/androidTest/.../RemoteDocumentComposePlayerTest.kt` | `testPlayDocument()` | `RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.DarkGray)) { RemoteText("Hello world!") }` |
| `remote-player-compose/src/androidTest/.../BlendModeTest.kt` | `RemoteBlendModeVisual()` | `RemoteBox(RemoteModifier.size(100.rdp).border(1.rdp, Color.Black.rc).padding(8.rdp))` |

---

## 3. RemoteColumn

### integration-tests/demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../modifier/CombinedClickableDemo.kt` | `CombinedClickableDemo()` | `RemoteColumn(modifier = RemoteModifier.fillMaxSize())` |
| `integration-tests/demos/.../modifier/PaddingDemo.kt` | `PaddingDemo()` | `RemoteColumn(modifier = RemoteModifier.fillMaxSize())` |
| `integration-tests/demos/.../modifier/ClickableDemo.kt` | `ClickableDemo()` | `RemoteColumn(modifier = RemoteModifier.fillMaxSize())` |
| `integration-tests/demos/.../modifier/TouchActionDemo.kt` | `TouchActionDemo()` | `RemoteColumn(modifier = RemoteModifier.fillMaxSize())` |
| `integration-tests/demos/.../modifier/RotateDemo.kt` | `RotateDemo()` | `RemoteColumn(modifier = RemoteModifier.fillMaxSize())` |

### integration-tests/player-view-demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `DemoWeather()` | `RemoteColumn(modifier = RemoteModifier.fillMaxWidth().padding(16.rdp), horizontalAlignment = RemoteAlignment.CenterHorizontally)` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherDetail()` | `RemoteColumn(horizontalAlignment = RemoteAlignment.End)` |
| `integration-tests/player-view-demos/.../examples/ScrollView.kt` | 日历组件 | `RemoteColumn(modifier = modifier, horizontalAlignment = RemoteAlignment.End)` |
| `integration-tests/player-view-demos/.../examples/old/compose/AnimatedChangesDemo.kt` | `AnimatedChangesDemo()` | `RemoteColumn(modifier = RemoteModifier.fillMaxSize(), verticalArrangement = RemoteArrangement.Center, horizontalAlignment = RemoteAlignment.CenterHorizontally)` |

### remote-creation-compose/src/test/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testBasicDocument()` | `RemoteColumn { RemoteText(...); RemoteRow { ... } }` |

### remote-creation-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/androidTest/.../layout/RemoteColumnTest.kt` | `RemoteColumnTest` | 专门测试各种 arrangement/alignment/spacedBy 组合 |
| `remote-creation-compose/src/androidTest/.../modifier/ClickableModifierTest.kt` | `testClickable()` | `RemoteColumn(modifier = RemoteModifier.fillMaxSize().semantics(mergeDescendants = true) {})` |
| `remote-creation-compose/src/androidTest/.../integration/GesturePropagationTest.kt` | 多个测试 | `RemoteColumn(modifier = RemoteModifier.fillMaxSize(), horizontalAlignment = RemoteAlignment.CenterHorizontally)` |
| `remote-creation-compose/src/androidTest/.../a11y/ListA11yTest.kt` | `ListContent()` | `RemoteColumn(modifier = modifier.verticalScroll(scrollState).background(Color.White), horizontalAlignment = RemoteAlignment.CenterHorizontally)` |

### remote-player-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-player-compose/src/androidTest/.../state/RemoteStateTest.kt` | `testWidthState()` | `RemoteColumn(modifier = RemoteModifier.size(100.rdp))` |

---

## 4. RemoteRow

### integration-tests/demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../modifier/ClickableDemo.kt` | `ClickableDemo()` | `RemoteRow { RemoteText("Clicks: ".rs + clickCounter.toRemoteString()) }` |
| `integration-tests/demos/.../modifier/TouchActionDemo.kt` | `TouchActionDemo()` | `RemoteRow { RemoteText("Downs: "...); }` |

### integration-tests/player-view-demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherContent()` | `RemoteRow(modifier = RemoteModifier.widthIn(min = 100.rdp).heightIn(min = rowHeight), verticalAlignment = RemoteAlignment.CenterVertically)` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherDays()` | `RemoteRow(modifier = RemoteModifier.fillMaxWidth().widthIn(min = 200.rdp), verticalAlignment = RemoteAlignment.CenterVertically)` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `Temp()` | `RemoteRow(modifier = RemoteModifier.width(70.rdp), horizontalArrangement = RemoteArrangement.End, verticalAlignment = RemoteAlignment.CenterVertically)` |
| `integration-tests/player-view-demos/.../widgets/MyWidget.kt` | `Content()` | `RemoteRow(RemoteModifier.background(Color.White).fillMaxSize(), horizontalArrangement = RemoteArrangement.Center, verticalAlignment = RemoteAlignment.CenterVertically)` |
| `integration-tests/player-view-demos/.../examples/ScrollView.kt` | 日历组件 | `RemoteRow(modifier = RemoteModifier.height(IntrinsicSize.Min))` |
| `integration-tests/player-view-demos/.../examples/PathChecks.kt` | `SimplePath()` | `RemoteRow(modifier = RemoteModifier.fillMaxSize(), verticalAlignment = RemoteAlignment.CenterVertically)` |
| `integration-tests/player-view-demos/.../examples/Clock.kt` | `Clock()` | `RemoteRow(modifier = RemoteModifier.fillMaxSize(), verticalAlignment = RemoteAlignment.CenterVertically)` |

### remote-creation-compose/src/test/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testBasicDocument()` | `RemoteRow { RemoteSpacer(modifier = RemoteModifier.weight(1f)); RemoteText(text = "End".rs) }` |

### remote-creation-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/androidTest/.../layout/RemoteRowTest.kt` | `RemoteRowTest` | 专门测试各种 arrangement/alignment/spacedBy/Absolute 组合 |
| `remote-creation-compose/src/androidTest/.../BasicLayoutTest.kt` | 多个测试 | RemoteRow 与 RemoteBox/RemoteColumn 嵌套 |

---

## 5. RemoteText

RemoteText 是使用最广泛的组件，以下列出主要使用位置：

### integration-tests/demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../modifier/ClickableDemo.kt` | `ClickableDemo()` | `RemoteText("Tap me!")` / `RemoteText("Clicks: ".rs + clickCounter.toRemoteString())` |
| `integration-tests/demos/.../modifier/CombinedClickableDemo.kt` | `CombinedClickableDemo()` | `RemoteText("Single: ".rs + clickCounter.toRemoteString(), color = Color.Black.rc)` |
| `integration-tests/demos/.../modifier/PaddingDemo.kt` | `PaddingDemo()` | `RemoteText("Padding Start: 20dp")` |
| `integration-tests/demos/.../modifier/RotateDemo.kt` | `RotateDemo()` | `RemoteText("Rotate: 0f")` / `RemoteText("Rotate: 45f")` |
| `integration-tests/demos/.../integration/GesturePropagationDemo.kt` | `GesturePropagationDemo()` | `RemoteText("HostAction.".rs)` / `RemoteText("ValueChange click counter: ".rs + ...)` |

### integration-tests/player-view-demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../widgets/MyWidget.kt` | `Button()` | `RemoteText(text, fontSize = 32.rsp, color = RemoteColor(Color.White))` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | 多个函数 | `RemoteText("Rio de Janeiro")` / `RemoteText("100º", fontSize = 26.rsp)` |
| `integration-tests/player-view-demos/.../examples/SwitchWidget.kt` | `SwitchWidget()` | `RemoteText("ON".rs)` / `RemoteText("OFF".rs)` |
| `integration-tests/player-view-demos/.../examples/TestDrawContentDemo.kt` | `TestDrawContentDemo()` | `RemoteText(text = "Hello world!")` |

### remote-creation-compose/src/test/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | 多个测试 | `RemoteText(text = "Hello V2".rs)` / `RemoteText(text = "State $state".rs)` |
| `remote-creation-compose/src/test/.../state/RemoteStateCreationTest.kt` | `testNamedRemoteString()` | `RemoteText(text = namedString)` |

### remote-creation-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/androidTest/.../v2/CaptureRemoteDocumentV2Test.kt` | 多个测试 | `RemoteText(text = "Hello world!".rs)` / `RemoteText(text = "Hello world!".rs, fontSize = 30.rsp)` |
| `remote-creation-compose/src/androidTest/.../layout/RemoteRowTest.kt` | `alignByBaseline()` | `RemoteText(text = "Large String", fontSize = 40.rsp)` / `RemoteText(text = "Small String", fontSize = 14.rsp)` |
| `remote-creation-compose/src/androidTest/.../layout/StateLayoutTest.kt` | 多个测试 | `RemoteText("State $state".rs)` |
| `remote-creation-compose/src/androidTest/.../a11y/BasicA11yTest.kt` | 多个测试 | `RemoteText("Item 1.1")` |

### remote-player-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-player-compose/src/androidTest/.../state/RemoteStateTest.kt` | `testWidthState()` | `RemoteText(RemoteString("Width: ") + width.toRemoteString(...))` |
| `remote-player-compose/src/androidTest/.../BlendModeTest.kt` | `RemoteBlendModeVisual()` | `RemoteText(name, fontSize = 12.rsp)` |

---

## 6. RemoteImage

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../player/BitmapLoaderDemo.kt` | `BitmapLoaderDemo()` | `RemoteImage(remoteBitmap = bitmap, contentDescription = "Color $hex".rs, modifier = RemoteModifier.size(100.rdp, 100.rdp))` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherContent()` | `RemoteImage(image, RemoteString(""), modifier = RemoteModifier.size(48.rdp))` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherDetail()` | `RemoteImage(refresh, RemoteString(""), modifier = RemoteModifier.size(20.rdp))` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `HourlyWeather()` | `RemoteImage(image, RemoteString(""), RemoteModifier.size(24.rdp))` |
| `remote-creation-compose/src/androidTest/.../layout/RemoteImageTest.kt` | `RemoteImageTest` | `RemoteImage(avatarImage, contentDescription = "background".rs, modifier = RemoteModifier.size(size.rdp))` — 专门测试类，含 ImageBitmap 版、RemoteBitmap 版、URL 版、ContentScale 版 |

---

## 7. RemoteCanvas

### integration-tests/player-view-demos/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../examples/SwitchWidget.kt` | `SwitchWidgetOnState()` | `RemoteCanvas(modifier = RemoteModifier.size(32.rdp)) { drawCircle(paint = paint, radius = 34f.rf) }` |
| `integration-tests/player-view-demos/.../examples/SwitchWidget.kt` | `SwitchWidgetOffState()` | `RemoteCanvas(modifier = RemoteModifier.size(20.rdp)) { drawCircle(paint = paint, radius = 34f.rf) }` |
| `integration-tests/player-view-demos/.../examples/ScrollView.kt` | 日历组件 | `RemoteCanvas(modifier = RemoteModifier.size(20.rdp)) { drawAnchoredText(...) }` |
| `integration-tests/player-view-demos/.../examples/PathChecks.kt` | `SimplePath()` | `RemoteCanvas(modifier = RemoteModifier.fillMaxWidth().fillMaxHeight()) { drawRect(...) }` |
| `integration-tests/player-view-demos/.../examples/Clock.kt` | `Clock()` | `RemoteCanvas(modifier = RemoteModifier.fillMaxWidth().fillMaxHeight()) { val w = remote.component.width ... }` |
| `integration-tests/player-view-demos/.../examples/old/compose/AnimatedChangesDemo.kt` | `AnimatedChangesDemo()` | `RemoteCanvas(modifier = RemoteModifier.fillMaxSize().background(Color.White)) { ... }` |

### remote-creation-compose/src/test/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/test/.../capture/CaptureRemoteDocumentTest.kt` | `testCapture()` | `RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { drawRect(paint = redPaint) }` |

### remote-creation-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-creation-compose/src/androidTest/.../layout/RemoteCanvasTest.kt` | `RemoteCanvasTest` | 专门测试类，含 drawAnchoredText、drawRect、drawCircle 等多种绘制测试 |
| `remote-creation-compose/src/androidTest/.../layout/RemoteCanvasScreenshotTest.kt` | `RemoteCanvasScreenshotTest` | RemoteCanvas 截图测试 |
| `remote-creation-compose/src/androidTest/.../vector/RemoteVectorPainterTest.kt` | 多个测试 | `RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { with(painter) { onDraw() } }` |
| `remote-creation-compose/src/androidTest/.../capture/shapes/RemoteShapeTest.kt` | 测试方法 | `RemoteCanvas(RemoteModifier.width(100.rdp).height(100.rdp)) { ... }` |

### remote-player-compose/src/androidTest/

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `remote-player-compose/src/androidTest/.../BlendModeTest.kt` | `RemoteBlendModeVisual()` | `RemoteCanvas(RemoteModifier.size(100.rdp)) { ... }` |

---

## 8. RemoteSpacer

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../integration/GesturePropagationDemo.kt` | `GesturePropagationDemo()` | `RemoteSpacer(modifier = RemoteModifier.width(5.rdp))` / `RemoteSpacer(modifier = RemoteModifier.height(10.rdp))` |
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testBasicDocument()` | `RemoteSpacer(modifier = RemoteModifier.weight(1f))` |
| `remote-creation-compose/src/androidTest/.../layout/RemoteSpacerTest.kt` | `RemoteSpacerTest` | `RemoteSpacer(modifier = RemoteModifier.size(100.rdp).background(Color.Red))` — 专门测试类 |

---

## 9. RemoteStateLayout

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../layout/RemoteStateLayoutDemos.kt` | `RemoteStateLayoutSimpleDemo()` | `RemoteStateLayout(state = remoteState, states = states) { state -> val color = when (state) { 0 -> Color.Red ... } }` |
| `integration-tests/demos/.../layout/RemoteBoxDemos.kt` | `RemoteBoxAlignmentsDemo()` | `RemoteStateLayout(modifier = ..., state = currentState, states = alignments.map { it.first }.toIntArray())` |
| `integration-tests/player-view-demos/.../examples/SwitchWidget.kt` | `SwitchWidget()` | `RemoteStateLayout(modifier = RemoteModifier.wrapContentSize(), state = value) { state -> when (state) { Off -> ...; On -> ... } }` |
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testStateLayout()` | `RemoteStateLayout(state = checked) { state -> RemoteText(text = "State $state".rs) }` |
| `remote-creation-compose/src/androidTest/.../layout/StateLayoutTest.kt` | `StateLayoutTest` | 专门测试类，含 Enum/Boolean/Int 三种状态类型测试 |
| `remote-creation-compose/src/androidTest/.../BasicLayoutTest.kt` | 多个测试 | `RemoteStateLayout(state = checked, modifier = RemoteModifier.fillMaxSize()) { state -> when (state) { ... } }` |

---

## 10. RemoteCollapsibleColumn

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `DemoWeather()` | `RemoteCollapsibleColumn(modifier = RemoteModifier.fillMaxWidth().widthIn(min = 50.rdp), horizontalAlignment = RemoteAlignment.CenterHorizontally)` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherDays()` | `RemoteCollapsibleColumn(modifier = RemoteModifier.fillMaxWidth().padding(16.rdp), verticalArrangement = RemoteArrangement.spacedBy(2.rdp))` |
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testCollapsibleColumnRow()` | `RemoteCollapsibleColumn { RemoteText(text = "Fixed"); RemoteCollapsibleRow(modifier = RemoteModifier.weight(1f)) { ... } }` |
| `remote-creation-compose/src/androidTest/.../layout/RemoteCollapsibleColumnTest.kt` | `RemoteCollapsibleColumnTest` | 专门测试类，含各种 arrangement/alignment/spacedBy 组合 |

---

## 11. RemoteCollapsibleRow

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherRow()` | `RemoteCollapsibleRow(modifier = RemoteModifier.fillMaxWidth().heightIn(min = rowHeight2), horizontalArrangement = RemoteArrangement.SpaceBetween, verticalAlignment = RemoteAlignment.CenterVertically)` |
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testCollapsibleColumnRow()` | `RemoteCollapsibleRow(modifier = RemoteModifier.weight(1f)) { RemoteText(text = "Weighted Row Content") }` |
| `remote-creation-compose/src/androidTest/.../layout/RemoteCollapsibleRowTest.kt` | `RemoteCollapsibleRowTest` | 专门测试类，含各种 arrangement/alignment/spacedBy 组合 |

---

## 12. RemoteFlowRow

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/demos/.../layout/RemoteFlowRowDemos.kt` | `RemoteFlowRowDemo()` | `RemoteFlowRow(modifier = RemoteModifier.size(RemoteDp(200.dp)).background(RemoteColor(Color.LightGray)), maxItemsInEachRow = 3)` |
| `remote-creation-compose/src/androidTest/.../layout/RemoteFlowRowTest.kt` | `RemoteFlowRowTest` | 专门测试类，含 outOfBounds/arrangement/spacedBy/maxItemsInEachRow/maxLines 等测试 |

---

## 13. FitBox

| 文件路径 | 函数/类 | 代码片段 |
|---------|---------|---------|
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `DemoWeather()` | `FitBox(RemoteModifier.fillMaxSize(), verticalArrangement = RemoteArrangement.Top) { ... }` |
| `integration-tests/player-view-demos/.../examples/DemoWeather.kt` | `WeatherDays()` | `FitBox(modifier = RemoteModifier.fillMaxWidth()) { RemoteRow(...) }` |
| `integration-tests/player-view-demos/.../examples/RcFitBox.kt` | `RcFitBox()` | DSL 方式创建 FitBox demo |
| `remote-creation-compose/src/test/.../RemoteComposeTest.kt` | `testFitBox()` | `FitBox { RemoteText(text = "Fit Content") }` |
| `remote-creation-compose/src/androidTest/.../layout/FitBoxTest.kt` | `FitBoxTest` | 专门测试类，含各种 alignment/arrangement 组合 |

---

## 14. 综合示例：DemoWeather.kt

**文件路径**: `integration-tests/player-view-demos/.../examples/DemoWeather.kt`

这是项目中最全面的布局组件综合示例，使用了 **9 种**布局组件：

| 组件 | 使用位置 | 用途 |
|------|---------|------|
| FitBox | `DemoWeather()` | 顶层自适应容器 |
| RemoteCollapsibleColumn | `DemoWeather()`, `WeatherDays()` | 可折叠垂直布局，空间不足时折叠 |
| RemoteColumn | `DemoWeather()`, `WeatherDetail()` | 垂直排列天气信息 |
| RemoteRow | `WeatherContent()`, `WeatherDays()`, `Temp()` | 水平排列天气数据 |
| RemoteCollapsibleRow | `WeatherRow()` | 可折叠水平行 |
| RemoteBox | `Temp()` | 内容对齐容器 |
| RemoteText | 多处 | 显示温度、城市名、天气描述 |
| RemoteImage | `WeatherContent()`, `WeatherDetail()`, `HourlyWeather()`, `Temp()` | 显示天气图标 |
| RemoteCanvas | 未直接使用 | — |

---

## 15. 专用测试文件索引

每个布局组件对应的专门测试类（位于 `remote-creation-compose/src/androidTest/.../layout/`）：

| 组件 | 测试文件 | 说明 |
|------|---------|------|
| RemoteBox | `RemoteBoxTest.kt` | 测试各种 alignment 组合 |
| RemoteColumn | `RemoteColumnTest.kt` | 测试 arrangement/alignment/spacedBy |
| RemoteRow | `RemoteRowTest.kt` | 测试 arrangement/alignment/spacedBy/Absolute |
| RemoteText | 无专门测试 | 在其他测试中广泛使用 |
| RemoteImage | `RemoteImageTest.kt` | 测试 ImageBitmap/RemoteBitmap/URL/ContentScale |
| RemoteCanvas | `RemoteCanvasTest.kt` + `RemoteCanvasScreenshotTest.kt` | 测试绘制和截图 |
| RemoteSpacer | `RemoteSpacerTest.kt` | 测试尺寸和背景 |
| RemoteStateLayout | `StateLayoutTest.kt` | 测试 Enum/Boolean/Int 状态 |
| RemoteCollapsibleColumn | `RemoteCollapsibleColumnTest.kt` | 测试可折叠列 |
| RemoteCollapsibleRow | `RemoteCollapsibleRowTest.kt` | 测试可折叠行 |
| RemoteFlowRow | `RemoteFlowRowTest.kt` | 测试流式布局 |
| FitBox | `FitBoxTest.kt` | 测试自适应盒子 |
