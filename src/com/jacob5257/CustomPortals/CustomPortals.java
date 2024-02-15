package com.jacob5257.CustomPortals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.lang.*;

public class CustomPortals extends JavaPlugin implements Listener {
    public static boolean DEBUG_MODE = true;

    // TODO implement non-flint and steel activation
//    public static boolean PORTALS_REQUIRE_ACTIVATION = true;

    private HashMap<String, ArrayList<Portal>> worldPortals = new HashMap<>();
    private HashMap<String, BlockData> portalMaterials = new HashMap<>();
    private HashMap<String, Double> worldScales = new HashMap<>();

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadConfig();
        loadPortals();
    
        this.getServer().getPluginManager().registerEvents(this, this);

        getServer().getConsoleSender().sendMessage("CustomPortals has been enabled.");
    }

    private void loadConfig() {
        ConfigurationSection portalsConfig = config.getConfigurationSection("portal-materials");
        if (portalsConfig == null) {
        	getServer().getConsoleSender().sendMessage("[CustomPortals] Invalid portal material in configuration file!");
        	System.exit(0);
        }
        Set<String> portalWorlds = portalsConfig.getKeys(false);
        for (String world : portalWorlds) {
        	if (portalsConfig.getString(world) == null) {
        		getServer().getConsoleSender().sendMessage("[CustomPortals] Invalid world in configuration file!");
        		System.exit(0);
        	}
        	String worldString = portalsConfig.getString(world);
            BlockData material = getBlockData(worldString);
            portalMaterials.put(world, material);
            worldPortals.put(world, new ArrayList<>());
        }

        ConfigurationSection worldScaleConfig = config.getConfigurationSection("world-scale");
        if (worldScaleConfig == null) {
        	getServer().getConsoleSender().sendMessage("[CustomPortals] Invalid world scale in configuration file!");
        	System.exit(0);
        }
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

    @Override
    public void onDisable() {
        savePortals();
        getServer().getConsoleSender().sendMessage("CustomPortals has been disabled.");
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

                log("Target start location: " + startLocation);
                log("Result start location: " + portalBlocks[0][0].getLocation());

                log("Target end location: " + endLocation);
                log("Result end location: " + portalBlocks[3][4].getLocation());

                Portal portal = new Portal(portalWorldString, targetWorldString, portalBlocks);
                if (isPortalValid(portal)) {
                    ArrayList<Portal> portals = worldPortals.get(portalWorldString);
                    portals.add(portal);
                    worldPortals.put(portalWorldString, portals);
                    portal.ignitePortal();

                    log("Portal remains valid: " + portal);
                } else {
                    log("Portal no longer valid: " + portal);
                }
            }
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

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        Location sourceLocation = event.getFrom();
        Portal sourcePortal = getNearestPortal(sourceLocation, null);

        if (sourcePortal != null) {
            // Found a portal that they're stood at!
//            player.sendMessage("You are near: " + sourcePortal.getLocation());

            World world = Bukkit.getWorld(sourcePortal.getTargetWorld());
            if (world != null) {
                Location targetLocation = getWorldCoordinates(sourceLocation, world);

                Portal targetPortal = getNearestPortal(targetLocation, sourcePortal.getTargetWorld());
                if (targetPortal != null) {
                    targetPortal.teleportPlayer(player);
                } else {
                    // Create a portal?
                    targetPortal = createPortal(targetLocation, sourcePortal.getPortalWorld());

                    if (targetPortal != null) {
                        targetPortal.teleportPlayer(player);
                    } else {
                        player.sendMessage("Could not create a portal.");
                        return;
                    }
                }
            } else {
                player.sendMessage("World " + sourcePortal.getTargetWorld() + " is invalid.");
            }
        } else {
            // We don't know about this portal :(
            player.sendMessage("Confused :(");
        }
    }

    private Portal createPortal(Location targetLocation, String sourceWorldName) {
        log("Attempting to create a portal in " + targetLocation.getWorld().getName() + ", target location: " + targetLocation);

        World targetWorld = targetLocation.getWorld();
        World sourceWorld = Bukkit.getWorld(sourceWorldName);

        BlockData BlockData = getPortalMaterial(sourceWorldName);
        log("Portal should be constructed from " + BlockData + " to return to world " + sourceWorldName);

        int targetX = targetLocation.getBlockX();
        int targetZ = targetLocation.getBlockZ();

        int xAxis = 4;
        int yAxis = 4;
        int zAxis = 3;

        log(targetWorld.getHighestBlockAt(targetLocation).getType());
        boolean worldHasRoof = targetWorld.getHighestBlockAt(targetLocation).getType() == Material.BEDROCK;
        if (worldHasRoof) {
            log("The world has a roof, finding block manually");

            // Find the space manually
            int targetMaxHeight = targetWorld.getEnvironment() == World.Environment.NETHER ? 127 : targetWorld.getMaxHeight() - 1;

            boolean found = false;
            Block portalBase = null;
            for (int portalY = 0; portalY < targetMaxHeight - yAxis; portalY++) {
                portalBase = targetWorld.getBlockAt(targetX, portalY, targetZ);
                if (testLoc(portalBase)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log("No space found to create portal (manual method) :(");
                return null;
            }

            return getPortal(targetWorld, sourceWorld, BlockData, portalBase);
        } else {
            log("Finding location via Bukkit");

            // Ask Bukkit
            Block highestBlock = targetWorld.getHighestBlockAt(targetLocation);
            Block space = highestBlock.getRelative(0, 1, 0);

            if (highestBlock.getY() > targetWorld.getMaxHeight() - 5) {
                // No space :(
                return null;
            }

            int srcMod = -Portal.PORTAL_CREATION_RANGE;
            boolean searching = true;
            while (searching && srcMod < 13) {
                Block searchLoc = space.getRelative(srcMod, 0, srcMod);
                if (!searchLoc.isEmpty()) {
                    srcMod++;
                    continue;
                }

                for (int x = 0; x < xAxis; x++) {
                    for (int y = 0; y < yAxis; y++) {
                        for (int z = 0; z < zAxis; z++) {
                            Block portalSearchLoc = searchLoc.getRelative(x, y, z);
                            if (!portalSearchLoc.isEmpty()) {
                                srcMod++;
                            }
                        }
                    }
                }

                space = searchLoc;
                searching = false;
            }

            if (srcMod == 13) {
                // Couldn't find a space :(
                return null;
            }

            return getPortal(targetWorld, sourceWorld, BlockData, space);
        }
    }

    private Portal getPortal(World targetWorld, World sourceWorld, BlockData BlockData, Block space) {
        Block[][] portalBlocks = new Block[4][5];
        portalBlocks[0][0] = space.getRelative(0, -1, 0);
        portalBlocks[1][0] = space.getRelative(1, -1, 0);
        portalBlocks[2][0] = space.getRelative(2, -1, 0);
        portalBlocks[3][0] = space.getRelative(3, -1, 0);

        portalBlocks[0][1] = space.getRelative(0, 0, 0);
        portalBlocks[1][1] = space.getRelative(1, 0, 0);
        portalBlocks[2][1] = space.getRelative(2, 0, 0);
        portalBlocks[3][1] = space.getRelative(3, 0, 0);

        portalBlocks[0][2] = space.getRelative(0, 1, 0);
        portalBlocks[1][2] = space.getRelative(1, 1, 0);
        portalBlocks[2][2] = space.getRelative(2, 1, 0);
        portalBlocks[3][2] = space.getRelative(3, 1, 0);

        portalBlocks[0][3] = space.getRelative(0, 2, 0);
        portalBlocks[1][3] = space.getRelative(1, 2, 0);
        portalBlocks[2][3] = space.getRelative(2, 2, 0);
        portalBlocks[3][3] = space.getRelative(3, 2, 0);

        portalBlocks[0][4] = space.getRelative(0, 3, 0);
        portalBlocks[1][4] = space.getRelative(1, 3, 0);
        portalBlocks[2][4] = space.getRelative(2, 3, 0);
        portalBlocks[3][4] = space.getRelative(3, 3, 0);

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

        for (Block block : outerPortalBlocks) {
            block.setType(block.getType());
            //todo see what this does
            //block.setData(BlockData.getData(), false);
        }

        Portal targetPortal = new Portal(targetWorld.getName(), sourceWorld.getName(), portalBlocks);
        log("Created portal in " + targetWorld.getName());
        ArrayList<Portal> portals = worldPortals.get(targetWorld.getName());
        portals.add(targetPortal);
        worldPortals.put(targetWorld.getName(), portals);
        log(worldPortals.get(targetWorld.getName()));

        targetPortal.ignitePortal();
        return targetPortal;
    }

    private boolean testLoc(Block portalBase) {
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 3; z++) {
                    Block testBlock = portalBase.getRelative(x, y, z);
                    if (!testBlock.isEmpty()) return false;
                }
            }
        }

        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getItem() == null) return;
        if (!(event.getItem().getType() == Material.FLINT_AND_STEEL)) return;
        if (!player.hasPermission("customportals.create")) return;

        Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getRelative(BlockFace.DOWN);
        String targetWorld = getPortalTarget(block);
        if (targetWorld == null) return;
        BlockData material = getPortalMaterial(targetWorld);

        boolean down = isValidBlock(block, material);

        // Makes calculations easier!
        // Only allow lighting the portal from the bottom blocks
        if (down) {
            Block[][] portalBlocks = new Block[4][5];

            Block portalBaseNorth = block.getRelative(BlockFace.NORTH);
            Block portalBaseEast = block.getRelative(BlockFace.EAST);
            Block portalBaseSouth = block.getRelative(BlockFace.SOUTH);
            Block portalBaseWest = block.getRelative(BlockFace.WEST);

            boolean north = isValidBlock(portalBaseNorth, material);
            boolean east = isValidBlock(portalBaseEast, material);
            boolean south = isValidBlock(portalBaseSouth, material);
            boolean west = isValidBlock(portalBaseWest, material);

            if (north || south) {
                Block portalNorthAir = portalBaseNorth.getRelative(BlockFace.UP);
                Block portalSouthAir = portalBaseSouth.getRelative(BlockFace.UP);

                if (portalNorthAir.getType() == Material.AIR) {
                    // There is also a space for the portal to the north!
                    portalBlocks[0][0] = portalBaseNorth.getRelative(BlockFace.NORTH);
                    portalBlocks[1][0] = portalBaseNorth;
                    portalBlocks[2][0] = block;
                    portalBlocks[3][0] = block.getRelative(BlockFace.SOUTH);

                } else if (portalSouthAir.getType() == Material.AIR) {
                    // There is also a space for the portal to the south!
                    portalBlocks[0][0] = portalBaseNorth;
                    portalBlocks[1][0] = block;
                    portalBlocks[2][0] = portalBaseSouth;
                    portalBlocks[3][0] = portalBaseSouth.getRelative(BlockFace.SOUTH);

                } else {
                    // The only "air" (fire) was the flint and steel just placed
                    player.sendMessage("Not a portal 4 :(");
                    return;
                }

            } else if (west || east) {
                Block portalWestAir = portalBaseWest.getRelative(BlockFace.UP);
                Block portalEastAir = portalBaseEast.getRelative(BlockFace.UP);

                if (portalWestAir.getType() == Material.AIR) {
                    // There is also a space for the portal to the west!
                    portalBlocks[0][0] = portalBaseWest.getRelative(BlockFace.WEST);
                    portalBlocks[1][0] = portalBaseWest;
                    portalBlocks[2][0] = block;
                    portalBlocks[3][0] = portalBaseEast;

                } else if (portalEastAir.getType() == Material.AIR) {
                    // There is also a space for the portal to the east!
                    portalBlocks[0][0] = portalBaseWest;
                    portalBlocks[1][0] = block;
                    portalBlocks[2][0] = portalBaseEast;
                    portalBlocks[3][0] = portalBaseEast.getRelative(BlockFace.EAST);

                } else {
                    // The only "air" (fire) was the flint and steel just placed
                    player.sendMessage("Not a portal 3 :(");
                    return;
                }

            } else {
                player.sendMessage("Not a portal 2 :(");
                return;
            }

            portalBlocks[0][1] = portalBlocks[0][0].getRelative(BlockFace.UP);
            portalBlocks[1][1] = portalBlocks[1][0].getRelative(BlockFace.UP);
            portalBlocks[2][1] = portalBlocks[2][0].getRelative(BlockFace.UP);
            portalBlocks[3][1] = portalBlocks[3][0].getRelative(BlockFace.UP);

            portalBlocks[0][2] = portalBlocks[0][1].getRelative(BlockFace.UP);
            portalBlocks[1][2] = portalBlocks[1][1].getRelative(BlockFace.UP);
            portalBlocks[2][2] = portalBlocks[2][1].getRelative(BlockFace.UP);
            portalBlocks[3][2] = portalBlocks[3][1].getRelative(BlockFace.UP);

            portalBlocks[0][3] = portalBlocks[0][2].getRelative(BlockFace.UP);
            portalBlocks[1][3] = portalBlocks[1][2].getRelative(BlockFace.UP);
            portalBlocks[2][3] = portalBlocks[2][2].getRelative(BlockFace.UP);
            portalBlocks[3][3] = portalBlocks[3][2].getRelative(BlockFace.UP);

            portalBlocks[0][4] = portalBlocks[0][3].getRelative(BlockFace.UP);
            portalBlocks[1][4] = portalBlocks[1][3].getRelative(BlockFace.UP);
            portalBlocks[2][4] = portalBlocks[2][3].getRelative(BlockFace.UP);
            portalBlocks[3][4] = portalBlocks[3][3].getRelative(BlockFace.UP);

            log(portalBlocks[0][0].getLocation());
            log(portalBlocks[3][4].getLocation());

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

            boolean isPortal = areValidBlocks(outerPortalBlocks, material);
            if (isPortal) {
                player.sendMessage(targetWorld + " portal created!");

                // Create the portal object!
                Portal portal = new Portal(player.getWorld().getName(), targetWorld, portalBlocks);
                ArrayList<Portal> portals = worldPortals.get(player.getWorld().getName());
                portals.add(portal);
                worldPortals.put(player.getWorld().getName(), portals);

                portal.ignitePortal();

                log("Portal created: " + portal);
            } else {
                player.sendMessage("An error occured.");
            }
        }
    }

    private Location getWorldCoordinates(Location location, World targetWorld) {
        World currentWorld = location.getWorld();

        double originalY = location.getY();
        double currentWorldScale = worldScales.get(currentWorld.getName());
        double targetWorldScale = worldScales.get(targetWorld.getName());

        location = location.clone().multiply(1D / currentWorldScale);
        location.multiply(targetWorldScale);
        location.setY(originalY);
        location.setWorld(targetWorld);

        return location;
    }

    private Portal getNearestPortal(Location location, String destinationWorld) {
        World world = location.getWorld();

        ArrayList<Portal> portals = worldPortals.get(world.getName());
        log("Searching for portals in " + world.getName());
        log(portals);
        double minDistance = 0;
        Portal closestPortal = null;
        for (Portal portal : portals) {
            if (destinationWorld != null && !portal.getPortalWorld().equals(destinationWorld)) continue;
            double distance = portal.getDistance(location);
            if (distance < 0 || distance > minDistance) continue;
            closestPortal = portal;
            minDistance = distance;
        }
        log("No portal found");
        return closestPortal;
    }

    public static void log(Object message) {
        if (DEBUG_MODE) System.out.println("[CustomPortals] " + message);
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

    private boolean areValidBlocks(Block[] blocks, BlockData material) {
        for (Block block : blocks) {
            if (!isValidBlock(block, material)) return false;
        }
        return true;
    }

    private boolean isValidBlock(Block block, BlockData material) {
        return material.equals(getBlockData(block));
    }

    private String getPortalTarget(Block block) {
        BlockData BlockData = getBlockData(block);
        for (Map.Entry<String, BlockData> entry : portalMaterials.entrySet()) {
            if (entry.getValue().equals(BlockData)) return entry.getKey();
        }
        return null;
    }

    private BlockData getPortalMaterial(String targetWorld) {
        return portalMaterials.get(targetWorld);
    }

    private BlockData getBlockData(Block block) {
        return block.getBlockData();
    }

    private BlockData getBlockData(String blockData) {
        return Bukkit.createBlockData(blockData);
    }
}
