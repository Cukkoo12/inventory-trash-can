package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;

public class InstantLeafDecayConfig {

    public static int DECAY_TICKS = 2;      // Kaç tick sonra düşsün
    public static boolean PARTICLES = true;  // Parçacık efekti
    public static boolean SOUND = true;      // Ses efekti

    private static final Path CONFIG_PATH = Paths.get("config/instant_leaf_decay.json");    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                Reader reader = Files.newBufferedReader(CONFIG_PATH);
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                reader.close();
                DECAY_TICKS = Math.max(1, data.decayTicks);
                PARTICLES = data.particles;
                SOUND = data.sound;
            } else {
                save(); // İlk çalıştırmada default config oluştur
            }
        } catch (IOException e) {
            System.err.println("[HizliYaprak] Config yüklenemedi: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Writer writer = Files.newBufferedWriter(CONFIG_PATH);
            GSON.toJson(new ConfigData(), writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[HizliYaprak] Config kaydedilemedi: " + e.getMessage());
        }
    }

    private static class ConfigData {
        int decayTicks = DECAY_TICKS;
        boolean particles = PARTICLES;
        boolean sound = SOUND;
    }
}