# Kotlin DSL 模式

使用 Kotlin DSL 创建 RemoteCompose 文档的常见用法模式。

## 基本布局
```kotlin
RemoteComposeContextAndroid(500, 500, "Demo", platform) {
    root {
        column(Modifier.fillMaxSize().padding(16f)) {
            text("Title", fontSize = 24f)
            box(Modifier.size(100f).background(Color.BLUE))
        }
    }
}
```

## 状态和交互
```kotlin
val count = addNamedInt("clickCount", 0)
column {
    text("Count: " + count)
    box(Modifier.size(50f).clickable(
        ValueIntegerChangeAction(count, count + 1)
    ))
}
```

## Canvas 绘图
```kotlin
canvas(Modifier.fillMaxWidth().height(200f)) {
    painter.setColor(Color.RED).setStrokeWidth(5f).commit()
    drawLine(0f, 0f, ComponentWidth(), ComponentHeight())
}
```

## 响应式尺寸
```kotlin
row(Modifier.fillMaxWidth()) {
    box(Modifier.horizontalWeight(1f).background(Color.GRAY))
    box(Modifier.horizontalWeight(2f).background(Color.BLUE))
}
```
