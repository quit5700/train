# 当前列车项目适配补充（test8）

只记录当前Git根目录本次十版的接口和验证，不引用其他项目的缺陷。

网络注册必须沿用各版同文件已有入口：老版本使用本项目Legacy兼容层；1.21系列Fabric API用playC2S，26.x实际版本用serverboundPlay。不得只凭较新版本接口直接套用所有版本。

Player事件回调中的对象为Player，不能调用仅ServerPlayer存在的serverLevel；直接使用回调传入并已验证的服务端Level。AccessService中的ServerPlayer维度读取沿用各版本原有serverLevel/level形式。

旧版LocalPlayer提示用displayClientMessage，新版按该版已存在的消息接口。26.2/26.3当前界面读取gui.screen()，更早版本保持screen字段。

neighborChanged签名复用同版本PowerGeneratorBlock：旧版public并有sourcePos，新版protected并有Orientation。新增货运容器监听只复用签名，不覆盖原供电逻辑。

服务器诊断玩家不具备网络连接，startRiding双参数在较新版本已变为三参数；最终诊断在通用单参数调用处处理关系已完成后的网络通知，不覆盖错误签名。生产代码不捕获此错误。

实体查询、强制区块票据与ENTITY_LOAD生命周期异步，不在addFreshEntity的同一个回调内检查最终空间索引。临时服务器采用真实生命周期、有限等待和精确库存/掉落计数。用户世界不添加诊断票据。

回收队列不得把重启后的LEGACY_UNCONFIRMED当成DESTROYED。实际保存重载检查覆盖该边界。更多根因、失败尝试与验证见docs/bugs/BUG-090.md。

1.20.1/1.20.4的Loom 1.6生成refmap并不等于混入配置已引用；运行JAR必须同时校验配置、文件及startUseItem→method_1583映射。1.21系列则直接重映射注解，26系列保留原生命名，不强加旧版refmap。生产启动测试需校验加载清单含目标火车和快捷建筑。Windows Java21启动参数文件同样实测按原生代码页读取，统一GetACP处理，JVM内文本仍UTF-8；不可重用已失败的UTF-8参数文件方案。

归档补充：Git空白检查对复制的新版本目录报告137处历史格式；逐项与test7核对（行尾先按Git规则归一化），均为原末尾空行或字体许可证原文尾随空格。保留已验证源码字节与许可原文，未因此批量格式化或重写运行包。

## test10诊断与生成兼容补充

ChunkPos在26.3是记录类型，不能共用旧版公开x/z字段；跨版本诊断从BlockPos坐标右移4位取得区块坐标。旧版仅添加PLAYER加载票据不能代表起点方块实体持续更新，曾出现发电机最后更新停在300刻的无效供电场景。26.3使用PLAYER_LOADING与PLAYER_SIMULATION；旧版临时世界在起点及独立生成测试区临时开启原生更新票据，700刻后检查真实燃料与active状态，结束前撤销，不向用户世界复制票据数据。

生成测试区在初始化时提前加载并等待进入世界实体索引，再执行重复生成检查；新加载区同刻addFreshEntity后立即查询可能不完整。已进入跟踪状态的测试区可验证重复生成拒绝，不能将无效测试区的结果当作版本缺陷。生产生成额外显式检查可交互实体/列车包围盒，保留方块碰撞与旧枚举序号。主动供电刷新放在路径和接入段扫描后；26.3 test9误报已实际复现，旧版同步防护但不宣称同一缺陷已逐版复现。详见BUG-093/094。

补充：1.21.8存在与新版本同名票据但不能只凭字段名认定已持续模拟方块实体；active字段在停止更新后仍可能保持true。诊断必须核对lastObservedGameTime与当前时间相差不超过1刻。最终发车专项统一在独立世界预加载生成区、临时维持起点/测试区更新并撤销，报告同时绑定诊断脚本摘要，避免使用旧诊断假阳性。模组运行逻辑仍依赖正常玩家加载，不加入这些测试票据。

## 0.2.42-test11：自动货运自然卸载与持久化接口

本节只对应本仓库BUG-095。独立临时世界真实生成列车、自然卸载、移走起点观察范围、真实停服重启及重新实体化为主验证，直接构造OfflineTrainState（离线状态）只能保留为旧逻辑回归，不能替代交接实测。生产代码不添加永久强制加载。

- 1.20.1：SavedData（保存数据）沿用DATA_ID、load/save(CompoundTag)与三参数computeIfAbsent；1.20.4：SavedData.Factory与单参CompoundTag；1.21.1/1.21.4：Factory与HolderLookup.Provider（注册表查询提供器）；1.21.8/1.21.10/1.21.11：SavedDataType首参是String（字符串）；26.x首参使用Identifier（标识符）。以该版已有OfflineTrainSavedData实现为基线，不覆盖已适配的保存入口。
- 1.20.1/1.20.4的容器getItems（取得物品列表）声明在RandomizableContainerBlockEntity，其余支持版使用BaseContainerBlockEntity。旧版独立保留fabric-train-simulator-refmap.json（公共引用映射）和client-fabric-train-simulator-refmap.json（客户端引用映射），必须同时核验公共getItems与原有startUseItem的生产命名。
- 26.1.2/26.2/26.3的ServerChunkEvents.CHUNK_LOAD（区块加载事件）回调是三参数，含isNew；其余本轮支持版是双参数。
- 已尝试失败：在CHUNK_LOAD回调中调用Container.setItem/setChanged，会重新等待尚未完成的同一区块，触发看门狗。加载时只通过受限访问器原地替换已拥有的列表内容，正常游戏刻再绑定与恢复燃料状态。
- 已尝试失败：在卸载回调中通过LoadedRailWorld读取方块种类，区块已被移出加载表时读到air，导致权威库存匹配失败。使用方块实体自身BlockState，且只接受当前绑定物理实体的单次卸载提交。SERVER_STOPPING（服务器停止前）统一捕获后，迟到卸载回调不再覆盖后台结算。
- 已尝试失败：用Map.toString直接生成线路签名，Map.copyOf在不同Java进程中顺序不稳定。按节点索引TreeMap排序后比较；新增规范顺序回归及三道岔真实货运重启场景。
- 原实体被卸载不代表列车已经到达终点。test11行车回归等待真实库存回收，同时确认自动配额与离线状态清理，保留准确的车头与车厢数量要求，不降低预期。

失败原始证据保存在docs/bugs/evidence/BUG-095中；最终每版验证结果仍以交付说明及对应源码指纹为准，不能以某一版编译或服务器通过代替其余版本。
