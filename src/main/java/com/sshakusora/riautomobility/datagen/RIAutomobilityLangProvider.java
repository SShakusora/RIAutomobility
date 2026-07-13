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
            add("key.categories.riautomobility", "RIAutomobility");
            add("key.riautomobility.boarding_as_passenger", "Board / Switch Seat");
            add("itemGroup.riautomobility.components", "RIAutomobility");
            add("itemGroup.riautomobility.custom_components", "RIAutomobility: Custom");
            add("tooltip.riautomobility.missing_car_pack_resources", "Missing car-pack resources for this component");
            add("entity.riautomobility.riautomobile", "RIAutomobile");
            add("entity.riautomobility.hitbox", "RIAutomobile");
            add("container.riautomobility.hitbox", "Box");
            add("container.riautomobility.vehicle_import", "Vehicle Import Table");
            add("block.riautomobility.vehicle_import_table", "Vehicle Import Table");
            add("commands.riautomobility.carpacks.reload.started", "Reloading RIAutomobility car packs...");
            add("commands.riautomobility.carpacks.reload.success", "RIAutomobility car packs reloaded");
            add("commands.riautomobility.carpacks.reload.failed", "Failed to reload RIAutomobility car packs: %s");
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
            add("key.categories.riautomobility", "飞天奇匠");
            add("key.riautomobility.boarding_as_passenger", "上车 / 切换座位");
            add("itemGroup.riautomobility.components", "飞天奇匠");
            add("itemGroup.riautomobility.custom_components", "飞天奇匠：自定义");
            add("tooltip.riautomobility.missing_car_pack_resources", "该组件缺少对应车包资源");
            add("entity.riautomobility.riautomobile", "机动车");
            add("entity.riautomobility.hitbox", "机动车");
            add("container.riautomobility.hitbox", "后备箱");
            add("container.riautomobility.vehicle_import", "车辆导入台");
            add("block.riautomobility.vehicle_import_table", "车辆导入台");
            add("commands.riautomobility.carpacks.reload.started", "正在重载 RIAutomobility 车包……");
            add("commands.riautomobility.carpacks.reload.success", "RIAutomobility 车包已重载");
            add("commands.riautomobility.carpacks.reload.failed", "RIAutomobility 车包重载失败：%s");
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
}
