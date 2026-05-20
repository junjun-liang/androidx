/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.genwidget.model

import com.google.gson.annotations.SerializedName

/**
 * 微件规格说明 - LLM 输出的 JSON 映射到的数据结构
 */
data class WidgetSpec(
    @SerializedName("widget") val widget: WidgetData
)

data class WidgetData(
    @SerializedName("type") val type: String,
    @SerializedName("version") val version: Int = 1,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("density") val density: Int = 4,
    @SerializedName("profile") val profile: String = "WIDGETS_V7",
    @SerializedName("components") val components: List<Component>,
    @SerializedName("resources") val resources: Resources,
    @SerializedName("interactions") val interactions: List<Interaction>? = null,
    @SerializedName("actions") val actions: List<ActionDefinition>? = null
)

data class Component(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: Int,
    @SerializedName("layout") val layout: LayoutParams? = null,
    @SerializedName("modifier") val modifier: Modifier? = null,
    @SerializedName("children") val children: List<Component>? = null,
    @SerializedName("textResourceId") val textResourceId: Int? = null,
    @SerializedName("bitmapResourceId") val bitmapResourceId: Int? = null,
    @SerializedName("style") val style: TextStyle? = null,
    @SerializedName("size") val size: Size? = null,
    @SerializedName("clickAction") val clickAction: ClickAction? = null
)

data class LayoutParams(
    @SerializedName("horizontalAlignment") val horizontalAlignment: String = "START",
    @SerializedName("verticalArrangement") val verticalArrangement: String = "TOP",
    @SerializedName("horizontalArrangement") val horizontalArrangement: String? = null,
    @SerializedName("verticalAlignment") val verticalAlignment: String? = null
)

data class Modifier(
    @SerializedName("fillMaxSize") val fillMaxSize: Boolean? = null,
    @SerializedName("fillMaxWidth") val fillMaxWidth: Boolean? = null,
    @SerializedName("fillMaxHeight") val fillMaxHeight: Boolean? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("padding") val padding: Padding? = null,
    @SerializedName("background") val background: String? = null,
    @SerializedName("clip") val clip: String? = null
)

data class Padding(
    @SerializedName("left") val left: Int = 0,
    @SerializedName("top") val top: Int = 0,
    @SerializedName("right") val right: Int = 0,
    @SerializedName("bottom") val bottom: Int = 0
)

data class TextStyle(
    @SerializedName("fontSize") val fontSize: Float,
    @SerializedName("fontWeight") val fontWeight: Int = 400,
    @SerializedName("color") val color: String = "#000000",
    @SerializedName("textAlign") val textAlign: String = "LEFT"
)

data class Size(
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

data class Resources(
    @SerializedName("texts") val texts: List<TextResource>,
    @SerializedName("bitmaps") val bitmaps: List<BitmapResource>
)

data class TextResource(
    @SerializedName("id") val id: Int,
    @SerializedName("content") val content: String,
    @SerializedName("description") val description: String? = null
)

data class BitmapResource(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("format") val format: String = "PNG",
    @SerializedName("description") val description: String? = null
)
