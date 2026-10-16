# Codex Cloud Android 开发环境

本说明用于 APS 的 Debug 单元测试与 Android 测试源码编译，不生成 Release APK、不触发发布。
产品施工依据为 `PROJECT/UI/README.md`，业务范围为 `PROJECT/SPEC/RELEASE-SCOPE-0.1.1.md`。

## 工具要求

| 工具 | 要求 |
|---|---|
| JDK | 完整 JDK 17，`java`、`javac` 与 Gradle Launcher JVM 均为 17 |
| Gradle | 8.10；当前仓库无 Wrapper，需要外部安装 |
| Android | Command-line Tools、Platform 35、Build Tools 34.0.0 / 35.0.0、platform-tools |
| Python | Python 3，用于保存命令、退出码及 JUnit 结果 |

`compileSdk 35` 不等于 `buildToolsVersion 35`。当前 AGP 8.7.3 默认会使用 Build Tools 34.0.0，需提前安装，避免首次构建隐式下载。

## 安装与环境变量

所有工具安装到仓库外，例如 `/workspace/aps-toolchain/`。从 Eclipse Temurin 官方发行渠道安装 Linux x64 JDK 17；从 Gradle 官方获取 `gradle-8.10-bin.zip`，校验官方 SHA-256 后解压。

```text
Gradle: https://services.gradle.org/distributions/gradle-8.10-bin.zip
SHA-256: 5b9c5eb3f9fc2c94abaea57d90bd78747ca117ddbbf96c859d3741181a12bf2a
SDK tools: https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
```

Android 工具解压后的层级应为 `android-sdk/cmdline-tools/latest/bin/sdkmanager`，不要多套一层 `cmdline-tools/`。

```bash
export JAVA_HOME=/workspace/aps-toolchain/jdk-17
export ANDROID_HOME=/workspace/aps-toolchain/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME=/workspace/aps-toolchain/gradle-home
export PATH="$JAVA_HOME/bin:/workspace/aps-toolchain/gradle-8.10/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager --licenses
sdkmanager 'platforms;android-35' 'build-tools;34.0.0' 'build-tools;35.0.0' platform-tools
```

需要允许访问 Gradle 分发及其 GitHub 下载重定向、Google Android SDK / Maven、Maven Central、Gradle Plugin Portal。保留云端代理与 TLS 校验。Java 工具不一定自动读取 `HTTPS_PROXY`；有代理时应根据环境提供的代理设置 Java 的 `http.proxyHost` / `https.proxyHost` 和端口，并将环境配置的 CA 导入这份独立 JDK 的 truststore。不要禁用证书校验，不要把令牌、代理认证信息或密钥提交到仓库。

本次环境生成了仓库外的 `/workspace/aps-toolchain/env.sh`，当前会话可 `source` 该文件。它不是仓库自带文件；重建环境后应重新安装并配置，不应假设路径仍存在。

## 验证

```bash
java -version
javac -version
gradle --version
sdkmanager --list_installed
gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestJavaWithJavac --stacktrace --console=plain --no-daemon --max-workers=2
```

如需自动保存证据，在一个尚不存在的仓库外目录执行：

```bash
python3 scripts/cloud-verify.py --output /workspace/aps-cloud-validation/run-001
```

脚本检查工具版本、SDK 文件，并强制重新执行现有测试和编译任务。输出包括完整 Commit SHA、分支、工作树状态、已跟踪文件差异、逐命令退出码、日志及本次成功运行的 JUnit XML。脚本不会收集环境变量全集或凭证。工作树有修改时，记录中的 SHA 仅表示基底，不能将结果冒充该提交的干净验证。

- 退出 `0`：命令成功且确有 JUnit 用例，无失败或错误。
- 退出 `2`：前置环境不满足，Android 任务未执行。
- 其他非零：查看 `summary.json` 与 `android-tests.log`；区分编译失败、依赖失败和测试断言失败。
- `compileDebugAndroidTest*` 只证明源码编译，不代表 instrumentation tests 已运行。
- 单元测试和编译成功不代表设备交互、存储权限、真实云端认证或项目隔离已经验收。

完整 Android 测试命令以 `.github/workflows/android-tests.yml` 为对照。不得使用 Release 构建或发布来替代环境验证。
