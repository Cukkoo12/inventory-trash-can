package com.cukkoo.inventorytrashcan.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    public int slotX = 0;
    public int slotY = 0;

    public static ModConfig load(Path configDir) {
        Path configPath = configDir.resolve("inventory_trash_can.json");
        try {
            if (Files.exists(configPath))
                return new Gson().fromJson(Files.readString(configPath), ModConfig.class);
        } catch (Exception e) { /* ignore */ }
        ModConfig config = new ModConfig();
        config.save(configDir);
        return config;
    }

    public void save(Path configDir) {
        try {
            Files.createDirectories(configDir);
            Files.writeString(configDir.resolve("inventory_trash_can.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (Exception e) { /* ignore */ }
    }
}
