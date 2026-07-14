# 车辆导入台 GUI 美术交付包

请以 1:1 逻辑像素绘制。图集固定为 256x256，不要缩放画布、移动区域或改变任何 Sprite 的尺寸。
可以使用透明像素；未使用区域建议保留至少 1 像素安全间距。文字、物品图标、滚动位置与 3D 车辆预览均为动态内容，不应画入图集。

## 文件

- `vehicle_import_template.png`：按照当前界面颜色生成的可编辑模板。
- `vehicle_import_guide.png`：放大后的区域、名称、UV、尺寸及九宫格边距参考图。
- `vehicle_import_atlas.json`：与代码一致的机器可读坐标表。
- `vehicle_import.png.mcmeta`：关闭模糊采样并启用边缘约束。

## Sprite 分组

- `SCREEN/SIDEBAR/CONTROLS/PREVIEW/SELECTION/INVENTORY`：页面与各功能区背景。
- `BUTTON_*`：按钮普通、悬浮、禁用及 Shift 精细调整状态。
- `INPUT_*`：输入框普通、聚焦及禁用状态。
- `SLOT_*`：普通背包槽与输出槽。
- `ICON_*`：部件选择图标的普通、悬浮、选中及禁用状态。
- `TOGGLE_*`：开关关闭、开启及禁用状态。
- `DROPDOWN/ROW_*`：下拉目录背景与目录项状态。
- `SCROLL_*`：横向、纵向滚动条轨道和滑块。

`border` 大于 0 的 Sprite 会采用九宫格拉伸。四角和边框应在指定边距内完成，中间区域允许平铺；`border` 为 0 的 Sprite 会整体缩放到控件尺寸。

## 交付与应用

1. 将完成的图集保存为 `vehicle_import.png`。
2. 放入 `src/main/resources/assets/riautomobility/textures/gui/vehicle_import.png`。
3. 将 `vehicle_import.png.mcmeta` 放在同一目录。
4. 在开发客户端按 `F3+T` 重新加载资源。

最终稿不存在时，界面会自动使用 `runData` 生成的 `vehicle_import_template.png`；最终稿出现后会自动优先使用最终稿。
