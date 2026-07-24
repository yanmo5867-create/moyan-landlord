# 漠视诺言 - 单机斗地主

包名: `com.moyan`
应用名: 漠视诺言

## 功能清单

### 核心玩法
- 1人 vs 2个AI（纯单机离线）
- 标准54张牌 + 双副牌模式（108张）
- 完整13种牌型识别 + 大小比较
- 叫分→抢地主→出牌完整流程
- 春天/反春天判定

### 6大游戏模式
1. 经典标准场
2. 不洗牌高爆场
3. 天地癞子场
4. 明牌练技术场
5. 快速精简对局
6. 五十K独立模式
7. 双副牌模式

### AI系统（10档难度）
- 菜鸟 → 新手 → 入门 → 熟手 → 进阶 → 高手 → 精英 → 大师 → 宗师 → 至尊
- 难度随斗币自动升降
- 至尊AI：全局记牌、概率博弈、诱炸骗牌

### 特效系统（7档画质）
- 顺子：流光滑动
- 连对：彩色链条
- 飞机：粒子飞行
- 炸弹：火光炸裂+屏幕抖动
- 王炸：闪电高光+超强振动
- 五十K：彩带绽放

### 音效/振动
- 13种牌型独立音效
- TTS语音播报出牌
- 四级振动强度

### 系统功能
- 斗币经济系统（斗币锁底0）
- 13级段位体系
- 记牌器（可开关、可拖拽）
- 帧率控制（30/60/90/120Hz + 自动拉满）
- 音量三通道独立
- 开发者选项（运气值0~100%）
- 夜间护眼模式
- 切后台自动暂停

## 用AIDE在手机上编译APK（无需电脑、无需root）

### 准备工作
1. 手机安装 **AIDE**（Google Play 搜 "AIDE - Android IDE"）
2. 确保手机存储空间 ≥ 200MB

### 导入项目
1. 将整个 `moyan-project` 文件夹复制到手机存储的 `AppProjects` 目录
   - 路径应为：`/storage/emulated/0/AppProjects/moyan-project/`
   - 如果 `AppProjects` 文件夹不存在，用文件管理器新建一个
2. 打开 AIDE
3. AIDE 会自动识别项目，点击 "Open this App Project"

### 构建APK
1. 在 AIDE 中打开项目后，点击底部 **▶️ Run** 按钮
2. AIDE 会自动编译 → 生成 APK → 安装到手机
3. 首次编译可能需要 3~5 分钟（后续增量编译很快）

### 导出签名APK（发布用）
1. 在 AIDE 中点击菜单 → **More** → **Export Signed APK**
2. 创建或选择 keystore
3. 输入密钥密码
4. 等待生成已签名的 APK 文件

### 常见问题
- **编译报错 "SDK not found"**：在 AIDE 设置中下载对应 SDK 版本（Android 9.0 / API 28）
- **内存不足**：关闭其他应用，确保 ≥ 500MB 可用内存
- **卡在 Building**：首次构建需要下载依赖，确保网络畅通

## 项目结构

```
moyan-project/
├── AndroidManifest.xml       # 应用配置（权限、Activity声明）
├── build.gradle              # 构建脚本
├── project.properties        # AIDE项目属性
├── proguard-rules.pro       # 混淆规则
├── README.md
├── src/
│   └── com/moyan/
│       ├── MoyanApp.java          # 全局Application
│       ├── audio/
│       │   ├── AudioManager.java      # 音效+语音播报
│       │   └── VibrationManager.java # 振动控制
│       ├── effect/
│       │   └── EffectManager.java    # 特效引擎
│       ├── engine/
│       │   ├── CardEngine.java       # 牌型识别+比较
│       │   ├── GameEngine.java       # 核心对局逻辑
│       │   ├── FiftyKEngine.java     # 五十K模式
│       │   ├── AIEngine.java         # AI决策(10档)
│       │   ├── CardCounter.java      # 记牌器
│       │   ├── CoinRankManager.java  # 斗币+段位
│       │   └── RefreshRateManager.java # 帧率控制
│       ├── model/
│       │   ├── Card.java             # 扑克牌模型
│       │   └── CardType.java        # 牌型枚举
│       └── ui/
│           ├── MainActivity.java      # 竖屏主菜单
│           ├── GameActivity.java     # 横屏对局
│           ├── SettingsActivity.java # 设置页
│           ├── CardView.java         # 自定义卡牌视图
│           └── CardTouchHandler.java# 滑动选牌
└── res/
    ├── drawable/  (背景、图标、形状)
    ├── layout/    (activity_main.xml, activity_game.xml, activity_settings.xml)
    └── values/    (strings.xml, colors.xml, styles.xml)
```

## 版权
MIT License
