package net.andrewcpu.portals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.block.data.Directional;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Portal {
    private Location center;
    private Location topLeft;
    private Location bottomRight;

    public PortalDirection direction;

    private List<Location> cache = new ArrayList<>();

    public Portal(Location center, Location topLeft, Location bottomRight) {
        this.center = center;
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        this.direction = topLeft.getZ() == bottomRight.getZ() ? PortalDirection.NORTH_SOUTH : PortalDirection.EAST_WEST;
        buildCache();
    }

    List<BlockFace> faces = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    public boolean touchesAir(Location location) {
        if (location.getBlock().getType() == Material.AIR || location.getBlock().getType().isInteractable()) {
            return true;
        }
        for (BlockFace face : faces) {
            if (!location.getBlock().getRelative(face).getType().isOccluding()) {
                return true;
            }
        }
        return false;
    }

    private int cacheSize = 12;

    public Location getCacheMin() {
        Location start = getCenter().clone();
        Location min = start.clone().subtract(cacheSize, cacheSize, cacheSize);
        return min;
    }

    public Location getCacheMax() {
        Location start = getCenter().clone();
        Location max = start.clone().add(cacheSize, cacheSize, cacheSize);
        return max;
    }

    public boolean between(Location a, Location mid, Location b) {
        double minX = Math.min(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxX = Math.max(a.getX(), b.getX());
        double maxY = Math.max(a.getY(), b.getY());
        double maxZ = Math.max(a.getZ(), b.getZ());
        if (mid.getX() >= minX && mid.getX() <= maxX) {
            if (mid.getY() >= minY && mid.getY() <= maxY) {
                if (mid.getZ() >= minZ && mid.getZ() <= maxZ) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInCache(Location location) {
        return between(getCacheMin(), location, getCacheMax());
    }

    public void buildCache() {
        Location start = getCenter().clone();
        List<Location> blocks = blocksFromTwoPoints(getCacheMin(), getCacheMax());
        blocks = blocks.stream().filter(this::touchesAir).collect(Collectors.toList());
        this.cache = blocks;
        System.out.println("Cache size " + cache.size());
    }

    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public Location getTopLeft() {
        return topLeft;
    }

    public void setTopLeft(Location topLeft) {
        this.topLeft = topLeft;
    }

    public Location getBottomRight() {
        return bottomRight;
    }

    public void setBottomRight(Location bottomRight) {
        this.bottomRight = bottomRight;
    }

    public double getRotationAngle(Portal from) {
        double rotationAngle = from.direction == PortalDirection.NORTH_SOUTH ? Math.PI / 2 : -Math.PI / 2;
        if (from.direction == direction)
            return 0;
        return rotationAngle;
    }

    public boolean isBlockFrame(Block block) {
        if (direction == PortalDirection.EAST_WEST) {
            if (block.getLocation().distance(getCenter()) <= 4) {
                if (block.getX() == getCenter().getBlockX()) {
                    return true;
                }
            }
        } else if (direction == PortalDirection.NORTH_SOUTH) {
            if (block.getLocation().distance(getCenter()) <= 4) {
                if (block.getZ() == getCenter().getBlockZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Location accept(Portal from, Player player, Location location, Vector vector, double steps, int maxSteps) {
        double step = steps;
        Block hit = null;

//        RayTraceResult result = player.getWorld().rayTraceBlocks(location, vector, (maxSteps - step));

        Location position = location.clone();
        for (double i = steps; i < maxSteps; i++) {
            position = position.add(vector.normalize());
            if (isBlockFrame(position.getBlock())) {
                continue;
            } else if (position.getBlock().getType() != Material.AIR) {
                hit = position.getBlock();
                break;
            }
        }

//        if (result != null && result.getHitBlock() != null) {
//            hit = result.getHitBlock();
//        }
////        while(step <= maxSteps){
////            if(now.getBlock().getType() != Material.AIR){
////                hit = now.getBlock();
////                break;
////            }
////            now = now.add(vector);
////            step++;
////        }

//        hit =

        if (hit == null) {
            return null;
        }
//        //get relative block to get THIS center
        Vector direction = getCenter().toVector().subtract(hit.getLocation().toVector());
        Vector otherPerspective = this.direction != from.direction ? direction.rotateAroundY(getRotationAngle(from)) : direction.clone();
        Location location2 = from.getCenter().toVector().subtract(otherPerspective).toLocation(location.getWorld());
//        if(from.direction == PortalDirection.EAST_WEST){
//            location2 = location2.subtract(1,0,0);
//        }
//        else{
//            location2 = location2.subtract(0,0,1);
//        }
//        double differenceInCenterHeight = getCenter().getBlockY() - from.getCenter().getBlockY();
//        location2 = location2.add(0, differenceInCenterHeight, 0);
        BlockState state = hit.getState();
        BlockData data = state.getBlockData();
        if (data instanceof Directional directional) {
            if (from.direction != this.direction) {
                double rotAng = getRotationAngle(from);
                BlockFace rotation = directional.getFacing();

                if (rotAng > 0) {
                    if (rotation == BlockFace.EAST) {
                        rotation = BlockFace.NORTH;
                    } else if (rotation == BlockFace.WEST) {
                        rotation = BlockFace.SOUTH;
                    } else if (rotation == BlockFace.NORTH) {
                        rotation = BlockFace.WEST;
                    } else if (rotation == BlockFace.SOUTH) {
                        rotation = BlockFace.EAST;
                    }
                } else {
                    if (rotation == BlockFace.EAST) {
                        rotation = BlockFace.SOUTH;
                    } else if (rotation == BlockFace.WEST) {
                        rotation = BlockFace.NORTH;
                    } else if (rotation == BlockFace.NORTH) {
                        rotation = BlockFace.EAST;
                    } else if (rotation == BlockFace.SOUTH) {
                        rotation = BlockFace.WEST;
                    }
                }

//                if (rotAng > 0) {
//                    rotation = BlockFace.valueOf(rotation.name().replaceAll("EAST", "NORTH"));
//                    rotation = BlockFace.valueOf(rotation.name().replaceAll("WEST", "SOUTH"));
//                } else {
//                    rotation = BlockFace.valueOf(rotation.name().replaceAll("WEST", "NORTH"));
//                    rotation = BlockFace.valueOf(rotation.name().replaceAll("EAST", "SOUTH"));
//                }

                directional.setFacing(rotation);
//                state.setBlockData((BlockData)directional);
            }
        } else {
        }
        player.sendBlockChange(location2, data);
        return location2;
    }


    public HashMap<Player, List<Location>> hiddenBlocks = new HashMap<>();

    public void unhideAll(Player player) {
        if (!hiddenBlocks.containsKey(player)) return;
        List<Location> locations = hiddenBlocks.get(player);
        locations.forEach(location -> player.sendBlockChange(location, location.getBlock().getBlockData()));
        hiddenBlocks.remove(player);
    }

    public boolean isInPortal(Location location) {
        double smallX = Math.min(getTopLeft().getBlockX(), getBottomRight().getBlockX());
        double smallY = Math.min(getTopLeft().getBlockY(), getBottomRight().getBlockY());
        double smallZ = Math.min(getTopLeft().getBlockZ(), getBottomRight().getBlockZ());
        double maxX = Math.max(getTopLeft().getBlockX(), getBottomRight().getBlockX());
        double maxY = Math.max(getTopLeft().getBlockY(), getBottomRight().getBlockY());
        double maxZ = Math.max(getTopLeft().getBlockZ(), getBottomRight().getBlockZ());
        if (location.getBlockX() >= smallX && location.getBlockX() <= maxX) {
            if (location.getBlockY() >= smallY && location.getBlockY() <= maxY) {
                if (location.getBlockZ() >= smallZ && location.getBlockZ() <= maxZ) {
                    return true;
                }
            }
        }
        return false;
    }

    public Location getOppositeLocation(Location point, Portal to) {
        Vector direction = getCenter().toVector().add(new Vector(0.5, 0, 0.5)).subtract(point.toVector());
        Vector otherPerspective = this.direction != to.direction ? direction.rotateAroundY(getRotationAngle(to)) : direction.clone();
        Location location2 = to.getCenter().toVector().add(new Vector(0.5, 0, 0.5)).subtract(otherPerspective).toLocation(point.getWorld());

//        location2.setDirection(eyeLocation.getDirection().rotateAroundY(from.getRotationAngle(to)));
//        to.getRotationAngle(from);
        return location2;
    }


    public List<Location> blocksFromCache(Location loc1, Location loc2) {

        List<Location> blocks = new ArrayList<Location>();

        Block block = loc2.getBlock();
        Block block2 = loc1.getBlock();

        blocks = cache.stream().parallel().filter(i -> between(block2.getLocation(), i, block.getLocation())).map(Location::clone).collect(Collectors.toList());

//        for(Location location : cache){
//            if(between(block2.getLocation(), location, block.getLocation())){
//                blocks.add(location.clone());
//            }
//        }
//
//        double topBlockX = (loc1.getX() < loc2.getX() ? loc2.getX() : loc1.getX());
//        double bottomBlockX = (loc1.getX() > loc2.getX() ? loc2.getX() : loc1.getX());
//
//        double topBlockY = (loc1.getY() < loc2.getY() ? loc2.getY() : loc1.getY());
//        double bottomBlockY = (loc1.getY() > loc2.getY() ? loc2.getY() : loc1.getY());
//
//        double topBlockZ = (loc1.getZ() < loc2.getZ() ? loc2.getZ() : loc1.getZ());
//        double bottomBlockZ = (loc1.getZ() > loc2.getZ() ? loc2.getZ() : loc1.getZ());
//        for(double x = bottomBlockX; x <= topBlockX; x++)
//        {
//            for(double z = bottomBlockZ; z <= topBlockZ; z++)
//            {
//                for(double y = bottomBlockY; y <= topBlockY; y++)
//                {
////                    Location location = new Location(loc1.getWorld(), x, y, z);
//                    if(cache.contains(hashTest)){
//                        blocks.add(new Location(loc1.getWorld(), x, y, z));
//                    }
//                    else{
//                        System.out.println(hashTest + " +! ");
//                    }
//                    //                    if(this.cache.contains(location)){
////                        blocks.add(loc1);
////                    }
////                    if(!blocks.contains(location)){
////                        blocks.add(location);
////                    }
////                    Block block = loc1.getWorld().getBlockAt(new Location(loc1.getWorld(), x, y, z));
////                    if(blocks.contains(block.getLocation())){
////                        continue;
////                    }
////                    else{
////                        blocks.add(block.getLocation());
////                    }
//                }
//            }
//        }

        return blocks;
    }

    public List<Location> blocksFromTwoPoints(Location loc1, Location loc2) {
        List<Location> blocks = new ArrayList<Location>();

        double topBlockX = (loc1.getX() < loc2.getX() ? loc2.getX() : loc1.getX());
        double bottomBlockX = (loc1.getX() > loc2.getX() ? loc2.getX() : loc1.getX());

        double topBlockY = (loc1.getY() < loc2.getY() ? loc2.getY() : loc1.getY());
        double bottomBlockY = (loc1.getY() > loc2.getY() ? loc2.getY() : loc1.getY());

        double topBlockZ = (loc1.getZ() < loc2.getZ() ? loc2.getZ() : loc1.getZ());
        double bottomBlockZ = (loc1.getZ() > loc2.getZ() ? loc2.getZ() : loc1.getZ());


        for (double x = bottomBlockX; x <= topBlockX; x++) {
            for (double z = bottomBlockZ; z <= topBlockZ; z++) {
                for (double y = bottomBlockY; y <= topBlockY; y++) {
                    Block block = loc1.getWorld().getBlockAt(new Location(loc1.getWorld(), x, y, z));
                    if (blocks.contains(block.getLocation())) {
                        continue;
                    } else {
                        blocks.add(block.getLocation());
                    }
                }
            }
        }

        return blocks;
    }

    public void fillPortal(Player player) {
        List<Location> blocks = blocksFromTwoPoints(getTopLeft(), getBottomRight());
        BlockData blockData = getTopLeft().getBlock().getRelative(BlockFace.UP).getBlockData();
        for (Location b : blocks) {
            player.sendBlockChange(b, blockData);
        }
    }

    public void emptyPortal(Player player) {
        List<Location> blocks = blocksFromTwoPoints(getTopLeft(), getBottomRight());
//        BlockData blockData = getTopLeft().getBlock().getRelative(BlockFace.UP).getBlockData();
        for (Location b : blocks) {
            player.sendBlockChange(b, b.getBlock().getBlockData());
        }
    }

    public boolean isShowingToPlayer(Player player) {
        return hiddenBlocks.containsKey(player) ? hiddenBlocks.get(player).size() > 0 : false;
    }

    public Collection<Location> newAccept(Vector v1, Vector v2, double maxDistance, Portal from, Player player) {
        List<Location> locations = new ArrayList<>();
        Location startLeft = getTopLeft();
        Location startRight = getBottomRight();
        if (direction == PortalDirection.NORTH_SOUTH) {
            startLeft = startLeft.clone().add(new Vector(1.5, 1.5, 0.5));
            startRight = startRight.clone().add(new Vector(0, 0, 0.5));
        } else if (direction == PortalDirection.EAST_WEST) {
//            startLeft = startLeft.clone().subtract(new Vector(0.5,0,0.5));
//            startRight = startRight.clone().add(new Vector(0.5, 0, 0.5));
        }

        double lastDistance = -1;
        double d = 0.0;
        Vector normalizedV1 = v1.clone().normalize();
        Vector normalizedV2 = v2.clone().normalize();
        int heightRange = 3;
        int widthRange = 12;
        do {
            Location l1 = startLeft.clone().add(normalizedV1.clone().multiply(d));
            Location l2 = startRight.clone().add(normalizedV2.clone().multiply(d));
            l1 = new Location(l1.getWorld(), Math.min(l1.getX(), startLeft.getX() + widthRange), l1.getY(), Math.min(l1.getZ(), startLeft.getZ() + widthRange));
            l2 = new Location(l2.getWorld(), Math.min(l2.getX(), startRight.getX() + widthRange), l2.getY(), Math.min(l2.getZ(), startRight.getZ() + widthRange));

            if (l2.getBlockY() < startRight.getBlockY() - heightRange) {
                l2.setY(startRight.getBlockY() - heightRange);
            }
            if (l1.getBlockY() > startLeft.getBlockY() + heightRange) {
                l1.setY(startLeft.getBlockY() + heightRange);
            }
//            System.out.println(l1 + ", " + l2);
//            List<Location> blcks = ;
            locations.addAll(blocksFromCache(l1, l2).stream().parallel().filter(location -> !locations.contains(location)).toList());
//            for(Location location : blcks){
//                if(locations.contains(location)){
//                    continue;
//                }
//                locations.add(location);
//            }
//            System.out.println(blcks.size());
//            for(Location b : blcks){
////                if(locations.contains(b.getBlock().getLocation())){
////                    continue;
////                }
////                List<BlockFace> faces = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
////                boolean foundAir = false;
////                for(BlockFace face : faces){
////                    if(!b.getBlock().getRelative(face).getType().isOccluding() || b.getBlock().getType() == Material.AIR){
////                        foundAir = true;
////                        break;
////                    }
////                }
////                if(foundAir){
//                    locations.add(b.getBlock().getLocation());
////                }
//            }
            d++;
//            lastDistance = l1.distance(getTopLeft()) * 0.5 + l2.distance(getBottomRight()) * 0.5;
        } while (d < maxDistance);
//        for(double d = 0; d<maxDistance; d+=1){
////            System.out.println(v1.normalize().multiply(d));
//            Location l1 = startLeft.clone().add(v1.clone().normalize().multiply(d));
//            Location l2 = startRight.clone().add(v2.clone().normalize().multiply(d));
////            System.out.println(l1 + ", " + l2);
//            List<Block> blcks = blocksFromTwoPoints(l1, l2);
//            for(Block b : blcks){
//                if(locations.contains(b.getLocation())){
//                    continue;
//                }
//                locations.add(b.getLocation());
//            }
//        }
//        System.out.println(startLeft.clone().add(v1.clone().normalize().multiply(maxDistance)));
//        System.out.println(startRight.clone().add(v2.clone().normalize().multiply(maxDistance)));
//        List<Location> nL = new ArrayList<>();
//        nL = locations.stream().parallel().map(loc -> getOppositeLocation(loc, from)).collect(Collectors.toList());

        HashMap<Location, BlockData> output = new HashMap<>();

        locations.stream().forEach(location -> {
            Location nl = getOppositeLocation(location, from);
//            nL.add(nl);
            BlockState state = location.getBlock().getState();
            BlockData data = state.getBlockData();
            if (from.direction != this.direction) {
                if (data instanceof Directional directional) {
                    double rotAng = getRotationAngle(from);
                    BlockFace rotation = directional.getFacing();

                    if (rotAng > 0) {
                        if (rotation == BlockFace.EAST) {
                            rotation = BlockFace.NORTH;
                        } else if (rotation == BlockFace.WEST) {
                            rotation = BlockFace.SOUTH;
                        } else if (rotation == BlockFace.NORTH) {
                            rotation = BlockFace.WEST;
                        } else if (rotation == BlockFace.SOUTH) {
                            rotation = BlockFace.EAST;
                        }
                    } else {
                        if (rotation == BlockFace.EAST) {
                            rotation = BlockFace.SOUTH;
                        } else if (rotation == BlockFace.WEST) {
                            rotation = BlockFace.NORTH;
                        } else if (rotation == BlockFace.NORTH) {
                            rotation = BlockFace.EAST;
                        } else if (rotation == BlockFace.SOUTH) {
                            rotation = BlockFace.WEST;
                        }
                    }
                    directional.setFacing(rotation);
                }
            }
            output.put(nl, data);
        });

        output.keySet().stream().parallel().forEach(o -> player.sendBlockChange(o, output.get(o)));
//        for (Location location : output.keySet()) {
//            player.sendBlockChange(location, output.get(location));
//        }
        return output.keySet();
    }

    public void passthrough(Player player, Portal portal, int maxSteps) {
        if (!hiddenBlocks.containsKey(player)) {
            hiddenBlocks.put(player, new ArrayList<>());
//            unhideAll(player);
        }
        Location eyePosition = player.getEyeLocation().clone();
        double stepSize = 0.1;

        Location startLeft = getTopLeft();
        Location startRight = getBottomRight();

//        System.out.println(direction);
        if (direction == PortalDirection.NORTH_SOUTH) {
            startLeft = startLeft.clone().add(new Vector(1.5, 1.5, 1));
            startRight = startRight.clone().add(new Vector(0, 0, 0.5));

//            startRight = startRight.clone().add(new Vector(1, 0, 0));
        } else if (direction == PortalDirection.EAST_WEST) {
//            startLeft = startLeft.clone().add(new Vector(2,0,0.5));
//            startRight = startRight.clone().add(new Vector(-1, 0, 0.5));
        }
//        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, startRight, 10);
//        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, startLeft, 10);

        Vector v1 = startLeft.toVector().subtract(eyePosition.toVector());
        Vector v2 = startRight.toVector().subtract(eyePosition.toVector());
        if (direction != portal.direction) {
            v1 = v1.rotateAroundY((getRotationAngle(portal)));
            v2 = v2.rotateAroundY((getRotationAngle(portal)));
        }

        Collection<Location> output = portal.newAccept(v1, v2, 10, this, player);


//        List<Location> found = new ArrayList<>();
//
//        int n = 0;
//        for (double i = -2; i <= 2.0; i += stepSize) {
//            for (double j = -2; j <= 2.0; j += stepSize) {
//                Location currentPoint = getCenter().clone();
//                if (direction == PortalDirection.NORTH_SOUTH) {
//                    currentPoint = currentPoint.add(i, j, 0);
//                } else if (direction == PortalDirection.EAST_WEST) {
//                    currentPoint = currentPoint.add(0, j, i);
//                }
//
//                Vector vector = currentPoint.toVector().subtract(eyePosition.toVector());
//                if (direction != portal.direction) {
//                    vector = vector.rotateAroundY((getRotationAngle(portal)));
//                }
//
//
//                Location location = portal.getCenter().toVector().add(vector).toLocation(player.getWorld());
//                location = location.subtract(vector.normalize());
//                Location replacedLocation = portal.accept(this, player, location, vector, vector.length(), maxSteps);
//                if (replacedLocation != null) {
//                    if (hiddenBlocks.get(player).stream().filter(hid -> hid.getBlockX() == replacedLocation.getBlockX() && hid.getBlockY() == replacedLocation.getBlockY() && hid.getBlockZ() == replacedLocation.getBlockZ()).count() != 0) {
//                        found.add(replacedLocation);
//                    } else {
//                        found.add(replacedLocation);
////                        hiddenBlocks.get(player).add(replacedLocation);
//                    }
//                }
//            }
//        }


        for (Location loc : hiddenBlocks.get(player)) {
            if (!output.contains(loc)) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
        hiddenBlocks.get(player).clear();
        hiddenBlocks.get(player).addAll(output);
//                        hiddenBlocks.get(player).add(replacedLocation);

    }

}
