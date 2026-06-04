# Plan: 整理 Remote Compose 中 Image URL 的写入 RC 文件和桌面显示流程

## Summary

整理 Remote Compose 中 Image URL 从创建端写入到播放端显示的完整流程，输出文档到 `my_docs` 目录。

## Current State Analysis

- Image URL 流程跨越 3 个模块：remote-core（协议定义）、remote-creation-core/remote-creation-compose（创建端）、remote-player-core/remote-player-view（播放端）
- BitmapData 定义了 4 种编码方式：INLINE、URL、FILE、EMPTY
- URL 编码时 bitmap 字段存储 URL 字符串的 UTF-8 字节
- 播放端通过 BitmapLoader 接口加载 URL 图像，默认实现为同步 HTTP 下载
- 双层安全门控：Limits.ENABLE_IMAGE_URLS 在解析和解码两阶段检查

## Proposed Changes

### 1. 创建文档 `my_docs/image-url-flow.md`

文档内容包含以下章节：

1. 概述：Image URL 在 Remote Compose 中的定位
2. 创建端 — 写入 RC 文件
   - Kotlin DSL API：remoteBitmapUrl() / remoteNamedBitmapUrl()
   - Java Writer API：addBitmapUrl() / addNamedBitmapUrl()
   - Compose 状态 API：rememberNamedRemoteBitmap()
   - 序列化流程：Writer → Buffer → WireBuffer
   - BitmapData 线路格式：opcode 101, 字段编码
3. 播放端 — 桌面显示
   - 文档解析：BitmapData.read()
   - 操作执行：BitmapData.apply() → RemoteContext.loadBitmap()
   - Android 实现：AndroidRemoteContext → RemoteBitmapDecoder
   - BitmapLoader 接口与 AndroidBitmapLoader 默认实现
   - 自定义 BitmapLoader 注入
   - 绘制时使用：AndroidPaintContext.drawBitmap()
4. 安全机制
   - 双层 ENABLE_IMAGE_URLS 门控
   - 尺寸校验防恶意文档
5. 缓存机制
6. 完整流程图（Mermaid）
7. 使用方法与最佳实践

## Assumptions & Decisions

- 使用 Mermaid 语法绘制流程图
- 文档语言为中文
- 所有内容基于实际代码分析
- 重点突出端到端流程和关键设计决策

## Verification Steps

1. 确认文档文件已生成且内容完整
2. 确认 Mermaid 图表语法正确
3. 确认所有类和方法引用与实际代码一致
