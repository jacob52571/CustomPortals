package com.jacob5257.CustomPortals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.lang.*;

public class CustomPortals extends JavaPlugin implements Listener {
    public static boolean DEBUG_MODE = true;

    // TODO implement non-flint and steel activation
//    public static boolean PORTALS_REQUIRE_ACTIVATION = true;

    private HashMap<String, ArrayList<Portal>> worldPortals = new HashMap<>();
    //todo find proper class
    private HashMap<String, MaterialData> portalMaterials = new HashMap<>();
    private HashMap<String, Double> worldScales = new HashMap<>();

    private FileConfiguration config;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadConfig();
        loadPortals();
        this.getServer().getPluginManager().registerEvents(this, this);
        //load
        getServer().getConsoleSender().sendMessage("CustomPortals has been enabled!");
    }

    @Override
    public void onDisable() {
        savePortals();
        getServer().getConsoleSender().sendMessage("CustomPortals has been disabled!");
    }

    public static void log(Object message) {
        if (DEBUG_MODE) System.out.println("[CustomPortals] " + message);
    }

    private void loadConfig() {
        ConfigurationSection portalsConfig = config.getConfigurationSection("portal-materials");
        Set<String> portalWorlds = portalsConfig.getKeys(false);
        for (String world : portalWorlds) {
            // todo check if this is the right way to do this
            MaterialData material = getMaterialData(portalsConfig.getString(world));
            portalMaterials.put(world, material);
            worldPortals.put(world, new ArrayList<>());
        }

        ConfigurationSection worldScaleConfig = config.getConfigurationSection("world-scale");
        Set<String> scaleWorlds = worldScaleConfig.getKeys(false);
        for (String world : scaleWorlds) {
            double worldScale = worldScaleConfig.getDouble(world);
            worldScales.put(world, worldScale);
        }

        Portal.PORTAL_MAX_DISTANCE = config.getInt("portal-search-range", 64);
        Portal.PORTAL_MAX_DISTANCE_SQUARED = Portal.PORTAL_MAX_DISTANCE * Portal.PORTAL_MAX_DISTANCE;
        Portal.PORTAL_CREATION_RANGE = config.getInt("portal-creation-range", 24);
//        PORTALS_REQUIRE_ACTIVATION = config.getBoolean("require-activation", true);
        DEBUG_MODE = config.getBoolean("debug-mode", false);
        log("Debug mode enabled.");
    }

    private void loadPortals() {
        File portalsFile = new File(getDataFolder(), "portals.json");
        try {
            portalsFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(portalsFile))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                String[] parts = currentLine.split(";;");
                String portalWorldString = parts[0];
                String targetWorldString = parts[1];
                String startLocationString = parts[2];
                String endLocationString = parts[3];

                World portalWorld = Bukkit.getWorld(portalWorldString);
                Location startLocation = parseLocation(portalWorld, startLocationString);
                Location endLocation = parseLocation(portalWorld, endLocationString);

                int modX = startLocation.getBlockX() == endLocation.getBlockX() ? 0 : 1;
                int modZ = startLocation.getBlockZ() == endLocation.getBlockZ() ? 0 : 1;

                Block space = startLocation.getBlock();

                Block[][] portalBlocks = new Block[4][5];
                for (int y = 0; y < 5; y++) {
                    for (int x = 0; x < 4; x++) {
                        portalBlocks[x][y] = space.getRelative(modX * x, y, modZ * x);
                    }
                }

//                log("Target start location: " + startLocation);
//                log("Result start location: " + portalBlocks[0][0].getLocation());

//                log("Target end location: " + endLocation);
//                log("Result end location: " + portalBlocks[3][4].getLocation());

                Portal portal = new Portal(portalWorldString, targetWorldString, portalBlocks);
                if (isPortalValid(portal)) {
                    ArrayList<Portal> portals = worldPortals.get(portalWorldString);
                    portals.add(portal);
                    worldPortals.put(portalWorldString, portals);
                    portal.ignitePortal();

                    log("Portal remains valid: " + portal.toString());
                } else {
                    log("Portal no longer valid: " + portal.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MaterialData getMaterialData(Block block) {
        return new MaterialData(block.getType(), block.getData());
    }

    private MaterialData getMaterialData(String materialData) {
        String[] parts = materialData.split(":");
        return new MaterialData(Integer.parseInt(parts[0]), Byte.parseByte(parts[1]));
    }

    private void savePortals() {
        File portalsFile = new File(getDataFolder(), "portals.json");

        ArrayList<Portal> allPortals = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Portal>> entry : worldPortals.entrySet()) {
            allPortals.addAll(entry.getValue());
        }

        try (Writer writer = new FileWriter(portalsFile)) {
            StringBuilder builder = new StringBuilder();
            for (Portal portal : allPortals) {
                builder.append(portal.toString()).append("\n");
            }
            writer.write(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Location parseLocation(World world, String locationString) {
        String[] locationParts = locationString.split(";");
        double x = Double.parseDouble(locationParts[0]);
        double y = Double.parseDouble(locationParts[1]);
        double z = Double.parseDouble(locationParts[2]);
        return new Location(world, x, y, z);
    }

    private boolean isPortalValid(Portal portal) {
        Block[][] portalBlocks = portal.getPortalBlocks();

        Block[] outerPortalBlocks = new Block[]{
                portalBlocks[1][0], // Bottom middle
                portalBlocks[2][0],
                portalBlocks[0][1], // First layer
                portalBlocks[3][1],
                portalBlocks[0][2], // Second layer
                portalBlocks[3][2],
                portalBlocks[0][3], // Third layer
                portalBlocks[3][3],
                portalBlocks[1][4], // Top middle
                portalBlocks[2][4]
        };

        return areValidBlocks(outerPortalBlocks, getPortalMaterial(portal.getTargetWorld()));
    }

    private boolean areValidBlocks(Block[] blocks, MaterialData material) {
        for (Block block : blocks) {
            if (!isValidBlock(block, material)) return false;
        }
        return true;
    }

    private boolean isValidBlock(Block block, MaterialData material) {
        return material.equals(getMaterialData(block));
    }

    private MaterialData getPortalMaterial(String targetWorld) {
        return portalMaterials.get(targetWorld);
    }
}
