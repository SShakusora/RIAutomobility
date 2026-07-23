package com.sshakusora.riautomobility.interaction;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public sealed interface VehicleInteractionAction
        permits VehicleInteractionAction.OpenContainer, VehicleInteractionAction.Mount,
        VehicleInteractionAction.Molang {
    int MAX_CHANNEL = 31;

    boolean requiresAccess();

    default Trigger trigger() {
        return Trigger.RIGHT_CLICK;
    }

    JsonObject toJson();

    static VehicleInteractionAction fromJson(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type").toLowerCase();
        boolean requiresAccess = GsonHelper.getAsBoolean(json, "requires_access",
                !"molang".equals(type));
        return switch (type) {
            case "open_container" -> new OpenContainer(requiresAccess);
            case "mount" -> new Mount(GsonHelper.getAsInt(json, "seat", -1), requiresAccess);
            case "molang" -> new Molang(
                    checkedChannel(GsonHelper.getAsInt(json, "channel")),
                    MolangOperation.parse(GsonHelper.getAsString(json, "operation", "pulse")),
                     checkedValue(GsonHelper.getAsFloat(json, "value", 1.0F)),
                     checkedTicks(GsonHelper.getAsInt(json, "duration_ticks", 10), "duration_ticks"),
                     checkedTicks(GsonHelper.getAsInt(json, "transition_ticks", 0), "transition_ticks"),
                     Trigger.parse(GsonHelper.getAsString(json, "trigger", "right_click")),
                     requiresAccess
             );
            default -> throw new IllegalArgumentException("Unsupported vehicle interaction action '" + type + "'");
        };
    }

    private static int checkedChannel(int channel) {
        if (channel < 0 || channel > MAX_CHANNEL) {
            throw new IllegalArgumentException("Molang interaction channel must be between 0 and " + MAX_CHANNEL);
        }
        return channel;
    }

    private static float checkedValue(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Molang interaction value must be finite");
        }
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static int checkedTicks(int ticks, String field) {
        if (ticks < 0 || ticks > 72_000) {
            throw new IllegalArgumentException(field + " must be between 0 and 72000");
        }
        return ticks;
    }

    record OpenContainer(boolean requiresAccess) implements VehicleInteractionAction {
        public OpenContainer {
            requiresAccess = true;
        }

        @Override
        public JsonObject toJson() {
            return baseJson("open_container", requiresAccess);
        }
    }

    record Mount(int seat, boolean requiresAccess) implements VehicleInteractionAction {
        public Mount {
            if (seat < -1 || seat > 255) {
                throw new IllegalArgumentException("Interaction seat must be -1 or between 0 and 255");
            }
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = baseJson("mount", requiresAccess);
            if (seat >= 0) {
                json.addProperty("seat", seat);
            }
            return json;
        }
    }

    record Molang(int channel, MolangOperation operation, float value, int durationTicks,
                  int transitionTicks, Trigger trigger,
                  boolean requiresAccess) implements VehicleInteractionAction {
        public Molang(int channel, MolangOperation operation, float value, int durationTicks,
                      int transitionTicks, boolean requiresAccess) {
            this(channel, operation, value, durationTicks, transitionTicks,
                    Trigger.RIGHT_CLICK, requiresAccess);
        }

        public Molang {
            checkedChannel(channel);
            value = checkedValue(value);
            checkedTicks(durationTicks, "duration_ticks");
            checkedTicks(transitionTicks, "transition_ticks");
            if (trigger == null) {
                throw new IllegalArgumentException("Molang interaction trigger cannot be null");
            }
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = baseJson("molang", requiresAccess);
            json.addProperty("channel", channel);
            json.addProperty("operation", operation.serializedName);
            json.addProperty("value", value);
            if (trigger != Trigger.RIGHT_CLICK) {
                json.addProperty("trigger", trigger.serializedName);
            }
            if (durationTicks != 10 || operation == MolangOperation.PULSE) {
                json.addProperty("duration_ticks", durationTicks);
            }
            if (transitionTicks > 0) {
                json.addProperty("transition_ticks", transitionTicks);
            }
            return json;
        }
    }

    enum MolangOperation {
        SET("set"),
        TOGGLE("toggle"),
        PULSE("pulse");

        private final String serializedName;

        MolangOperation(String serializedName) {
            this.serializedName = serializedName;
        }

        static MolangOperation parse(String value) {
            for (MolangOperation operation : values()) {
                if (operation.serializedName.equalsIgnoreCase(value)) {
                    return operation;
                }
            }
            throw new IllegalArgumentException("Unsupported Molang interaction operation '" + value + "'");
        }
    }

    enum Trigger {
        LEFT_CLICK("left_click"),
        RIGHT_CLICK("right_click"),
        SHIFT_LEFT_CLICK("shift_left_click"),
        SHIFT_RIGHT_CLICK("shift_right_click");

        private final String serializedName;

        Trigger(String serializedName) {
            this.serializedName = serializedName;
        }

        public static Trigger fromInput(boolean attack, boolean shiftDown) {
            if (attack) {
                return shiftDown ? SHIFT_LEFT_CLICK : LEFT_CLICK;
            }
            return shiftDown ? SHIFT_RIGHT_CLICK : RIGHT_CLICK;
        }

        static Trigger parse(String value) {
            for (Trigger trigger : values()) {
                if (trigger.serializedName.equalsIgnoreCase(value)) {
                    return trigger;
                }
            }
            throw new IllegalArgumentException("Unsupported vehicle interaction trigger '" + value + "'");
        }
    }

    private static JsonObject baseJson(String type, boolean requiresAccess) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        boolean defaultRequiresAccess = !"molang".equals(type);
        if (requiresAccess != defaultRequiresAccess) {
            json.addProperty("requires_access", requiresAccess);
        }
        return json;
    }
}
