package cpw.mods.inventorysorter;

import net.minecraftforge.common.ForgeConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static class ServerConfig {
        static final ServerConfig CONFIG;
        static final ForgeConfigSpec SPEC;

        static {
            final Pair<ServerConfig, ForgeConfigSpec> conf = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
            CONFIG = conf.getLeft();
            SPEC = conf.getRight();
        }

        final ForgeConfigSpec.ConfigValue<List<? extends String>> containerBlacklist;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> slotBlacklist;
        private ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Inventory sorter blacklists");
            builder.push("blacklists");
            containerBlacklist = builder
                    .comment("Container blacklist")
                    .translation("inventorysorter.config.containerblacklist")
                    .defineList("containerBlacklist", ArrayList::new, t -> true);
            slotBlacklist = builder
                    .comment("Slot type blacklist")
                    .translation("inventorysorter.config.slotblacklist")
                    .defineList("slotBlacklist", new ArrayList<>(), t -> true);
            builder.pop();
        }
    }
    public static class ClientConfig {
        static final ClientConfig CONFIG;
        static final ForgeConfigSpec SPEC;

        static {
            final Pair<ClientConfig, ForgeConfigSpec> conf = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
            CONFIG = conf.getLeft();
            SPEC = conf.getRight();
        }
        final ForgeConfigSpec.ConfigValue<Boolean> sortingModule;
        final ForgeConfigSpec.ConfigValue<Boolean> wheelmoveModule;

        private ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Inventory sorter modules");
            builder.push("modules");
            sortingModule = builder
                    .comment("Sorting module")
                    .translation("inventorysorter.config.sortingmodule")
                    .define("sortingmodule", true);

            wheelmoveModule = builder
                    .comment("Wheel move module")
                    .translation("inventorysorter.config.wheelmovemodule")
                    .define("wheelmovemodule", true);

            builder.pop();
        }
    }
}
