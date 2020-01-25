package cpw.mods.inventorysorter;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Config {
    static final Config CONFIG;
    static final ForgeConfigSpec SPEC;

    static {
        final Pair<Config, ForgeConfigSpec> conf = new ForgeConfigSpec.Builder().configure(Config::new);
        CONFIG = conf.getLeft();
        SPEC = conf.getRight();
    }

    final ForgeConfigSpec.ConfigValue<List<? extends String>> containerBlacklist;
    final ForgeConfigSpec.ConfigValue<List<? extends String>> slotBlacklist;

    private Config(ForgeConfigSpec.Builder builder) {
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
