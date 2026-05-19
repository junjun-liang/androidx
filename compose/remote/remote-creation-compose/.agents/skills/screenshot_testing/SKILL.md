---
name: build_screenshot_tests
description: 在 Remote Compose 中使用 RemoteScreenshotTestRule 构建仪器化截图测试的技能
---

# 使用 RemoteScreenshotTestRule 构建截图测试

> [!IMPORTANT]
> **AI 指示：** 不要假设所有测试都应该使用 `RemoteScreenshotTestRule`。如果用户要求你编写截图测试，你**必须**首先明确询问他们："你想让我使用 `RemoteScreenshotTestRule` 来编写这个测试吗？"只有在他们确认后才使用此技能。

本技能为 `@compose/remote/remote-creation-compose` 项目中的截图测试提供指导。

## 目的

`RemoteScreenshotTestRule` 是一个 JUnit 规则，允许对 Remote Compose 组件进行截图。它处理：
1. 捕获远程文档。
2. 使用 `RemoteDocumentPlayer` 渲染它。
3. 将渲染的图像与"金标"（golden）截图进行验证。

## 设置

1. **测试类注解**：
   使用 `@MediumTest`、`@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)` 和 `@RunWith(AndroidJUnit4::class)` 注解测试类。

2. **添加规则**：
   在测试类内部定义规则。
   ```kotlin
   @get:Rule
   val composeTestRule: RemoteScreenshotTestRule by lazy {
       RemoteScreenshotTestRule(
           moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
           context = ApplicationProvider.getApplicationContext(),
           matcher = MSSIMMatcher(threshold = 0.999),
       )
   }
   ```
   *注意：`SCREENSHOT_GOLDEN_DIRECTORY` 通常在模块中定义，例如 `androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY`。*

## 如何使用 `RemoteScreenshotTestRule`

### 1. 基本用法（直接测试）

使用 `runScreenshotTest` 并提供一个包含要测试的 Remote Composable 内容的 lambda。

```kotlin
@Test
fun simpleTest() {
    composeTestRule.runScreenshotTest {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize().background(Color.Red)
        )
    }
}
```

### 2. 覆盖配置

如果需要，可以覆盖 profile、布局方向或提供外部内容。

```kotlin
@Test
fun alignByBaseline() {
    composeTestRule.runScreenshotTest(profile = TestProfiles.androidXExperimental) {
        RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
            RemoteRow(modifier = RemoteModifier.fillMaxWidth()) {
                RemoteText(
                    text = "Large String",
                    fontSize = 40.rsp,
                    modifier = RemoteModifier.alignByBaseline(),
                )
                RemoteText(
                    text = "Small String",
                    fontSize = 14.rsp,
                    modifier = RemoteModifier.alignByBaseline(),
                )
            }
        }
    }
}
```

### 3. 网格测试（可选）

为了高效测试多个变体，可以将此规则与 `GridScreenshotUI` 结合使用。
有关使用网格工具的详细说明，请参阅 `build_grid_screenshot_tests` 技能。

## 最佳实践

- **阈值**：使用 `MSSIMMatcher(threshold = 0.999)` 进行高精度匹配。
- **金标目录**：确保 `moduleDirectory` 指向正确的金标资源文件夹。
- **Remote 范围**：在 `runScreenshotTest` lambda 内部，你处于 `@RemoteComposable @Composable` 范围。确保使用 `RemoteModifier` 和 Remote 组件（例如 `RemoteBox`、`RemoteText`）。
