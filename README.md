# ChooseMeal

一个面向大学生的 Android 决策助手，帮你在“去哪个食堂 / 去几楼 / 吃什么”之间快速做决定。

## 功能特性

- `转盘决策`：按当前筛选范围进行动态转盘抽取，转盘扇区数量随可选项实时变化。
- `候选精选`：一次生成多条候选项，你可直接点选确认结果。
- `多级筛选`：支持按食堂、楼层过滤候选范围。
- `数据管理`：可增删改查食堂、楼层、伙食，并支持启停状态。
- `随机去重`：内置冷却策略，降低重复命中概率。
- `导入导出`：支持 JSON 备份与恢复。
- `离线可用`：无需联网即可运行。

## 社区配置共享

支持通过 GitHub 社区共享“学校食堂配置文件”，让后来者可直接导入使用。

- 配置索引：`community/index.json`
- 配置目录：`community/configs/*.json`（可直接在 App 中导入）
- 贡献入口：Issue / PR（仓库内已提供模板）
- 自动校验：PR 会触发 `validate-community-configs` 工作流，检查结构与引用有效性

新用户导入步骤：

1. 在仓库中查看 `community/index.json`，找到你的学校。
2. 下载对应 JSON 配置文件到手机本地。
3. 打开 App -> `导入导出` -> `导入 JSON`，选择文件完成导入。

## 技术栈

- Kotlin
- Jetpack Compose (Material 3)
- Room
- DataStore
- Coroutines / Flow
- Gradle Kotlin DSL

## 项目结构

```text
ChooseMeal/
├─ app/
│  ├─ src/main/java/com/choosemeal/app/
│  │  ├─ data/        # Room、DataStore、导入导出、仓储
│  │  ├─ domain/      # 随机决策模型与引擎
│  │  └─ ui/          # Compose 页面与主题
│  └─ src/test/       # 单元测试
├─ community/         # 社区共享配置中心
├─ docs/
│  ├─ BUILD_AND_RUN.md
│  ├─ JSON_SCHEMA.md
│  └─ RELEASE_SIGNING.md
└─ gradle/ wrapper
```

## 运行环境

- Windows / macOS / Linux
- JDK 17+
- Android SDK（`platform-tools`、`build-tools`、`platforms;android-35`）

## 快速开始

```powershell
cd d:\work\ChooseMeal
.\gradlew.bat assembleDebug
```

Debug 包路径：

- `app/build/outputs/apk/debug/app-debug.apk`

## 质量检查

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

## Release 打包

项目支持本地临时签名打包：

```powershell
.\gradlew.bat assembleRelease
```

Release 包路径：

- `app/build/outputs/apk/release/app-release.apk`

签名配置见：

- `keystore.properties.example`
- `docs/RELEASE_SIGNING.md`

## JSON 数据格式

导入导出使用 `v1` 合约，字段包括：

- `version`
- `cafeterias[]`
- `floors[]`
- `meals[]`

详见：`docs/JSON_SCHEMA.md`

社区共享规范见：

- `community/README.md`

## 备注

- 本项目默认用于学习和个人使用。
- 如用于长期分发，请替换为你自己的正式签名证书与发布流程。
