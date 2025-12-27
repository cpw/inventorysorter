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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.fml.loading.StringUtils;

public class InventorySorterCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> invsorterBuilder = Commands.literal("invsorter").
                requires(cs->cs.permissions().hasPermission(Permissions.COMMANDS_MODERATOR));

        Stream.of(CommandAction.values()).forEach(a->invsorterBuilder.then(a.buildCommand()));
        invsorterBuilder.executes(InventorySorterCommand::help);
        dispatcher.register(invsorterBuilder);
    }

    private static int help(final CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("inventorysorter.commands.inventorysorter.usage"));
        return 0;
    }

    private static SuggestionProvider<CommandSourceStack> suggester(Supplier<Stream<Identifier>> containers) {
        return (ctx, suggestionbuilder) -> SharedSuggestionProvider.suggestResource(containers.get(), suggestionbuilder);
    }
    private enum CommandAction {
        BLADD(InventorySorter::blackListAdd, Permissions.COMMANDS_MODERATOR, Commands.argument("container", new ContainerIdentifierArgument()).suggests(suggester(InventorySorter::listContainers))),
        BLREMOVE(InventorySorter::blackListRemove, Permissions.COMMANDS_OWNER, Commands.argument("container", new ContainerIdentifierArgument()).suggests(suggester(InventorySorter::listBlacklist))),
        SHOWLAST(InventorySorter::showLast, Permissions.COMMANDS_MODERATOR, null),
        LIST(InventorySorter::showBlacklist, Permissions.COMMANDS_MODERATOR, null);

        private final Permission permissionLevel;
        private RequiredArgumentBuilder<CommandSourceStack, Identifier> suggester;
        private final ToIntFunction<CommandContext<CommandSourceStack>> action;

        CommandAction(final ToIntFunction<CommandContext<CommandSourceStack>> action, final Permission permissionLevel, final RequiredArgumentBuilder<CommandSourceStack, Identifier> suggester) {
            this.action = action;
            this.permissionLevel = permissionLevel;
            this.suggester = suggester;
        }

        public ArgumentBuilder<CommandSourceStack, ?> buildCommand() {
            final var base = Commands.literal(StringUtils.toLowerCase(name()))
                    .requires(cs -> cs.permissions().hasPermission(permissionLevel));
            if (this.suggester != null)
                base.then(this.suggester.executes(this.action::applyAsInt));
            else
                base.executes(this.action::applyAsInt);
            return base;
        }
    }

    public static class ContainerIdentifierArgument implements ArgumentType<Identifier> {
        private static final List<String> EXAMPLES = Collections.singletonList("minecraft:chest");
        @Override
        public Identifier parse(final StringReader reader) throws CommandSyntaxException {
            return Identifier.read(reader);
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }
}
