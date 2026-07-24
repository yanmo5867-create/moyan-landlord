# 漠视诺言 (Moyan Landlord)

纯单机离线安卓斗地主游戏，无网络、无广告、无弹窗。

## 项目信息
- 应用名：漠视诺言
- 包名：com.moyan
- 最低支持：Android 6.0 (API 23)
- 目标版本：Android 14 (API 34)

## 功能特性
- 经典斗地主 + 五十K模式
- 10档AI难度（新手→至尊）
- 斗币系统 + 13级段位
- 记牌器、语音播报、振动反馈
- 顺子/连对/飞机/炸弹/王炸全套特效
- 30/60/90/120Hz帧率调节
- 7档画质设置
- 开发者选项（运气值调节）
- 竖屏首页→横屏对局

## 构建方式
### GitHub Actions自动构建
推送代码到main分支后自动触发构建，在Actions页面下载APK。

### 本地构建
```
gradle wrapper --gradle-version 8.0
./gradlew assembleDebug
```

## 项目结构
```
app/
  src/main/java/com/moyan/
    MoyanApp.java          # Application入口
    StatsManager.java       # 战绩统计
    audio/
      AudioManager.java     # 音效+语音播报
      VibrationManager.java # 振动反馈
    effect/
      EffectManager.java    # 特效管理
    engine/
      CardEngine.java       # 牌型识别核心
      GameEngine.java       # 对局逻辑
      AIEngine.java         # AI决策
      CardCounter.java      # 记牌器
      CoinRankManager.java  # 斗币+段位
      SettingsManager.java  # 设置管理
      RefreshRateManager.java # 高刷适配
      FiftyKEngine.java     # 五十K模式
    model/
      Card.java             # 卡牌模型
      CardType.java         # 牌型枚举
    ui/
      MainActivity.java     # 首页(竖屏)
      GameActivity.java     # 对局(横屏)
      SettingsActivity.java # 设置页
  src/main/res/
    layout/                 # 界面布局
    drawable/               # 图标/背景
    values/                 # 配置资源
```

## License
MIT
