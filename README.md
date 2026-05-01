# RIAutomobility

English | [中文](#中文)

## English

### Overview

`RIAutomobility` is a Forge add-on for `Automobility` on Minecraft `1.20.1`.

It extends Automobility with:

- new custom frames and wheels
- custom multi-seat vehicle behavior
- custom hitbox and camera definitions
- separate creative tabs for built-in and datapack custom components
- data-driven custom `Frame` and `Wheel` support
- support for both `JsonEM` and `GeckoLib` driven custom models

The mod is designed both for normal gameplay and for content authors who want to add their own vehicles through datapacks and resource packs.

### Main Features

- Adds multiple custom frames and wheels on top of Automobility
- Adds custom seat layouts for larger or special vehicles
- Adds custom hitbox and culling definitions for RIA vehicles
- Adds a separate creative tab for built-in RIA `Frame` and `Wheel` components
- Adds a second creative tab named `RIAutomobility: Custom` for datapack-defined components
- Supports datapack-defined custom `Frame` and `Wheel` components
- Supports resource-pack-defined rendering for those custom components
- Supports two rendering pipelines for custom components:
  - `JsonEM`
  - `GeckoLib`
- Handles missing custom resources safely with a placeholder model and tooltip warning

### Dependencies

- Minecraft `1.20.1`
- Forge `47.1.x`
- `Automobility 0.4.2+1.20.1-forge`
- `GeckoLib 4`

### Built-in Content

This mod includes several built-in RIA vehicles and components, such as:

- double motorcar variants
- quad motorcar variants
- lorry
- DMC12
- standard formula
- matching custom wheels for selected vehicles

### Creative Tab

RIA components are separated from the default Automobility tab.

There are two RIA tabs:

- `RIAutomobility`: built-in RIA frames and wheels
- `RIAutomobility: Custom`: datapack-defined custom frames and wheels

If a custom component is defined through datapacks and marked visible, it will appear in `RIAutomobility: Custom`.

### Missing Resource Behavior

If a datapack component exists but its model resources are missing:

- the game will not crash
- the component will render using a barrier-texture placeholder
- the tooltip will show `Missing resource pack for this component`

This applies to both `JsonEM` and `GeckoLib` custom components.

When the required resource pack is available, custom `JsonEM` and `GeckoLib` models are applied automatically after joining the world. Manual `F3 + T` is not required for normal startup.

## Data-Driven Guide

### Concept

Custom vehicle components are split into two parts:

1. Datapack: gameplay definition
2. Resource pack: model, texture, animation resources

The datapack defines stats, dimensions, seats, hitboxes, wheelbase, and model references.

The resource pack provides the assets referenced by the datapack.

### Datapack Paths

Custom frame definitions:

- `data/<namespace>/riautomobility/frames/<id>.json`

Custom wheel definitions:

- `data/<namespace>/riautomobility/wheels/<id>.json`

Optional Automobility recipes:

- `data/automobility/recipes/frame/<recipe>.json`
- `data/automobility/recipes/wheel/<recipe>.json`

### Resource Pack Paths

For `JsonEM` models:

- `assets/<namespace>/models/entity/automobile/frame/<name>/main.json`
- `assets/<namespace>/models/entity/automobile/wheel/<name>/main.json`

For `GeckoLib` models:

- `assets/<namespace>/geo/...`
- `assets/<namespace>/animations/...`
- `assets/<namespace>/textures/...`

Translations:

- `assets/<namespace>/lang/en_us.json`
- `assets/<namespace>/lang/zh_cn.json`

## Frame JSON Format

Example:

```json
{
  "weight": 0.85,
  "model": {
    "type": "jsonem",
    "texture": "examplepack:textures/entity/automobile/frame/example_buggy.png",
    "model_id": "examplepack:frame_example_buggy",
    "layer_location": "examplepack:automobile/frame/example_buggy",
    "render_type": "entity_cutout",
    "rotation_y": 0.0
  },
  "wheel_base": {
    "forward_separation": 44.0,
    "side_separation": 26.0
  },
  "length_px": 44.0,
  "seat_height": 4.0,
  "engine_pos_back": 14.0,
  "engine_pos_up": 3.0,
  "rear_attachment_pos": 18.0,
  "front_attachment_pos": 18.0,
  "dimensions": {
    "width": 1.75,
    "height": 0.95
  },
  "seats": [
    { "x": 0.35, "y": 0.0, "z": 0.05 },
    { "x": -0.35, "y": 0.0, "z": 0.05 }
  ],
  "camera_positions": [
    { "x": -3.0, "y": 0.5, "z": 0.0 }
  ],
  "hitboxes": [
    { "x": 0.0, "y": 0.0, "z": 0.65, "width": 1.65, "height": 0.95, "container": false }
  ],
  "front_attachment_enabled": false,
  "rear_attachment_enabled": false,
  "show_in_creative_tab": true
}
```

### Frame Fields

- `weight`: frame weight
- `model`: rendering definition
- `wheel_base`: wheel layout
- `length_px`: frame render length in pixels
- `seat_height`: base seat height in pixels
- `engine_pos_back`: engine position on Z axis in pixels
- `engine_pos_up`: engine position on Y axis in pixels
- `rear_attachment_pos`: rear attachment anchor in pixels
- `front_attachment_pos`: front attachment anchor in pixels
- `dimensions.width`: entity width in blocks
- `dimensions.height`: entity height in blocks
- `seats`: seat positions in block coordinates
- `camera_positions`: camera offsets in block coordinates
- `hitboxes`: custom hitbox definitions
- `front_attachment_enabled`: whether front attachments are allowed
- `rear_attachment_enabled`: whether rear attachments are allowed
- `show_in_creative_tab`: whether the component appears in the RIA tab

### Wheel Base Formats

Simple symmetric layout:

```json
"wheel_base": {
  "forward_separation": 44.0,
  "side_separation": 26.0
}
```

Custom per-wheel layout:

```json
"wheel_base": {
  "wheels": [
    { "forward": 22.0, "right": -13.0, "scale": 1.0, "yaw": 0.0, "end": "front", "side": "left" },
    { "forward": -22.0, "right": -13.0, "scale": 1.0, "yaw": 0.0, "end": "back", "side": "left" },
    { "forward": 22.0, "right": 13.0, "scale": 1.0, "yaw": 180.0, "end": "front", "side": "right" },
    { "forward": -22.0, "right": 13.0, "scale": 1.0, "yaw": 180.0, "end": "back", "side": "right" }
  ]
}
```

## Wheel JSON Format

Example:

```json
{
  "size": 0.78,
  "grip": 0.62,
  "radius": 5.0,
  "width": 3.5,
  "model": {
    "type": "jsonem",
    "texture": "examplepack:textures/entity/automobile/wheel/example_buggy.png",
    "model_id": "examplepack:wheel_example_buggy",
    "layer_location": "examplepack:automobile/wheel/example_buggy",
    "render_type": "entity_cutout",
    "rotation_y": -90.0
  },
  "show_in_creative_tab": true
}
```

### Wheel Fields

- `size`: wheel size used by Automobility logic
- `grip`: grip value
- `radius`: model radius in pixels
- `width`: model width in pixels
- `model`: rendering definition
- `show_in_creative_tab`: whether the component appears in the RIA tab

## Model Definition Types

### JsonEM

Use this when you want a baked JSON entity model.

```json
"model": {
  "type": "jsonem",
  "texture": "examplepack:textures/entity/automobile/frame/example_buggy.png",
  "model_id": "examplepack:frame_example_buggy",
  "layer_location": "examplepack:automobile/frame/example_buggy",
  "render_type": "entity_cutout",
  "rotation_y": 0.0
}
```

Fields:

- `type`: must be `jsonem`
- `texture`: render texture
- `model_id`: Automobility runtime model id
- `layer_location`: baked layer source
- `render_type`: one of `entity_cutout`, `entity_cutout_no_cull`, `entity_translucent`, `entity_translucent_cull`, `entity_solid`
- `rotation_y`: optional Y rotation in degrees

### GeckoLib

Use this when you want to render a GeckoLib geo model.

```json
"model": {
  "type": "geckolib",
  "texture": "examplepack:textures/entity/automobile/frame/example_buggy.png",
  "model_id": "examplepack:frame_example_buggy_gecko",
  "geo_model": "examplepack:geo/frame/example_buggy.geo.json",
  "animation": "examplepack:animations/example_buggy.animation.json"
}
```

Fields:

- `type`: must be `geckolib`
- `texture`: render texture
- `model_id`: Automobility runtime model id
- `geo_model`: GeckoLib geo file
- `animation`: GeckoLib animation file

## Recipe Example

Custom frame recipe:

```json
{
  "type": "automobility:auto_mechanic_table",
  "category": "automobility:frames",
  "sortnum": 200,
  "ingredients": [
    { "item": "automobility:automobile_frame", "component": "automobility:standard_red" },
    { "item": "minecraft:iron_ingot" }
  ],
  "result": {
    "item": "automobility:automobile_frame",
    "component": "examplepack:example_buggy"
  }
}
```

## Example Pack

A complete minimal example is included in this repository:

- `examples/examplepack-data/`
- `examples/examplepack-resources/`
- `examples/examplepack/README.md`

It contains:

- a `JsonEM` frame and wheel example
- a `GeckoLib` frame and wheel example
- matching recipes
- translations
- resource-pack model files

## Installation for Custom Content

1. Put your datapack in the world's `datapacks/` folder.
2. Put your resource pack in Minecraft's `resourcepacks/` folder.
3. Enable the resource pack.
4. Enter the world.
5. Run `/reload` if needed.

## Notes for Pack Authors

- Datapacks can define components without resource packs, but they will render as placeholders.
- Missing-resource placeholders use a barrier texture so they are easy to identify in inventories.
- `JsonEM` components need valid `assets/<namespace>/models/entity/.../main.json` files.
- `GeckoLib` components need valid `geo`, `animation`, and texture resources.
- Tooltips will warn the player when the required resource pack is missing.

---

## 中文

### 模组简介

`RIAutomobility` 是一个基于 Minecraft `1.20.1 Forge` 的 `Automobility` 附属模组。

它在 Automobility 的基础上扩展了：

- 新的车架与车轮
- 自定义多座位车辆逻辑
- 自定义碰撞箱与摄像机定义
- 内置组件与数据包自定义组件分离的创造模式标签页
- 可通过数据包添加的自定义 `Frame` 与 `Wheel`
- 同时支持 `JsonEM` 与 `GeckoLib` 的资源驱动模型

这个模组既可以直接用于游玩，也适合内容作者通过数据包和资源包扩展自己的车辆组件。

### 主要功能

- 为 Automobility 添加多种新的 RIA 车架和车轮
- 为特殊车型提供自定义座位布局
- 为 RIA 车辆提供自定义碰撞箱与视锥盒定义
- 为内置 RIA `Frame` / `Wheel` 提供独立创造标签页
- 为数据包添加的组件提供第二个 `RIAutomobility: Custom` 标签页
- 支持通过数据包定义自定义 `Frame` / `Wheel`
- 支持通过资源包为这些组件提供渲染资源
- 支持两种自定义模型渲染方式：
  - `JsonEM`
  - `GeckoLib`
- 当资源缺失时使用占位模型并在 tooltip 中提示

### 依赖

- Minecraft `1.20.1`
- Forge `47.1.x`
- `Automobility 0.4.2+1.20.1-forge`
- `GeckoLib 4`

### 内置内容

模组内置了多种 RIA 车辆部件，例如：

- 双座 机动车 系列
- 四座 机动车 系列
- 大运
- DMC12
- standard formula
- 对应的自定义车轮

### 创造模式标签页

RIA 组件不会放在默认 Automobility 标签页中。

当前分为两个 RIA 标签页：

- `RIAutomobility`：内置 RIA 车架与车轮
- `RIAutomobility: Custom`：通过数据包定义的自定义组件

通过数据包定义且设置为可见的自定义组件，会显示在 `RIAutomobility: Custom` 标签页中。

### 资源缺失时的行为

如果数据包中存在组件定义，但资源包没有提供对应模型资源：

- 游戏不会崩溃
- 组件会显示为屏障贴图占位模型
- tooltip 会显示 `该组件缺少对应资源包`

这个机制同时适用于 `JsonEM` 和 `GeckoLib` 自定义组件。

当资源包正确加载时，自定义 `JsonEM` 和 `GeckoLib` 模型会在进入世界后自动应用，正常情况下不需要手动按 `F3 + T`。

## 数据驱动教程

### 基本概念

自定义车辆组件分为两部分：

1. 数据包：负责玩法定义
2. 资源包：负责模型、贴图、动画资源

数据包定义组件的数值、尺寸、座位、碰撞箱、轮距以及模型引用。

资源包提供这些模型引用实际对应的资源文件。

### 数据包路径

自定义车架定义：

- `data/<namespace>/riautomobility/frames/<id>.json`

自定义车轮定义：

- `data/<namespace>/riautomobility/wheels/<id>.json`

可选的 Automobility 配方：

- `data/automobility/recipes/frame/<recipe>.json`
- `data/automobility/recipes/wheel/<recipe>.json`

### 资源包路径

`JsonEM` 模型：

- `assets/<namespace>/models/entity/automobile/frame/<name>/main.json`
- `assets/<namespace>/models/entity/automobile/wheel/<name>/main.json`

`GeckoLib` 模型：

- `assets/<namespace>/geo/...`
- `assets/<namespace>/animations/...`
- `assets/<namespace>/textures/...`

翻译文件：

- `assets/<namespace>/lang/en_us.json`
- `assets/<namespace>/lang/zh_cn.json`

## Frame JSON 格式

示例：

```json
{
  "weight": 0.85,
  "model": {
    "type": "jsonem",
    "texture": "examplepack:textures/entity/automobile/frame/example_buggy.png",
    "model_id": "examplepack:frame_example_buggy",
    "layer_location": "examplepack:automobile/frame/example_buggy",
    "render_type": "entity_cutout",
    "rotation_y": 0.0
  },
  "wheel_base": {
    "forward_separation": 44.0,
    "side_separation": 26.0
  },
  "length_px": 44.0,
  "seat_height": 4.0,
  "engine_pos_back": 14.0,
  "engine_pos_up": 3.0,
  "rear_attachment_pos": 18.0,
  "front_attachment_pos": 18.0,
  "dimensions": {
    "width": 1.75,
    "height": 0.95
  },
  "seats": [
    { "x": 0.35, "y": 0.0, "z": 0.05 },
    { "x": -0.35, "y": 0.0, "z": 0.05 }
  ],
  "camera_positions": [
    { "x": -3.0, "y": 0.5, "z": 0.0 }
  ],
  "hitboxes": [
    { "x": 0.0, "y": 0.0, "z": 0.65, "width": 1.65, "height": 0.95, "container": false }
  ],
  "front_attachment_enabled": false,
  "rear_attachment_enabled": false,
  "show_in_creative_tab": true
}
```

### Frame 字段说明

- `weight`：车架重量
- `model`：渲染定义
- `wheel_base`：轮组布局
- `length_px`：渲染长度，单位像素
- `seat_height`：座位基准高度，单位像素
- `engine_pos_back`：引擎在 Z 轴上的位置，单位像素
- `engine_pos_up`：引擎在 Y 轴上的位置，单位像素
- `rear_attachment_pos`：后部挂载点位置，单位像素
- `front_attachment_pos`：前部挂载点位置，单位像素
- `dimensions.width`：实体宽度，单位方块
- `dimensions.height`：实体高度，单位方块
- `seats`：座位坐标，单位方块
- `camera_positions`：摄像机偏移，单位方块
- `hitboxes`：自定义碰撞箱定义
- `front_attachment_enabled`：是否允许前挂件
- `rear_attachment_enabled`：是否允许后挂件
- `show_in_creative_tab`：是否显示在 RIA 创造标签页中

### wheel_base 写法

对称简写：

```json
"wheel_base": {
  "forward_separation": 44.0,
  "side_separation": 26.0
}
```

逐轮自定义：

```json
"wheel_base": {
  "wheels": [
    { "forward": 22.0, "right": -13.0, "scale": 1.0, "yaw": 0.0, "end": "front", "side": "left" },
    { "forward": -22.0, "right": -13.0, "scale": 1.0, "yaw": 0.0, "end": "back", "side": "left" },
    { "forward": 22.0, "right": 13.0, "scale": 1.0, "yaw": 180.0, "end": "front", "side": "right" },
    { "forward": -22.0, "right": 13.0, "scale": 1.0, "yaw": 180.0, "end": "back", "side": "right" }
  ]
}
```

## Wheel JSON 格式

示例：

```json
{
  "size": 0.78,
  "grip": 0.62,
  "radius": 5.0,
  "width": 3.5,
  "model": {
    "type": "jsonem",
    "texture": "examplepack:textures/entity/automobile/wheel/example_buggy.png",
    "model_id": "examplepack:wheel_example_buggy",
    "layer_location": "examplepack:automobile/wheel/example_buggy",
    "render_type": "entity_cutout",
    "rotation_y": -90.0
  },
  "show_in_creative_tab": true
}
```

### Wheel 字段说明

- `size`：Automobility 逻辑使用的轮子尺寸
- `grip`：抓地力
- `radius`：模型半径，单位像素
- `width`：模型宽度，单位像素
- `model`：渲染定义
- `show_in_creative_tab`：是否显示在 RIA 创造标签页中

## 模型定义类型

### JsonEM

适用于使用烘焙 JSON 实体模型的情况。

```json
"model": {
  "type": "jsonem",
  "texture": "examplepack:textures/entity/automobile/frame/example_buggy.png",
  "model_id": "examplepack:frame_example_buggy",
  "layer_location": "examplepack:automobile/frame/example_buggy",
  "render_type": "entity_cutout",
  "rotation_y": 0.0
}
```

字段说明：

- `type`：固定为 `jsonem`
- `texture`：渲染贴图
- `model_id`：Automobility 运行时模型 id
- `layer_location`：烘焙模型层来源
- `render_type`：可选 `entity_cutout`、`entity_cutout_no_cull`、`entity_translucent`、`entity_translucent_cull`、`entity_solid`
- `rotation_y`：可选 Y 轴旋转角度

### GeckoLib

适用于使用 GeckoLib geo 模型的情况。

```json
"model": {
  "type": "geckolib",
  "texture": "examplepack:textures/entity/automobile/frame/example_buggy.png",
  "model_id": "examplepack:frame_example_buggy_gecko",
  "geo_model": "examplepack:geo/frame/example_buggy.geo.json",
  "animation": "examplepack:animations/example_buggy.animation.json"
}
```

字段说明：

- `type`：固定为 `geckolib`
- `texture`：渲染贴图
- `model_id`：Automobility 运行时模型 id
- `geo_model`：GeckoLib geo 文件
- `animation`：GeckoLib 动画文件

## 配方示例

自定义车架配方：

```json
{
  "type": "automobility:auto_mechanic_table",
  "category": "automobility:frames",
  "sortnum": 200,
  "ingredients": [
    { "item": "automobility:automobile_frame", "component": "automobility:standard_red" },
    { "item": "minecraft:iron_ingot" }
  ],
  "result": {
    "item": "automobility:automobile_frame",
    "component": "examplepack:example_buggy"
  }
}
```

## 示例包

仓库中已经附带了一套完整最小示例：

- `examples/examplepack-data/`
- `examples/examplepack-resources/`
- `examples/examplepack/README.md`

其中包含：

- 一套 `JsonEM` 车架与车轮示例
- 一套 `GeckoLib` 车架与车轮示例
- 对应配方
- 翻译文件
- 资源包模型文件

## 自定义内容安装方式

1. 将数据包放入存档的 `datapacks/` 文件夹。
2. 将资源包放入 Minecraft 的 `resourcepacks/` 文件夹。
3. 在游戏中启用资源包。
4. 进入存档。
5. 如有需要执行 `/reload`。

## 给内容作者的提示

- 只装数据包不装资源包也是允许的，但组件会显示为占位模型。
- 缺失资源时，占位模型会使用屏障贴图，方便在物品栏中识别。
- `JsonEM` 组件需要正确的 `assets/<namespace>/models/entity/.../main.json`。
- `GeckoLib` 组件需要正确的 `geo`、`animation` 和贴图资源。
- 当资源缺失时，tooltip 会提示玩家缺少对应资源包。
