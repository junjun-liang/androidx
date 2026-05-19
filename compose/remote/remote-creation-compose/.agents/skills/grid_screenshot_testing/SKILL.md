---
name: build_grid_screenshot_tests
description: 在 Remote Compose 中使用 GridScreenshotUI 构建仪器化测试的技能
---

# 使用 GridScreenshotUI 构建仪器化测试

> [!IMPORTANT]
> **AI 指示：** 不要假设此目录中的所有测试都应该使用 `GridScreenshotUI`。如果用户要求你编写截图测试，你**必须**首先明确询问他们："你想让我使用 `GridScreenshotUI` 工具来编写这个测试吗？"只有在他们确认后才使用此技能。

本技能为 `@compose/remote/remote-creation-compose` 项目中使用 `GridScreenshotUI` 构建截图测试提供指导。

## 目的

`GridScreenshotUI` 是一个工具类，设计用于将多个小型 Remote UI 组件以网格形式布局。这在截图测试中特别有用，因为它允许你在单个截图中捕获组件的许多变体（例如不同的对齐方式、修饰器或排列方式），使测试更高效且更易于视觉比较。

## 如何使用 `GridScreenshotUI`

1. **测试类设置**：
   创建一个测试类，使用 `@MediumTest`、`@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)` 和 `@RunWith(AndroidJUnit4::class)` 注解。

2. **添加截图规则**：
   定义一个 `RemoteScreenshotTestRule`，明确指定模块目录和匹配器。
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

3. **实例化 GridScreenshotUI**：
   在测试类中创建 `GridScreenshotUI` 的实例。
   ```kotlin
   private val gridScreenshotUI = GridScreenshotUI()
   ```

4. **定义 UI 变体**：
   创建一个方法或变量，提供 `Pair<String, @RemoteComposable @Composable () -> Unit>` 的列表。字符串是变体的标签，lambda 是实际的 Compose UI 内容。
   如果你不想手动指定标签，可以使用 `GridScreenshotUI.Companion` 中定义的 `.toInput()` 扩展函数，轻松将 composable 列表转换为所需的 pair 格式。

5. **编写测试**：
   使用 `composeTestRule.runScreenshotTest` 并调用 `gridScreenshotUI.GridContent(...)` 传入你的变体列表。
   ```kotlin
   @Test
   fun exampleGridTest() =
       composeTestRule.runScreenshotTest {
           gridScreenshotUI.GridContent(getLayoutAlignmentUIs())
       }
   ```

## 最佳实践
- **重用尺寸**：使用 `GridScreenshotUI.Companion.DefaultContainerSize` 在不同测试中保持一致的容器尺寸。
- **RTL 测试**：你可以通过传递 `layoutDirection = LayoutDirection.Rtl` 给 `GridContent` 来轻松测试 RTL（从右到左）布局。
  ```kotlin
  gridScreenshotUI.GridContent(
      getLayoutAlignmentUIs(),
      layoutDirection = LayoutDirection.Rtl,
  )
  ```
- **辅助方法**：如果你有参数的组合爆炸（例如通过 `sequence` 尝试所有 `Arrangements` 和 `Alignments` 的组合），创建构建器方法来生成 `Pair` 项列表。
  ```kotlin
  private fun getLayoutAlignmentUIs(): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
      sequence {
          for (alignment in alignments) {
              for (arrangement in arrangements) {
                  yield(
                      "${alignment.propertyName()} ${arrangement.propertyName()}" to
                          @RemoteComposable @Composable {
                              RemoteRow(
                                  modifier = RemoteModifier.size(DefaultContainerSize),
                                  horizontalArrangement = arrangement,
                                  verticalAlignment = alignment,
                              ) {
                                  // 你的内容在这里
                              }
                          }
                  )
              }
          }
      }.toList()
  ```
