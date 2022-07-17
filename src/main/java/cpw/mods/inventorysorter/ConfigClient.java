package cpw.mods.inventorysorter;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigClient {

    static final ConfigClient CONFIG;
    static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<ConfigClient, ForgeConfigSpec> conf = new ForgeConfigSpec.Builder().configure(ConfigClient::new);
        CONFIG = conf.getLeft();
        CLIENT_SPEC = conf.getRight();
    }

    final ForgeConfigSpec.ConfigValue<List<? extends String>> containerBlacklistClient;

    private ConfigClient(ForgeConfigSpec.Builder builder) {
        builder.comment("Inventory sorter blacklists");
        builder.push("blacklists");
        containerBlacklistClient = builder
                .comment("Container blacklist on Client")
                .translation("inventorysorter.config.containerblacklistclient")
                .defineList("containerBlacklistClient", ArrayList::new, value -> ForgeRegistries.CONTAINERS.containsKey(new ResourceLocation(Objects.toString(value))));
        builder.pop();
    }

}