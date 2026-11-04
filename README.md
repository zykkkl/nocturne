# 夜航 · Nocturne

一个把 AI 当作「贾维斯」的安卓学习应用：你说想学什么，它做诊断、拆知识星图、讲课、出题、安排复习，并记住你走到哪里。

- 深墨夜景 + 无色玻璃的单一深色主题
- 自带模型配置：DeepSeek / OpenAI / 阿里百炼 / Kimi / GLM / 火山方舟 / 硅基流动 / OpenRouter / Gemini / Ollama / 自定义
- API Key 只存本机，Android Keystore 加密；请求从手机直连服务商，没有后端
- 学习进度本地持久化，可导出（规划中）

## 构建

要求：JDK 21、Android SDK 35。

```bash
export JAVA_HOME=/path/to/jdk21
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Windows 示例：

```powershell
$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\jbr"
.\gradlew.bat :app:assembleDebug
```

首次构建前在根目录创建 `local.properties`（不要提交）：

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

## 当前进度

- [x] 设计系统：夜景、玻璃、香槟金主操作、衬线标题
- [x] 引导（目标 + 每日时长）
- [x] 首页 / 星图 / 复习 / 我的 四个页签
- [x] 模型服务配置（模板、连接测试、Keystore、持久化）
- [x] 贾维斯教学会话：诊断 → 拆分知识星图 → 苏格拉底式讲授
- [x] 工具循环：get_next_kp / save_plan / update_mastery / schedule_review / record_note / end_session
- [x] 学习状态本地持久化；首页、星图、复习读取真实掌握度与到期项
- [ ] 每周复盘与学习报告
- [ ] 导出备份
- [ ] 正式签名与 Release 发布

## 本地联调

没有真实 API Key 时，可用仓库里的 OpenAI 兼容 mock 跑通工具循环：

```bash
python tools/mock_openai.py
```

然后在 App 里新增自定义服务：Base URL 填 `http://10.0.2.2:8787/v1`（模拟器访问宿主机），模型 `mock-tutor`，Key 任意。请求日志写在 `tools/mock_requests.jsonl`（不提交）。

## 结构

```
app/src/main/java/com/nocturne/learn/
  data/         设置、密钥、模型模板、学习状态
  ai/           OpenAI 兼容客户端、导师引擎、工具
  ui/design/    夜景与玻璃组件
  ui/screens/   页面
  ui/nav/       导航与路由
```

## CI

`.github/workflows/android.yml` 在 push / PR 时构建 debug APK 并上传 artifact。

## 发布（GitHub Releases）

`.github/workflows/release.yml` 在推送 `v*` tag 时构建**正式签名** APK，并自动创建 GitHub Release。

### 一次性准备

1. 在 GitHub 新建仓库，推上去：

```bash
git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git push -u origin main
```

2. 打开仓库 **Settings → Secrets and variables → Actions**，新建 4 个 secret：

| Secret | 值 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | `nocturne-release.jks` 的 Base64（单行、无换行） |
| `RELEASE_STORE_PASSWORD` | `keystore.properties` 里的 `storePassword` |
| `RELEASE_KEY_ALIAS` | `nocturne` |
| `RELEASE_KEY_PASSWORD` | `keystore.properties` 里的 `keyPassword` |

生成 Base64：

```bash
# Git Bash / Linux
base64 -w0 nocturne-release.jks | clip
```

```powershell
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("nocturne-release.jks")) | Set-Clipboard
```

3. **备份** `nocturne-release.jks` 和 `keystore.properties`（两者都已 gitignore）。签名一旦丢失，已安装的正式版无法覆盖升级，只能卸载重装。

### 发一个版本

```bash
git add -A
git commit -m "夜航 0.3.0: 本次改了什么"
git tag v0.3.0
git push origin main --tags
```

Actions 跑完后，GitHub Releases 会出现「夜航 v0.3.0」，下载 `app-release.apk` 直接安装。CI 把 versionCode 设为 `100000 + run_number`，每次发布都高于上一版，可覆盖升级并保留本机数据。

本地也可以构建同样签名的包：

```bash
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

首次构建前需保证根目录有 `keystore.properties`（本机已生成，不提交）。
