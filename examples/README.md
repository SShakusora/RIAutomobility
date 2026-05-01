# ExamplePack

English | [中文](#中文)

## English

This folder contains a complete minimal example for adding custom `Frame` and `Wheel` components to `RIAutomobility`.

### Contents

- `examplepack-data/`: datapack files
- `examplepack-resources/`: resource pack files

### Install

1. Copy `examplepack-data` into your world `datapacks/` folder.
2. Copy `examplepack-resources` into your Minecraft `resourcepacks/` folder.
3. Enable the resource pack in-game.
4. Run `/reload`.

### What It Adds

- `examplepack:example_buggy` frame
- `examplepack:example_buggy` wheel
- `examplepack:example_buggy_gecko` frame
- `examplepack:example_buggy_gecko` wheel

All four components will appear in the `RIAutomobility: Custom` creative tab and can also be crafted in the Auto Mechanic Table using the included recipe JSON files.

### Important Paths

Datapack component definitions:

- `data/examplepack/riautomobility/frames/example_buggy.json`
- `data/examplepack/riautomobility/frames/example_buggy_gecko.json`
- `data/examplepack/riautomobility/wheels/example_buggy.json`
- `data/examplepack/riautomobility/wheels/example_buggy_gecko.json`

Resource pack models:

- `assets/examplepack/models/entity/automobile/frame/example_buggy/main.json`
- `assets/examplepack/models/entity/automobile/wheel/example_buggy/main.json`
- `assets/examplepack/geo/frame/example_buggy.geo.json`
- `assets/examplepack/geo/wheel/example_buggy.geo.json`

Translations:

- `assets/examplepack/lang/en_us.json`

### Notes

- This example reuses Automobility's built-in textures, so no PNG files are required.
- `model.texture` in the datapack points to an existing texture resource.
- `model.layer_location` controls which JSON model file is baked.
- `model.model_id` is the runtime model id used by Automobility item and entity rendering.
- The `example_buggy` pair uses `JsonEM`.
- The `example_buggy_gecko` pair uses `GeckoLib` with `geo_model` and `animation` fields.
- If the resource pack is missing, the components will render with a barrier-texture placeholder instead of crashing the game.
- In that case, tooltips will show a missing resource pack warning.
- When the resource pack is enabled, both `JsonEM` and `GeckoLib` examples should apply automatically after joining the world. Manual `F3 + T` is normally not required.

### Creating Your Own Variant

1. Change the ids from `example_buggy` to your own name.
2. Update recipe result `component` ids to match.
3. Replace `layer_location`, `model_id`, and `texture` with your own assets.
4. If you add your own textures, place them in your resource pack under `assets/<namespace>/textures/...`.

---

## 中文

这个目录提供了一套完整的最小示例，用于为 `RIAutomobility` 添加自定义 `Frame` 和 `Wheel` 组件。

### 内容

- `examplepack-data/`：数据包文件
- `examplepack-resources/`：资源包文件

### 安装方法

1. 将 `examplepack-data` 复制到存档的 `datapacks/` 文件夹。
2. 将 `examplepack-resources` 复制到 Minecraft 的 `resourcepacks/` 文件夹。
3. 在游戏中启用资源包。
4. 执行 `/reload`。

### 添加了什么

- `examplepack:example_buggy` 车架
- `examplepack:example_buggy` 车轮
- `examplepack:example_buggy_gecko` 车架
- `examplepack:example_buggy_gecko` 车轮

这四个组件都会显示在 `RIAutomobility: Custom` 创造模式标签页中，也可以通过附带的 Auto Mechanic Table 配方进行制作。

### 重要路径

数据包组件定义：

- `data/examplepack/riautomobility/frames/example_buggy.json`
- `data/examplepack/riautomobility/frames/example_buggy_gecko.json`
- `data/examplepack/riautomobility/wheels/example_buggy.json`
- `data/examplepack/riautomobility/wheels/example_buggy_gecko.json`

资源包模型：

- `assets/examplepack/models/entity/automobile/frame/example_buggy/main.json`
- `assets/examplepack/models/entity/automobile/wheel/example_buggy/main.json`
- `assets/examplepack/geo/frame/example_buggy.geo.json`
- `assets/examplepack/geo/wheel/example_buggy.geo.json`

翻译文件：

- `assets/examplepack/lang/en_us.json`

### 说明

- 这套示例复用了 Automobility 自带贴图，因此不需要额外 PNG。
- 数据包中的 `model.texture` 指向已有的贴图资源。
- `model.layer_location` 用于指定要烘焙的 JSON 模型文件。
- `model.model_id` 是 Automobility 物品与实体渲染时使用的运行时模型 id。
- `example_buggy` 这一对组件使用 `JsonEM`。
- `example_buggy_gecko` 这一对组件使用 `GeckoLib`，并通过 `geo_model` 与 `animation` 字段引用资源。
- 如果缺少资源包，组件会显示为屏障贴图占位，而不会导致游戏崩溃。
- 此时 tooltip 会提示缺少资源包。
- 当资源包启用后，`JsonEM` 和 `GeckoLib` 示例通常会在进入世界后自动生效，不需要手动按 `F3 + T`。

### 创建你自己的版本

1. 将 `example_buggy` 改成你自己的组件名称。
2. 同步修改配方结果里的 `component` id。
3. 将 `layer_location`、`model_id` 和 `texture` 替换成你自己的资源路径。
4. 如果你添加了自己的贴图，请放在资源包的 `assets/<namespace>/textures/...` 路径下。
