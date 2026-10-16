# Codex Cloud 环境基线结果

日期：2026-10-16 UTC。
基底分支：`main`。
业务源码对应 Commit：`024ca95d542ed7e74e1d7bf726376f36e1d7613d`。
首轮运行期间只有施工基线文档、忽略规则与验证脚本在工作树修改；Android 源码、依赖及构建配置未修改。最终工作分支的验证另以脚本记录的完整 SHA 和实际 PR Check 为准。

## 已确认

- 已读取根目录 AGENTS.md、README.md、当前 UI、产品规范、0.1.1 交付范围及 CI 测试工作流。
- 基底提交缺少 `docs/CODEX-CLOUD-BOOTSTRAP.md`，本批补齐。
- 已记录用户确认的 Task 完成、UI 施工、应用级文件权限与项目地址隔离规则；这是设计基线，不是功能交付。
- 在仓库外 `/workspace/aps-toolchain/` 安装工具，未改变系统默认 Java 或修改 APS 业务代码。

## 已验证

| 命令 | 退出码 | 实际结果 |
|---|---|---|
| `java -version` | 0 | Eclipse Temurin OpenJDK 17.0.20.1 |
| `javac -version` | 0 | javac 17.0.20.1 |
| `gradle --version` | 0 | Gradle 8.10，Launcher JVM 17 |
| `sdkmanager --list_installed` | 0 | Platform 35 revision 2、Build Tools 35.0.0、platform-tools 已安装 |
| 下述 Gradle 命令 | 0 | BUILD SUCCESSFUL；35 个任务实际执行；33 个 JUnit 测试套件，共 129 个用例，失败 0、错误 0、跳过 0 |

```bash
gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestJavaWithJavac --stacktrace --console=plain --no-daemon --max-workers=2
```

AGP 首次执行时还自动安装了默认使用的 Build Tools 34.0.0，因此 bootstrap 文档同时列出 34.0.0 和 35.0.0。未为此修改 app/build.gradle。

当前云端工作区原始记录：`/workspace/aps-cloud-validation/baseline/commands.json`，Gradle 完整输出：`/workspace/aps-cloud-validation/baseline/04.log`。这些路径是本次会话证据，不是保证长期保留的 GitHub Artifact；本文件保存可追溯摘要。

## 失败、警告与限制

- 修复前 `javac`、`gradle`、`sdkmanager` 均无法执行（127）；默认 Java 是 21。安装后本轮构建无失败。
- 现有警告：`startActivityForResult`、测试中的 `createTempDir` 已弃用，以及两处测试类型检查恒真。本批不修改无关业务或测试。
- 环境状态连接器重新查询曾返回 `The environment configuration changed. Try again.`；这里的工具可用性以实际版本检查、SDK 列表和构建执行结果为依据，不将连接器错误视为业务失败。
- 当前 SDK/JDK、下载缓存与代理/CA 配置属于这个云端实例；新实例仍需按 bootstrap 说明重建，不宣称平台模板已永久配置。

## 未执行

- Android instrumentation tests 在设备或模拟器上运行。
- 真机文件权限、公开 APS/ 日志、项目地址隔离、实际云端凭证和完整业务链验收。
- Release APK 构建、签名、发布、合并。
- 对 UI 差距清单中的业务缺口进行实现；清单见 `AI_WORKSPACE/TASK/2026-10-16-UI-IMPLEMENTATION-PLAN.md`。

云端编译与单元测试通过不能将上述未执行项改为“已验证”。
