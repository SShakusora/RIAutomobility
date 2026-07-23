package com.sshakusora.riautomobility.datagen;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.LanguageProvider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

abstract class RIAutomobilityLangProvider extends LanguageProvider {
    protected RIAutomobilityLangProvider(PackOutput output, String locale) {
        super(output, RIAutomobility.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        addFixedTranslations();
        addComponentTranslations();
    }

    protected abstract void addFixedTranslations();

    protected abstract String frameName(ResourceLocation id);

    protected abstract String wheelName(ResourceLocation id);

    private void addComponentTranslations() {
        RIAutomobileFrame.reload();
        RIAutomobileWheel.reload();

        AutomobileFrame.REGISTRY.forEach(frame -> {
            if (!frame.isEmpty() && RIAutomobility.MODID.equals(frame.getId().getNamespace())) {
                add(componentKey("frame", frame.getId()), frameName(frame.getId()));
            }
        });

        AutomobileWheel.REGISTRY.forEach(wheel -> {
            if (!wheel.isEmpty() && RIAutomobility.MODID.equals(wheel.getId().getNamespace())) {
                add(componentKey("wheel", wheel.getId()), wheelName(wheel.getId()));
            }
        });
    }

    private static String componentKey(String type, ResourceLocation id) {
        return type + "." + id.getNamespace() + "." + id.getPath();
    }

    protected static String englishDisplayName(ResourceLocation id) {
        StringBuilder builder = new StringBuilder();
        for (String token : id.getPath().split("_")) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(englishToken(token));
        }
        return builder.toString();
    }

    private static String englishToken(String token) {
        return switch (token) {
            case "dmc12" -> "DMC12";
            case "doublemotorcar" -> "DoubleMotorcar";
            case "quadmotorcar" -> "QuadMotorcar";
            default -> Character.toUpperCase(token.charAt(0)) + token.substring(1).toLowerCase(Locale.ROOT);
        };
    }

    public static final class EnUs extends RIAutomobilityLangProvider {
        public EnUs(PackOutput output) {
            super(output, "en_us");
        }

        @Override
        protected void addFixedTranslations() {
            addVehicleImportTranslations(false);
            add("key.categories.riautomobility", "RIAutomobility");
            add("key.riautomobility.boarding_as_passenger", "Board / Switch Seat");
            add("itemGroup.riautomobility.components", "RIAutomobility");
            add("tooltip.riautomobility.missing_car_pack_resources", "Missing car-pack resources for this component");
            add("tooltip.riautomobility.component_author", "Author: %s");
            add("gui.riautomobility.scrollbar", "Scrollbar");
            add("entity.riautomobility.riautomobile", "RIAutomobile");
            add("entity.riautomobility.hitbox", "RIAutomobile");
            add("container.riautomobility.hitbox", "Box");
            add("container.riautomobility.vehicle_import", "Vehicle Import Table");
            add("block.riautomobility.vehicle_import_table", "Vehicle Import Table");
            add("item.riautomobility.vehicle_key", "Blank Vehicle Key");
            add("item.riautomobility.vehicle_key.bound", "Vehicle Key");
            add("tooltip.riautomobility.vehicle_key.id", "Vehicle: %s");
            add("tooltip.riautomobility.vehicle_key.hold_shift", "Hold %s to view usage");
            add("tooltip.riautomobility.vehicle_key.blank_usage.1", "Keep this key in your off hand when placing a vehicle");
            add("tooltip.riautomobility.vehicle_key.blank_usage.2", "It will bind to that vehicle and restrict driver access");
            add("tooltip.riautomobility.vehicle_key.bound_usage.1", "Keep this key in your inventory to access its vehicle");
            add("tooltip.riautomobility.vehicle_key.bound_usage.2", "Right-click to locate and highlight its vehicle");
            add("tooltip.riautomobility.vehicle_key.bound_usage.3", "Craft it by itself to turn it back into a blank key");
            add("message.riautomobility.vehicle_key.denied", "You do not have this vehicle's key.");
            add("message.riautomobility.vehicle_key.blank", "This vehicle key is blank.");
            add("message.riautomobility.vehicle_key.invalid", "The bound vehicle no longer exists. The key is now blank.");
            add("message.riautomobility.vehicle_key.unavailable", "The vehicle's location is temporarily unavailable. The key remains bound.");
            add("message.riautomobility.vehicle_key.location", "Vehicle: %s, %s, %s in %s");
            add("command.riautomobility.vehicle_key.not_keyed", "The selected entity is not a keyed vehicle.");
            add("command.riautomobility.vehicle_key.recovered", "Recovered a key for %s.");
            add("message.riautomobility.carpacks.mismatch", "Car packs do not match the server.");
            add("message.riautomobility.carpacks.missing", "Missing: %s.");
            add("message.riautomobility.carpacks.different", "Different: %s.");
            add("message.riautomobility.carpacks.client_only", "Client only: %s.");
        }

        @Override
        protected String frameName(ResourceLocation id) {
            return englishDisplayName(id);
        }

        @Override
        protected String wheelName(ResourceLocation id) {
            return englishDisplayName(id);
        }
    }

    public static final class ZhCn extends RIAutomobilityLangProvider {
        private static final Map<String, String> TOKEN_MAP = new HashMap<>();

        static {
            TOKEN_MAP.put("wooden", "木质");
            TOKEN_MAP.put("copper", "铜质");
            TOKEN_MAP.put("steel", "钢质");
            TOKEN_MAP.put("golden", "黄金");
            TOKEN_MAP.put("bejeweled", "璀璨");
            TOKEN_MAP.put("doublemotorcar", "双座汽车");
            TOKEN_MAP.put("quadmotorcar", "四座汽车");
            TOKEN_MAP.put("lorry", "大运");
            TOKEN_MAP.put("dmc12", "DMC12");
            TOKEN_MAP.put("standard", "标准");
            TOKEN_MAP.put("formula", "方程式");
        }

        public ZhCn(PackOutput output) {
            super(output, "zh_cn");
        }

        @Override
        protected void addFixedTranslations() {
            addVehicleImportTranslations(true);
            add("key.categories.riautomobility", "飞天奇匠");
            add("key.riautomobility.boarding_as_passenger", "上车 / 切换座位");
            add("itemGroup.riautomobility.components", "飞天奇匠");
            add("tooltip.riautomobility.missing_car_pack_resources", "该组件缺少对应车包资源");
            add("tooltip.riautomobility.component_author", "作者：%s");
            add("gui.riautomobility.scrollbar", "滚动条");
            add("entity.riautomobility.riautomobile", "机动车");
            add("entity.riautomobility.hitbox", "机动车");
            add("container.riautomobility.hitbox", "后备箱");
            add("container.riautomobility.vehicle_import", "车辆导入台");
            add("block.riautomobility.vehicle_import_table", "车辆导入台");
            add("item.riautomobility.vehicle_key", "空白机动车钥匙");
            add("item.riautomobility.vehicle_key.bound", "机动车钥匙");
            add("tooltip.riautomobility.vehicle_key.id", "车辆：%s");
            add("tooltip.riautomobility.vehicle_key.hold_shift", "按住 %s 查看使用说明");
            add("tooltip.riautomobility.vehicle_key.blank_usage.1", "放置或组装车辆时，将此钥匙放在副手");
            add("tooltip.riautomobility.vehicle_key.blank_usage.2", "钥匙将绑定该车辆，并限制其驾驶权限");
            add("tooltip.riautomobility.vehicle_key.bound_usage.1", "将钥匙放在背包中，即可使用其绑定的车辆");
            add("tooltip.riautomobility.vehicle_key.bound_usage.2", "右键使用可定位并高亮其绑定的车辆");
            add("tooltip.riautomobility.vehicle_key.bound_usage.3", "单独合成可将其重置为空白钥匙");
            add("message.riautomobility.vehicle_key.denied", "你的背包中没有该车辆的钥匙。");
            add("message.riautomobility.vehicle_key.blank", "这是一把空白机动车钥匙。");
            add("message.riautomobility.vehicle_key.invalid", "这把钥匙对应的车辆已不存在，钥匙已恢复为空白钥匙。");
            add("message.riautomobility.vehicle_key.unavailable", "暂时无法获取车辆位置，钥匙仍保持绑定。");
            add("message.riautomobility.vehicle_key.location", "车辆位置：%s，%s，%s（%s）");
            add("command.riautomobility.vehicle_key.not_keyed", "选择的实体不是已绑定钥匙的车辆。");
            add("command.riautomobility.vehicle_key.recovered", "已恢复 %s 的钥匙。");
            add("message.riautomobility.carpacks.mismatch", "车包与服务器不一致。");
            add("message.riautomobility.carpacks.missing", "缺少：%s。");
            add("message.riautomobility.carpacks.different", "内容不同：%s。");
            add("message.riautomobility.carpacks.client_only", "仅客户端存在：%s。");
        }

        @Override
        protected String frameName(ResourceLocation id) {
            return chineseDisplayName(id);
        }

        @Override
        protected String wheelName(ResourceLocation id) {
            return chineseDisplayName(id);
        }

        private static String chineseDisplayName(ResourceLocation id) {
            StringBuilder builder = new StringBuilder();
            for (String token : id.getPath().split("_")) {
                builder.append(TOKEN_MAP.getOrDefault(token, englishDisplayName(new ResourceLocation(id.getNamespace(), token))));
            }
            return builder.toString();
        }
    }

    protected final void addVehicleImportTranslations(boolean chinese) {
        String p = "editor.riautomobility.vehicle_import.";
        String[][] entries = chinese ? new String[][]{
                {"page.frame", "外壳"}, {"page.wheel", "轮子"}, {"page.engine", "引擎"},
                {"tab.basic", "基础"}, {"tab.wheels", "轮位"}, {"tab.seats", "座椅"}, {"tab.hitboxes", "碰撞"}, {"tab.attachments", "附件"}, {"tab.advanced", "高级"},
                {"button.reset_view", "重置视图"}, {"button.select_preview", "选择预览"}, {"button.import_model", "导入文件"}, {"button.export_pack", "导出文件"}, {"button.export_item", "导出物品"}, {"button.select_list", "选择名单"},
                {"label.name", "名字："}, {"label.weight", "重量："}, {"label.engine_back", "引擎后移："}, {"label.engine_height", "引擎高度："}, {"label.size", "尺寸："}, {"label.grip", "抓地力："}, {"label.radius", "半径："}, {"label.width", "宽度："}, {"label.height", "高度："}, {"label.torque", "扭矩："}, {"label.speed", "速度："}, {"label.rotation_y", "Y轴旋转："}, {"label.x", "X："}, {"label.y", "Y："}, {"label.z", "Z："},
                {"header.wheel", "轮位 %s/%s"}, {"header.seat", "座椅 %s/%s"}, {"header.entity_hitbox", "实体碰撞箱"}, {"header.additional_hitbox", "附加碰撞箱 %s/%s"}, {"status.invalid_number", "数字格式无效"}
        } : new String[][]{
                {"page.frame", "Frame"}, {"page.wheel", "Wheel"}, {"page.engine", "Engine"}, {"tab.basic", "Basic"}, {"tab.wheels", "Wheels"}, {"tab.seats", "Seats"}, {"tab.hitboxes", "Hitboxes"}, {"tab.attachments", "Attachments"}, {"tab.advanced", "Advanced"},
                {"button.reset_view", "Reset View"}, {"button.select_preview", "Select Preview"}, {"button.import_model", "Import File"}, {"button.export_pack", "Export File"}, {"button.export_item", "Export Item"}, {"button.select_list", "Select List"},
                {"label.name", "Name:"}, {"label.weight", "Weight:"}, {"label.engine_back", "Engine Back:"}, {"label.engine_height", "Engine Height:"}, {"label.size", "Size:"}, {"label.grip", "Grip:"}, {"label.radius", "Radius:"}, {"label.width", "Width:"}, {"label.height", "Height:"}, {"label.torque", "Torque:"}, {"label.speed", "Speed:"}, {"label.rotation_y", "Y Rotation:"}, {"label.x", "X:"}, {"label.y", "Y:"}, {"label.z", "Z:"},
                {"header.wheel", "Wheel %s/%s"}, {"header.seat", "Seat %s/%s"}, {"header.entity_hitbox", "Entity Hitbox"}, {"header.additional_hitbox", "Additional Hitbox %s/%s"}, {"status.invalid_number", "Invalid number format"}
        };
        for (String[] entry : entries) add(p + entry[0], entry[1]);
        add(p + "button.back", chinese ? "返回" : "Back");
        add(p + "button.select_all", chinese ? "全选" : "Select All");
        add(p + "button.clear", chinese ? "清空" : "Clear");
        add(p + "selection.title", chinese ? "选择 %s" : "Select %s");
        add(p + "selection.front_attachments", chinese ? "前附件名单" : "Front Attachment List");
        add(p + "selection.rear_attachments", chinese ? "后附件名单" : "Rear Attachment List");
        add(p + "label.inventory", chinese ? "背包" : "Inventory");
        add(p + "label.output", chinese ? "输出" : "Output");
        add(p + "label.forward", chinese ? "前后：" : "Forward:");
        add(p + "label.right", chinese ? "左右：" : "Right:");
        add(p + "label.scale", chinese ? "缩放：" : "Scale:");
        add(p + "label.yaw", chinese ? "偏航角：" : "Yaw:");
        add(p + "label.axle", chinese ? "轴位：" : "Axle:");
        add(p + "label.side", chinese ? "侧位：" : "Side:");
        add(p + "label.hide_engine", chinese ? "隐藏引擎：" : "Hide Engine:");
        add(p + "label.engine_animation", chinese ? "引擎动画：" : "Engine Animation:");
        add(p + "label.container_hitbox", chinese ? "容器碰撞箱：" : "Container Hitbox:");
        add(p + "label.hide_engine.off", chinese ? "否" : "No");
        add(p + "label.hide_engine.on", chinese ? "是" : "Yes");
        add(p + "label.axle.rear", chinese ? "后" : "Rear");
        add(p + "label.axle.front", chinese ? "前" : "Front");
        add(p + "label.side.right", chinese ? "右" : "Right");
        add(p + "label.side.left", chinese ? "左" : "Left");
        add(p + "label.engine_animation.stopped", chinese ? "停止" : "Stopped");
        add(p + "label.engine_animation.running", chinese ? "运行" : "Running");
        add(p + "label.container_hitbox.off", chinese ? "否" : "No");
        add(p + "label.container_hitbox.on", chinese ? "是" : "Yes");
        add(p + "label.front_attachment", chinese ? "前附件：" : "Front Attachment:");
        add(p + "label.rear_attachment", chinese ? "后附件：" : "Rear Attachment:");
        add(p + "label.front_list", chinese ? "前名单：" : "Front List:");
        add(p + "label.rear_list", chinese ? "后名单：" : "Rear List:");
        add(p + "label.front_attachment_position", chinese ? "前附件位：" : "Front Attachment Position:");
        add(p + "label.rear_attachment_position", chinese ? "后附件位：" : "Rear Attachment Position:");
        add(p + "label.entity_width", chinese ? "实体宽度：" : "Entity Width:");
        add(p + "label.entity_height", chinese ? "实体高度：" : "Entity Height:");
        add(p + "label.preview_view", chinese ? "预览视角：" : "Preview View:");
        add(p + "label.front_attachment.disabled", chinese ? "禁用" : "Disabled");
        add(p + "label.front_attachment.enabled", chinese ? "启用" : "Enabled");
        add(p + "label.rear_attachment.disabled", chinese ? "禁用" : "Disabled");
        add(p + "label.rear_attachment.enabled", chinese ? "启用" : "Enabled");
        add(p + "label.front_list.blacklist", chinese ? "黑名单" : "Blacklist");
        add(p + "label.front_list.whitelist", chinese ? "白名单" : "Whitelist");
        add(p + "label.rear_list.blacklist", chinese ? "黑名单" : "Blacklist");
        add(p + "label.rear_list.whitelist", chinese ? "白名单" : "Whitelist");
        add(p + "option.first_person", chinese ? "第一人称" : "First Person");
        add(p + "option.external", chinese ? "外部" : "External");
        add(p + "position.wheel", chinese ? "轮位 %s (%.1f, %.1f)" : "Wheel %s (%.1f, %.1f)");
        add(p + "position.seat", chinese ? "座椅 %s (%s, %s, %s)" : "Seat %s (%s, %s, %s)");
        add(p + "position.additional_hitbox", chinese ? "附加碰撞箱 %s (%s, %s, %s)" : "Additional Hitbox %s (%s, %s, %s)");
        add(p + "dialog.open_file", chinese ? "选择 BBModel 或 RIAuto 文件" : "Select a BBModel or RIAuto file");
        add(p + "dialog.import_filter", "BBModel and RIAuto files (*.bbmodel, *.riauto)");
        add(p + "dialog.export_pack", chinese ? "导出 .riauto" : "Export .riauto");
        add(p + "dialog.export_filter", "RIAuto files (*.riauto)");
        add(p + "message.no_preview", chinese ? "请选择要预览的部件，或导入对应文件" : "Select a component to preview or import its file");
        add(p + "status.file_only", chinese ? "只支持 .bbmodel 和 .riauto 文件" : "Only .bbmodel and .riauto files are supported");
        add(p + "status.import_failed", chinese ? "导入失败：%s" : "Import failed: %s");
        add(p + "status.loading_preview", chinese ? "正在载入预览..." : "Loading preview...");
        add(p + "status.preview_loaded", chinese ? "预览已载入" : "Preview loaded");
        add(p + "status.preview_failed", chinese ? "预览失败：%s" : "Preview failed: %s");
        add(p + "status.exported", chinese ? "已导出：%s" : "Exported: %s");
        add(p + "status.export_failed", chinese ? "导出失败：%s" : "Export failed: %s");
        add(p + "status.output_not_empty", chinese ? "请先取走输出槽中的物品" : "Remove the item from the output slot first");
        add(p + "status.export_permission_required", chinese ? "导出物品需要服务器管理员权限" : "Exporting items requires server administrator permission");
        add(p + "status.installing_component", chinese ? "正在生成并安装组件..." : "Generating and installing component...");
        add(p + "status.item_export_failed", chinese ? "物品导出失败：%s" : "Item export failed: %s");
        add(p + "status.component_installed_syncing", chinese ? "组件已安装，正在同步客户端资源..." : "Component installed; synchronizing client resources...");
        add(p + "status.item_exported", chinese ? "组件已安装，物品已发送到输出槽" : "Component installed; item sent to output slot");
        add(p + "status.invalid_resource_id", chinese ? "名单包含无效资源 ID：%s" : "List contains an invalid resource ID: %s");
        add(p + "tooltip.name", chinese ? "导出后物品的显示名与 .riauto 文件名基础，不会改变内部资源 ID。" : "The exported item's display name and .riauto filename basis. This does not change the internal resource ID.");
        add(p + "tooltip.weight", chinese ? "外壳的物理重量，影响加速、操控与抓地。" : "The frame's physical weight, affecting acceleration, handling, and grip.");
        add(p + "tooltip.engine_back", chinese ? "引擎相对外壳中心向车尾移动的距离，单位为模型像素。" : "Engine offset toward the rear from the frame center, in model pixels.");
        add(p + "tooltip.engine_height", chinese ? "引擎相对外壳的垂直位置，单位为模型像素。" : "Engine vertical offset from the frame, in model pixels.");
        add(p + "tooltip.forward", chinese ? "轮位沿外壳前后方向的位置，单位为模型像素。" : "Wheel position along the frame's forward axis, in model pixels.");
        add(p + "tooltip.right", chinese ? "轮位沿外壳左右方向的位置，单位为模型像素。" : "Wheel position along the frame's lateral axis, in model pixels.");
        add(p + "tooltip.scale", chinese ? "该轮位轮胎模型的独立缩放倍率。" : "Independent tire-model scale for this wheel position.");
        add(p + "tooltip.yaw", chinese ? "轮胎模型绕 Y 轴的朝向修正角，单位为度。" : "Tire model Y-axis orientation adjustment, in degrees.");
        add(p + "tooltip.size", chinese ? "轮胎的物理尺寸参数。" : "The tire's physical size parameter.");
        add(p + "tooltip.grip", chinese ? "轮胎抓地参数，与外壳重量共同决定整车抓地能力。" : "Tire grip parameter; together with frame weight it determines vehicle grip.");
        add(p + "tooltip.radius", chinese ? "轮胎模型半径，单位为模型像素。" : "Tire model radius, in model pixels.");
        add(p + "tooltip.width.wheel", chinese ? "轮胎模型宽度，单位为模型像素。" : "Tire model width, in model pixels.");
        add(p + "tooltip.width.hitbox", chinese ? "附加碰撞箱的水平宽度，单位为方块。" : "Additional hitbox horizontal width, in blocks.");
        add(p + "tooltip.rotation_y.wheel", chinese ? "整个轮子模型的 Y 轴朝向修正，单位为度。" : "Whole wheel-model Y-axis orientation adjustment, in degrees.");
        add(p + "tooltip.rotation_y.engine", chinese ? "整个引擎模型的 Y 轴朝向修正，单位为度。" : "Whole engine-model Y-axis orientation adjustment, in degrees.");
        add(p + "tooltip.torque", chinese ? "引擎扭矩参数，主要提高整车加速度。" : "Engine torque parameter, primarily increasing acceleration.");
        add(p + "tooltip.speed", chinese ? "引擎速度参数，决定舒适行驶速度范围。" : "Engine speed parameter, determining the comfortable driving speed range.");
        add(p + "tooltip.front_attachment_position", chinese ? "前附件的安装位置，单位为模型像素。" : "Front attachment mounting position, in model pixels.");
        add(p + "tooltip.rear_attachment_position", chinese ? "后附件的安装位置，单位为模型像素。" : "Rear attachment mounting position, in model pixels.");
        add(p + "tooltip.entity_width", chinese ? "车辆主体实体碰撞箱的水平宽度，单位为方块。" : "Vehicle entity hitbox horizontal width, in blocks.");
        add(p + "tooltip.entity_height", chinese ? "车辆主体实体碰撞箱高度，单位为方块。" : "Vehicle entity hitbox height, in blocks.");
        add(p + "tooltip.height", chinese ? "附加碰撞箱从底面向上的高度，单位为方块。" : "Additional hitbox height above its base, in blocks.");
        add(p + "tooltip.x.seat", chinese ? "座椅局部 X 偏移，单位为方块。" : "Seat local X offset, in blocks.");
        add(p + "tooltip.y.seat", chinese ? "座椅局部 Y 偏移，单位为方块。" : "Seat local Y offset, in blocks.");
        add(p + "tooltip.z.seat", chinese ? "座椅局部 Z 偏移，单位为方块。" : "Seat local Z offset, in blocks.");
        add(p + "tooltip.x.hitbox", chinese ? "附加碰撞箱底面中心的 X 偏移，单位为方块。" : "Additional hitbox base-center X offset, in blocks.");
        add(p + "tooltip.y.hitbox", chinese ? "附加碰撞箱底面中心的 Y 偏移，单位为方块。" : "Additional hitbox base-center Y offset, in blocks.");
        add(p + "tooltip.z.hitbox", chinese ? "附加碰撞箱底面中心的 Z 偏移，单位为方块。" : "Additional hitbox base-center Z offset, in blocks.");
        add(p + "tooltip.hide_engine", chinese ? "隐藏引擎模型，不会移除引擎或改变其性能。" : "Hides the engine model without removing the engine or changing performance.");
        add(p + "tooltip.axle", chinese ? "标记当前轮位属于前轴或后轴。" : "Marks the current wheel position as front or rear axle.");
        add(p + "tooltip.side", chinese ? "标记当前轮位位于车辆左侧或右侧。" : "Marks the current wheel position as the vehicle's left or right side.");
        add(p + "tooltip.engine_animation", chinese ? "仅启动或停止编辑器预览中的引擎动画。" : "Only starts or stops the engine animation in the editor preview.");
        add(p + "tooltip.container_hitbox", chinese ? "与此碰撞箱交互时打开车辆储物界面。" : "Interacting with this hitbox opens the vehicle storage screen.");
        add(p + "tooltip.front_attachment", chinese ? "控制是否允许安装前附件。" : "Controls whether front attachments may be installed.");
        add(p + "tooltip.rear_attachment", chinese ? "控制是否允许安装后附件。" : "Controls whether rear attachments may be installed.");
        add(p + "tooltip.front_list", chinese ? "前附件筛选名单；可选择白名单或黑名单。" : "Front attachment filter list; choose whitelist or blacklist mode.");
        add(p + "tooltip.rear_list", chinese ? "后附件筛选名单；可选择白名单或黑名单。" : "Rear attachment filter list; choose whitelist or blacklist mode.");
        add(p + "tooltip.preview_view", chinese ? "切换座椅页的外部和第一人称预览视角。" : "Switches the seat page between external and first-person preview views.");
        add(p + "tooltip.wheel", chinese ? "选择当前编辑并在预览中高亮的轮位。" : "Selects the wheel position being edited and highlighted in preview.");
        add(p + "tooltip.seat", chinese ? "选择当前编辑并在预览中高亮的座椅。" : "Selects the seat being edited and highlighted in preview.");
        add(p + "tooltip.collision", chinese ? "选择当前编辑并在预览中高亮的碰撞箱。" : "Selects the hitbox being edited and highlighted in preview.");
        add(p + "default_name.frame", chinese ? "新外壳" : "New Frame");
        add(p + "default_name.wheel", chinese ? "新轮子" : "New Wheel");
        add(p + "default_name.engine", chinese ? "新引擎" : "New Engine");
        add(p + "validation.display_name_length", chinese ? "名称长度必须为 1 到 80 个字符" : "Display name must contain 1-80 characters");
        add(p + "validation.frame_weight", chinese ? "外壳重量必须大于零" : "Frame weight must be greater than zero");
        add(p + "validation.item_length", chinese ? "物品显示长度必须大于零" : "Item display length must be greater than zero");
        add(p + "validation.frame_positions", chinese ? "外壳位置必须是有限数值" : "Frame positions must contain finite numbers");
        add(p + "validation.frame_dimensions", chinese ? "外壳尺寸必须大于零" : "Frame dimensions must be greater than zero");
        add(p + "validation.seat_camera_positions", chinese ? "座椅和相机位置必须是有限数值" : "Seat and camera positions must contain finite numbers");
        add(p + "validation.hitbox_dimensions", chinese ? "碰撞箱尺寸必须大于零且所有数值有限" : "Hitbox dimensions must be greater than zero and all values must be finite");
        add(p + "validation.choose_model", chinese ? "请为 %s 选择 BBModel 文件" : "Choose a BBModel file for %s");
        add(p + "validation.bbmodel_only", chinese ? "当前组件的模型源必须是 .bbmodel 文件" : "The current component model source must be a .bbmodel file");
        add(p + "validation.frame_wheels", chinese ? "外壳至少需要一个轮位" : "A frame requires at least one wheel position");
        add(p + "validation.invalid_resource_id", chinese ? "%s 包含无效资源 ID：%s" : "%s contains an invalid resource ID: %s");
    }
}
