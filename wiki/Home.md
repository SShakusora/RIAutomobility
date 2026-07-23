# RIAutomobility Wiki

RIAutomobility 是 Minecraft 1.20.1 上的 Automobility Forge 附属模组。它提供内置车辆
部件、多座位与自定义碰撞箱、车辆钥匙、Blockbench 模型与动画支持，以及在游戏内制作、
预览和分享自定义车架、车轮、引擎的车辆导入台。

## 文档导航

- [车辆导入台：制作、预览与导出](Vehicle-Import-Table.md)
- [车辆钥匙与多座位](Vehicle-Keys-and-Multi-Seat.md)
- [Blockbench 动画与 Molang](Blockbench-Animation-Molang.md)
- [OBB 车辆交互盒](Vehicle-Interaction-Boxes.md)
- [车包存储与多服务器同步](Car-Pack-Storage-and-Synchronization.md)
- [项目 README 与安装说明](../README.md)

## 兼容环境

| 项目 | 当前代码要求 |
| --- | --- |
| Minecraft | `1.20.1` |
| Forge | `47.x`，开发环境使用 `47.1.3` |
| Automobility | `0.4.2+1.20.1-forge` |
| Blockbench 工程格式 | `4.10` 至 `5.0` |

客户端和服务端都需要安装 RIAutomobility 及其依赖。自定义组件由服务器确定并同步给
在线客户端，客户端无需预先手动复制服务器正在使用的 `.riauto` 文件。

## 最短制作流程

1. 在 Blockbench 中制作车架、车轮或引擎，并将所有贴图以内嵌 PNG 保存到 `.bbmodel`。
2. 合成并打开车辆导入台，选择目标部件页后导入 `.bbmodel`。
3. 调整参数并检查三维预览；车架还可以配置轮位、座位、碰撞箱、交互盒和附件。
4. 选择“导出文件”生成可分享的 `.riauto`，或由服务器管理员选择“导出物品”直接安装
   组件并取得对应物品。
5. 其他玩家可以在车辆导入台中重新导入 `.riauto`，检查内容后导出或安装。

`.riauto` 是车辆导入台生成并校验的交换格式。当前工作流不支持手写组件 JSON、
手工拼装 RIAuto 归档或直接把任意资源包当作车辆组件。

## 玩家与管理员功能

| 操作 | 普通玩家 | 权限等级 2（管理员） |
| --- | :---: | :---: |
| 导入 `.bbmodel` 或 `.riauto` 并预览 | ✓ | ✓ |
| 导出本地 `.riauto` 文件 | ✓ | ✓ |
| 将组件发布到当前服务器并导出物品 | — | ✓ |
| 配置共享车包目录 | — | 需要修改服务器配置 |

导出文件不会修改源 `.bbmodel`。从原始 `.bbmodel` 首次导出时，当前 Minecraft 玩家名
会成为作者；重新导入已有 `.riauto` 后再次导出，则保留归档中已有的作者。

## 运行时目录概览

默认情况下，模组在游戏目录下使用 `riautomobility/`：

```text
<游戏目录>/riautomobility/
├─ *.riauto             # 默认的服务器权威车包
├─ exports/             # 导出文件对话框的默认目录
└─ cache/               # 下载、上传、传输和编辑器临时文件
```

专用服务器可通过 `config/riautomobility-common.toml` 把权威 `.riauto` 目录指向一个
共享位置；缓存仍保留在各实例自己的游戏目录。详见
[车包存储与多服务器同步](Car-Pack-Storage-and-Synchronization.md)。
