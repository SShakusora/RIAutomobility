# RIAutomobility

**A powerful addon for Automobility — More vehicles, more freedom, more fun.**

---

English | [中文](#中文)

---

## English

### Introduction

RIAutomobility is a Forge addon for [Automobility](https://www.curseforge.com/minecraft/mc-mods/automobility), bringing a rich new vehicle experience to Minecraft 1.20.1.

Whether you want to drive unique automobiles in survival mode, or you are a map / modpack author looking to create custom vehicles for your players, this mod has you covered.

---

### Features

#### Ready-to-Drive New Vehicles

The mod includes a variety of carefully designed built-in vehicle components, available directly in the creative inventory or craftable at the Auto Mechanic Table:

- **Double Motorcar Series** — Wooden, Copper, Steel, Golden, Bejeweled (5 variants)
- **Quad Motorcar Series** — Wooden, Copper, Steel, Golden, Bejeweled (5 variants)
- **Special Vehicles**
  - **Lorry** — Large truck with container storage
  - **DMC12** — Classic sports car styling
  - **Standard Formula** — Formula racing style
- **Matching Wheels** — Custom wheels for DMC12 and Standard Formula

#### Multi-Seat Vehicle System

RIAutomobility adds true multi-seat support. Invite your friends for a ride, or switch seats while driving for a different travel experience.

#### Single-Component RIAuto Files

The most powerful feature of this mod: **create your own vehicles using only JSON — no coding required!**

- Define one new frame, wheel, or engine in each RIAuto file's `data/` directory
- Freely adjust vehicle stats: weight, dimensions, grip, seat layout, hitboxes, camera position
- Ship that component's models and textures in the same file's `assets/` directory
- Combine independently installed component files when assembling a vehicle
- Optional: add Auto Mechanic Table recipes for survival-mode crafting

#### Three Rendering Engine Support

Whether you are a modeling beginner or veteran, RIAutomobility has the right tools for you:

- **JsonEM** — Simple JSON entity models, great for getting started quickly
- **GeckoLib** — Powerful animated models with complex animation support
- **BBModel** — Direct Blockbench project loading with cubes, meshes, multiple textures, and animation

#### Safe & Stable Resource Handling

- Missing resources won't crash the game. A placeholder model is shown with a friendly tooltip warning.
- Custom models are automatically loaded after entering a world — no manual refresh needed.

---

### Dependencies

| Mod | Minimum Version | Note |
|------|----------------|------|
| [Forge](https://files.minecraftforge.net/) | `47.1.x` | Mod loader |
| [Automobility](https://www.curseforge.com/minecraft/mc-mods/automobility) | `0.4.2+1.20.1-forge` | **Required dependency** |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | `4.x` | **Required dependency** |

---

### Installation

1. Install Forge for Minecraft 1.20.1
2. Place the following jars into your `mods` folder:
   - `automobility-0.4.2+1.20.1-forge.jar`
   - `geckolib-forge-1.20.1-4.x.x.jar`
   - `riautomobility-1.0.4.jar`
3. Launch the game. New vehicle components can be found in the **RIAutomobility** creative tab.

---

### Screenshots

<div align="center">
  <img src="ScreenShots/1.png" alt="RIAutomobility Vehicles" width="800">
  <br>
  <em>Built-in vehicles: DMC12, Standard Formula, Quad Motorcar, and Lorry</em>
</div>

---

### For Content Authors

Want to add exclusive vehicles for your modpack or server? RIAutomobility makes it simple:

1. Write one frame, wheel, or engine definition under the component source's `data/` directory
2. Put that component's models and textures under its `assets/` directory; the first BBModel export records the exporting Minecraft player as author, while later RIAuto and item exports preserve that informational attribution
3. Add a format-2 `riauto.json` manifest that declares exactly that one component; BBModel PNG textures are stored once as external assets
4. ZIP the contents, rename the archive to `.riauto`, and place it in the game directory's `riautomobility/` folder

For detailed JSON format documentation, field explanations, and a complete example, please refer to the [README](https://github.com/your-username/RIAutomobility#car-pack-guide) in the GitHub repository.

---

### Open Source & License

This project is open-sourced under the **MIT License**.

- GitHub Repository: [Click to visit](https://github.com/your-username/RIAutomobility)
- Found a bug? Feel free to report it on GitHub Issues

---

*Developed and maintained by Shinonome Shakusora.*

---

## 中文

### 简介

RIAutomobility 是 [Automobility](https://www.curseforge.com/minecraft/mc-mods/automobility) 的 Forge 附属模组，为 Minecraft 1.20.1 带来了丰富的全新载具体验。

无论你是想在生存模式中驾驶独一无二的汽车，还是作为地图/整合包作者为玩家定制专属载具，这个模组都能满足你的需求。

---

### 特性一览

#### 即开即玩的新车

模组内置了多种精心设计的车辆部件，可直接在创造模式物品栏中取用或在汽车修理台中合成：

- **双座汽车系列** — 木质、铜质、钢质、黄金、璀璨，共 5 款
- **四座汽车系列** — 木质、铜质、钢质、黄金、璀璨，共 5 款
- **特殊载具**
  - **大运** — 大型货车，带集装箱存储功能
  - **DMC12** — 经典跑车造型
  - **标准方程式** — 方程式赛车风格
- **配套车轮** — DMC12 和标准方程式专用车轮

#### 多座位载具系统

RIAutomobility 为车辆添加了真正的多座位支持。你可以邀请好友一起乘车，甚至可以在行驶中切换座位，享受不一样的旅途体验。

#### 单组件 RIAuto 文件

这是本模组最强大的功能：**无需编写任何代码，仅用 JSON 就能创造属于自己的车辆！**

- 在每个 RIAuto 文件的 `data/` 目录只定义一个全新车架、车轮或引擎
- 自由调整车辆属性：重量、尺寸、抓地力、座位布局、碰撞箱、摄像机位置
- 在同一文件的 `assets/` 目录附带该组件的模型和贴图
- 组装车辆时组合多个独立安装的组件文件
- 自定义配方，让车辆在生存模式中可合成

#### 三种渲染引擎支持

无论你是模型制作新手还是老手，RIAutomobility 都为你准备好了工具：

- **JsonEM** — 使用简单的 JSON 实体模型，适合快速上手
- **GeckoLib** — 使用功能强大的动画模型，支持复杂动画效果
- **BBModel** — 直接加载 Blockbench 工程，支持方块、网格、多贴图与动画

#### 安全稳定的资源处理

- 资源缺失时不会崩溃，而是显示占位模型并在 Tooltip 中友好提示
- 进入世界后自动加载自定义模型，无需手动刷新

---

### 依赖模组

| 模组 | 最低版本 | 说明 |
|------|----------|------|
| [Forge](https://files.minecraftforge.net/) | `47.1.x` | 模组加载器 |
| [Automobility](https://www.curseforge.com/minecraft/mc-mods/automobility) | `0.4.2+1.20.1-forge` | **前置模组，必须安装** |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | `4.x` | **前置模组，必须安装** |

---

### 安装方法

1. 安装 Forge 1.20.1
2. 下载并放入 `mods` 文件夹：
   - `automobility-0.4.2+1.20.1-forge.jar`
   - `geckolib-forge-1.20.1-4.x.x.jar`
   - `riautomobility-1.0.4.jar`
3. 启动游戏，即可在创造模式物品栏的「飞天奇匠」标签页中找到新车辆部件

---

### 截图

<div align="center">
  <img src="ScreenShots/1.png" alt="RIAutomobility 载具展示" width="800">
  <br>
  <em>内置载具：DMC12、标准方程式、四座汽车和大运</em>
</div>

---

### 给内容作者

想要为自己的整合包或服务器添加专属载具？RIAutomobility 让这一切变得简单：

1. 在组件源码的 `data/` 目录中编写一个车架、车轮或引擎定义
2. 在其 `assets/` 目录中放入该组件的模型和贴图文件；BBModel 首次导出时记录当前 Minecraft 玩家，后续 RIAuto 和物品导出保留这项署名信息
3. 添加只声明这一个组件的格式 2 `riauto.json` 清单；BBModel PNG 纹理只作为外置资源保存一次
4. 压缩目录内容并将归档改名为 `.riauto`，再放入游戏目录的 `riautomobility/` 文件夹

详细的 JSON 格式说明、字段解释和完整示例，请参阅 GitHub 仓库中的 [README](https://github.com/your-username/RIAutomobility#车包教程)。

---

### 开源与授权

本项目采用 **MIT License** 开源。

- GitHub 仓库：[点击访问](https://github.com/your-username/RIAutomobility)
- 发现问题？欢迎在 GitHub Issues 中反馈

---

*由 Shinonome Shakusora 开发与维护。*
