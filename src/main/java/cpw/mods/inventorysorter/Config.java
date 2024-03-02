package cpw.mods.inventorysorter;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public static class ServerConfig {
        static final ServerConfig CONFIG;
        static final ModConfigSpec SPEC;

        static {
            final Pair<ServerConfig, ModConfigSpec> conf = new ModConfigSpec.Builder().configure(ServerConfig::new);
            CONFIG = conf.getLeft();
            SPEC = conf.getRight();
        }

        final ModConfigSpec.ConfigValue<List<? extends String>> containerBlacklist;
        final ModConfigSpec.ConfigValue<List<? extends String>> slotBlacklist;
        final ModConfigSpec.BooleanValue debug;
        private ServerConfig(ModConfigSpec.Builder builder) {
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

            debug = builder
                    .comment("Enable debug logging")
                    .define("debug", false);
            builder.pop();
        }
    }
    public static class ClientConfig {
        static final ClientConfig CONFIG;
        static final ModConfigSpec SPEC;

        static {
            final Pair<ClientConfig, ModConfigSpec> conf = new ModConfigSpec.Builder().configure(ClientConfig::new);
            CONFIG = conf.getLeft();
            SPEC = conf.getRight();
        }
        final ModConfigSpec.ConfigValue<Boolean> sortingModule;
        final ModConfigSpec.ConfigValue<Boolean> wheelmoveModule;

        private ClientConfig(ModConfigSpec.Builder builder) {
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
