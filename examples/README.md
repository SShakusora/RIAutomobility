# ExamplePack

English | [中文](#中文)

## English

This folder contains a complete minimal unified car pack for adding custom `Frame` and `Wheel` components to `RIAutomobility`.

### Contents

- `examplepack/`: one unpacked development car pack containing `riauto.json`, `pack.mcmeta`, `data/`, and `assets/`

### Install

1. ZIP the contents of `examplepack` (without an extra enclosing directory), rename the archive to `examplepack.riauto`, and copy it into the game directory's `riautomobility/` folder.
2. Start the game, or run `/riautomobility carpacks reload` while a world/server is running.

RIAutomobility automatically enables both the data and resource sides of every direct child pack. `.riauto` is the supported distribution format; legacy folder and `.zip` packs remain readable for migration. Move a pack into `riautomobility/disabled/` to disable it.

### What It Adds

- `examplepack:example_buggy` frame
- `examplepack:example_buggy` wheel
- `examplepack:example_buggy_gecko` frame
- `examplepack:example_buggy_gecko` wheel
- `examplepack:example_buggy_bbmodel` frame
- `examplepack:example_buggy_bbmodel` wheel

All six components will appear in the `RIAutomobility: Custom` creative tab and can also be crafted in the Auto Mechanic Table using the included recipe JSON files.

### Important Paths

Component definitions:

- `data/examplepack/riautomobility/frames/example_buggy.json`
- `data/examplepack/riautomobility/frames/example_buggy_gecko.json`
- `data/examplepack/riautomobility/wheels/example_buggy.json`
- `data/examplepack/riautomobility/wheels/example_buggy_gecko.json`
- `data/examplepack/riautomobility/frames/example_buggy_bbmodel.json`
- `data/examplepack/riautomobility/wheels/example_buggy_bbmodel.json`

Car-pack models:

- `assets/examplepack/models/entity/automobile/frame/example_buggy/main.json`
- `assets/examplepack/models/entity/automobile/wheel/example_buggy/main.json`
- `assets/examplepack/geo/frame/example_buggy.geo.json`
- `assets/examplepack/geo/wheel/example_buggy.geo.json`
- `assets/examplepack/models/entity/automobile/frame/example_buggy.bbmodel`
- `assets/examplepack/models/entity/automobile/wheel/example_buggy.bbmodel`

Translations:

- `assets/examplepack/lang/en_us.json`

### Notes

- This example reuses Automobility's built-in textures, so no PNG files are required.
- `model.texture` in the component definition points to an existing texture resource.
- `model.layer_location` controls which JSON model file is baked.
- `model.model_id` is the runtime model id used by Automobility item and entity rendering.
- The `example_buggy` pair uses `JsonEM`.
- The `example_buggy_gecko` pair uses `GeckoLib` with `geo_model` and `animation` fields.
- The `example_buggy_bbmodel` pair loads native Blockbench project files. Its frame demonstrates groups, cubes, a mesh, multiple textures, and a looping animation.
- BBModel component definitions use the one-line `model` resource shorthand; textures and the default animation come from the project file.
- `example_buggy_gecko` also sets `hide_engine: true`, so the vehicle keeps its real engine logic but renders the built-in `AutomobileEngine.EMPTY` model.
- If a client is missing the car pack resources, the components will render with a barrier-texture placeholder instead of crashing the game.
- In that case, tooltips will show a missing car-pack resource warning.
- The `JsonEM`, `GeckoLib`, and `BBModel` examples are loaded automatically. Manual resource-pack selection and `F3 + T` are not required.
- On a dedicated server, install car packs only in the server `riautomobility/` folder. Joining clients automatically download missing or changed packs, verify their SHA-256 digests, and cache them under `riautomobility/cache/packs/`.
- A matching manually installed client pack is reused without downloading. While connected, the server's pack list is authoritative; client-only packs are not enabled for that server.
- Network transfers use 256 KiB chunks, have size and archive-safety limits, and disconnect the client if a required pack cannot be verified or loaded.

### Creating Your Own Variant

1. Change the ids from `example_buggy` to your own name.
2. Update recipe result `component` ids to match.
3. Replace `layer_location`, `model_id`, and `texture` with your own assets.
4. If you add your own textures, place them in the same car pack under `assets/<namespace>/textures/...`.

---

## 中文

这个目录提供了一套完整的统一车包最小示例，用于为 `RIAutomobility` 添加自定义 `Frame` 和 `Wheel` 组件。

### 内容

- `examplepack/`：同时包含 `riauto.json`、`pack.mcmeta`、`data/` 和 `assets/` 的开发态车包目录

### 安装方法

1. 将 `examplepack` 内的内容直接压缩（不要额外套一层目录），把归档改名为 `examplepack.riauto`，再复制到游戏目录的 `riautomobility/` 文件夹。
2. 启动游戏，或在世界/服务器运行时执行 `/riautomobility carpacks reload`。

RIAutomobility 会自动启用每个直接子车包的数据和资源部分。`.riauto` 是正式分发格式；旧文件夹和 `.zip` 在迁移期仍可读取。要停用车包，将其移入 `riautomobility/disabled/`。

### 添加了什么

- `examplepack:example_buggy` 车架
- `examplepack:example_buggy` 车轮
- `examplepack:example_buggy_gecko` 车架
- `examplepack:example_buggy_gecko` 车轮
- `examplepack:example_buggy_bbmodel` 车架
- `examplepack:example_buggy_bbmodel` 车轮

这六个组件都会显示在 `RIAutomobility: Custom` 创造模式标签页中，也可以通过附带的 Auto Mechanic Table 配方进行制作。

### 重要路径

组件定义：

- `data/examplepack/riautomobility/frames/example_buggy.json`
- `data/examplepack/riautomobility/frames/example_buggy_gecko.json`
- `data/examplepack/riautomobility/wheels/example_buggy.json`
- `data/examplepack/riautomobility/wheels/example_buggy_gecko.json`
- `data/examplepack/riautomobility/frames/example_buggy_bbmodel.json`
- `data/examplepack/riautomobility/wheels/example_buggy_bbmodel.json`

车包模型：

- `assets/examplepack/models/entity/automobile/frame/example_buggy/main.json`
- `assets/examplepack/models/entity/automobile/wheel/example_buggy/main.json`
- `assets/examplepack/geo/frame/example_buggy.geo.json`
- `assets/examplepack/geo/wheel/example_buggy.geo.json`
- `assets/examplepack/models/entity/automobile/frame/example_buggy.bbmodel`
- `assets/examplepack/models/entity/automobile/wheel/example_buggy.bbmodel`

翻译文件：

- `assets/examplepack/lang/en_us.json`

### 说明

- 这套示例复用了 Automobility 自带贴图，因此不需要额外 PNG。
- 组件定义中的 `model.texture` 指向已有的贴图资源。
- `model.layer_location` 用于指定要烘焙的 JSON 模型文件。
- `model.model_id` 是 Automobility 物品与实体渲染时使用的运行时模型 id。
- `example_buggy` 这一对组件使用 `JsonEM`。
- `example_buggy_gecko` 这一对组件使用 `GeckoLib`，并通过 `geo_model` 与 `animation` 字段引用资源。
- `example_buggy_bbmodel` 直接加载 Blockbench 工程；其中车架展示了 Group、Cube、Mesh、多贴图和循环动画。
- BBModel 组件使用单行 `model` 资源简写，贴图与默认动画直接来自工程文件。
- `example_buggy_gecko` 还设置了 `hide_engine: true`，因此载具仍然保留真实引擎逻辑，但渲染时会改用内置的 `AutomobileEngine.EMPTY` 模型。
- 如果客户端缺少车包资源，组件会显示为屏障贴图占位，而不会导致游戏崩溃。
- 此时 tooltip 会提示缺少车包资源。
- `JsonEM`、`GeckoLib` 和 `BBModel` 示例都会自动加载，无需手动选择资源或按 `F3 + T`。
- 专用服务器只需把车包安装到服务端的 `riautomobility/` 目录。客户端加入时会自动下载缺失或已更新的车包，校验 SHA-256，并缓存到 `riautomobility/cache/packs/`。
- 如果客户端手工安装的车包内容完全一致，会直接复用而不下载。连接服务器期间以服务端车包清单为准，不会启用仅客户端存在的车包。
- 网络传输使用 256 KiB 分块，并限制压缩包大小、文件数量与解压后大小；必需车包无法通过校验或加载时会断开连接。

### 创建你自己的版本

1. 将 `example_buggy` 改成你自己的组件名称。
2. 同步修改配方结果里的 `component` id。
3. 将 `layer_location`、`model_id` 和 `texture` 替换成你自己的资源路径。
4. 如果你添加了自己的贴图，请放在同一车包的 `assets/<namespace>/textures/...` 路径下。
