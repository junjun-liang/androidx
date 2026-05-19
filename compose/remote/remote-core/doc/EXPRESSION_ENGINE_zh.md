# RemoteCompose 表达式引擎

表达式引擎允许播放器在本地执行数学计算和逻辑运算，从而实现流畅的动画和响应式 UI，无需与主机持续通信。

## RPN（逆波兰表示法）
表达式使用 RPN 编码，操作数先于运算符。这允许在播放器上进行高效的基于栈的求值。

### 示例：`(A + B) * 2`
在 RemoteCompose 中，这表示为：`[A_ID, B_ID, ADD, 2.0, MUL]`

## 求值逻辑
1.  **依赖关系**：每个表达式跟踪它依赖的变量。
2.  **脏标记跟踪**：当某个变量（如 `Time` 或 `TouchX`）发生变化时，所有依赖该变量的表达式被标记为"脏"。
3.  **栈求值器**：`AnimatedFloatExpression.eval()` 遍历 RPN 数组：
    - 值被压入栈中。
    - 运算符从栈中弹出操作数并将结果压回栈中。

## 支持的运算符

| 类型 | 运算符 |
| :--- | :--- |
| **算术运算** | `ADD`、`SUB`、`MUL`、`DIV`、`MOD`、`POW` |
| **三角函数** | `SIN`、`COS`、`TAN`、`ASIN`、`ACOS`、`ATAN`、`ATAN2` |
| **逻辑运算** | `EQ`、`NEQ`、`GT`、`GE`、`LT`、`LE`、`AND`、`OR`、`IFELSE` |
| **特殊运算** | `ABS`、`MIN`、`MAX`、`CLAMP`、`RAND`、`PINGPONG`、`SQUARE`、`SQRT` |
| **系统变量** | `VAR1`、`VAR2`（用于循环和路径表达式） |

## 变量存储
- **`RemoteContext`**：保存文档中每个浮点数 ID 的当前值。
- **全局变量**：由播放器更新的系统提供值：
    - `WINDOW_WIDTH`、`WINDOW_HEIGHT`
    - `ANIMATION_TIME`、`CONTINUOUS_SEC`
    - `TOUCH_X`、`TOUCH_Y`、`TOUCH_PRESSED`
    - `ACCELEROMETER_X`、`Y`、`Z`
