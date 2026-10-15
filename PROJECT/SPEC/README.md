# APS Specification

正式产品需求与行为规范入口。

当前唯一正式规格主文件：

- `PROJECT/SPEC/APS-PRODUCT-SPEC.md`

版本范围与后续规划：

- `PROJECT/SPEC/RELEASE-SCOPE-0.1.1.md`：基础版交付范围、三个模块边界、施工与审查规则及完成判定。
- `PROJECT/SPEC/ROADMAP-0.1.2.md`：模块管理、第三方模块、连接层增强和协作能力等后续候选需求。

版本施工判断：

- 当前产品行为仍以正式产品规格与 UI 文档为准。
- 判断本次施工是否属于当前版本、是否构成阻塞时，以 `RELEASE-SCOPE-0.1.1.md` 的版本边界为准。
- 不影响 0.1.1 安全性、构建、已确认需求或主链验证的未来需求，登记到 0.1.2，不自动中断当前施工。
- 版本范围文件只定义目标和验收边界，不代表功能已经实现或通过验证。

职责：

- 定义当前产品模型；
- 定义正式行为边界；
- 区分已确认、待施工、已实现、已验证、阻塞、废弃；
- 作为后续 UI、架构与施工判断的产品基线；
- 区分当前版本交付要求与后续版本候选需求，避免未来需求无条件阻塞当前施工。

历史规格资料可以保留在 `docs/` 或历史审计目录，但不得与本文件冲突。

当前产品定位：**APS（AI + Context + Service）**。


## API 多协议版本记录

- 当前主线 Android 配置：`versionCode=4`、`versionName=0.1.1`、`applicationId=com.aps`、应用显示名称 `APS`。
- API Profile 已支持稳定协议 ID，以及 OpenAI 兼容、Anthropic Messages、Google Gemini 适配器；该能力不代表当前版本号为 `0.3.0`。
- 当前 Android `namespace` 与 Kotlin/Java 源码 package 仍为 `com.owla19s.aps`，尚未完成全量包迁移；安装包标识以 `applicationId=com.aps` 为准。
- 版本号与包标识只说明源码配置；实际交付仍以工作流、APK Manifest 检查和真机安装启动验证为准。
