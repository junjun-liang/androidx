---
name: Scaffold Remote Component
description: 为新的 RemoteCompose 组件搭建脚手架，包含测试、示例和预览
---

# 搭建 Remote 组件脚手架

本技能为新的 RemoteCompose Creation 组件/修饰器搭建脚手架，自动生成组件实现、示例、预览和截图测试所需的样板代码。

由于本技能位于 `remote-creation-compose` 模块内，因此专门用于为该模块生成组件。

### 所需输入
确保你知道新组件的名称（例如 `RemoteBox`）。如果用户未指定，请在继续之前询问。将其记为 `$COMPONENT_NAME`。

**需要记住的关键架构规则**：
* **描述性命名**：预览或测试变体的名称必须描述它们*包含*什么（例如 `${COMPONENT_NAME}WithColors`），而不是否定它们省略的内容。
* **独立的 Composable**：不同的预览状态必须分解为独立的顶层 `@Composable` 函数。
* **组件测试**：测试必须直接调用预览文件中定义的显式 `*Preview` composables 或状态 composables。
* **onClick 参数**：在示例、预览和测试中，使用 `Action.Empty` 来模拟任何虚拟的 `onClick` 动作。

### 步骤 1：创建组件实现
根据组件是布局（如 `RemoteBox`、`RemoteColumn`）还是修饰器（如 `AlphaModifier`），在 remote-creation-compose 项目根目录下创建文件：
`src/main/java/androidx/compose/remote/creation/compose/layout/$COMPONENT_NAME.kt`
或
`src/main/java/androidx/compose/remote/creation/compose/modifier/$COMPONENT_NAME.kt`

使用此基础模板：
```kotlin
package androidx.compose.remote.creation.compose.layout // 或 .modifier

import androidx.compose.runtime.Composable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier

@Composable
public fun $COMPONENT_NAME(
    modifier: RemoteModifier = RemoteModifier,
    // 在此处添加其他相关参数
) {
    // TODO: 实现组件
}
```

记得在步骤 2 创建示例后，使用 `@sample` 标签将示例 composable 链接到组件的 KDoc 中。

### 步骤 2：创建示例
在 remote-creation-compose 项目根目录下创建示例文件：
`samples/src/main/java/androidx/compose/remote/creation/compose/samples/${COMPONENT_NAME}Sample.kt`

使用此基础模板：
```kotlin
package androidx.compose.remote.creation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun ${COMPONENT_NAME}Sample() {
    // TODO: 实现示例函数
}
```

确保使用 `@sample` 在步骤 1 创建的对应组件/修饰器的 KDoc 中链接此示例。

### 步骤 3：创建预览
在 remote-creation-compose 项目根目录下创建预览文件：
`samples/src/main/java/androidx/compose/remote/creation/compose/previews/${COMPONENT_NAME}Preview.kt`

使用此基础模板，将组件分离为清晰的描述性变体，描述它们包含的内容：

```kotlin
package androidx.compose.remote.creation.compose.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper

@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun ${COMPONENT_NAME}DefaultPreview() {
    // TODO: 应用默认预览实现
}
```

### 步骤 4：创建截图测试
在 remote-creation-compose 项目根目录下创建测试文件，根据组件是布局还是修饰器：
`src/androidTest/java/androidx/compose/remote/creation/compose/layout/${COMPONENT_NAME}ScreenshotTest.kt`
或
`src/androidTest/java/androidx/compose/remote/creation/compose/modifier/${COMPONENT_NAME}ScreenshotTest.kt`

使用此样板代码，确保为你生成的每个显式预览变体都存在单独的 `@Test` 方法：
```kotlin
package androidx.compose.remote.creation.compose.layout // 或 .modifier

import androidx.compose.remote.creation.compose.layout.previews.${COMPONENT_NAME}Default
import androidx.compose.remote.creation.compose.modifier.samples.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ${COMPONENT_NAME}ScreenshotTest {
    @get:Rule
    val screenshotRule = RemoteScreenshotTestRule(
        moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
        context = ApplicationProvider.getApplicationContext(),
    )

    @Test
    fun test${COMPONENT_NAME}Default() {
        screenshotRule.runScreenshotTest {
            ${COMPONENT_NAME}Default()
        }
    }
}
```

### 步骤 5：创建示例的截图测试
在 remote-creation-compose 项目根目录下创建测试文件，根据组件是布局还是修饰器：
`src/androidTest/java/androidx/compose/remote/creation/compose/layout/samples/${COMPONENT_NAME}SampleScreenshotTest.kt`
或
`src/androidTest/java/androidx/compose/remote/creation/compose/modifier/samples/${COMPONENT_NAME}SampleScreenshotTest.kt`

使用此样板代码进行测试：
```kotlin
package androidx.compose.remote.creation.compose.layout.samples // 或 .modifier.samples

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.Center
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.samples.${COMPONENT_NAME}Sample
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ${COMPONENT_NAME}SampleScreenshotTest {
    @get:Rule
    val composeTestRule: RemoteScreenshotTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun ${COMPONENT_NAME}Sample() =
        composeTestRule.runScreenshotTest {
            RemoteBox(modifier = RemoteModifier.fillMaxSize(), contentAlignment = Center) {
                ${COMPONENT_NAME}Sample()
            }
        }
}
```

### 步骤 6：完成
通知用户组件样板代码已生成，并询问他们是否现在想要为 $COMPONENT_NAME 组件实现具体行为。
