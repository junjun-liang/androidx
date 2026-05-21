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

package com.yingjie.androidaiagent.data.widget.compiler

import android.annotation.SuppressLint
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.WireBuffer
import com.google.gson.Gson
import com.yingjie.androidaiagent.data.widget.model.*
import com.yingjie.androidaiagent.data.widget.model.WidgetSpec

/**
 * Widget 编译器 - 将 WidgetSpec 编译为 Wire Format 二进制数据
 */
@SuppressLint("RestrictedApi")
object WidgetCompiler {

    private val gson = Gson()

    /**
     * 从 JSON 字符串编译为 Wire Format
     */
    fun compileFromJson(jsonString: String): ByteArray {
        val spec = gson.fromJson(jsonString, WidgetSpec::class.java)
        return compile(spec)
    }

    /**
     * 从 WidgetSpec 编译为 Wire Format
     */
    fun compile(spec: WidgetSpec): ByteArray {
        val buffer = WireBuffer()
        val widget = spec.widget

        // 1. 写入头部信息
        writeHeader(buffer, widget)

        // 2. 注册资源（文本和位图）
        writeResources(buffer, widget.resources)

        // 3. 写入组件树
        writeComponentTree(buffer, widget.components)

        return buffer.buffer
    }

    /**
     * 写入头部信息
     */
    private fun writeHeader(buffer: WireBuffer, widget: WidgetData) {
        buffer.writeByte(Operations.HEADER)
        buffer.writeInt(widget.version)
        buffer.writeInt(widget.width)
        buffer.writeInt(widget.height)
        buffer.writeInt(widget.density)
        buffer.writeLong(getProfileCode(widget.profile))
    }

    /**
     * 获取 Profile 代码
     */
    private fun getProfileCode(profile: String): Long {
        return when (profile) {
            "WIDGETS_V7" -> 0x0000000000000001L
            "ANDROIDX_EXPERIMENTAL" -> 0x0000000000000002L
            else -> 0x0000000000000001L
        }
    }

    /**
     * 注册资源
     */
    private fun writeResources(buffer: WireBuffer, resources: Resources) {
        // 注册文本资源
        resources.texts.forEach { text ->
            buffer.writeByte(Operations.DATA_TEXT)
            buffer.writeInt(text.id)
            // 写入字符串：先写入长度，再写入字节
            val textBytes = text.content.toByteArray(Charsets.UTF_8)
            buffer.writeInt(textBytes.size)
            textBytes.forEach { b -> buffer.writeByte(b.toInt()) }
        }

        // 注册位图资源
        resources.bitmaps.forEach { bitmap ->
            buffer.writeByte(Operations.DATA_BITMAP)
            buffer.writeInt(bitmap.id)
            buffer.writeInt(bitmap.width)
            buffer.writeInt(bitmap.height)
            // 注意：实际应用中需要写入真实的像素数据
            // 这里只写入占位符
            buffer.writeInt(0) // 像素数据大小
        }
    }

    /**
     * 写入组件树
     */
    private fun writeComponentTree(buffer: WireBuffer, components: List<Component>) {
        components.forEach { component ->
            writeComponent(buffer, component)
        }
    }

    /**
     * 写入单个组件
     */
    private fun writeComponent(buffer: WireBuffer, component: Component) {
        when (component.type) {
            "Column" -> writeColumn(buffer, component)
            "Row" -> writeRow(buffer, component)
            "Box" -> writeBox(buffer, component)
            "Text" -> writeText(buffer, component)
            "Image" -> writeImage(buffer, component)
            "Canvas" -> writeCanvas(buffer, component)
            else -> throw IllegalArgumentException("Unknown component type: ${component.type}")
        }
    }

    /**
     * 写入 Column 组件
     */
    private fun writeColumn(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.COMPONENT_START)
        buffer.writeInt(component.id)
        buffer.writeInt(LayoutType.COLUMN)

        // 写入修饰器
        component.modifier?.let { writeModifier(buffer, it) }

        // 写入子组件
        component.children?.forEach { child ->
            writeComponent(buffer, child)
        }

        buffer.writeByte(Operations.CONTAINER_END)
    }

    /**
     * 写入 Row 组件
     */
    private fun writeRow(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.COMPONENT_START)
        buffer.writeInt(component.id)
        buffer.writeInt(LayoutType.ROW)

        component.modifier?.let { writeModifier(buffer, it) }

        component.children?.forEach { child ->
            writeComponent(buffer, child)
        }

        buffer.writeByte(Operations.CONTAINER_END)
    }

    /**
     * 写入 Box 组件
     */
    private fun writeBox(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.COMPONENT_START)
        buffer.writeInt(component.id)
        buffer.writeInt(LayoutType.BOX)

        component.modifier?.let { writeModifier(buffer, it) }

        component.children?.forEach { child ->
            writeComponent(buffer, child)
        }

        buffer.writeByte(Operations.CONTAINER_END)
    }

    /**
     * 写入 Text 组件
     */
    private fun writeText(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.DRAW_TEXT_RUN)
        buffer.writeInt(component.textResourceId ?: throw IllegalArgumentException("Text needs textResourceId"))

        // 写入文本样式
        component.style?.let { style ->
            buffer.writeFloat(style.fontSize)
            buffer.writeInt(style.fontWeight)
            buffer.writeInt(parseColor(style.color))
            buffer.writeInt(parseTextAlign(style.textAlign))
        } ?: run {
            // 默认样式
            buffer.writeFloat(36f)
            buffer.writeInt(400)
            buffer.writeInt(-0x1000000) // 黑色
            buffer.writeInt(0) // LEFT
        }

        // ⭐ 写入点击交互（如果有）
        component.clickAction?.let { clickAction ->
            writeClickModifier(buffer, clickAction)
        }
    }

    /**
     * 写入 Image 组件
     */
    private fun writeImage(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.DRAW_BITMAP)
        buffer.writeInt(component.bitmapResourceId ?: throw IllegalArgumentException("Image needs bitmapResourceId"))

        component.size?.let { size ->
            buffer.writeInt(size.width)
            buffer.writeInt(size.height)
        } ?: run {
            buffer.writeInt(100)
            buffer.writeInt(100)
        }
    }

    /**
     * 写入 Canvas 组件
     */
    private fun writeCanvas(buffer: WireBuffer, component: Component) {
        buffer.writeByte(Operations.LAYOUT_CANVAS)
        buffer.writeInt(component.id)

        component.modifier?.let { writeModifier(buffer, it) }

        // Canvas 内部操作（如果有）
        // 这里可以根据需要扩展

        // Canvas 结束由 CONTAINER_END 处理
    }

    /**
     * 写入修饰器
     */
    private fun writeModifier(buffer: WireBuffer, modifier: Modifier) {
        var modifierFlags = 0

        // 填充标志
        if (modifier.fillMaxSize == true) modifierFlags = modifierFlags or 0x01
        if (modifier.fillMaxWidth == true) modifierFlags = modifierFlags or 0x02
        if (modifier.fillMaxHeight == true) modifierFlags = modifierFlags or 0x04

        buffer.writeInt(modifierFlags)

        // 尺寸
        buffer.writeInt(modifier.width ?: -1)
        buffer.writeInt(modifier.height ?: -1)

        // 内边距
        modifier.padding?.let { padding ->
            buffer.writeInt(padding.left)
            buffer.writeInt(padding.top)
            buffer.writeInt(padding.right)
            buffer.writeInt(padding.bottom)
        } ?: run {
            buffer.writeInt(0)
            buffer.writeInt(0)
            buffer.writeInt(0)
            buffer.writeInt(0)
        }

        // 背景色
        modifier.background?.let { bg ->
            buffer.writeInt(parseColor(bg))
        } ?: run {
            buffer.writeInt(0) // 透明
        }

        // 裁剪
        modifier.clip?.let { clip ->
            // 写入字符串：先写入长度，再写入字节
            val clipBytes = clip.toByteArray(Charsets.UTF_8)
            buffer.writeInt(clipBytes.size)
            clipBytes.forEach { buffer.writeByte(it.toInt()) }
        }
    }

    // ========================================================================
    // ⭐ 交互功能实现
    // ========================================================================

    /**
     * 写入点击修饰器
     *
     * Wire Format 结构:
     * [OP_CODE: MODIFIER_CLICK]
     *   [OP_CODE: HOST_ACTION 或 VALUE_INTEGER_CHANGE_ACTION]
     *   [actionId 或 targetValueId + newValue]
     */
    private fun writeClickModifier(buffer: WireBuffer, action: ClickAction) {
        // 1. 写入 MODIFIER_CLICK
        buffer.writeByte(Operations.MODIFIER_CLICK)

        // 2. 根据类型写入不同的动作
        when (action.type) {
            "host_action" -> {
                // 写入 HOST_ACTION
                buffer.writeByte(Operations.HOST_ACTION)
                buffer.writeInt(action.actionId)
                // metadata 可以在 HostNamedActionOperation 中使用
                action.metadata?.let { metadata ->
                    // 写入字符串：先写入长度，再写入字节
                    val metadataBytes = metadata.toByteArray(Charsets.UTF_8)
                    buffer.writeInt(metadataBytes.size)
                    metadataBytes.forEach { b -> buffer.writeByte(b.toInt()) }
                }
            }
            "value_change" -> {
                // 写入 VALUE_INTEGER_CHANGE_ACTION
                buffer.writeByte(Operations.VALUE_INTEGER_CHANGE_ACTION)
                buffer.writeInt(action.targetValueId ?: 0)
                buffer.writeInt(action.newValue ?: 0)
            }
        }
    }

    // ========================================================================
    // 辅助函数
    // ========================================================================

    private fun parseColor(colorHex: String): Int {
        return colorHex.removePrefix("#").toLong(16).toInt() or -0x1000000
    }

    private fun parseTextAlign(align: String): Int {
        return when (align) {
            "LEFT" -> 0
            "CENTER" -> 1
            "RIGHT" -> 2
            "JUSTIFY" -> 3
            else -> 0
        }
    }
}

/**
 * 布局类型常量
 */
object LayoutType {
    const val COLUMN = 0
    const val ROW = 1
    const val BOX = 2
}
