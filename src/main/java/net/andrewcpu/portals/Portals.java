package net.andrewcpu.portals;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Portals extends JavaPlugin implements Listener {

    private Portal p3, p4;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        portal1 = new Location(Bukkit.getWorld("world"), 16, 68, 42);
        portal2 = new Location(Bukkit.getWorld("world"), 7, 69, 28);
        World world = Bukkit.getWorld("world");
//        Portal portal1 = new Portal(new Location(world, 16, 68, 42), new Location(world, 15, 69, 42), new Location(world, 17, 67, 42));
//        Portal portal2 = new Portal(new Location(world, 7, 69, 28), new Location(world, 7, 70, 27), new Location(world, 7, 68, 29));
//        p1 = portal1;
//        p2 = portal2;
        p3 = new Portal(new Location(world, -17, 69, 10), new Location(world, -16, 70, 10), new Location(world, -18, 68, 10));
        p4 = new Portal(new Location(world, -66, 32, 77), new Location(world, -65, 33, 77), new Location(world, -67, 31, 77));
//        portalConnections.add(new PortalConnection(p1, p2));
        portalConnections.add(new PortalConnection(p3, p4));
        for(PortalConnection connection : portalConnections){
            connection.getPortal1().getCenter().getChunk().addPluginChunkTicket(this);
            connection.getPortal2().getCenter().getChunk().addPluginChunkTicket(this);
        }
//        portalConnections.add(new PortalConnection(
//                new Portal(
//                    new Location(world, 6, 18, 2),
//                    new Location(world, 7, 19, 2),
//                    new Location(world, 5, 17, 2)
//                ),
//                new Portal(
//                        new Location(world, 16, 14, -22),
//                        new Location(world, 16, 15, -23),
//                        new Location(world, 16, 13, -21))));

    }

    private Portal p1;
    private Portal p2;

    private Location portal1;
    private Location portal2;

    public static List<Block> blocksFromTwoPoints(Location loc1, Location loc2) {
        List<Block> blocks = new ArrayList<Block>();

        int topBlockX = (loc1.getBlockX() < loc2.getBlockX() ? loc2.getBlockX() : loc1.getBlockX());
        int bottomBlockX = (loc1.getBlockX() > loc2.getBlockX() ? loc2.getBlockX() : loc1.getBlockX());

        int topBlockY = (loc1.getBlockY() < loc2.getBlockY() ? loc2.getBlockY() : loc1.getBlockY());
        int bottomBlockY = (loc1.getBlockY() > loc2.getBlockY() ? loc2.getBlockY() : loc1.getBlockY());

        int topBlockZ = (loc1.getBlockZ() < loc2.getBlockZ() ? loc2.getBlockZ() : loc1.getBlockZ());
        int bottomBlockZ = (loc1.getBlockZ() > loc2.getBlockZ() ? loc2.getBlockZ() : loc1.getBlockZ());

        for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                for (int y = bottomBlockY; y <= topBlockY; y++) {
                    Block block = loc1.getWorld().getBlockAt(x, y, z);

                    blocks.add(block);
                }
            }
        }

        return blocks;
    }

    public boolean doesPlayerHaveLineOfSight(Player player, Portal portal) {

        int maxDistance = 100;
        List<Block> blocks = blocksFromTwoPoints(portal.getTopLeft(), portal.getBottomRight());

        List<Block> los = player.getLineOfSight(null, maxDistance);
        for (Block b : los) {
            if (blocks.contains(b)) {
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void async(AsyncPlayerChatEvent event) {
        Vector direction = p2.getCenter().toVector().subtract(event.getPlayer().getLocation().toVector());

        Vector otherPerspective = direction.rotateAroundY(Math.PI / 2);
        Location location = portal1.toVector().subtract(otherPerspective).toLocation(event.getPlayer().getWorld());
        location.setDirection(event.getPlayer().getEyeLocation().getDirection().rotateAroundY(Math.PI / 2));


        if (event.getMessage().equals("log")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                event.getPlayer().teleport(location);
            }, 1);
        } else if (event.getMessage().equals("update")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                p2.passthrough(event.getPlayer(), p1, 200);
//                List<RayTraceResult> results = p1.rayTrace(location);
//                for(RayTraceResult result : results){
//                    if(result == null) continue;
//                    if(result.getHitBlock() != null){
//                        Location blockLocation = result.getHitBlock().getLocation();
//                        Location newPosition = result.getHitBlock().getLocation().toVector().subtract(location.toVector()).rotateAroundAxis(new Vector(0, 1,0), -Math.PI / 2).add(event.getPlayer().getLocation().toVector()).toLocation(event.getPlayer().getWorld());
////                        Location newPosition = blockLocation.toVector().rotateAroundAxis(new Vector(0, 1,0), -Math.PI / 2).toLocation(event.getPlayer().getWorld());
//                        event.getPlayer().sendBlockChange(newPosition, blockLocation.getBlock().getBlockData());
//                        System.out.println(newPosition.toString());
//                    }
//
//
//                }
            }, 1);

//            RayTraceResult result = event.getPlayer().getWorld().rayTraceBlocks(location, otherPerspective, 100);
        } else if (event.getMessage().equals("test")) {
            Location blockLocation = new Location(event.getPlayer().getWorld(), 16, 68, 41);
            Location newPosition = blockLocation.toVector().subtract(location.toVector()).rotateAroundAxis(new Vector(0, 1, 0), -Math.PI / 2).add(event.getPlayer().getLocation().toVector()).toLocation(event.getPlayer().getWorld());
            System.out.println(newPosition);
        }
    }

    public Location getOppositeLocation(Location point, Location eyeLocation, Portal from, Portal to) {
        double zX = 0;
        double distance2Center = from.getCenter().clone().subtract(point).getZ();
        Vector direction = from.getCenter().toVector().add(new Vector(0.5, 0, distance2Center)).subtract(point.toVector());
        Vector otherPerspective = from.direction != to.direction ? direction.rotateAroundY(from.getRotationAngle(to)) : direction.clone();
        Location location2 = to.getCenter().toVector().add(new Vector(0.5, 0, distance2Center)).subtract(otherPerspective).toLocation(point.getWorld());

        location2.setDirection(eyeLocation.getDirection().rotateAroundY(from.getRotationAngle(to)));
//        to.getRotationAngle(from);
        return location2;
    }

    public HashMap<Player, Long> cooldownTime = new HashMap<>();

    private List<PortalConnection> portalConnections = new ArrayList<>();

    private HashMap<Player, Boolean> isInPortal = new HashMap<>();
    @EventHandler
    public void chunkUnload(ChunkUnloadEvent event){

    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!cooldownTime.containsKey(event.getPlayer())) {
            cooldownTime.put(event.getPlayer(), 0L);
        }
        if (event.getFrom().distance(event.getTo()) != 0) {
            if (cooldownTime.containsKey(event.getPlayer())) {
                if (System.currentTimeMillis() - cooldownTime.get(event.getPlayer()) > 1000) {
                    for (PortalConnection portalConnection : portalConnections) {
                        Portal inPortal;
                        if (!portalConnection.getPortal1().isInPortal(event.getFrom()) && portalConnection.getPortal1().isInPortal(event.getTo())) {
                            inPortal = portalConnection.getPortal1();
                        } else if (!portalConnection.getPortal2().isInPortal(event.getFrom()) && portalConnection.getPortal2().isInPortal(event.getTo())) {
                            inPortal = portalConnection.getPortal2();
                        } else {
                            inPortal = null;
                        }

                        if (inPortal != null) {
                            cooldownTime.put(event.getPlayer(), System.currentTimeMillis());
                            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                                Vector velocity = event.getPlayer().getVelocity();

                                Portal to = portalConnection.getPortal1() == inPortal ? portalConnection.getPortal2() : portalConnection.getPortal1();
                                to.emptyPortal(event.getPlayer());
                                event.getPlayer().teleport(getOppositeLocation(event.getPlayer().getLocation(), event.getPlayer().getEyeLocation(), inPortal, to));
                                event.getPlayer().setVelocity(velocity.rotateAroundY(portalConnection.getPortal1().getRotationAngle(portalConnection.getPortal2())));
                                }, 1);
                            return;
                        }
                    }
                }
            }
        }


        if (event.getPlayer().isSneaking()) {
            return;
        }


        int maxSteps = 150;
        int hideDistance = 0;
        for (PortalConnection portalConnection : portalConnections) {
            if (doesPlayerHaveLineOfSight(event.getPlayer(), portalConnection.getPortal1())) {
                if (event.getPlayer().getLocation().distance(portalConnection.getPortal2().getCenter()) > hideDistance) {
                    portalConnection.getPortal2().unhideAll(event.getPlayer());
                }
                if (portalConnection.getPortal1().getCenter().distance(event.getPlayer().getEyeLocation()) > 1.2) {
                    portalConnection.getPortal1().passthrough(event.getPlayer(), portalConnection.getPortal2(), maxSteps);
                }
            } else {
                if (event.getPlayer().getLocation().distance(portalConnection.getPortal1().getCenter()) > hideDistance) {
                    portalConnection.getPortal1().unhideAll(event.getPlayer());
                }
                if (doesPlayerHaveLineOfSight(event.getPlayer(), portalConnection.getPortal2())) {
                    if (portalConnection.getPortal2().getCenter().distance(event.getPlayer().getEyeLocation()) > 1.2) {
                        portalConnection.getPortal2().passthrough(event.getPlayer(), portalConnection.getPortal1(), maxSteps);
                    }
                } else {
                    if (event.getPlayer().getLocation().distance(portalConnection.getPortal2().getCenter()) > hideDistance) {
                        portalConnection.getPortal2().unhideAll(event.getPlayer());
                    }
                }
            }
            if(!portalConnection.getPortal1().isShowingToPlayer(event.getPlayer())&& portalConnection.getPortal1().getCenter().distance(event.getPlayer().getEyeLocation()) >= 1.5){
                portalConnection.getPortal1().fillPortal(event.getPlayer());
            }
            else{
                portalConnection.getPortal1().emptyPortal(event.getPlayer());
            }
            if(!portalConnection.getPortal2().isShowingToPlayer(event.getPlayer()) && portalConnection.getPortal2().getCenter().distance(event.getPlayer().getEyeLocation()) >= 1.5){
                portalConnection.getPortal2().fillPortal(event.getPlayer());
            }
            else{
                portalConnection.getPortal2().emptyPortal(event.getPlayer());
            }


        }
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (p3.isBlockFrame(event.getClickedBlock())) {
            Bukkit.broadcastMessage("FRAME!");
        }
    }
}
