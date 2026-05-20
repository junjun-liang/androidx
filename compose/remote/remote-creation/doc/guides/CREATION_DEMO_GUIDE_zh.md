# 编写 RemoteCompose Creation 演示指南

本指南介绍如何使用 `remote-creation` API（不使用 `@Composable`）编写过程式绘图演示。这些演示用于测试 RemoteCompose 的核心绘图、布局和表达式引擎。

## 核心原则

- **过程式逻辑**：你编写的代码会向 `RemoteComposeWriter` *发射* 一系列操作。
- **远程类型**：使用 `RemoteComposeWriter`（或 `RemoteComposeWriterAndroid`）定义常量、表达式和绘图命令。
- **无状态执行**：演示函数理想情况下应该是一个返回 `RemoteComposeWriter` 的静态方法。

## Creation 演示的结构

一个典型的演示包含：
1. **平台服务**：初始化 `RcPlatformServices`。
2. **Writer 初始化**：创建 `RemoteComposeWriterAndroid`。
3. **变量和表达式**：定义 `floatConstant`、`floatExpression` 等。
4. **布局根节点**：使用 `rc.root(...)` 启动组件树。
5. **布局和绘图**：使用 `box`、`column`、`row` 和 `startCanvas` 进行结构化和绘图。

### 示例模板（Java）

```java
import static androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;

import android.graphics.Color;
import android.graphics.Paint;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;
import androidx.compose.remote.core.RcPlatformServices;

public class MyDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    public static RemoteComposeWriterAndroid createDemo() {
        // 1. 初始化 Writer（宽度、高度、名称、版本、profile、平台）
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(
                500, 500, "MyDemo", 7, PROFILE_ANDROIDX, sPlatform);

        // 2. 定义动态变量
        float time = RemoteComposeWriterAndroid.TIME_IN_CONTINUOUS_SEC;
        float speed = rc.addFloatConstant(2.0f);
        float animatedValue = rc.floatExpression(time, speed, MUL);

        // 3. 定义 UI 根节点
        rc.root(() -> {
            rc.box(new RecordingModifier().fillMaxSize().background(Color.LTGRAY), () -> {
                rc.startCanvas(new RecordingModifier().size(200, 200));
                
                // 绘图操作
                rc.getPainter()
                  .setColor(Color.BLUE)
                  .setStyle(Paint.Style.FILL)
                  .commit();
                  
                rc.drawCircle(100, 100, 50);
                
                rc.endCanvas();
            });
        });

        return rc;
    }
}
```

## 关键 API 章节

### 1. 表达式 (`rc.floatExpression`)
表达式允许你定义在播放器（远程端）运行的数学运算，无需重新发送整个文档。
- `rc.addFloatConstant(value)`
- `rc.floatExpression(a, b, OP)`，其中 `OP` 来自 `Rc.FloatExpression`（例如 `ADD`、`MUL`、`SIN`、`COS`、`PINGPONG`）。

### 2. Painter (`rc.getPainter()`)
`RemotePaint` 桥允许你设置标准的 Android `Paint` 属性，然后将其序列化。
- **重要**：设置属性后必须调用 `.commit()` 以同步到 writer。
- 示例：`rc.getPainter().setColor(Color.RED).setStrokeWidth(5).commit();`

### 3. 布局组件
- `rc.box(modifier, content)`
- `rc.column(modifier, horizontalAlign, verticalAlign, content)`
- `rc.row(modifier, horizontalAlign, verticalAlign, content)`
- `rc.startCanvas(modifier)` / `rc.endCanvas()`

### 4. 修饰器 (`RecordingModifier`)
修饰器通过链式调用来定义布局行为。
- `.fillMaxSize()`、`.fillMaxWidth()`、`.fillMaxHeight()`
- `.width(w)`、`.height(h)`、`.size(w, h)`
- `.padding(p)`、`.background(color)`
- `.visibility(id)`（接受远程 ID）

### 5. 高级绘图
- **路径**：`rc.addPathData(remotePath)`、`rc.drawPath(pathId)`。
- **文本**：`rc.createTextFromFloat(...)`、`rc.drawTextAnchored(...)`。
- **循环**：`rc.startLoopVar(start, step, end)`、`rc.endLoop()`。

## 注册

要在 Player View Demos 应用中显示演示，必须在 `DemosCreation.java` 中使用 `getp` 注册：

```java
// 在 DemosCreation.java 中
getp("Category/MyDemoName", MyDemo::createDemo),
```

- `getp(path, supplier)`：标准过程式演示。
- `getpc(path, supplier)`：可能需要上下文（如 `Activity`）的过程式演示。

## 请求创建新演示时的提示

当要求 agent 创建新演示时：
1. **指定目标**："创建一个展示 [功能，例如路径变形] 的 creation 演示。"
2. **提及 API**："使用 `RemoteComposeWriterAndroid` 和过程式布局。"
3. **目标文件**："将其添加到 `integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/examples/`。"
4. **注册**："在 `DemosCreation.java` 中注册。"
