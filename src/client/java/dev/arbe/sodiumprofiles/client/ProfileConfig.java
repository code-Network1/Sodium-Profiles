package dev.arbe.sodiumprofiles.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class ProfileConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "sodium-profiles.json";

    private static ProfileConfig instance;

    public ProfileType selectedProfile = ProfileType.BALANCED;
    public String activeProfileName = null;
    public Map<String, ProfileSettings> customProfiles = new LinkedHashMap<>();

    public static ProfileConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ProfileConfig load() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ProfileConfig config = GSON.fromJson(reader, ProfileConfig.class);
                if (config != null) {
                    if (config.customProfiles == null) {
                        config.customProfiles = new LinkedHashMap<>();
                    }
                    return config;
                }
            } catch (Exception e) {
                // Fall through to default
            }
        }
        return new ProfileConfig();
    }

    public void save() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save sodium-profiles config", e);
        }
    }

    public void addCustomProfile(String name, ProfileSettings settings) {
        customProfiles.put(name, settings);
    }

    public void removeCustomProfile(String name) {
        customProfiles.remove(name);
        if (name.equals(activeProfileName)) {
            activeProfileName = null;
        }
    }

    public ProfileSettings getCustomProfile(String name) {
        return customProfiles.get(name);
    }

    public Set<String> getCustomProfileNames() {
        return customProfiles.keySet();
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
