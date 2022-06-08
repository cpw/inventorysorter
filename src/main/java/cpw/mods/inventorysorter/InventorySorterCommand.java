package cpw.mods.inventorysorter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.fml.loading.StringUtils;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class InventorySorterCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> invsorterBuilder = Commands.literal("invsorter").
                requires(cs->cs.hasPermission(1));

        Stream.of(CommandAction.values()).forEach(a->invsorterBuilder.then(a.buildCommand()));
        invsorterBuilder.executes(InventorySorterCommand::help);
        dispatcher.register(invsorterBuilder);
    }

    private static int help(final CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("inventorysorter.commands.inventorysorter.usage"));
        return 0;
    }

    private static SuggestionProvider<CommandSourceStack> suggester(Supplier<Stream<ResourceLocation>> containers) {
        return (ctx, suggestionbuilder) -> SharedSuggestionProvider.suggestResource(containers.get(), suggestionbuilder);
    }
    private enum CommandAction {
        BLADD(InventorySorter::blackListAdd, 1, Commands.argument("container", new ContainerResourceLocationArgument()).suggests(suggester(InventorySorter::listContainers))),
        BLREMOVE(InventorySorter::blackListRemove, 4, Commands.argument("container", new ContainerResourceLocationArgument()).suggests(suggester(InventorySorter::listBlacklist))),
        SHOWLAST(InventorySorter::showLast, 1, null),
        LIST(InventorySorter::showBlacklist, 1, null);

        private final int permissionLevel;
        private RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> suggester;
        private final ToIntFunction<CommandContext<CommandSourceStack>> action;

        CommandAction(final ToIntFunction<CommandContext<CommandSourceStack>> action, final int permissionLevel, final RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> suggester) {
            this.action = action;
            this.permissionLevel = permissionLevel;
            this.suggester = suggester;
        }

        public ArgumentBuilder<CommandSourceStack, ?> buildCommand() {
            final var base = Commands.literal(StringUtils.toLowerCase(name()))
                    .requires(cs -> cs.hasPermission(permissionLevel));
            if (this.suggester != null)
                base.then(this.suggester.executes(this.action::applyAsInt));
            else
                base.executes(this.action::applyAsInt);
            return base;
        }
    }

    public static class ContainerResourceLocationArgument implements ArgumentType<ResourceLocation> {
        private static final List<String> EXAMPLES = Collections.singletonList("minecraft:chest");
        @Override
        public ResourceLocation parse(final StringReader reader) throws CommandSyntaxException {
            return ResourceLocation.read(reader);
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }
}
