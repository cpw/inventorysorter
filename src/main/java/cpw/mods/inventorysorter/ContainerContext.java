package cpw.mods.inventorysorter;

import com.google.common.collect.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import org.apache.logging.log4j.*;

import java.util.*;

class ContainerContext
{
    final Slot slot;
    final InventoryHandler.InventoryMapping slotMapping;
    final ServerPlayerEntity player;
    final ImmutableBiMap<IInventory, InventoryHandler.InventoryMapping> mapping;
    private InventoryHandler.InventoryMapping slotTarget;

    static final IInventory PLAYER_HOTBAR = new Inventory(0);
    static final IInventory PLAYER_MAIN = new Inventory(0);
    static final IInventory PLAYER_OFFHAND = new Inventory(0);

    static boolean validSlot(Slot slot) {
        // Skip slots without an inventory - they're probably dummy slots
        return slot != null && slot.inventory != null
                // Skip blacklisted slots
                && !InventorySorter.INSTANCE.slotblacklist.contains(slot.getClass().getName());
    }

    public ContainerContext(Slot slot, ServerPlayerEntity playerEntity)
    {
        this.slot = slot;
        this.player = playerEntity;
        Map<IInventory, InventoryHandler.InventoryMapping> mapping = new HashMap<>();
        final Container openContainer = playerEntity.openContainer;
        openContainer.inventorySlots.stream().filter(ContainerContext::validSlot).forEach(sl->
        {
            final InventoryHandler.InventoryMapping inventoryMapping = mapping.computeIfAbsent(sl.inventory, k -> new InventoryHandler.InventoryMapping(sl.inventory, openContainer, sl.inventory, sl.getClass()));
            inventoryMapping.addSlot(sl);
            if (sl == slot)
            {
                slotTarget = inventoryMapping;
            }
        });

        if (mapping.containsKey(playerEntity.inventory)) {
            final InventoryHandler.InventoryMapping playerMapping = mapping.remove(playerEntity.inventory);
            if (slotTarget == playerMapping) slotTarget = null;
            int mainStart = 9;
            int mainEnd = 36;
            int offhandStart = 40;

            InventoryHandler.InventoryMapping hotbarMapping = new InventoryHandler.InventoryMapping(PLAYER_HOTBAR, openContainer, playerEntity.inventory, Slot.class);
            InventoryHandler.InventoryMapping mainMapping = new InventoryHandler.InventoryMapping(PLAYER_MAIN, openContainer, playerEntity.inventory, Slot.class);
            InventoryHandler.InventoryMapping offhandMapping = new InventoryHandler.InventoryMapping(PLAYER_OFFHAND, openContainer, playerEntity.inventory, Slot.class);
            InventoryHandler.InventoryMapping inventoryMapping;
            for (int i = playerMapping.begin; i<=playerMapping.end; i++)
            {
                Slot s = openContainer.getSlot(i);
                if (s.getSlotIndex() < mainStart && s.inventory == playerEntity.inventory)
                {
                    hotbarMapping.begin = Math.min(s.slotNumber, hotbarMapping.begin);
                    hotbarMapping.end = Math.max(s.slotNumber, hotbarMapping.end);
                    mapping.put(PLAYER_HOTBAR, hotbarMapping);
                    inventoryMapping = hotbarMapping;
                }
                else if (s.getSlotIndex() < mainEnd && s.inventory == playerEntity.inventory)
                {
                    mainMapping.begin = Math.min(s.slotNumber, mainMapping.begin);
                    mainMapping.end = Math.max(s.slotNumber, mainMapping.end);
                    mapping.put(PLAYER_MAIN, mainMapping);
                    inventoryMapping = mainMapping;
                }
                else if (s.getSlotIndex() >= offhandStart && s.inventory == playerEntity.inventory)
                {
                    offhandMapping.begin = Math.min(s.slotNumber, offhandMapping.begin);
                    offhandMapping.end = Math.max(s.slotNumber, offhandMapping.end);
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
        for (Map.Entry<IInventory, InventoryHandler.InventoryMapping> map : Sets.newLinkedHashSet(mapping.entrySet()))
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
