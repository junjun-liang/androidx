# AndroidX 项目文档

本仓库包含 AndroidX 项目的软件架构设计、业务流程文档以及 Glance+RemoteCompose 微件开发入门手册。

## 文档清单

### 核心文档

- **[architecture.md](architecture.md)** - 项目软件架构设计文档
  - 顶层架构设计
  - 核心模块架构（Compose、FLAN、数据存储等）
  - 构建系统架构
  - 模块依赖关系
  - 版本管理策略
  - 测试体系架构

- **[business_flow.md](business_flow.md)** - 业务流程文档
  - 开发工作流
  - 构建与发布流程
  - 测试流程
  - 核心运行时业务流程（Compose 渲染、Lifecycle、Navigation 等）

- **[diagrams.md](diagrams.md)** - Mermaid 图表文档
  - 17 张架构图和流程图
  - 可在 GitHub、VS Code 等工具中直接渲染

### Glance + RemoteCompose 微件开发

- **[remote-architecture.md](remote-architecture.md)** - "帮我制作微件" 入门手册
  - 技术概览与架构设计
  - 三种开发方式（Glance API、RemoteCompose API、RemoteComposeWidget）
  - 组件参考与 Modifier 参考
  - 动画与粒子效果
  - 触摸交互
  - 完整 Demo 代码（3 个可运行微件）
  - 常见问题解答

### 设计文档

- **glance_remote_compose_translator_design.md** - Glance 到 RemoteCompose 翻译器设计
- **wire_format_transport_api.md** - Wire Format 传输 API 设计
- **wire_format_to_widget_rendering.md** - Wire Format 到 Widget 渲染流程
- **wirebuffer_generation_flow.md** - WireBuffer 生成流程
- **text_to_wirebuffer_flow.md** - 文本到 WireBuffer 流程
- **make_my_widget_dynamic_generation.md** - 动态微件生成
- **remote_compose_glance_widget.md** - RemoteCompose Glance Widget 设计
- **weather_widget_remote_compose.md** - 天气微件 RemoteCompose 实现
- **ticker_widget_provider_analysis.md** - Ticker Widget Provider 分析

## 使用方式

### 查看架构图

在支持 Mermaid 的编辑器（如 VS Code + Mermaid 插件）中打开 `diagrams.md` 即可查看渲染后的图表。

### 运行 Demo 代码

`remote-architecture.md` 中包含了 3 个完整的可运行微件 Demo：

1. **WeatherWidget** - 响应式天气微件（Glance API）
2. **AnimatedWidget** - 动画时钟微件（RemoteCompose API）
3. **ParticleWidget** - 粒子效果微件（RemoteComposeWidget）

按照文档中的步骤配置依赖和 AndroidManifest.xml 即可运行。

## 文档统计

- 总文件数：18 个
- 总行数：12,852 行
- 核心文档：5 个
- 设计文档：13 个

## 许可证

遵循 AndroidX 项目的 Apache 2.0 许可证。
