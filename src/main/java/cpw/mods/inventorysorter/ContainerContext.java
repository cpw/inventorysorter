package cpw.mods.inventorysorter;

import com.google.common.collect.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.*;

import java.util.*;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

class ContainerContext
{
    final Slot slot;
    final InventoryHandler.InventoryMapping slotMapping;
    final ServerPlayer player;
    final ImmutableBiMap<Container, InventoryHandler.InventoryMapping> mapping;
    private InventoryHandler.InventoryMapping slotTarget;

    static final Container PLAYER_HOTBAR = new SimpleContainer(0);
    static final Container PLAYER_MAIN = new SimpleContainer(0);
    static final Container PLAYER_OFFHAND = new SimpleContainer(0);

    static boolean validSlot(Slot slot) {
        // Skip slots without an inventory - they're probably dummy slots
        return slot != null && slot.container != null
                // Skip blacklisted slots
                && !InventorySorter.INSTANCE.slotblacklist.contains(slot.getClass().getName());
    }

    public ContainerContext(Slot slot, ServerPlayer playerEntity)
    {
        this.slot = slot;
        this.player = playerEntity;
        Map<Container, InventoryHandler.InventoryMapping> mapping = new HashMap<>();
        final AbstractContainerMenu openContainer = playerEntity.containerMenu;
        openContainer.slots.stream().filter(ContainerContext::validSlot).forEach(sl->
        {
            final InventoryHandler.InventoryMapping inventoryMapping = mapping.computeIfAbsent(sl.container, k -> new InventoryHandler.InventoryMapping(sl.container, openContainer, sl.container, sl.getClass()));
            inventoryMapping.addSlot(sl);
            if (sl == slot)
            {
                slotTarget = inventoryMapping;
            }
        });

        if (mapping.containsKey(playerEntity.getInventory())) {
            final InventoryHandler.InventoryMapping playerMapping = mapping.remove(playerEntity.getInventory());
            if (slotTarget == playerMapping) slotTarget = null;
            int mainStart = 9;
            int mainEnd = 36;
            int offhandStart = 40;

            InventoryHandler.InventoryMapping hotbarMapping = new InventoryHandler.InventoryMapping(PLAYER_HOTBAR, openContainer, playerEntity.getInventory(), Slot.class);
            InventoryHandler.InventoryMapping mainMapping = new InventoryHandler.InventoryMapping(PLAYER_MAIN, openContainer, playerEntity.getInventory(), Slot.class);
            InventoryHandler.InventoryMapping offhandMapping = new InventoryHandler.InventoryMapping(PLAYER_OFFHAND, openContainer, playerEntity.getInventory(), Slot.class);
            InventoryHandler.InventoryMapping inventoryMapping;
            for (int i = playerMapping.begin; i<=playerMapping.end; i++)
            {
                Slot s = openContainer.getSlot(i);
                if (s.getSlotIndex() < mainStart && s.container == playerEntity.getInventory())
                {
                    hotbarMapping.begin = Math.min(s.index, hotbarMapping.begin);
                    hotbarMapping.end = Math.max(s.index, hotbarMapping.end);
                    mapping.put(PLAYER_HOTBAR, hotbarMapping);
                    inventoryMapping = hotbarMapping;
                }
                else if (s.getSlotIndex() < mainEnd && s.container == playerEntity.getInventory())
                {
                    mainMapping.begin = Math.min(s.index, mainMapping.begin);
                    mainMapping.end = Math.max(s.index, mainMapping.end);
                    mapping.put(PLAYER_MAIN, mainMapping);
                    inventoryMapping = mainMapping;
                }
                else if (s.getSlotIndex() >= offhandStart && s.container == playerEntity.getInventory())
                {
                    offhandMapping.begin = Math.min(s.index, offhandMapping.begin);
                    offhandMapping.end = Math.max(s.index, offhandMapping.end);
                    mapping.put(PLAYER_OFFHAND, offhandMapping);
                    inventoryMapping = offhandMapping;
                }
                else
                {
                    inventoryMapping = null;
                }
                if (s == slot)
                {
                    slotTarget = inventoryMapping;
                }
            }
        }
        for (Map.Entry<Container, InventoryHandler.InventoryMapping> map : Sets.newLinkedHashSet(mapping.entrySet()))
        {
            if (map.getValue().markForRemoval) {
                mapping.remove(map.getKey());
                if (slotTarget == map.getValue()) slotTarget = null;
            }
        }
        this.slotMapping = slotTarget;
        this.mapping = ImmutableBiMap.copyOf(mapping);
        InventorySorter.LOGGER.log(Level.DEBUG, "Slot mapping {}", ()->this.mapping);
        InventorySorter.LOGGER.log(Level.DEBUG, "Action slot {}", ()->this.slotMapping);
    }
}
