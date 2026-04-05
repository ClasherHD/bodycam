package dev.ClasherHD.bodycam.config;

public class ModServerConfig {
    public static final ConfigValue<Integer> MAX_MONITOR_DISTANCE = new ConfigValue<>(500);
    public static final ConfigValue<Boolean> ENABLE_REACH_ENCHANTMENT = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_JAMMER = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_DIMENSION_LOCATOR = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_HOLOGRAM_BLOCK = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_ANONYMIZER = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> OP_ONLY_MODE = new ConfigValue<>(false);

    public static void load() {
        java.io.File file = new java.io.File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(),
                "bodycam-server.json");
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        try {
            if (file.exists()) {
                com.google.gson.JsonObject json = gson.fromJson(new java.io.FileReader(file),
                        com.google.gson.JsonObject.class);
                if (json.has("maxMonitorDistance"))
                    MAX_MONITOR_DISTANCE.set(json.get("maxMonitorDistance").getAsInt());
                if (json.has("enableReachEnchantment"))
                    ENABLE_REACH_ENCHANTMENT.set(json.get("enableReachEnchantment").getAsBoolean());
                if (json.has("enableJammer"))
                    ENABLE_JAMMER.set(json.get("enableJammer").getAsBoolean());
                if (json.has("enableDimensionLocator"))
                    ENABLE_DIMENSION_LOCATOR.set(json.get("enableDimensionLocator").getAsBoolean());
                if (json.has("enableHologramBlock"))
                    ENABLE_HOLOGRAM_BLOCK.set(json.get("enableHologramBlock").getAsBoolean());
                if (json.has("enableAnonymizer"))
                    ENABLE_ANONYMIZER.set(json.get("enableAnonymizer").getAsBoolean());
                if (json.has("opOnlyMode"))
                    OP_ONLY_MODE.set(json.get("opOnlyMode").getAsBoolean());
            } else {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("maxMonitorDistance", MAX_MONITOR_DISTANCE.get());
                obj.addProperty("enableReachEnchantment", ENABLE_REACH_ENCHANTMENT.get());
                obj.addProperty("enableJammer", ENABLE_JAMMER.get());
                obj.addProperty("enableDimensionLocator", ENABLE_DIMENSION_LOCATOR.get());
                obj.addProperty("enableHologramBlock", ENABLE_HOLOGRAM_BLOCK.get());
                obj.addProperty("enableAnonymizer", ENABLE_ANONYMIZER.get());
                obj.addProperty("opOnlyMode", OP_ONLY_MODE.get());
                java.io.FileWriter writer = new java.io.FileWriter(file);
                gson.toJson(obj, writer);
                writer.close();
            }
        } catch (Exception e) {
        }
    }

    public static void save() {
        java.io.File file = new java.io.File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(),
                "bodycam-server.json");
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        try {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("maxMonitorDistance", MAX_MONITOR_DISTANCE.get());
            obj.addProperty("enableReachEnchantment", ENABLE_REACH_ENCHANTMENT.get());
            obj.addProperty("enableJammer", ENABLE_JAMMER.get());
            obj.addProperty("enableDimensionLocator", ENABLE_DIMENSION_LOCATOR.get());
            obj.addProperty("enableHologramBlock", ENABLE_HOLOGRAM_BLOCK.get());
            obj.addProperty("enableAnonymizer", ENABLE_ANONYMIZER.get());
            obj.addProperty("opOnlyMode", OP_ONLY_MODE.get());
            java.io.FileWriter writer = new java.io.FileWriter(file);
            gson.toJson(obj, writer);
            writer.close();
        } catch (Exception e) {
        }
    }

    public static class ConfigValue<T> {
        private T value;

        public ConfigValue(T def) {
            this.value = def;
        }

        public T get() {
            return value;
        }

        public void set(T val) {
            this.value = val;
        }
    }
}
