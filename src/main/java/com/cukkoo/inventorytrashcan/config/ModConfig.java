package com.cukkoo.inventorytrashcan.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    public int slotX = 0;
    public int slotY = 0;

    public static ModConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("inventory_trash_can.json");
        try {
            if (Files.exists(configPath))
                return new Gson().fromJson(Files.readString(configPath), ModConfig.class);
        } catch (Exception e) { /* ignore */ }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("inventory_trash_can.json");
            Files.writeString(configPath,
                new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (Exception e) { /* ignore */ }
    }
}
