# AndroidX 项目架构图与流程图 (Mermaid)

本文档包含所有架构图和业务流程图的 Mermaid 源码，可在 GitHub、VS Code (Mermaid 插件)、Typora 等支持 Mermaid 的工具中直接渲染。

---

## 一、软件架构图

### 1.1 项目顶层架构图

```mermaid
graph TB
    subgraph 应用集成层
        Car[Car App<br/>车载应用]
        Wear[Wear OS<br/>可穿戴]
        XR[XR<br/>扩展现实]
        TV[TV<br/>电视]
        Health[Health<br/>健康]
    end

    subgraph UI框架层
        direction TB
        subgraph Compose生态
            M3[Material3]
            M2[Material]
            Fdn[Foundation]
            UI[UI]
            RT[Runtime]
            M3 --> M2 --> Fdn --> UI --> RT
        end
        subgraph View生态
            AppCompat[AppCompat]
            CL[ConstraintLayout]
            CoL[CoordinatorLayout]
            RV[RecyclerView]
            AppCompat --> CL --> CoL --> RV
        end
        Graphics[Graphics]
        Ink[Ink]
        PDF[PDF]
        Glance[Glance]
        WebGPU[WebGPU]
    end

    subgraph 架构组件层
        subgraph FLAN生态
            Nav[Navigation]
            Frag[Fragment]
            Act[Activity]
            LC[Lifecycle]
            SS[SavedState]
            Nav --> Frag --> Act --> LC --> SS
        end
        Work[WorkManager]
        Paging[Paging]
        Hilt[Hilt]
        DS[DataStore]
        Startup[Startup]
    end

    subgraph 数据存储层
        Room[Room3]
        SQLite[SQLite]
        DS2[DataStore Core]
        Coll[Collection]
    end

    subgraph 平台集成层
        Core[Core]
        Window[Window]
        Camera[Camera]
        Browser[Browser]
        WebKit[WebKit]
        Bio[Biometric]
        Cred[Credentials]
    end

    subgraph 基础设施层
        Anno[Annotation]
        Conc[Concurrent]
        Trace[Tracing]
        Kruth[Kruth]
        Test[Test]
    end

    应用集成层 --> UI框架层
    UI框架层 --> 架构组件层
    架构组件层 --> 数据存储层
    数据存储层 --> 平台集成层
    平台集成层 --> 基础设施层
```

### 1.2 Compose 分层架构图

```mermaid
graph TB
    subgraph 设计系统层
        M3[Material3<br/>Material You]
        M3Adaptive[Material3 Adaptive<br/>自适应布局]
        M2[Material 2]
        M3 --> M2
        M3Adaptive --> M3
    end

    subgraph 基础层
        Fdn[Foundation<br/>LazyColumn/手势/边框]
        FdnLayout[Foundation Layout<br/>布局基础]
        Fdn --> FdnLayout
    end

    subgraph 编排层
        CUI[UI<br/>布局/输入/图形/文本]
        Anim[Animation<br/>动画系统]
        AnimCore[Animation Core]
        Anim --> AnimCore
    end

    subgraph 核心引擎层
        Runtime[Runtime<br/>状态管理/组合树]
        RTSavable[Runtime Saveable<br/>状态保存恢复]
        RTAnn[Runtime Annotation]
        RTSavable --> Runtime --> RTAnn
    end

    设计系统层 --> 基础层
    基础层 --> 编排层
    编排层 --> 核心引擎层
```

### 1.3 FLAN 架构图

```mermaid
graph TB
    subgraph Navigation导航层
        NavCompose[Navigation Compose]
        NavFragment[Navigation Fragment]
        NavSafeArgs[Safe Args Plugin]
        NavRuntime[Navigation Runtime]
        NavCommon[Navigation Common]
        NavCompose --> NavRuntime
        NavFragment --> NavRuntime
        NavSafeArgs --> NavRuntime
        NavRuntime --> NavCommon
    end

    subgraph Fragment与Activity
        FragCompose[Fragment Compose]
        ActCompose[Activity Compose]
        FragKTX[Fragment KTX]
        ActKTX[Activity KTX]
        FragCore[Fragment Core]
        ActCore[Activity Core]
        FragCompose --> FragKTX --> FragCore
        ActCompose --> ActKTX --> ActCore
    end

    subgraph Lifecycle生命周期层
        VMCompose[ViewModel Compose]
        RTCompose[Runtime Compose]
        VM[ViewModel]
        LD[LiveData]
        LCRuntime[Runtime]
        LCCommon[Lifecycle Common]
        VMCompose --> VM
        RTCompose --> LCRuntime
        VM --> LCCommon
        LD --> LCCommon
        LCRuntime --> LCCommon
    end

    subgraph SavedState状态保存层
        SSCompose[SavedState Compose]
        SSKTX[SavedState KTX]
        SSCore[SavedState Core]
        SSCompose --> SSKTX --> SSCore
    end

    Navigation导航层 --> Fragment与Activity
    Fragment与Activity --> Lifecycle生命周期层
    Lifecycle生命周期层 --> SavedState状态保存层
```

### 1.4 数据存储架构图

```mermaid
graph TB
    subgraph Room3 ORM层
        RoomPaging[Room Paging]
        RoomRuntime[Room Runtime]
        RoomTesting[Room Testing]
        RoomCommon[Room Common<br/>@Entity @Dao @Database]
        RoomCompiler[Room Compiler<br/>KSP代码生成]
        RoomXProc[Room Compiler Processing<br/>XProcessing]
        RoomMigration[Room Migration]
        RoomPaging --> RoomRuntime --> RoomCommon
        RoomCompiler --> RoomXProc
        RoomCompiler --> RoomCommon
    end

    subgraph SQLite驱动层
        Bundled[Bundled Driver<br/>内嵌SQLite]
        Framework[Framework Driver<br/>Android原生]
        WebDrv[Web Driver<br/>Web Worker]
        SQLiteIf[SQLite Driver 抽象<br/>KMP]
        Bundled --> SQLiteIf
        Framework --> SQLiteIf
        WebDrv --> SQLiteIf
    end

    subgraph DataStore KV存储
        PrefDS[Preferences DataStore]
        ProtoDS[Proto DataStore]
        Tink[Tink加密]
        DSCore[DataStore Core<br/>OkIO]
        PrefDS --> DSCore
        ProtoDS --> DSCore
        Tink --> DSCore
    end

    Room3 ORM层 --> SQLite驱动层
```

### 1.5 Camera 分层架构图

```mermaid
graph TB
    subgraph 应用集成层
        CamCompose[Camera Compose]
        CamView[Camera View]
        MLKit[ML Kit Vision]
    end

    subgraph 功能层
        Video[Video 视频录制]
        Effects[Effects 视觉效果]
        Extensions[Extensions 设备特效]
    end

    subgraph 生命周期层
        CamLifecycle[Camera Lifecycle]
    end

    subgraph 核心抽象层
        CamCore[Camera Core<br/>UseCase: Preview/ImageCapture<br/>ImageAnalysis/VideoCapture]
    end

    subgraph 实现层
        Cam2Pipe[Camera2 Pipe<br/>高性能抽象层]
        Cam2[Camera2<br/>标准实现]
    end

    CamCompose --> CamLifecycle
    CamView --> CamLifecycle
    MLKit --> CamCore
    Video --> CamCore
    Effects --> CamCore
    Extensions --> CamCore
    CamLifecycle --> CamCore
    CamCore --> Cam2Pipe
    CamCore --> Cam2
```

### 1.6 构建系统架构图

```mermaid
graph TB
    subgraph 顶层构建入口
        TopBuild[build.gradle<br/>AndroidXRootPlugin]
        Settings[settings.gradle<br/>450+子模块]
        GradleProps[gradle.properties<br/>JVM/SDK/缓存配置]
    end

    subgraph buildSrc构建基础设施
        subgraph 核心插件
            AXPlugin[AndroidXPlugin]
            AXRoot[AndroidXRootPlugin]
            AXImpl[AndroidXImplPlugin]
            AXKmp[AndroidXKmpPlugin]
            AXCompose[AndroidXComposePlugin]
        end
        subgraph 导入插件
            BCV[Binary Compatibility Validator]
            BenchPlugin[Benchmark Plugin]
            BPPlugin[Baseline Profile Plugin]
            RoomPlugin[Room Plugin]
            GlanceGen[Glance Layout Generator]
            StableAIDL[Stable AIDL Plugin]
        end
    end

    subgraph 版本管理
        LibVer[libraryversions.toml<br/>版本号+原子版本组]
        EnvOverride[环境变量覆盖<br/>AGP/Metalava/Lint]
    end

    subgraph 构建类型过滤
        BuildType[ANDROIDX_PROJECTS<br/>MAIN|COMPOSE|FLAN|WEAR|XR|...]
    end

    subgraph 依赖与缓存
        DepGraph[project-dependency-graph.groovy]
        GCPCache[GCP远程构建缓存]
        ConfigCache[Configuration Cache]
    end

    顶层构建入口 --> buildSrc构建基础设施
    buildSrc构建基础设施 --> 版本管理
    buildSrc构建基础设施 --> 构建类型过滤
    版本管理 --> 依赖与缓存
```

### 1.7 模块依赖关系图

```mermaid
graph LR
    Annotation[Annotation] --> Core[Core]
    Core --> AppCompat[AppCompat]
    Core --> Activity[Activity]
    Core --> Fragment[Fragment]

    Activity --> ActivityCompose[Activity Compose]
    Fragment --> FragmentCompose[Fragment Compose]

    Core --> Lifecycle[Lifecycle]
    Lifecycle --> LCRuntime[Runtime]
    Lifecycle --> ViewModel[ViewModel]
    Lifecycle --> LiveData[LiveData]

    LCRuntime --> LCRTCompose[Runtime Compose]
    ViewModel --> VMCompose[ViewModel Compose]

    Lifecycle --> SavedState[SavedState]

    subgraph Compose生态
        ComposeRT[Compose Runtime] --> ComposeUI[Compose UI]
        ComposeUI --> ComposeFdn[Compose Foundation]
        ComposeFdn --> ComposeM2[Compose Material]
        ComposeM2 --> ComposeM3[Compose Material3]
    end

    ActivityCompose --> ComposeRT
    FragmentCompose --> ComposeRT
    LCRTCompose --> ComposeRT
    VMCompose --> ComposeRT

    subgraph 数据层
        SQLite[SQLite] --> Room[Room3]
        Room --> RoomPaging[Room Paging]
    end

    subgraph 跨域集成
        NavCompose[Navigation Compose]
        CamCompose[Camera Compose]
        PagingCompose[Paging Compose]
        HiltNavCompose[Hilt Nav Compose]
    end

    NavCompose --> ComposeRT
    CamCompose --> ComposeRT
    PagingCompose --> ComposeRT
    HiltNavCompose --> ComposeRT
```

---

## 二、业务流程图

### 2.1 开发环境搭建流程

```mermaid
flowchart TD
    A[repo init -b androidx-main] --> B[repo sync]
    B --> C[设置 ANDROIDX_PROJECTS 环境变量]
    C --> D{选择 BuildType}
    D -->|COMPOSE| E[./gradlew studio]
    D -->|FLAN| E
    D -->|WEAR| E
    D -->|CAMERA| E
    D -->|XR| E
    E --> F[启动定制版 Android Studio]
    F --> G[开发与调试]
```

### 2.2 代码提交与审查流程

```mermaid
flowchart TD
    A[本地开发完成] --> B[运行本地检查]
    B --> C{API 变更?}
    C -->|无变更| D[继续提交]
    C -->|有变更| E[运行 updateApi]
    E --> D
    D --> F[git push to Gerrit]
    F --> G[Pre-submit CI]
    G --> H{CI 通过?}
    H -->|否| A
    H -->|是| I[代码审查]
    I --> J{OWNER 批准?}
    J -->|否| K[修改代码]
    K --> A
    J -->|是| L[合并到 androidx-main]
    L --> M[Post-submit 全量测试]
```

### 2.3 构建流程

```mermaid
flowchart TD
    A[触发 Gradle 构建] --> B[配置阶段]
    B --> C[BuildType 过滤模块]
    C --> D[依赖图计算]
    D --> E[Configuration Cache 检查]
    E --> F[依赖解析]
    F --> G[libraryversions.toml 版本解析]
    G --> H[本地预构建优先]
    H --> I[编译阶段]
    I --> J[KSP/KAPT 注解处理]
    J --> K[Kotlin + Java 编译]
    K --> L[输出到 ../../out]
```

### 2.4 版本发布流程

```mermaid
flowchart LR
    subgraph androidx-main分支
        A[Alpha 开发<br/>功能迭代/API可变] --> B[Beta 功能冻结<br/>仅修Bug]
    end

    subgraph release分支
        B -->|cherry-pick| C[RC 发布候选<br/>仅关键Bug]
        C --> D[Stable 正式发布<br/>Maven Central]
        D --> E[Bugfix 版本<br/>1.2.1]
    end

    D -->|新minor版本| A
```

### 2.5 API 管理流程

```mermaid
flowchart TD
    A[修改公共 API] --> B[checkApi 检测变更]
    B --> C{有 API 变更?}
    C -->|无| D[通过]
    C -->|有| E{变更是否有意?}
    E -->|无意| F[修复代码恢复 API]
    F --> B
    E -->|有意| G[运行 updateApi 更新 api/current.txt]
    G --> H[checkApiRelease 版本兼容性验证]
    H --> I{兼容性通过?}
    I -->|否| J[调整 API 或升级 Major 版本]
    J --> G
    I -->|是| D
```

### 2.6 测试执行流程

```mermaid
flowchart TD
    A[开发者提交代码] --> B[Pre-submit CI]
    B --> C[AffectedModuleDetector<br/>识别受影响模块]
    C --> D[受影响模块 JVM 单元测试]
    D --> E[受影响模块设备端子集测试]
    E --> F[API 兼容性检查]
    F --> G{Pre-submit 通过?}
    G -->|否| H[修复问题]
    H --> A
    G -->|是| I[合并到 main]
    I --> J[Post-submit CI]
    J --> K[全量 JVM 单元测试]
    K --> L[全量设备端测试<br/>API 23-36]
    L --> M[FTL 远程测试]
    M --> N[截图测试 Scuba]
```

### 2.7 Compose 渲染流程

```mermaid
flowchart TD
    A[State 变更] --> B[Runtime 标记 dirty]
    B --> C[Composer 遍历组合树]
    C --> D[识别 dirty 节点]
    D --> E[调用 @Composable 重组]
    E --> F[智能跳过未变更子树]
    F --> G[LayoutNode 树构建/更新]
    G --> H[Measure 阶段<br/>父约束→子测量]
    H --> I[Layout 阶段<br/>子尺寸→父放置]
    I --> J[DrawNode 树遍历]
    J --> K[Canvas 绘制指令]
    K --> L{平台渲染}
    L -->|Android| M[RenderNode → 硬件加速]
    L -->|Desktop| N[Swing/AWT Canvas]
    L -->|iOS| O[Core Graphics]
    L -->|Web| P[HTML Canvas / Wasm]
```

### 2.8 Lifecycle 生命周期流程

```mermaid
stateDiagram-v2
    [*] --> Created
    Created --> Started
    Started --> Resumed
    Resumed --> Paused
    Paused --> Resumed
    Paused --> Stopped
    Stopped --> Started
    Stopped --> Destroyed
    Destroyed --> [*]
```

### 2.9 Navigation 导航流程

```mermaid
flowchart TD
    A[NavController.navigate] --> B[NavGraph 路由匹配]
    B --> C[解析路径参数]
    C --> D{DeepLink?}
    D -->|是| E[DeepLink 匹配]
    D -->|否| F[普通路由匹配]
    E --> G[NavBackStackEntry 管理]
    F --> G
    G --> H{栈操作}
    H -->|push| I[新 Entry 入栈]
    H -->|pop| J[当前 Entry 出栈]
    H -->|popUpTo| K[弹出到指定目的地]
    I --> L[目的地渲染]
    J --> L
    K --> L
    L --> M{渲染方式}
    M -->|Compose| N[NavHost → Composable]
    M -->|Fragment| O[NavHostFragment → FragmentTransaction]
    N --> P[ViewModel 作用域管理]
    O --> P
```

### 2.10 Room 数据访问流程

```mermaid
flowchart TD
    A[编译时: @Database/@Entity/@Dao] --> B[Room Compiler KSP 代码生成]
    B --> C[运行时: Room.databaseBuilder]
    C --> D[选择 SQLiteDriver]
    D --> E[Dao 方法调用]
    E --> F{返回类型}
    F -->|挂起函数| G[直接执行查询]
    F -->|Flow| H[观察查询+变更通知]
    F -->|LiveData| I[LiveData 观察]
    F -->|PagingSource| J[分页查询]
    G --> K[SQLiteStatement 执行]
    H --> K
    I --> K
    J --> K
    K --> L[结果映射到 Entity]
    L --> M[InvalidationTracker 监控表变更]
    M -->|数据变更| H
```

### 2.11 WorkManager 任务调度流程

```mermaid
flowchart TD
    A[定义 WorkRequest] --> B[设置 Constraints]
    B --> C[WorkManager.enqueue]
    C --> D[持久化到 Room 数据库]
    D --> E[选择调度器<br/>JobScheduler/GCM/AlarmManager]
    E --> F[等待约束满足]
    F --> G[Worker.doWork 执行]
    G --> H{执行结果}
    H -->|success| I[更新 WorkInfo]
    H -->|retry| J[BackoffPolicy 重试]
    H -->|failure| I
    J --> F
    I --> K[LiveData/Flow 观察]
```

### 2.12 Paging 分页加载流程

```mermaid
flowchart TD
    A[配置 Pager + PagingConfig] --> B[PagingSource 定义]
    B --> C{加载类型}
    C -->|Refresh| D[初始加载/刷新]
    C -->|Append| E[向后加载更多]
    C -->|Prepend| F[向前加载更多]
    D --> G[返回 LoadResult.Page]
    E --> G
    F --> G
    G --> H{有 RemoteMediator?}
    H -->|是| I[RemoteMediator.load<br/>网络→数据库]
    I --> J[PagingSource 观察<br/>数据库变更]
    J --> K[触发 UI 更新]
    H -->|否| K
    K --> L{UI 框架}
    L -->|Compose| M[LazyPagingItems → LazyColumn]
    L -->|View| N[PagingDataAdapter → RecyclerView]
```

### 2.13 Camera 使用流程

```mermaid
flowchart TD
    A[获取 ProcessCameraProvider] --> B[选择 CameraSelector]
    B --> C[配置 UseCase]
    C --> D{UseCase 类型}
    D -->|Preview| E[预览]
    D -->|ImageCapture| F[拍照]
    D -->|ImageAnalysis| G[图像分析]
    D -->|VideoCapture| H[视频录制]
    E --> I[bindToLifecycle]
    F --> I
    G --> I
    H --> I
    I --> J[自动管理相机生命周期]
```

### 2.14 Hilt 依赖注入流程

```mermaid
flowchart TD
    A[编译时: 注解处理] --> B[@HiltAndroidApp 生成 Application 组件]
    A --> C[@AndroidEntryPoint 生成注入代码]
    A --> D[@HiltViewModel 生成 ViewModel 组件]
    A --> E[@HiltWorker 生成 Worker 组件]
    B --> F[Dagger 生成依赖图]
    C --> F
    D --> F
    E --> F
    F --> G[运行时: @Inject 注入]
    G --> H{作用域}
    H -->|@Singleton| I[Application 级别]
    H -->|@ActivityScoped| J[Activity 级别]
    H -->|@ViewModelScoped| K[ViewModel 级别]
    H -->|@WorkerScoped| L[Worker 级别]
```

### 2.15 DataStore 存储流程

```mermaid
flowchart TD
    A[创建 DataStore 实例] --> B{类型}
    B -->|Preferences| C[preferencesDataStore]
    B -->|Proto| D[dataStore + Serializer]
    C --> E[读取: dataStore.data Flow]
    D --> E
    E --> F[写入: dataStore.edit/updateData]
    F --> G[读取当前数据]
    G --> H[应用转换函数]
    H --> I[序列化到临时文件]
    I --> J[原子性重命名]
    J --> K[发射新数据到 Flow]
```

### 2.16 版本预发布周期流程

```mermaid
flowchart LR
    Alpha[Alpha<br/>功能开发中<br/>API可变] -->|功能冻结| Beta[Beta<br/>仅修Bug<br/>API冻结]
    Beta -->|cherry-pick| RC[RC<br/>发布候选<br/>仅关键Bug]
    RC --> Stable[Stable<br/>正式发布<br/>二进制兼容保证]
```

### 2.17 KMP 多平台发布流程

```mermaid
flowchart TD
    A[commonMain 共享逻辑] --> B[expect 声明平台差异]
    B --> C[平台 actual 实现]
    C --> D{平台}
    D -->|androidMain| E[Android 实现]
    D -->|jvmMain| F[JVM Desktop 实现]
    D -->|nativeMain| G[iOS/Native 实现]
    D -->|wasmJsMain| H[Web Wasm 实现]
    E --> I[API 兼容性验证]
    F --> I
    G --> I
    H --> I
    I --> J[api/current.txt JVM API]
    I --> K[bcv/ KMP ABI]
    I --> L[Native ABI .klib]
    J --> M[Maven 多平台发布]
    K --> M
    L --> M
```

---

## 三、综合架构关系图

### 3.1 现代 Compose 应用完整架构

```mermaid
graph TB
    subgraph UI层
        Comp[@Composable 函数]
        M3Comp[Material3 组件]
        NavComp[Navigation Compose]
        PagComp[Paging Compose]
        ActComp[Activity Compose]
    end

    subgraph ViewModel层
        HiltVM[@HiltViewModel]
        StateFlow[StateFlow UiState]
        SSH[SavedStateHandle]
        LCComp[lifecycleScope]
    end

    subgraph Repository层
        Repo[@Inject Repository]
        RoomDAO[Room3 DAO]
        Net[网络请求]
        DSPref[DataStore Preferences]
        RemoteMed[RemoteMediator]
    end

    subgraph 数据层
        RoomDB[Room3 SQLite]
        DSFile[DataStore OkIO]
        WMgr[WorkManager]
    end

    subgraph 横切关注点
        HiltDI[Hilt 依赖注入]
        Nav[Navigation 导航]
        LCycle[Lifecycle 生命周期]
        Startup[Startup 初始化]
        Bench[Benchmark + Baseline Profile]
        Tracing[Tracing 追踪]
    end

    Comp --> M3Comp
    Comp --> NavComp
    Comp --> PagComp
    Comp --> ActComp
    Comp --> HiltVM
    HiltVM --> StateFlow
    HiltVM --> SSH
    HiltVM --> LCComp
    HiltVM --> Repo
    Repo --> RoomDAO
    Repo --> Net
    Repo --> DSPref
    Repo --> RemoteMed
    RoomDAO --> RoomDB
    DSPref --> DSFile
    RemoteMed --> RoomDB
    RemoteMed --> Net
```

### 3.2 测试金字塔

```mermaid
graph TB
    FTL[FTL 远程集成测试<br/>多 API 级别覆盖] --> Device[设备端测试<br/>connectedAndroidTest]
    Device --> Screenshot[截图测试<br/>Scuba 渲染验证]
    Screenshot --> JVM[JVM 单元测试<br/>Robolectric]
    JVM --> Bench2[基准测试<br/>Micro/Macrobenchmark]
```
