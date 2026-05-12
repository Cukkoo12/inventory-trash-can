package com.cukkoo.inventorytrashcan.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("inventory_trash_can.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int slotX = 0;
    public int slotY = 0;

    public static ModConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return GSON.fromJson(Files.readString(CONFIG_PATH), ModConfig.class);
            }
        } catch (Exception ignored) {
        }
        ModConfig def = new ModConfig();
        def.save();
        return def;
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (Exception ignored) {
        }
    }
}
