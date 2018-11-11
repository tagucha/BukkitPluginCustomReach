package ALCO.plugin;

import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.EntityLiving;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by taguhca on 2018/11/11.
 * Full spec ver
 */
public class CustomReach {
    private double range;
    private Player player;
    private double vecX;
    private double vecY;
    private double vecZ;
    private Map<EntityLiving,Location> entities;
    private World world;
    private double trueRange;

    public void attack(Player player,double range,boolean rangeATK,boolean overblock){
        this.range = range;
        this.player = player;
        this.world = player.getWorld();
        this.entities = new HashMap<>();
        this.trueRange = range;

        double yaw = Math.toRadians(player.getLocation().getYaw());
        double pitch = Math.toRadians(player.getLocation().getPitch());
        vecX = Math.sin(yaw) * Math.cos(pitch);
        vecY = Math.sin(pitch);
        vecZ = -Math.cos(yaw) * Math.cos(pitch);
        if (!overblock)this.trueRange = getTNBCDist();

        List<LivingEntity> les = new ArrayList<>();
        for (Entity entity:player.getNearbyEntities(range + 1,range + 1,range + 1)) if (entity instanceof LivingEntity) les.add((LivingEntity)entity);
        if (les.size() == 0) return;
        if (!isConatain(les)) return;

        if (!entities.isEmpty()){
            if (!rangeATK) {
                double minRange = range + 1;
                EntityLiving entity = null;
                for (EntityLiving le : entities.keySet())if (entities.get(le).distance(player.getEyeLocation()) < minRange) {
                    minRange = entities.get(le).distance(player.getEyeLocation());
                    entity = le;
                }
                if (entity != null) {
                    ((CraftPlayer) player).getHandle().attack(entity);
                }
            } else {
                for (EntityLiving le:entities.keySet()){
                    ((CraftPlayer) player).getHandle().attack(le);
                }
            }
        }
    }

    private boolean isConatain(List<LivingEntity> livingEntities){

        Location loc = player.getEyeLocation();
        boolean contain = false;

        for (LivingEntity entity:livingEntities){
            EntityLiving el = ((CraftLivingEntity)entity).getHandle();

            AxisAlignedBB box = el.getBoundingBox();
            double baseX = loc.getX();
            double baseY = loc.getY();
            double baseZ = loc.getZ();
            BoundingBox2D box2D_butZ = new BoundingBox2D(box.minX - baseX,box.maxX - baseX,box.minY - baseY,box.maxY - baseY);
            BoundingBox2D box2D_butY = new BoundingBox2D(box.minZ - baseZ,box.maxZ - baseZ,box.minX - baseX,box.maxX - baseX);
            BoundingBox2D box2D_butX = new BoundingBox2D(box.minY - baseY,box.maxY - baseY,box.minZ - baseZ,box.maxZ - baseZ);

            List<Location> crossLocs = new ArrayList<>();

            if (box2D_butZ.isConatin(vecX * (box.minZ - baseZ) / vecZ,vecY * (box.minZ - baseZ) / vecZ)) crossLocs.add(new Location(world,baseX + vecX * (box.minZ - baseZ) / vecZ,baseY + vecY * (box.minZ - baseZ) / vecZ,baseZ));
            if (box2D_butZ.isConatin(vecX * (box.maxZ - baseZ) / vecZ,vecY * (box.maxZ - baseZ) / vecZ)) crossLocs.add(new Location(world,baseX + vecX * (box.maxZ - baseZ) / vecZ,baseY + vecY * (box.maxZ - baseZ) / vecZ,baseZ));
            if (box2D_butY.isConatin(vecZ * (box.minY - baseY) / vecY,vecX * (box.minY - baseY) / vecY)) crossLocs.add(new Location(world,baseX + vecX * (box.minY - baseY) / vecY,baseY,baseZ + vecZ * (box.minY - baseY) / vecY));
            if (box2D_butY.isConatin(vecZ * (box.maxY - baseY) / vecY,vecX * (box.maxY - baseY) / vecY)) crossLocs.add(new Location(world,baseX + vecX * (box.maxY - baseY) / vecY,baseY,baseZ + vecZ * (box.maxY - baseY) / vecY));
            if (box2D_butX.isConatin(vecY * (box.minX - baseX) / vecX,vecZ * (box.minX - baseX) / vecX)) crossLocs.add(new Location(world,baseX,baseY + vecY * (box.minX - baseX) / vecX,baseZ + vecZ * (box.minX - baseX) / vecX));
            if (box2D_butX.isConatin(vecY * (box.maxX - baseX) / vecX,vecZ * (box.maxX - baseX) / vecX)) crossLocs.add(new Location(world,baseX,baseY + vecY * (box.maxX - baseX) / vecX,baseZ + vecZ * (box.maxX - baseX) / vecX));

            if (crossLocs.isEmpty()) continue;

            Location tmnloc = crossLocs.get(0);
            for (int i = 1;i < crossLocs.size();i++) if (tmnloc.distance(loc) > crossLocs.get(i).distance(loc)) tmnloc = crossLocs.get(i);
            if (!(tmnloc.distance(loc) <= trueRange)) continue;
            player.sendMessage("" + tmnloc.distance(loc));
            entities.put(el,tmnloc);
            if (!contain) contain = true;
        }

        return contain;
    }

    private double getTNBCDist(){
        Block block = null;

        BlockIterator bi = new BlockIterator(player,(int)range + 1);

        while (bi.hasNext()){
            block = bi.next();
            if (block.getType().isSolid()) break;
        }
        if (block == null) return range;

        Location loc = player.getEyeLocation();
        Location bloc = block.getLocation();
        AxisAlignedBB box = new AxisAlignedBB(bloc.getBlockX(),bloc.getBlockY(),bloc.getBlockZ(),bloc.getBlockX() + 1,bloc.getBlockY() + 1,bloc.getBlockZ() + 1);

        double baseX = loc.getX();
        double baseY = loc.getY();
        double baseZ = loc.getZ();
        BoundingBox2D box2D_butZ = new BoundingBox2D(box.minX - baseX,box.maxX - baseX,box.minY - baseY,box.maxY - baseY);
        BoundingBox2D box2D_butY = new BoundingBox2D(box.minZ - baseZ,box.maxZ - baseZ,box.minX - baseX,box.maxX - baseX);
        BoundingBox2D box2D_butX = new BoundingBox2D(box.minY - baseY,box.maxY - baseY,box.minZ - baseZ,box.maxZ - baseZ);

        List<Location> crossLocs = new ArrayList<>();

        if (box2D_butZ.isConatin(vecX * (box.minZ - baseZ) / vecZ,vecY * (box.minZ - baseZ) / vecZ)) crossLocs.add(new Location(world,baseX + vecX * (box.minZ - baseZ) / vecZ,baseY + vecY * (box.minZ - baseZ) / vecZ,baseZ));
        if (box2D_butZ.isConatin(vecX * (box.maxZ - baseZ) / vecZ,vecY * (box.maxZ - baseZ) / vecZ)) crossLocs.add(new Location(world,baseX + vecX * (box.maxZ - baseZ) / vecZ,baseY + vecY * (box.maxZ - baseZ) / vecZ,baseZ));
        if (box2D_butY.isConatin(vecZ * (box.minY - baseY) / vecY,vecX * (box.minY - baseY) / vecY)) crossLocs.add(new Location(world,baseX + vecX * (box.minY - baseY) / vecY,baseY,baseZ + vecZ * (box.minY - baseY) / vecY));
        if (box2D_butY.isConatin(vecZ * (box.maxY - baseY) / vecY,vecX * (box.maxY - baseY) / vecY)) crossLocs.add(new Location(world,baseX + vecX * (box.maxY - baseY) / vecY,baseY,baseZ + vecZ * (box.maxY - baseY) / vecY));
        if (box2D_butX.isConatin(vecY * (box.minX - baseX) / vecX,vecZ * (box.minX - baseX) / vecX)) crossLocs.add(new Location(world,baseX,baseY + vecY * (box.minX - baseX) / vecX,baseZ + vecZ * (box.minX - baseX) / vecX));
        if (box2D_butX.isConatin(vecY * (box.maxX - baseX) / vecX,vecZ * (box.maxX - baseX) / vecX)) crossLocs.add(new Location(world,baseX,baseY + vecY * (box.maxX - baseX) / vecX,baseZ + vecZ * (box.maxX - baseX) / vecX));

        if (crossLocs.isEmpty()) return range;

        Location tmnloc = crossLocs.get(0);
        for (int i = 1;i < crossLocs.size();i++) if (tmnloc.distance(loc) > crossLocs.get(i).distance(loc)) tmnloc = crossLocs.get(i);
        return tmnloc.distance(player.getEyeLocation());
    }

    private class BoundingBox2D{
        public final double minX;
        public final double maxX;
        public final double minY;
        public final double maxY;

        public BoundingBox2D(double minX,double maxX,double minY,double maxY){
            this.maxX = maxX;
            this.maxY = maxY;
            this.minX = minX;
            this.minY = minY;
        }

        public boolean isConatin(double x,double y){
            return minX <= x && x <= maxX && minY <= y && y <= maxY;
        }
    }
}
