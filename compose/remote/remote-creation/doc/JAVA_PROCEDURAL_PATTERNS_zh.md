# Java 过程式模式

使用 Java `RemoteComposeWriter` API 创建 RemoteCompose 文档的常见用法模式。

## 基本结构
```java
RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "Demo", platform);
rc.root(() -> {
    rc.startColumn(new RecordingModifier().fillMaxSize().padding(16f), 0, 0);
    
    int textId = rc.addText("Title");
    rc.startTextComponent(new RecordingModifier(), textId, Color.BLACK, 24f, 0, 400, null, 0, 0, 1);
    rc.endTextComponent();
    
    rc.box(new RecordingModifier().size(100).background(Color.BLUE));
    
    rc.endColumn();
});
```

## 矩阵变换
```java
rc.save();
rc.translate(100, 100);
rc.rotate(45, 0, 0);
rc.drawRect(0, 0, 50, 50);
rc.restore();
```

## 创建动态路径
```java
RemotePath path = new RemotePath();
path.moveTo(0, 0);
path.lineTo(100, 100);
int pathId = rc.addPathData(path);

rc.getPainter().setStrokeWidth(2f).commit();
rc.drawPath(pathId);
```
