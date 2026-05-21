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

package androidx.compose.genwidget.sample

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appwidget.app.AppWidgetManager
import androidx.appwidget.app.RemoteViews
import androidx.compose.genwidget.compiler.WidgetCompiler
import androidx.compose.remote.player.view.RemoteComposePlayer

/**
 * 天气微件示例 - 展示如何使用 WidgetCompiler 创建带交互的天气微件
 */
object WeatherWidgetSample {
    
    /**
     * 创建天气微件的 JSON 规格
     * 
     * metadata 支持以下格式：
     * 1. 简单字符串：直接传递字符串值
     *    示例："metadata": "open_detail"
     * 
     * 2. Key-Value 格式（推荐）：使用 | 分隔多个键值对
     *    示例："metadata": "action:android.intent.action.VIEW|data:weather://detail"
     * 
     * 3. 纯 Action 格式：只指定 action
     *    示例："metadata": "action:com.example.REFRESH"
     * 
     * 4. 纯 Data 格式：只指定 data URI
     *    示例："metadata": "data:weather://refresh"
     * 
     * 5. 多参数格式：支持多个自定义参数
     *    示例："metadata": "action:VIEW|data:weather://detail|extra:temperature|value:24"
     */
    fun createWeatherWidgetJson(): String {
        return """
            {
              "widget": {
                "type": "weather",
                "version": 1,
                "width": 400,
                "height": 600,
                "density": 4,
                "profile": "WIDGETS_V7",
                "components": [
                  {
                    "type": "Column",
                    "id": 1,
                    "modifier": {
                      "fillMaxSize": true,
                      "background": "#FFFFFF",
                      "padding": {
                        "left": 32,
                        "top": 32,
                        "right": 32,
                        "bottom": 32
                      }
                    },
                    "children": [
                      {
                        "type": "Text",
                        "id": 2,
                        "textResourceId": 1,
                        "clickAction": {
                          "actionId": 1,
                          "metadata": "action:android.intent.action.VIEW|data:weather://detail",
                          "type": "host_action"
                        },
                        "style": {
                          "fontSize": 48,
                          "fontWeight": 500,
                          "color": "#000000",
                          "textAlign": "CENTER"
                        }
                      },
                      {
                        "type": "Text",
                        "id": 3,
                        "textResourceId": 2,
                        "style": {
                          "fontSize": 128,
                          "fontWeight": 700,
                          "color": "#000000",
                          "textAlign": "CENTER"
                        }
                      },
                      {
                        "type": "Text",
                        "id": 4,
                        "textResourceId": 3,
                        "style": {
                          "fontSize": 36,
                          "fontWeight": 400,
                          "color": "#666666",
                          "textAlign": "CENTER"
                        }
                      }
                    ]
                  }
                ],
                "resources": {
                  "texts": [
                    {
                      "id": 1,
                      "content": "Beijing",
                      "description": "City name"
                    },
                    {
                      "id": 2,
                      "content": "24°C",
                      "description": "Temperature"
                    },
                    {
                      "id": 3,
                      "content": "Sunny",
                      "description": "Weather condition"
                    }
                  ],
                  "bitmaps": []
                }
              }
            }
        """.trimIndent()
    }
    
    /**
     * 编译天气微件为 Wire Format
     */
    fun compileWeatherWidget(): ByteArray {
        val json = createWeatherWidgetJson()
        return WidgetCompiler.compileFromJson(json)
    }
    
    /**
     * 解析 metadata 中的 action 和 data
     * 
     * metadata 格式支持：
     * 1. Key-Value 格式："action:android.intent.action.VIEW|data:weather://detail"
     * 2. 简单字符串："open_detail"
     * 3. 纯 Action："action:com.example.REFRESH"
     * 4. 纯 Data："data:weather://refresh"
     * 5. 多参数："action:VIEW|data:detail|extra:temperature|value:24"
     * 
     * @param metadata metadata 字符串
     * @return Pair<action, data>，可能为 null
     */
    private fun parseMetadata(metadata: String): Pair<String?, String?> {
        var action: String? = null
        var data: String? = null
        
        // 尝试解析 Key-Value 格式
        if (metadata.contains(":")) {
            metadata.split("|").forEach { part ->
                val parts = part.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    when (key) {
                        "action" -> action = value
                        "data" -> data = value
                        // 可以扩展支持其他 key
                        "extra", "value" -> {
                            // 自定义参数，可以在回调中处理
                        }
                    }
                }
            }
        } else {
            // 简单字符串格式，直接作为 action 处理
            action = metadata
        }
        
        return Pair(action, data)
    }
    
    /**
     * 创建天气微件并注册到 RemoteViews
     * 
     * @param context 应用上下文
     * @param appWidgetId 微件 ID
     * @param remoteComposePlayer RemoteCompose 播放器实例
     */
    fun createWeatherWidget(
        context: Context, 
        appWidgetId: Int, 
        remoteComposePlayer: RemoteComposePlayer
    ) {
        // 1. 编译为 Wire Format
        val wireFormat = compileWeatherWidget()
        
        // 2. 创建 RemoteViews
        val instructions = RemoteViews.DrawInstructions.Builder(listOf(wireFormat)).build()
        val remoteViews = RemoteViews(instructions)
        
        // 3. 注册动作回调（从 metadata 中解析 action 和 data）
        remoteComposePlayer.addIdActionListener { actionId, metadata ->
            when (actionId) {
                1 -> {
                    // 从 metadata 中解析 action 和 data
                    val (action, dataUri) = parseMetadata(metadata)
                    
                    // 创建 Intent
                    val intent = Intent(action ?: Intent.ACTION_VIEW).apply {
                        dataUri?.let { data = Uri.parse(it) }
                    }
                    
                    // 启动 Activity
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                2 -> {
                    // 示例：处理刷新动作
                    val (action, _) = parseMetadata(metadata)
                    when (action) {
                        "com.example.REFRESH" -> {
                            // 刷新天气数据
                            val refreshIntent = Intent("com.example.REFRESH_WEATHER")
                            context.sendBroadcast(refreshIntent)
                        }
                    }
                }
                else -> {
                    // 默认处理：简单字符串 metadata
                    val (action, dataUri) = parseMetadata(metadata)
                    if (action != null && !action.contains(":")) {
                        // 简单字符串 action
                        when (action) {
                            "open_detail" -> {
                                val intent = Intent("android.intent.action.VIEW").apply {
                                    data = Uri.parse("weather://detail")
                                }
                                context.startActivity(intent)
                            }
                            "refresh" -> {
                                context.sendBroadcast(Intent("com.example.REFRESH"))
                            }
                        }
                    }
                }
            }
        }
        
        // 4. 更新微件
        AppWidgetManager.getInstance(context)
            .updateAppWidget(appWidgetId, remoteViews)
    }
}
