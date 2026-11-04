# 夜航 · Nocturne

把 AI 当作「贾维斯」的安卓学习应用。你说想学什么，它先诊断、拆出知识星图，再用苏格拉底式对话讲课、出题、安排复习，并记住你走到哪里。

## 下载

[最新正式版 APK](https://github.com/zykkkl/nocturne/releases/latest)

- Android 8.0（API 26）及以上
- 正式签名，覆盖安装会保留本机学习数据与模型配置
- 调试包的 applicationId 是 `com.nocturne.learn.debug`，可与正式包共存，但两者数据不互通

## 特点

- **不限学科**：直接说想学什么，AI 生成诊断与学习路径
- **贾维斯式教学**：一次只讲一个概念，先问再讲，答完更新掌握度
- **知识星图**：每个知识点有 0~1 掌握度，会随练习和复习点亮
- **间隔复习**：按 FSRS 思路安排到期复习，快忘记时再出现
- **自己的模型**：内置 DeepSeek / OpenAI / 阿里百炼 / Kimi / GLM / 火山方舟 / 硅基流动 / OpenRouter / Gemini / Ollama 模板，也支持任意 OpenAI 兼容接口
- **本地优先**：API Key 用 Android Keystore 加密，学习数据存在本机，没有账号、没有后端

学习闭环：诊断 → 学习路径 → 苏格拉底式讲授 → 练习与纠错 → 间隔复习 → 周复盘（开发中）。

## 当前进度

- [x] 深墨夜景 + 无色玻璃的设计系统
- [x] 引导、首页、知识星图、复习、我的
- [x] 模型服务配置：模板、连接测试、Keystore、持久化
- [x] 贾维斯教学会话与工具循环
- [x] 学习状态本地持久化，页面读取真实掌握度与到期项
- [x] 正式签名与 GitHub Release 自动发布
- [ ] 每周复盘与学习报告
- [ ] 学习数据导出 / 导入

## 构建

要求 JDK 21、Android SDK 35。

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Windows PowerShell：

```powershell
$env:JAVA_HOME="C:\path\to\jdk-21"
.\gradlew.bat :app:assembleDebug
```

首次构建前在根目录创建 `local.properties`，指向本机 Android SDK：

```properties
sdk.dir=/path/to/Android/Sdk
```

正式签名构建需要本地的 `keystore.properties` 和 keystore 文件（均不提交）：

```bash
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

## 技术栈

Kotlin 2.1 · Jetpack Compose · Haze · OkHttp · DataStore · kotlinx.serialization · Android Keystore

单 Activity + Navigation Compose；无 Room、无 Hilt、无后端。

## 持续集成

- push / PR：`.github/workflows/android.yml` 构建 debug APK
- 推送 `v*` tag：`.github/workflows/release.yml` 构建正式签名 APK 并创建 GitHub Release
