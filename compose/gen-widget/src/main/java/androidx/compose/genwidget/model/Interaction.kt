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
 * 交互定义 - 组件的点击行为
 */
data class Interaction(
    @SerializedName("componentId") val componentId: Int,
    @SerializedName("type") val type: String,
    @SerializedName("action") val action: Action
)

data class Action(
    @SerializedName("type") val type: String,
    @SerializedName("actionId") val actionId: Int? = null,
    @SerializedName("targetValueId") val targetValueId: Int? = null,
    @SerializedName("newValue") val newValue: Int? = null,
    @SerializedName("metadata") val metadata: String? = null,
    @SerializedName("intent") val intent: Intent? = null
)

data class Intent(
    @SerializedName("action") val action: String,
    @SerializedName("data") val data: String? = null
)

data class ActionDefinition(
    @SerializedName("actionId") val actionId: Int,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String? = null
)

/**
 * 点击动作 - 直接附加在组件上
 */
data class ClickAction(
    @SerializedName("actionId") val actionId: Int,
    @SerializedName("metadata") val metadata: String? = null,
    @SerializedName("type") val type: String = "host_action",
    @SerializedName("targetValueId") val targetValueId: Int? = null,
    @SerializedName("newValue") val newValue: Int? = null
)
