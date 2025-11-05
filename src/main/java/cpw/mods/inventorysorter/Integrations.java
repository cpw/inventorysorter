package cpw.mods.inventorysorter;

import net.neoforged.fml.ModList;

import java.util.Arrays;

/**
 * Blacklist commonly incompatible mod slots and containers for Inventory Sorter.
 * <p>
 * If any other mods need to be added, please create a PR or open an issue on GitHub.
 */
public class Integrations {
    public static void init() {
        blackListModSlots();
        blacklistModContainers();
    }

    private static void blackListModSlots() {
        addSlotIfLoaded("curios", "top.theillusivec4.curios.common.inventory.CurioSlot");

        addSlotIfLoaded("enderio", "com.enderio.machines.common.blocks.enchanter.EnchanterMenu$EnchanterOutputMachineSlot",
            "com.enderio.machines.common.blocks.base.menu.MachineSlot",
            "com.enderio.base.common.menu.ItemFilterSlot",
            "com.enderio.machines.common.menu.EnchanterMenu$EnchanterOutputMachineSlot",
            "com.enderio.base.common.filter.item.ItemFilterSlot",
            "com.enderio.machines.common.blocks.base.menu.GhostMachineSlot",
            "com.enderio.base.common.filter.fluid.FluidFilterSlot",
            "com.enderio.machines.common.menu.MachineSlot",
            "com.enderio.machines.common.menu.GhostMachineSlot",
            "com.enderio.machines.common.blocks.base.menu.PreviewMachineSlot",
            "com.enderio.base.common.menu.FluidFilterSlot",
            "com.enderio.machines.common.menu.PreviewMachineSlot"
        );

        addSlotIfLoaded("sophisticatedcore",
            "net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer$FilterLogicSlot",
            "net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler",
            "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot",
            "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase$StorageUpgradeSlot",
            "net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler"
        );

        addSlotIfLoaded("create", "com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu$SorterProofSlot");
        addSlotIfLoaded("productivebees", "productivebees:advanced_beehive");
    }

    private static void blacklistModContainers() {
        addContainerIfLoaded("occultism",
            "occultism:ritual_satchel_t2",
            "occultism:ritual_satchel_t1"
        );

        addContainerIfLoaded("mysticalagriculture",
            "mysticalagriculture:harvester"
        );

        addContainerIfLoaded("bonsaitrees",
            "bonsaitrees:bonsai_pot"
        );

        addSlotIfLoaded("titanium", "titanium:addon_container");
    }

    private static void addSlotIfLoaded(String modid, String... slotName) {
        if (ModList.get().isLoaded(modid)) {
            InventorySorter.INSTANCE.slotblacklist().addAll(Arrays.asList(slotName));
        }
    }

    private static void addContainerIfLoaded(String modid, String... containerName) {
        if (ModList.get().isLoaded(modid)) {
            InventorySorter.INSTANCE.containerblacklist().addAll(Arrays.asList(containerName));
        }
    }
}
