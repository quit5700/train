# Minecraft 26.2 交付报告

## 2026-08-13：0.2.23 固定停车轨道与远程车辆段权限修订

- 站点停车轨道固定为站点方块正上方 `(X,Y+1,Z)` 的普通信号铁轨。删除 `0.2.22` 已证实会选错分支的“周围 8 格候选端点、任意终点扫描和自动覆盖停车坐标”方案，线路恢复准确端点逐段扫描。
- 铁路线路设置器远程进入车辆段时，服务端按玩家、维度和车辆段坐标签发五分钟临时授权；保存、暂停/继续、立即生成、立即发车、列车列表及返回窗口共用同一权限判断。直接 `Ctrl（控制键）+右键`仍保留 8 格距离限制，拒绝时不再静默。
- Minecraft（我的世界）`26.2`、Java（爪哇）`25`、Fabric Loader（Fabric 模组加载器）`0.19.3`、Fabric API（Fabric 应用程序接口）`0.154.0+26.2`、Fabric Loom（Fabric 构建插件）`1.15.5`、Mojang Official Mappings（Mojang 官方命名映射）`26.2`；Yarn Mappings（Yarn 命名映射）未使用。
- 在 UTF-8 中文路径中执行 `gradlew clean build --no-daemon --console=plain（干净构建）`成功；186 项 JUnit（Java 单元测试）通过，失败 0、错误 0、跳过 0。
- 最终 JAR（Java 归档）内 78 个 JSON（JavaScript 对象表示法）经标准解析器检查全部有效；八种语言各 240 个翻译键且键集合一致；`fabric.mod.json（模组清单）`版本为 `0.2.23`。
- 普通 JAR：`fabric-train-simulator-0.2.23-mc26.2-fabric.jar`，SHA-256：`6EE3C8743DAF2E761639E58BA8988026CECE657ED2C2AE327C6B114AA5C637A8`，485808 字节。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.23-mc26.2-fabric-sources.jar`，SHA-256：`2DB396F0F7CC5BA602E629C07CFFFBDD010FC7184E5D691A24FA9C3F370B9C96`，211592 字节。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.23-fixed-station-stop-remote-depot`；普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`。分发、发布和安装文件 SHA-256 一致，活动模拟火车 JAR 只有一个；原 `0.2.21` 已移入可恢复备份 `mods\_fabric_train_backup\0.2.21-before-0.2.23`。
- 当前目录没有本模组独立 Git（版本控制）远程仓库，因此没有可安全使用的 GitHub Releases（GitHub 发行版）上传目标。真实世界线路刷新与远程按钮仍需玩家验收。

## 2026-08-12：0.2.19 自动道岔隔离、车辆段反馈与列车注销修订

- 玩家可见的左转/直行/右转只控制手动列车。线路自动列车只读取 Runtime Path Cache（运行时路径缓存）的隐形选路，不再写入或恢复现场 `RailShape（铁轨形状）`；两种模式仍共享安全占用锁。`0.2.18` 的“临时改现场道岔再恢复”已明确记为失败方案。
- 车辆段保存、暂停/继续、立即生成和立即发车执行后均保持设置窗口，固定显示成功或具体失败原因。列车列表负载保存原车辆段坐标，返回时精确重开原设置。
- 正常回收、脱轨掉落、创造模式删除和整列销毁统一清理在轨计数、道岔锁、运行缓存和列表可见记录。区块卸载不触发注销；旧存档历史身份先隐藏，实体重载时恢复原列车编号。
- Minecraft（我的世界）`26.2`、Java（爪哇）`25`、Fabric Loader（Fabric 模组加载器）`0.19.3`、Fabric API（Fabric 应用程序接口）`0.154.0+26.2`、Fabric Loom（Fabric 构建插件）`1.15.5`、Mojang Official Mappings（Mojang 官方命名映射）`26.2`；Yarn Mappings（Yarn 命名映射）未使用。
- 在 UTF-8 中文路径中执行 `gradlew clean build（干净构建）`成功；171 项 JUnit（Java 单元测试）通过，失败 0、错误 0、跳过 0。
- 最终 JAR（Java 归档）内 78 个 JSON（JavaScript 对象表示法）全部解析成功；八种语言各 237 个翻译键，键集合和参数占位符一致；`fabric.mod.json（模组清单）`版本为 `0.2.19`。
- 普通 JAR：`fabric-train-simulator-0.2.19-mc26.2-fabric.jar`，SHA-256：`83E57E86894CDA885838FAEAC73143EFCC112B5ECD0A1FB9C70873A921FB6CE7`，470474 字节。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.19-mc26.2-fabric-sources.jar`，SHA-256：`C63A84CF8A5A1F0F7537B21B8016FFE5E79C499F6EC116A791DC25A052AA0900`，206015 字节。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.19-automatic-switch-isolation-lifecycle`。普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装哈希与 `dist`一致且活动模拟火车 JAR 只有一个；旧 `0.2.18` 已移入可恢复备份 `mods\_fabric_train_backup\0.2.18`。
- 当前目录没有本模组独立 Git（版本控制）远程仓库，因此未执行 GitHub Releases（GitHub 发行版）上传。真实环线的自动道岔通过时间、小窗口操作反馈与区块卸载后编号恢复仍需玩家进世界验收。

## 2026-08-11：0.2.18 自动道岔与连续发车修订

- 道岔设置窗口已删除北、东、南、西固定入口按钮和“固定入口”文字；左转、直行、右转继续作为手动列车方向。线路自动列车直接读取 Runtime Path Cache（运行时路径缓存）的前后轨道，提前临时切换道岔并同步高亮，最后一节车厢离开后恢复玩家保存的手动方向。
- “立即发车”成功后启用连续自动发车，按已设置的间隔补充到“在轨自动列车”上限；低于上限后继续尝试。创造模式启动的任务不消耗车辆库存，生存模式启动的任务正常消耗；最近一次自动发车结果会保存并显示在车辆段窗口。
- Minecraft（我的世界）`26.2`、Java（爪哇）`25`、Fabric Loader（Fabric 模组加载器）`0.19.3`、Fabric API（Fabric 应用程序接口）`0.154.0+26.2`、Fabric Loom（Fabric 构建插件）`1.15.5`、Mojang Official Mappings（Mojang 官方命名映射）`26.2`；Yarn Mappings（Yarn 命名映射）未使用。
- 在 UTF-8 中文路径中执行 `gradlew clean build --no-daemon --console=plain（干净构建）`成功；167 项 JUnit（Java 单元测试）通过，失败 0、错误 0、跳过 0。
- 最终 JAR（Java 归档）内 78 个 JSON（JavaScript 对象表示法）全部解析成功；八种语言各 234 个翻译键，键集合和参数占位符一致；`fabric.mod.json（模组清单）`版本为 `0.2.18`。
- 普通 JAR：`fabric-train-simulator-0.2.18-mc26.2-fabric.jar`，SHA-256：`3852505BAD820A79E191DDA933BE26235C6D3615C96108FB4B116F3D546B314D`，467746 字节。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.18-mc26.2-fabric-sources.jar`，SHA-256：`3E2F596E9A83F73A7E9EBC1E25648E77A96940A307478DD07B3B6CD192D0B976`，204411 字节。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.18-automatic-switch-continuous-dispatch`。普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装哈希与 `dist`一致且活动模拟火车 JAR 只有一个；旧 0.2.17 已移入可恢复备份 `mods\_fabric_train_backup\0.2.17`。
- 当前目录没有本模组独立 Git（版本控制）远程仓库，因此未执行 GitHub Releases（GitHub 发行版）上传。真实世界中的自动道岔切换时间、创造/生存库存消耗和回收后补发仍需玩家按回归清单验收。

## 2026-08-10：0.2.17 车辆段列车列表更新

- 车辆段设置窗口右下角新增“列车列表”，点击后由服务端实时读取当前列车注册表；列表按编号排序，仅显示列车编号及“手动列车 / 线路自动列车”，支持空状态、小窗口和滚轮查看。
- Minecraft（我的世界）`26.2`、Java（爪哇）`25`、Fabric Loader（Fabric 模组加载器）`0.19.3`、Fabric API（Fabric 应用程序接口）`0.154.0+26.2`、Fabric Loom（Fabric 构建插件）`1.15.5`、Mojang Official Mappings（Mojang 官方命名映射）`26.2`；Yarn Mappings（Yarn 命名映射）未使用。
- 在 UTF-8 中文路径中执行 `gradlew clean build --no-daemon --console=plain（干净构建）`成功；159 项 JUnit（Java 单元测试）通过，失败 0、错误 0、跳过 0。
- 最终 JAR（Java 归档）内 78 个 JSON（JavaScript 对象表示法）全部解析成功；八种语言各 233 个翻译键，键集合和参数占位符一致；`fabric.mod.json（模组清单）`版本为 `0.2.17`。
- 普通 JAR：`fabric-train-simulator-0.2.17-mc26.2-fabric.jar`，SHA-256：`F889F1AFF09DC0CE19BF3E2D8C9BF28A0942643A3D29EAE65F942175D9532FD1`，465046 字节。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.17-mc26.2-fabric-sources.jar`，SHA-256：`828C7DEF5E1AE79B9086E9830AA74ABFB58F3E0167A2574CF18AD5C37EEC31A5`，202866 字节。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.17-depot-train-list`。普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装哈希与 `dist`一致且活动模拟火车 JAR 只有一个；旧 0.2.16 已移入可恢复备份 `mods\_fabric_train_backup\0.2.16`。
- 当前目录没有本模组独立 Git（版本控制）远程仓库，因此未执行 GitHub Releases（GitHub 发行版）上传。

## 2026-08-10：0.2.16 卸货持久化与自动线路运行缓存修订

- Minecraft（我的世界）`26.2`、Java（爪哇）`25`、Fabric Loader（Fabric 模组加载器）`0.19.3`、Fabric API（Fabric 应用程序接口）`0.154.0+26.2`、Fabric Loom（Fabric 构建插件）`1.15.5`、Mojang Official Mappings（Mojang 官方命名映射）`26.2`；Yarn Mappings（Yarn 命名映射）未使用。
- 在 UTF-8 中文路径中执行 `gradlew clean build --no-daemon --console=plain（干净构建）`成功；157 项 JUnit（Java 单元测试）通过，失败 0、错误 0、跳过 0。
- 最终 JAR（Java 归档）内 78 个 JSON（JavaScript 对象表示法）全部解析成功；八种语言各 226 个翻译键，键集合和参数占位符一致；`fabric.mod.json（模组清单）`版本为 `0.2.16`。
- 普通 JAR：`fabric-train-simulator-0.2.16-mc26.2-fabric.jar`，SHA-256：`CC0A5DA97A681C9434A43F6754606BB602C64A1D0967EF091CDA50621FD86BE1`，455607 字节。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.16-mc26.2-fabric-sources.jar`，SHA-256：`11147E406AEF9A270E276F6ECC537C5A21CAFBA4B904EA0D41B63E2D78FC83B4`，200494 字节。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.16-persistent-cargo-automatic-route`。普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装哈希与 `dist`一致且活动模拟火车 JAR 只有一个；旧 0.2.15 已移入可恢复备份 `mods\_fabric_train_backup\0.2.15`。
- 自动测试覆盖卸货目的地持久化、火车头只在保存时提交、空容器接收、发车动作到控制模式的显式映射和自动列车使用已验证缓存路径。真实存档中的完全卸货和连续多道岔仍需玩家按回归清单验收。
- 当前项目没有本模组独立 Git（版本控制）远程地址，因此没有上传到错误的 GitHub Releases（GitHub 发行版）；本地发行与游戏安装已经完成。

## 2026-08-03：0.2.15 三维停车轨道与列车卸货规则修订

- Minecraft（我的世界）`26.2`、Java（爪哇）`25`、Fabric Loader（Fabric 模组加载器）`0.19.3`、Fabric API（Fabric 应用程序接口）`0.154.0+26.2`、Fabric Loom（Fabric 构建插件）`1.15.5`、Mojang Official Mappings（Mojang 官方命名映射）`26.2`；Yarn Mappings（Yarn 命名映射）未使用。
- Java 25 在 UTF-8 中文路径中编译主端和客户端共 118 个源文件，生成 176 个类；最终普通 JAR 自身作为测试类路径时，152 项 JUnit（Java 单元测试）全部通过，失败 0、错误 0。
- 八种语言各 226 个翻译键，键集合和参数占位符一致；最终 JAR 内 78 个 JSON（JavaScript 对象表示法）全部解析成功，`fabric.mod.json（模组清单）`版本为 `0.2.15`。
- 普通 JAR（Java 归档）：`fabric-train-simulator-0.2.15-mc26.2-fabric.jar`，SHA-256：`2ECE6489986CBCD71E605C865DA9B5C1F5A6DF442E50023A7EBBDDDFD6271C9E`，403295 字节。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.15-mc26.2-fabric-sources.jar`，SHA-256：`6B6109365DEF6B664AA2BA5039C160B18E63DA3184F51946D349159DEE323C2C`，201986 字节。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.15-stop-track-unload-rules`。当前受限环境没有游戏目录写权限，因此没有覆盖 `G:\game\mcfabric\.minecraft\versions\26.2\mods`中的活动模组。
- 当前网络策略禁止 Gradle Wrapper（Gradle 包装器）下载；缓存 Fabric Loom 1.15.5 又缺少传递依赖元数据。最终普通 JAR 使用上一版清单确认的 `official（官方命名）`命名空间从零组装，只复用本次未修改、必须依赖 Access Widener（访问扩展器）编译的 `ModItemGroup（模组物品栏组）`类；旧车站手动卸货服务类确认未进入新包。此离线方案已记录在 `BUG-047`，不能误报为完整 Loom 构建成功。
- 当前项目没有本模组独立 Git（版本控制）远程地址，无法同步 GitHub Releases（GitHub 发行版）。高低站台、真实自动/手动停车与容器卸货仍需玩家按回归清单进游戏验收。

## 2026-08-02：0.2.14 持久道岔入口与手动编组方向修订

- Minecraft（我的世界）`26.2`、Java（爪哇）`25`、Fabric Loader（Fabric 模组加载器）`0.19.3`、Fabric API（Fabric 应用程序接口）`0.154.0+26.2`、Fabric Loom（Fabric 构建插件）`1.15.5`、Mojang Official Mappings（Mojang 官方命名映射）`26.2`；Yarn Mappings（Yarn 命名映射）未使用。
- 在 UTF-8 中文路径中执行 `gradlew clean build --no-daemon --console=plain`成功；141 项测试通过，0 失败，0 错误。
- 八种语言均为 210 个翻译键，键集合一致；JAR 内 `fabric.mod.json`版本已核对为 `0.2.14`。
- 普通 JAR（Java 归档）：`fabric-train-simulator-0.2.14-mc26.2-fabric.jar`，SHA-256：`8705CA27C75345A3AF2C6E7994E727B34802C9BA7CBD7B1063A91D3EF8C84200`。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.14-mc26.2-fabric-sources.jar`，SHA-256：`732AEC22641BCB23B7661A9C028283D6615B6853B6BB1C1B6A52065C1B38D3EF`。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.14-switch-entry-formation-direction`。普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装文件与 `dist`哈希一致且活动模拟火车 JAR 只有一个；旧 0.2.13 已移动到可恢复备份 `mods\_fabric_train_backup\0.2.13`。
- 当前项目没有本模组独立 Git（版本控制）远程地址，无法同步 GitHub Releases（GitHub 发行版）；本地发行文件已完整生成。

## 2026-08-02：0.2.13 重复道岔环线、暂停控制与全窗口响应式修订

- 在 UTF-8 中文路径中执行 `gradlew clean build（干净构建）`成功；135 项 JUnit（Java 单元测试）全部通过，失败 0、错误 0、跳过 0。
- 八个语言 JSON（JavaScript 对象表示法）均可解析，各 205 个翻译键，键集合及参数占位符一致。
- 普通 JAR（Java 归档）：`fabric-train-simulator-0.2.13-mc26.2-fabric.jar`，SHA-256：`268F34CF7235978EA91C6DD6C126C88B55980A5E59C148EC385AD5FE6410D4A4`。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.13-mc26.2-fabric-sources.jar`，SHA-256：`454860078A980B04BE57F4E1C82C87E75316D926B243303E4607F9FED5BD66FF`。
- 两个文件已保存到 `dist（分发目录）`和 `releases/0.2.13-route-loop-pause-responsive`。普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装文件与 `dist`哈希一致，活动模拟火车 JAR 只有一个；旧 0.2.12 已移动到可恢复备份 `mods\_fabric_train_backup\0.2.12`。
- 首次替换时 Minecraft（我的世界）仍在运行，Windows 正确拒绝移动被占用的 0.2.12；没有强制结束游戏或删除文件。玩家正常退出后完成备份和唯一活动版本校验。下次不得在旧 JAR 被锁定时误报安装完成。
- 自动测试覆盖同一道岔按节点序号重复经过、固定入口绝对出口、首尾车站/道岔循环、自动列车到站暂停与手动列车隔离、七个自定义窗口响应式布局和车辆段有序车厢显示。实际世界中的双道岔环线、暂停/继续及高 GUI Scale（界面缩放）仍需玩家验收。
- 当前目录没有本模组独立的 Git（版本控制）远程仓库，上级远程属于其他项目，因此没有把发行文件上传到错误的 GitHub Releases（GitHub 发行版）。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用；Mojang Official Mappings（Mojang 官方命名映射）`26.2`。

## 2026-08-02：0.2.12 道岔锁、响应式窗口与发车诊断修订

- 在 UTF-8 中文路径中执行 `gradlew clean build（干净构建）`成功；120 项 JUnit（Java 单元测试）全部通过，失败 0、错误 0。
- 八个语言 JSON（JavaScript 对象表示法）均可解析，各 187 个翻译键，键集合一致。
- 普通 JAR（Java 归档）：`fabric-train-simulator-0.2.12-mc26.2-fabric.jar`，SHA-256：`AB891D69F5A1B43A27722590A7EFBD55EB1E3512143FDF3A6F8154AE6D628B07`。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.12-mc26.2-fabric-sources.jar`，SHA-256：`0068BCBC64DA6FE530F2E910B3402BB8ADB2E5C6D1B05C68DCA996CCC5464938`。
- 普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装文件与 `dist（分发目录）`哈希一致，活动模拟火车 JAR 只有一个；旧 0.2.11 已移动至可恢复备份 `mods\_fabric_train_backup\0.2.11`。
- 自动测试覆盖道岔尾车释放状态、控制器锁生命周期接入、七种立即发车预检查、自动线路验证、响应式窗口边界和固定底部按钮。Minecraft（我的世界）内的实际 GUI Scale 4 显示、两道岔连续通行和线路世界扫描仍需玩家按回归清单验收。
- 未修改已经正常的列车连接、编组排序、整列逻辑运动和车厢跟随实现。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用；Mojang Official Mappings（Mojang 官方命名映射）`26.2`。

## 2026-08-02：0.2.11 车辆段操作、发车方向与货车停靠修订

- 在 UTF-8 中文路径中执行 `gradlew clean build（干净构建）`成功；111 项 JUnit（Java 单元测试）全部通过，失败 0、错误 0。
- 八个语言 JSON（JavaScript 对象表示法）均可解析，各 179 个翻译键，键集合一致。
- 普通 JAR（Java 归档）：`fabric-train-simulator-0.2.11-mc26.2-fabric.jar`，SHA-256：`77AB6B5E7EE1645AF479E0BC3B5E8FA0397298DC9E32281B7AE4474EB7C97963`。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.11-mc26.2-fabric-sources.jar`，SHA-256：`271D6A030055E98733B103F57826E9FF4A8C6FD67C1186D40DAC0E8B4B21CC7D`。
- 普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`；安装文件与 `dist（分发目录）`源文件哈希一致，活动模拟火车 JAR 只有一个。旧 0.2.10 已移动到可恢复备份目录 `mods\_fabric_train_backup\0.2.10`。
- 未修改已验收的车辆连接、整列逻辑控制器与车厢跟随实现；游戏内发车方向、自动调度、线路删除回收和道岔锁释放仍需按回归清单人工验收。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用；Mojang Official Mappings（Mojang 官方命名映射）`26.2`。

## 2026-08-01 0.2.9 发电机燃料延迟消耗修订

- 0.2.8 已能在服务器原子提交供电，但仍在启动时立即缩减燃料，红石块在界面中依然瞬间消失；该方案已标记为失败。
- 0.2.9 改为启动时预留燃料槽位并立即供电，红石粉满 72,000 游戏刻（1 小时）、红石块满 504,000 游戏刻（7 小时）后才消耗一个。
- 工作中的槽位禁止玩家和漏斗取走；`ReservedFuelSlot（预留燃料槽位）`与 `FuelUntilGameTime（供电结束游戏时间）`共同保存，重启后继续工作。旧 0.2.8 活跃燃料没有预留槽位，到期不会重复扣除。
- 未修改列车连接、整列控制器、车厢互动或运动代码；车辆段仍在发车前调用 `RailPowerService.refreshNow（立即刷新铁路供电）`。
- UTF-8 中文路径下执行 `clean build（干净构建）`成功；JUnit（Java 单元测试）共 101 项，0 失败、0 错误；八个语言文件键集合一致。
- 普通 JAR：`fabric-train-simulator-0.2.9-mc26.2-fabric.jar`，SHA-256：`EB8B44B368D93DF392811EFD22881C145536E6D59C07D340ECFE5093DC79B063`。
- sources JAR（源码 Java 归档）：`fabric-train-simulator-0.2.9-mc26.2-fabric-sources.jar`，SHA-256：`46BECAE2ADD53C0E3E0AE5973623306E45F62CEF50C86289A461272D4C4CC0BF`。
- 两个文件已保存到 `dist`及 `releases/0.2.9-generator-deferred-fuel`；普通 JAR 已安装到 Minecraft 26.2 的 `mods`目录，0.2.8 已移入可恢复备份，安装后只保留一个活动模拟火车 JAR。
- 直接执行安装脚本首次被本机 PowerShell 执行策略拦截，未产生文件改动；改用仅当前进程的 `-ExecutionPolicy Bypass`后成功。以后不再直接调用 `.ps1`并假设系统允许执行。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用，采用 Mojang Official Mappings（Mojang 官方命名映射）`26.2`。

## 2026-08-01 0.2.8 发电机服务器原子提交与绝对供电时间修订

- 0.2.7 实际复测仍出现红石块消失、车辆段无法发车；存档确认加载版本正确，但发电机库存为空且剩余燃料为 0，车辆段编组和车辆物品充足。
- 根因是 0.2.7 从不保证仅在服务器执行的 `setItem（设置库存物品）`入口产生业务副作用，并且先扣燃料、后判断服务器世界，允许客户端扣物而服务器未提交供电。
- 0.2.8 删除库存入口消耗，只有服务器方块实体游戏刻可以提交燃料；先保存绝对供电结束时间并刷新铁路网络，成功后才扣除一个燃料，失败和异常均回滚。
- 新增 `GeneratorFuelState（发电机燃料状态）`，保存 `FuelUntilGameTime（供电结束游戏时间）`；旧剩余游戏刻字段会自动迁移。
- 旧燃料测试在第一次真实执行时三项空指针失败，证明 0.2.7 的验证结论无效；新测试覆盖成功、失败、异常回滚、保存重载、旧数据迁移和服务器游戏刻入口。
- 修复 Ctrl 资源测试误指向 `src/main/java`的问题；改为真实 `src/client/java`后通过，未修改生产交互代码。
- 完整 Gradle（构建工具）测试共 100 项，失败 0；列车连接、运动、道岔、货运和线路代码未修改。
- `gradlew clean build`干净构建成功。普通 JAR（Java 归档）SHA-256：`5C3B66B3645EB66E4438D917EC52CC5853D00022DB2E1A00ABE2D9C11EE94688`；sources JAR（源码 Java 归档）SHA-256：`B139296D815081E64402972C7EEA69E6273956A50D0E68237054C8FFD4F3BF7C`。
- 两个文件已保存到 `dist`及 `releases/0.2.8-generator-server-atomic-fuel`；普通 JAR 已安装到 Minecraft 26.2 的 `mods`目录且哈希一致，0.2.7 已移到可恢复备份目录。活动的模拟火车 JAR 只有 0.2.8 一个。
- 当前目录没有本模组独立的 Git（版本控制）远程仓库，上级远程属于其他项目，因此未上传到错误的 GitHub Releases（GitHub 发行版）。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用；Mojang Official Mappings（Mojang 官方命名映射）`26.2`。

## 2026-08-01 0.2.7 发电机燃料、即时供电与原生 Ctrl 交互修订

- 实际存档确认故障不是缺少车辆物品、编组、上方铁轨或出口方向；红石块已从发电机库存扣除，但 `RemainingFuelTicks（剩余燃料游戏刻）`仍为 0，导致信号铁轨断电并阻止车辆段发车。
- 发电机改用单一服务端原子激活入口：一次完成扣除 1 个燃料、写入红石粉 72,000 游戏刻或红石块 504,000 游戏刻、标记激活与保存、登记发电机并立即重建供电网络。
- 车辆段在读取供电状态前强制执行 `refreshNow（立即刷新）`，不再等待周期性缓存更新。
- `Ctrl（控制键）+右键`改由优先级 2000 的客户端 Mixin（混入）在 `Minecraft.startUseItem（开始使用物品）`入口处理；0.2.6 的“仅提前 Fabric 回调”方案经实际游戏证实失败，已记录为不得重复。
- 普通右键车辆段库存、上方铺轨、列车连接、编组间距、轨迹与整列运动代码没有修改。
- 八种语言 JSON（JavaScript 对象表示法）均可解析，各 174 个翻译键，键集合与参数占位符完全一致。
- JAR 内容与字节码检查通过：版本为 0.2.7，Mixin 配置存在，红石燃料常量为 72,000/504,000，发车读取供电前调用即时刷新，且不含测试探针。
- 当前受 BUG-030 的 Java ZipFS（Java 压缩文件系统）`toRealPath`权限限制，Gradle Wrapper（Gradle 包装器）无法联网下载，全局离线缓存也不可写；因此本轮使用项目已有的 Minecraft 26.2 命名 JAR和 0.2.6 正式包进行离线增量编译及覆盖。0.2.6 的 93 项完整测试是本次未修改模块的最近基线；本次新增完整 Gradle 回归仍需在正常终端补跑，不虚报为已执行。
- 普通 JAR（Java 归档）SHA-256：`1AFC484BA0E72CB6884AD453F601E01A877199A9128EA1487ADCDB167490887F`。
- sources JAR（源码 Java 归档）SHA-256：`709A93D250E062799064604F57B9994096F7D63028715E0DE093494EB898BD43`。
- 两个文件已保存到 `dist`和独立版本目录 `releases/0.2.7-generator-power-native-depot-ctrl`。
- 当前目录仍没有本模组独立的 Git（版本控制）远程仓库，上级远程属于另一项目，因此不上传到错误的 GitHub Releases（GitHub 发行版）。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用；Mojang Official Mappings（Mojang 官方命名映射）`26.2`。
- 权限恢复后已把普通 JAR 安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`；安装文件 SHA-256 与 `dist`一致，活动的模拟火车 JAR 只有 `0.2.7`一个。`0.2.6`已移入可恢复的 `mods-backup\fabric-train-simulator`目录。
- 用户取消关机后未执行任何关机命令；原自动关机脚本已改为只安装和校验的 `安装0.2.7.ps1`。
- Ctrl+右键、燃料持久化及立即发车仍需实际游戏验收。

## 2026-08-01 0.2.6 车辆段设置入口与立即发车诊断修订

- 修复 `Ctrl（控制键）+右键`设置入口的事件注册顺序：控制键与网络发送仍留在客户端，但由最早注册的车辆段通用交互回调直接委托，不再等待客户端初始化末尾的第二个回调。
- 普通右键继续打开 54 格车辆段库存，未按 Ctrl 时不消费普通交互；未修改列车连接、编组间距、轨迹缓存或整列运动控制器。
- “立即发车”由布尔结果改为明确结果：成功、空编组、上方无铁轨、未供电、车辆物品不足、方向无法确定、编组轨道/空间无效与实体生成失败均有独立翻译提示。
- 只读检查实际测试世界确认：车辆段模板为 1 节客车、车辆物品充足、上方铁轨和出口方向有效；相连发电机库存为空且剩余燃料为 0，故本次未生成列车的现场原因是信号铁轨未供电。供电规则保持红石粉 1 小时、红石块 7 小时。
- `gradlew clean build` 构建成功；93 项测试全部通过，失败 0、错误 0、跳过 0；八种语言键集合一致。
- 普通 JAR（Java 归档）SHA-256：`E6DA0A7E5775D7368D98EA954335E75C29288BACF400A8FA6CBC578919D10712`。
- sources JAR（源码 Java 归档）SHA-256：`B7C07029D2A6BF35C7A44D746496E360091315B27AE78CA496C8C27DAAB5809E`。
- 两个文件已保存到独立本地版本目录 `releases/0.2.6-depot-interaction-dispatch-diagnostics`；普通 JAR 已安装到 26.2 游戏 `mods`目录且哈希一致，0.2.5 已移到可恢复备份目录。
- 当前目录仍没有本模组独立的 Git（版本控制）远程仓库，上级远程属于另一项目，因此不上传到错误的 GitHub Releases（GitHub 发行版）。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用；Mojang Official Mappings（Mojang 官方命名映射）`26.2`。
- 实际整合包中的 Ctrl+右键与通电后立即发车仍需玩家进入游戏各验收一次。排查日志时另发现连接器配方的 `minecraft:chain`在 26.2 无效，已登记为 BUG-041，本轮未越权修改。

## 2026-08-01 0.2.5 车辆段控制模式、速度与独立卸货站修订

- 车辆段默认模板改为 0 节；模板至少有一节车厢时自动附带一个火车头，空模板点击立即发车只显示提示，不生成单独火车头。
- 车辆段设置入口改为 `Ctrl（控制键）+右键`；普通右键继续打开 54 格库存，`Shift（潜行键）+右键`不再抢占铺轨操作。
- 客车、货车、特快分别保存车辆段速度上限；实际速度取车辆段设定、线路限速与车型上限的最低值。含货车编组继续强制货车类别，清空全部货车后恢复其他类别。
- 新增独立 `ControlMode（控制模式）`：定时发车为线路自动列车，禁止 W/S 且计入线路在轨数量；立即发车为手动列车，即使未选线路也可生成，驻停等待玩家驾驶且不计入线路在轨数量。
- `DepotGenerated（车辆段来源）`与控制模式分别持久化；立即发出的手动列车仍能与其他手动车辆连接，不能与线路自动列车混编。
- 每节货车继续独立保存卸货站。手动列车和散装车辆可选择所有已登记车站；线路自动列车的客户端选项与服务端更新均限制在所属线路车站。新增独立目的站到达服务，使无线路手动列车和散装货车经过所选站点时也会瞬间卸货。
- 散装火车头的卸货站作为批量默认值：连接时已有独立目的站的货车保持不变，未设置货车继承火车头目的站；货车设置窗口会定位到其当前选择。
- 诊断名称显示“手动列车 #编号”或“线路自动列车 #编号”，并用于连接、输入与运动诊断。
- 未修改已稳定的整列轨迹、客户端位置同步、车辆跟随间距与 W/S 运动实现。
- `gradlew clean build` 构建成功；90 项测试全部通过，失败 0、错误 0、跳过 0；八种语言 JSON（JavaScript 对象表示法）均可解析，每种 166 个键且参数占位符一致。
- 普通 JAR（Java 归档）SHA-256：`F0469FD4275DD030F7DBFF05A6FED91A65B1AF5B12667885E95896AA37444A0F`。
- sources JAR（源码 Java 归档）SHA-256：`AAFD62FB44549D3AD3BE3EF783EC71916F716AE0EC217996B85A65DB5302DFC5`。
- 两个文件已保存到独立本地版本目录 `releases/0.2.5-depot-control-modes-cargo-destinations`；普通 JAR 已安装到 26.2 游戏 `mods`目录，安装哈希一致且只保留一个模拟火车 JAR。0.2.4 已移动到可恢复的 `mods-backup/fabric-train-simulator`目录。
- 当前目录仍没有本模组的独立 Git（版本控制）远程仓库，上级远程属于另一个项目，因此未错误上传到 GitHub Releases（GitHub 发行版）。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用；Mojang Official Mappings（Mojang 官方命名映射）`26.2`。
- `Ctrl（控制键）`交互、三种速度输入、定时/立即驾驶权限及多货车分站卸货仍需玩家进入 26.2 实际验收。

## 2026-08-01 0.2.4 道岔入口、高亮与实际转向修订

- 道岔新增永久的车辆段入口与放置回退方向：无车辆段时入口为玩家放置朝向的反方向；每秒沿四个实际铁轨分支扫描最多 64 格，后来连接车辆段后自动采用最近车辆段方向。
- 正常显示两格亮色：入口常亮，另一个亮格为当前左转、直行或右转出口；四个底色使用青色钻石色，删除白色海晶灯整条路线。
- 修正四个行驶方向的左/直/右 `RailShape`（铁轨形状）映射，真实弯轨现在同时包含来向入口和选定出口；手动列车继续读取同一映射。
- 未修改车辆连接、编组、客户端位置同步或整列运动实现；0.2.3 稳定 JAR（Java 归档）已另行备份。
- `gradlew clean build` 构建成功；79 项测试全部通过，失败 0、错误 0；11 个道岔 JSON（JavaScript 对象表示法）资源均能解析。
- 普通 JAR SHA-256：`91E941EA6259B784B3EEA24ECF77997900857592EB8980CCC436C769E5311084`。
- sources JAR（源码 Java 归档）SHA-256：`0005DB3673ABBAB56B0EE86981B9E177B36BC0FD097AEA953C3AD05B8BD623F7`。
- 两个文件已保存到独立本地版本目录 `releases/0.2.4-switch-entry-highlight-routing`；普通 JAR 已安装到游戏 `mods` 目录并验证哈希一致。
- 当前目录仍没有本模组的独立 Git（版本控制）远程仓库，上级远程属于另一个 `pathfinding-beacon` 项目，因此未错误上传到该 GitHub Releases（GitHub 发行版）。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用。
- 入口高亮、车辆段自动识别与手动列车真实转向仍需游戏验收。

## 2026-08-01 0.2.3 客户端位置同步修复版

- 0.2.2 游戏诊断确认服务端已移动整列，但三个车辆实体在客户端每刻撤销网络插值，导致画面残影、视觉间距错误及火车头互动失败。
- 客户端现在保留原版矿车网络插值；只有服务端会恢复受逻辑控制车辆在原版物理阶段产生的位移，随后由整列逻辑控制器统一推进。
- 连接排列完成后主动标记全部车辆同步位置；未修改速度、轨道游标、线路、道岔、卸货或模型。
- `gradlew clean build` 构建成功；72 项测试全部通过，失败 0、错误 0。
- 八种语言 JSON（JavaScript 对象表示法）均可解析且各有 156 个一致键。
- 普通 JAR（Java 归档）SHA-256：`03C049BCF4AF2EC797A9A7CEBAC73F7D522236C73FC5283F582578C501516755`。
- sources JAR（源码 Java 归档）SHA-256：`BEDD638FEACA2AC0A215C01B6FC4C9686AB4D3419A9F05570B6ABA5294E3898F`。
- 两个文件已保存到独立本地版本目录 `releases/0.2.3-client-position-sync`；普通 JAR 已安装到游戏 `mods` 目录并验证哈希一致，0.2.2 已移入可恢复的回退目录。
- 当前目录仍没有本模组的独立 Git（版本控制）远程仓库，上级远程属于另一个 `pathfinding-beacon` 项目，因此未错误上传到该 GitHub Releases（GitHub 发行版）。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用。
- 实体网络位置的最终视觉表现仍需游戏验收，验收前不把 BUG-035 标记为完全关闭。

## 2026-07-31 0.2.2 整列运行诊断版

- 本版本只增加连接、驾驶输入、控制权、火车头实体和轨道游标诊断，不把尚未确认的假设当作修复。
- `gradlew clean build` 构建成功；70 项测试全部通过，失败 0、错误 0。
- 八种语言 JSON（JavaScript 对象表示法）均可解析；每种 156 个键，其中诊断键 12 个，键集合一致。
- 普通 JAR（Java 归档）SHA-256：`2F922258C7D3956FA8899F79130A6C84A16542FD133CE2146A978D3A989DB542`。
- sources JAR（源码 Java 归档）SHA-256：`D445637F810D3E7FEA112C716F920DACCD5268616AE1CF2B86E24F62DC30A475`。
- 普通 JAR 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装文件与分发文件哈希一致；0.2.1 已移入 `mods-backup\fabric-train-simulator` 回退目录。
- 当前项目目录不是独立 Git（版本控制）仓库，检测到的上级远程仓库属于另一个 `pathfinding-beacon` 项目，因此未错误上传到该 GitHub Releases（GitHub 发行版）。
- 游戏内根因仍待使用 `[列车诊断]` 信息验收，不能标记为已修复。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用。

## 2026-07-31 0.2.1 连接间距、控制权与交互修订

- 仅修改列车连接与运动交互；未修改道岔、车站、车辆段、卸货、线路或模型。
- 连接车辆中心间距由 1.25 格统一改为 1.05 格，连接排列、重排和运行跟随使用同一常量。
- 玩家可坐在任意客车中；距离逻辑首车最近的乘客拥有 W/S 控制权。
- 连接器右键优先于货车/火车头设置入口；普通右键客车可乘坐，货车仍打开27格库存。
- 完整 `gradlew clean build` 成功；70 项自动测试通过，0 失败，0 错误；8 种语言键集一致。
- 普通 JAR SHA-256：`A2B6EC5A9DF1B47FCA79260D751AFFDAD355871B26705DA12DC5443449251CEE`；sources JAR SHA-256：`683C3FD92D43E7A1CCDBED066FA9733B5C2A5C1EC65702A9F439B84FAF7615DA`。
- 游戏 `mods` 中只保留 0.2.1，安装后哈希一致；0.2.0 已移入本地回退目录。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用。

## 2026-07-31 0.2.0 整列逻辑控制器

- 连接选择、16 格范围、编组顺序保持不变。
- 已连接车辆使用服务端唯一运动控制器，客户端通过 `SynchedEntityData`（同步实体数据）获得控制状态。
- 火车头或无车头编组的第一节客车作为首车，后续车辆按 1.25 格轨道距离定位。
- W 逐渐加速；S 先减速到零再倒车；已连接车辆不再执行原版独立推挤。
- 完整 `gradlew clean build` 成功；68 项自动测试通过，0 失败，0 错误；八种语言键集合一致。
- 0.2.0 普通 JAR（Java 归档）SHA-256：`930E0635ABA3E4AAB9E6B870DC6AAF90E9E0A52117A1529EACBA906485653CFB`；sources JAR（源码 Java 归档）SHA-256：`323E9737750D3EE059E452C164BE71C554C8529359DC836606AA5119C7FF9CD0`。普通 JAR 已安装到 26.2 游戏目录并验证哈希一致。
- 0.1.0 最后旧包保留在 `releases/0.1.0-last-pre-logical-controller`，SHA-256 为 `8E01C4A745B76F349E067D72C17554B4225DD121722267A005C1D2115A20B578`。
- Minecraft（我的世界）`26.2`；Java（爪哇）`25`；Fabric Loader（Fabric 模组加载器）`0.19.3`；Fabric API（Fabric 应用程序接口）`0.154.0+26.2`；Fabric Loom（Fabric 构建插件）`1.15.5`；Yarn Mappings（Yarn 命名映射）未使用。

## 构建与安装

- 干净构建：成功。
- 自动测试：36 项通过，0 失败，0 错误。
- 专用服务器：成功加载 Minecraft 26.2、Fabric Loader 0.19.3、Fabric API 0.154.0+26.2 和本模组；加载 1594 个配方后进入 `Done`（已就绪）并保存关闭。
- 客户端：成功加载资源图集，未发现本模组的模型、纹理或翻译资源错误。开发账号的 Mojang/Realms 401 不属于模组故障。
- 已安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`。
- 普通 JAR SHA-256：`C316A966500857E4A642F9AFF3EB5CCC9DEE9112EE4D74DF9FBC302104C1322F`。
- sources JAR SHA-256：`8D629BCC7EE1AA9DCEB5780C290D48204F365FE593ABE582A7F899EFF4FC2B9E`。

## 2026-07-21 反馈修订

- 道岔改为四向十字路口外观，去除误用的探测铁轨压力板纹理，增加左转/直行/右转设置。
- 信号铁轨保留原版式自动直行、转弯和缓坡连接；模组铁轨移除红石红色纹理，使用钻石青蓝标记。
- 车站由铁轨改为独立站牌方块，自动绑定水平相邻信号铁轨。
- 新增铁路发电机、9 格燃料库存、漏斗输入、红石粉有线连接和持久化供电时间。
- 新增空状态也可打开的铁路线路管理界面，可选择站点、道岔、车辆段和线路。
- 新设备默认名称改为分类独立递增的“站点1/2/3”、“道岔1/2/3”和“车辆段1/2/3”，旧英文默认名自动迁移。
- 车辆实体与物品图标按用户参考图重做；imagegen（图像生成/编辑）生成的正交建模参考保存于 `docs/assets/train-vehicle-orthographic-reference.png`。

## 已知边界

- 当前已完成自动测试、客户端资源加载和专用服务器启动验证；完整铺轨、实际乘坐、多人争用和跨区块长时间游戏流程仍需玩家进入真实世界按回归清单验收。
- 离线目的地采用事务性货运账本保存物品，到 ETA 后等区块加载再写入真实容器；当前不保存容器“最后一次容量快照”，最终以加载时真实剩余空间为准，未容纳物品继续留在账本。
- 货车的 27 格库存与目的站设置为两个交互：普通右击库存，潜行或持设置器右击设置。
- 当前项目不是 Git 仓库，没有 GitHub 远程，因此未执行 GitHub Releases（GitHub 发行版）上传。

## 2026-07-29 连接器编组修订

- 删除碰撞自动连接，新增“列车连接器”两次右击连接、选择闪烁、16 格范围、同车断开、编组排序和解散整列。
- 车辆段按“通往车站或道岔的一侧”为前方生成列车，生成后的火车头和车厢立即属于同一正式编组。
- 新增连接距离、最大编组和车辆段方向规则测试；离线规则探针返回 `CONNECTOR_DEPOT_RULES_OK`。
- 八种语言文件均为 135 个翻译键，键集合一致；新物品、模型、配方和模组入口 JSON（JavaScript 对象表示法）已解析通过。
- 普通 JAR SHA-256：`3E02D9619B6F9BA7E625F9F368B9BFF95E4CC0DA0B83F393D0656046E2977A6E`。
- sources JAR SHA-256：`B07CFEC97540DD1ADFABE9FA04C16E81E37A43660107AA074AB0314FCB14A0A1`。
- 当前受限环境仍禁止 Java `toRealPath`，因此本次采用离线增量编译，未执行完整 Loom（Fabric 构建插件）回归；详见 `BUG-030`。
- 当时的受限环境曾拒绝写入游戏目录；该限制现已解除，最新安装状态以下一节为准。

## 2026-07-29 左键连接与道岔高亮修订

- 连接器改用左键操作；手持时禁止破坏方块并取消车辆攻击伤害。
- 同车连续选择只取消操作；同编组相邻两车可切断连接，非相邻车辆明确拒绝。
- 连接器改用模组自带的代码模型，不再引用缺失的原版链条贴图。
- 道岔当前真实通行路径使用亮色覆盖层高亮；设置窗口和红石脉冲会同步改变外观及轨道形状。
- 没有绑定线路的手动列车在道岔前按当前设置转向；自动列车仍按线路节点保存方向。
- 完整 `gradlew clean build` 成功；59 项测试通过，0 失败，0 错误。
- 八种语言均为 136 个翻译键；本次 9 个资源 JSON 均解析成功。
- 普通 JAR SHA-256：`2066C151A6E1A4FB92CE622CC0CFDB319EA9C6836CFA9336D2669904CF577B3A`。
- sources JAR SHA-256：`8A306CC9DBF3F92866B5931A9176EA13112A281E37779F5D0E26D005A10C6FF8`。
- 普通 JAR 已覆盖安装到 `G:\game\mcfabric\.minecraft\versions\26.2\mods`，安装后哈希一致。

## 2026-07-30 列车登记、物理编组与道岔独立状态

- 增加永久列车登记表、不可重复数字编号、车辆段来源和线路记录。
- 断开时火车头段继承原编号，尾段生成新编号；车辆段来源不会因断开而丢失。
- 禁止车辆段列车手动驾驶，也禁止手工列车与车辆段列车合并。
- 连接后统一清零速度、重置轨迹并按 1.25 格间距排列。
- 道岔高亮改用独立 `switch_choice` 状态；手动列车提前锁定实际形状，无有效出口时停车。
- 修复手持物品潜行右击车站、道岔和车辆段不稳定的问题。
- 完整 `gradlew clean build` 成功；62 项测试通过。
- 普通 JAR SHA-256：`A15A72EF30521E8D9F934A2D339F888B13C9AB1FA720E1CEE5F3D44F55D2F12B`。
- sources JAR SHA-256：`9D53496DC0E1288CA0FA24CB3B37F54AC558AD6868E4A92B2DA39174EAA40477`。
