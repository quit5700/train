# Minecraft 26.2（我的世界 26.2）移植记录

## 已验证成功的组合

Minecraft `26.2` + Java `25` + Fabric Loader `0.19.3` + Fabric API `0.154.0+26.2` + Fabric Loom `1.15.5` + Mojang Official Mappings（Mojang 官方命名映射）。Yarn Mappings（Yarn 命名映射）未使用。

## 成功方法

- JavaCompile（Java 编译）显式使用 UTF-8 和 Java 25 release（发行版）。
- Fabric API 子模块用于编译，完整 `fabric-api` 用 Gradle `runtimeOnly`（仅运行时）加入开发客户端和服务端。
- Minecraft 26.2 配方文件使用当前版本的物品组件式输出结构；专用服务端已实测加载 1593 个配方，比原版 1585 个多 8 个。
- 中文项目路径下，测试编译输出同步到临时 ASCII（美国信息交换标准代码）路径再运行，避免测试运行器解析中文类路径异常。
- Minecraft 26.2 服务端存档使用 Codec（编解码器）和 SavedData（存档数据）。

## 已尝试失败，禁止重复

- 不要在当前 Loom 配置中将完整 Fabric API 声明为 `modRuntimeOnly`：已实测不会把完整 API 模组放入此开发运行环境。使用 `runtimeOnly` 已成功。
- 不要沿用旧版配方 JSON（JavaScript 对象表示法）输出格式：Minecraft 26.2 会拒绝加载。
- 不要假设存在独立 `compileClientJava`（编译客户端 Java）任务：当前工程将客户端源码纳入主 Source Set（源集）。

详细根因与回归方式见 `docs/bugs`。
