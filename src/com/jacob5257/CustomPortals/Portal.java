package com.jacob5257.CustomPortals;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import static com.jacob5257.CustomPortals.CustomPortals.log;

public class Portal {
    public static int PORTAL_MAX_DISTANCE = 64;
    public static int PORTAL_MAX_DISTANCE_SQUARED = PORTAL_MAX_DISTANCE * PORTAL_MAX_DISTANCE;
    public static int PORTAL_CREATION_RANGE = 24;

    private String portalWorld;
    private String targetWorld;

    private Location startLocation;
    private Location endLocation;

    private Location centerLocation;

    private Block[][] portalBlocks;

    public Portal(String portalWorld, String targetWorld, Block[][] portalBlocks) {
        this.portalWorld = portalWorld;
        this.targetWorld = targetWorld;
        this.startLocation = portalBlocks[0][0].getLocation();
        this.endLocation = portalBlocks[3][4].getLocation();
        this.portalBlocks = portalBlocks;

        centerLocation = startLocation.clone().add(endLocation.clone().subtract(startLocation).multiply(0.5d));
    }

    public void ignitePortal() {
        Block[] innerPortalBlocks = new Block[]{
                portalBlocks[1][1],
                portalBlocks[2][1],
                portalBlocks[1][2],
                portalBlocks[2][2],
                portalBlocks[1][3],
                portalBlocks[2][3]
        };

        boolean northSouth = portalBlocks[0][0].getX() == portalBlocks[1][0].getX();

        // Create 'portal' within portal
        for (Block teleportBlock : innerPortalBlocks) {
            teleportBlock.setType(Material.NETHER_PORTAL, false);
            Orientable directional = (Orientable) teleportBlock.getBlockData();
            if (northSouth) {
            	directional.setAxis(Axis.Z);
            } else {
            	directional.setAxis(Axis.X);
            }
            teleportBlock.setBlockData(directional);
        }
        Block base1 = portalBlocks[1][0];
        Block base2 = portalBlocks[2][0];
        if (northSouth) {
        	spawnSafePlatform(base1, BlockFace.NORTH, BlockFace.SOUTH);
            spawnSafePlatform(base2, BlockFace.NORTH, BlockFace.SOUTH);
        }
        else {
            spawnSafePlatform(base1, BlockFace.EAST, BlockFace.WEST);
            spawnSafePlatform(base2, BlockFace.EAST, BlockFace.WEST);
        }
    }

    private static void spawnSafePlatform(Block base1, BlockFace blockFace1, BlockFace blockFace2) {
        if (base1.getRelative(blockFace1).getType().equals(Material.LAVA) || base1.getRelative(blockFace1).getType().equals(Material.AIR)) {
            // negative Z
            Block needBlockHere = base1.getRelative(blockFace1);
            needBlockHere.setBlockData(base1.getBlockData());
        }
        if (base1.getRelative(blockFace2).getType().equals(Material.LAVA) || base1.getRelative(blockFace2).getType().equals(Material.AIR)) {
            // negative Z
            Block needBlockHere = base1.getRelative(blockFace2);
            needBlockHere.setBlockData(base1.getBlockData());
        }
    }

    public void teleportPlayer(Player player) {
        log("Teleporting " + player.getName() + " to " + getPortalWorld());
        player.teleport(portalBlocks[2][3].getLocation().clone().add(0, 0, 0), TeleportCause.PLUGIN);
    }

    public String getPortalWorld() {
        return portalWorld;
    }

    public String getTargetWorld() {
        return targetWorld;
    }

    public Block[][] getPortalBlocks() {
        return portalBlocks;
    }

    public Location getLocation() {
        return centerLocation;
    }

    public double getDistance(Location location) {
        location = location.clone();
        location.setY(centerLocation.getY());
        double distance = this.centerLocation.distanceSquared(location);
        return distance <= PORTAL_MAX_DISTANCE_SQUARED ? distance : -1;
    }

    public String toString() {
        return portalWorld + ";;" + targetWorld + ";;" + toString(startLocation) + ";;" + toString(endLocation) + ";;";
    }

    private String toString(Location location) {
        return location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }
}
