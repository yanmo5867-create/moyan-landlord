# 漠视诺言 - AIDE 手机编译教程

## 前置条件
- 安卓手机（Android 5.0+）
- 已安装 AIDE（Google Play 搜索 "AIDE - Android IDE"）
- 手机存储 ≥ 500MB 可用空间
- **不需要电脑、不需要root、不需要配置环境变量**

## 完整步骤

### 第一步：准备项目文件夹
1. 在手机存储根目录创建文件夹：`AppProjects`
   - 路径：`/storage/emulated/0/AppProjects/`
2. 将 `moyan-project` 整个文件夹放入 `AppProjects` 中
   - 最终路径：`/storage/emulated/0/AppProjects/moyan-project/`

### 第二步：打开 AIDE
1. 启动 AIDE 应用
2. 它会自动扫描 `AppProjects` 目录
3. 找到 `moyan-project` 项目，点击它
4. AIDE 会提示 "Open this App Project" → 点击确认

### 第三步：等待索引完成
AIDE 首次打开会：
- 解析 AndroidManifest.xml
- 索引所有 Java 源文件
- 下载所需的 Android SDK 组件（如果提示）

### 第四步：编译运行
1. 点击 AIDE 底部的 **▶️ Run** 按钮（三角形图标）
2. 首次编译会下载 Gradle 依赖，约 2~5 分钟
3. 编译成功后，AIDE 会自动安装 APK 到手机
4. 点击图标即可启动「漠视诺言」

### 第五步：导出正式 APK（可选）
如果需要发布到应用商店：
1. AIDE 菜单 → **More** → **Export Signed APK**
2. 创建新 keystore（记住密码！）
3. 等待生成签名 APK

## 故障排查

| 问题 | 解决方案 |
|------|----------|
| "SDK not found" | AIDE设置 → 下载SDK → 选择 API 28 |
| 编译卡住 | 关闭AIDE重开，清理项目缓存 |
| 内存不足 | 关闭其他应用，重启手机后重试 |
| "Gradle sync failed" | 检查网络，AIDE需要联网下载依赖 |
| 图标不显示 | 确认 drawable 目录下有 ic_launcher 相关文件 |

## 项目信息
- 包名: `com.moyan`
- 应用名: 漠视诺言
- 最低支持: Android 5.0 (API 21)
- 目标版本: Android 9.0 (API 28)
- 总源码文件: 16个 Java 文件 + 9个资源文件
