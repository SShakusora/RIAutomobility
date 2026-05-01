# ExamplePack

This folder contains a complete minimal example for adding a custom `Frame` and `Wheel` to `RIAutomobility`.

## Contents

- `examplepack-data/`: datapack files
- `examplepack-resources/`: resource pack files

## Install

1. Copy `examplepack-data` into your world `datapacks/` folder.
2. Copy `examplepack-resources` into your Minecraft `resourcepacks/` folder.
3. Enable the resource pack in-game.
4. Run `/reload`.

## What it adds

- `examplepack:example_buggy` frame
- `examplepack:example_buggy` wheel
- `examplepack:example_buggy_gecko` frame
- `examplepack:example_buggy_gecko` wheel

Both will appear in the `RIAutomobility Frames & Wheels` creative tab and can also be crafted in the Auto Mechanic Table using the included recipe JSON files.

## Important paths

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

## Notes

- This example reuses Automobility's built-in textures, so no PNG files are required.
- `model.texture` in the datapack points to an existing texture resource.
- `model.layer_location` controls which JSON model file is baked.
- `model.model_id` is the runtime model id used by Automobility item/entity rendering.
- The `example_buggy` pair uses `JsonEM`.
- The `example_buggy_gecko` pair uses `GeckoLib` with `geo_model` and `animation` fields.

## Creating your own variant

1. Change the ids from `example_buggy` to your own name.
2. Update recipe result `component` ids to match.
3. Replace `layer_location`, `model_id`, and `texture` with your own assets.
4. If you add your own textures, place them in your resource pack under `assets/<namespace>/textures/...`.
