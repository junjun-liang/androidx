# RemoteCompose 协议规范

本文档定义了 RemoteCompose 二进制协议格式的部分内容。
每个操作由操作码（OpCode）后跟其特定的数据字段组成。
本文档并非权威规范，仅供了解协议风格而提供。

## 操作码参考

| 操作码 | 名称 | 用途 | 字段 |
| :--- | :--- | :--- | :--- |
| 0 | `HEADER` | 文档元数据 | 版本、宽度、高度、密度、配置档案 |
| 38 | `CLIP_PATH` | 设置裁剪路径 | 路径 ID |
| 39 | `CLIP_RECT` | 设置裁剪矩形 | 左、上、右、下 |
| 40 | `PAINT_VALUES` | 更新绘制状态 | 序列化的 `PaintBundle` |
| 42 | `DRAW_RECT` | 绘制矩形 | 左、上、右、下 |
| 43 | `DRAW_TEXT_RUN` | 绘制文本片段 | 文本 ID、起始、结束、x、y、从右到左 |
| 44 | `DRAW_BITMAP` | 绘制图片 | 图片 ID、左、上、右、下 |
| 46 | `DRAW_CIRCLE` | 绘制圆形 | 圆心 x、圆心 y、半径 |
| 47 | `DRAW_LINE` | 绘制线段 | x1、y1、x2、y2 |
| 51 | `DRAW_ROUND_RECT` | 绘制圆角矩形 | 左、上、右、下、rx、ry |
| 52 | `DRAW_SECTOR` | 绘制扇形 | 左、上、右、下、起始角度、扫过角度 |
| 53 | `DRAW_TEXT_ON_PATH`| 沿路径绘制文本 | 文本 ID、路径 ID、水平偏移、垂直偏移 |
| 63 | `THEME` | 设置活动主题 | 主题 ID（浅色/深色/任意） |
| 80 | `DATA_FLOAT` | 定义常量 | 浮点数 ID、值 |
| 81 | `ANIMATED_FLOAT` | 动态表达式 | 浮点数 ID、RPN 操作 |
| 101 | `DATA_BITMAP` | 序列化图片 | 图片 ID、宽度、高度、像素数据 |
| 102 | `DATA_TEXT` | 序列化字符串 | 文本 ID、字符串内容 |
| 123 | `DATA_PATH` | 序列化路径 | 路径 ID、点/命令 |
| 124 | `DRAW_PATH` | 绘制矢量路径 | 路径 ID |
| 126-129 | `MATRIX_*` | 变换操作 | 缩放、平移、倾斜、旋转 |
| 130-131 | `MATRIX_SAVE/RESTORE`| 矩阵入栈/出栈 | - |
| 133 | `DRAW_TEXT_ANCHOR` | 对齐文本 | 文本 ID、x、y、panX、panY、标志位 |
| 135 | `TEXT_FROM_FLOAT` | 浮点数转字符串 | 文本 ID、值 ID、格式化参数 |
| 139 | `DRAW_CONTENT` | 组件内容 | 用于 Canvas/Draw Content |
| 144 | `INTEGER_EXPRESSION`| 整数运算 | 整数 ID、掩码、操作 |
| 157 | `TOUCH_EXPRESSION` | 输入映射 | 默认值、最小值、最大值、模式、缓动、RPN |
| 161 | `PARTICLE_DEFINE` | 粒子系统 | ID、变量 ID 数组、数量、初始化方程 |
| 163 | `PARTICLE_LOOP` | 粒子更新 | ID、重启方程、更新方程 |
| 178 | `CONDITIONAL_OPS` | If 条件块 | 条件类型、a、b、子操作... |
| 193 | `PATH_EXPRESSION` | 算法路径 | 路径 ID、范围、X 方程、Y 方程 |
| 201 | `COMPONENT_START` | UI 节点 | 组件 ID、布局类型、标志位 |
| 230 | `CONTAINER_END` | 关闭代码块 | - |

## 数据类型
- **Int**：32 位有符号整数（小端序）。
- **Float**：32 位 IEEE 754 浮点数。
- **Short**：16 位有符号整数。
- **Byte**：8 位无符号整数。
- **String**：UTF-8 编码，前缀为长度（Int）。
- **Float ID**：编码为 `NaN` 值，其中有效数字部分包含 24 位 ID。
