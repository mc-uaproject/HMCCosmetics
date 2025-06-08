package com.hibiscusmc.hmccosmetics.cosmetic.types;

import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.manager.UserBackpackManager;
import com.hibiscusmc.hmccosmetics.user.manager.UserEntity;
import com.hibiscusmc.hmccosmetics.util.packets.HMCCPacketManager;
import lombok.Getter;
import me.lojosho.hibiscuscommons.util.packets.PacketManager;
import me.lojosho.shaded.configurate.ConfigurationNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class CosmeticBackpack2Type extends CosmeticBackpackType {

    private int height = -1;
    private ItemStack firstPersonBackpack;

    public CosmeticBackpack2Type(String id, ConfigurationNode config) {
        super(id, config);

        if (!config.node("firstperson-item").virtual()) {
            this.firstPersonBackpack = generateItemStack(config.node("firstperson-item"));
            this.height = config.node("height").getInt(5);
        }
    }

    @Override
    public void update(@NotNull CosmeticUser user) {
        Entity entity = user.getEntity();
        if (entity == null) return;

        Location entityLocation = entity.getLocation();
        Location loc = entityLocation.clone().add(0, 2, 0);

        if (user.isInWardrobe() || !user.isBackpack2Spawned()) return;
        if (user.isHidden()) {
            // Sometimes the backpack is not despawned when the player is hidden (weird ass logic happening somewhere)
            user.despawnBackpack2();
            return;
        }

        UserBackpackManager backpackManager = user.getUserBackpack2Manager();
        UserEntity entityManager = backpackManager.getEntityManager();
        int firstArmorStandId = backpackManager.getFirstArmorStandId();

        List<Player> outsideViewers = entityManager.refreshViewers(loc);

        entityManager.teleport(loc);
        entityManager.setRotation((int) loc.getYaw(), isFirstPersonCompadible());

        HMCCPacketManager.sendEntitySpawnPacket(entityLocation, firstArmorStandId, EntityType.ARMOR_STAND, UUID.randomUUID(), outsideViewers);
        HMCCPacketManager.sendArmorstandMetadata(firstArmorStandId, outsideViewers);
        PacketManager.equipmentSlotUpdate(firstArmorStandId, EquipmentSlot.HEAD, user.getUserCosmeticItem(this, getItem()), outsideViewers);
        final UserBackpackManager anotherManager = user.getUserBackpackManager();
        final int[] ridingIDs;
        if (anotherManager != null) {
            ridingIDs = new int[]{firstArmorStandId, anotherManager.getFirstArmorStandId()};
        } else {
            ridingIDs = new int[]{firstArmorStandId};
        }
        // If true, it will send the riding packet to all players. If false, it will send the riding packet only to new players
        if (Settings.isBackpackForceRidingEnabled()) HMCCPacketManager.sendRidingPacket(entity.getEntityId(), ridingIDs, entityManager.getViewers());
        else HMCCPacketManager.sendRidingPacket(entity.getEntityId(), ridingIDs, outsideViewers);

        if (!user.isInWardrobe() && isFirstPersonCompadible() && user.getPlayer() != null) {
            List<Player> owner = List.of(user.getPlayer());

            ArrayList<Integer> particleCloud = backpackManager.getAreaEffectEntityId();
            for (int i = 0; i < particleCloud.size(); i++) {
                if (i == 0) {
                    HMCCPacketManager.sendRidingPacket(entity.getEntityId(), particleCloud.get(i), owner);
                } else {
                    HMCCPacketManager.sendRidingPacket(particleCloud.get(i - 1), particleCloud.get(i) , owner);
                }
            }
            HMCCPacketManager.sendRidingPacket(particleCloud.get(particleCloud.size() - 1), firstArmorStandId, owner);
            if (!user.isHidden()) {
                //if (loc.getPitch() < -70) NMSHandlers.getHandler().equipmentSlotUpdate(user.getUserBackpack2Manager().getFirstArmorStandId(), EquipmentSlot.HEAD, new ItemStack(Material.AIR), owner);
                //else NMSHandlers.getHandler().equipmentSlotUpdate(user.getUserBackpack2Manager().getFirstArmorStandId(), EquipmentSlot.HEAD, firstPersonBackpack, owner);
                PacketManager.equipmentSlotUpdate(firstArmorStandId, EquipmentSlot.HEAD, user.getUserCosmeticItem(this, firstPersonBackpack), owner);
            }
            //MessagesUtil.sendDebugMessages("First Person Backpack Update[owner=" + user.getUniqueId() + ",player_location=" + loc + "]!", Level.INFO);
        }

        backpackManager.showBackpack();
    }

    public boolean isFirstPersonCompadible() {
        return firstPersonBackpack != null;
    }

}
