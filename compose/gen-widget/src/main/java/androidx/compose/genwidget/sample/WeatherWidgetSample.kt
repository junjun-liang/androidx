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
     * metadata 格式："action:android.intent.action.VIEW|data:weather://detail"
     */
    private fun parseMetadata(metadata: String): Pair<String?, String?> {
        var action: String? = null
        var data: String? = null
        
        metadata.split("|").forEach { part ->
            val (key, value) = part.split(":", limit = 2)
            when (key.trim()) {
                "action" -> action = value.trim()
                "data" -> data = value.trim()
            }
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
            }
        }
        
        // 4. 更新微件
        AppWidgetManager.getInstance(context)
            .updateAppWidget(appWidgetId, remoteViews)
    }
}
