package cpw.mods.inventorysorter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.jodah.typetools.TypeResolver;
import net.minecraft.command.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.loading.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.*;

public class InventorySorterCommand {
    public static void register(final CommandDispatcher<CommandSource> dispatcher) {
        final LiteralArgumentBuilder<CommandSource> invsorterBuilder = Commands.literal("invsorter").
                requires(cs->cs.hasPermissionLevel(1));

        Stream.of(CommandAction.values()).forEach(a->invsorterBuilder.then(a.getCommand()));
        invsorterBuilder.executes(InventorySorterCommand::help);
        dispatcher.register(invsorterBuilder);
    }

    private static int help(final CommandContext<CommandSource> context) {
        context.getSource().sendErrorMessage(new TranslationTextComponent("inventorysorter.commands.inventorysorter.usage"));
        return 0;
    }

    private enum CommandAction {
        BLADD(InventorySorter::blackListAdd, 1, Arguments.CONTAINER),
        BLREMOVE(InventorySorter::blackListRemove, 4, Arguments.BLACKLISTED),
        SHOWLAST(InventorySorter::showLast, 1),
        LIST(InventorySorter::showBlacklist, 1);

        private final int permissionLevel;
        private final ToIntFunction<CommandContext<CommandSource>> action;
        private final List<TypedArgumentHandler<?>> argumentSupplier;

        CommandAction(final ToIntFunction<CommandContext<CommandSource>> action, final int permissionLevel, final TypedArgumentHandler<?>... argumentSupplier) {
            this.action = action;
            this.permissionLevel = permissionLevel;
            this.argumentSupplier = Arrays.asList(argumentSupplier);
        }

        private void addArguments(LiteralArgumentBuilder<CommandSource> builder) {
            final Optional<ArgumentBuilder<CommandSource, ?>> argBuilder = argumentSupplier.stream()
                    .<ArgumentBuilder<CommandSource, ?>>map(TypedArgumentHandler::build)
                    .reduce(ArgumentBuilder::then);
            ifPresentOrElse(argBuilder, b -> builder.then(b.executes(this.action::applyAsInt)), ()->builder.executes(this.action::applyAsInt));
        }

        public LiteralArgumentBuilder<CommandSource> getCommand() {
            final LiteralArgumentBuilder<CommandSource> base = Commands.literal(StringUtils.toLowerCase(name()))
                    .requires(cs -> cs.hasPermissionLevel(permissionLevel));
            addArguments(base);
            return base;
        }

    }

    public static class Arguments {
        static final TypedArgumentHandler<ResourceLocation> CONTAINER = new TypedArgumentHandler<>("container", () -> new ContainerClassArgument(InventorySorter::listContainers));
        static final TypedArgumentHandler<ResourceLocation> BLACKLISTED = new TypedArgumentHandler<>("blacklisted", () -> new ContainerClassArgument(InventorySorter::listBlacklist));
    }

    public static class TypedArgumentHandler<T> {
        private final String argName;
        private final ArgumentType<T> argumentType;
        private final Class<T> clazz;

        @SuppressWarnings("unchecked")
        TypedArgumentHandler(final String argName, final Supplier<? extends ArgumentType<T>> argumentType) {
            this.argName = argName;
            this.argumentType = argumentType.get();
            final Class<?>[] classes = TypeResolver.resolveRawArguments(ArgumentType.class, this.argumentType.getClass());
            this.clazz = (Class<T>) classes[0];
        }

        public static <A> TypedArgumentHandler<A> of(final String argumentName, final Supplier<? extends ArgumentType<A>> supplier) {
            return new TypedArgumentHandler<>(argumentName, supplier);
        }

        public RequiredArgumentBuilder<CommandSource, T> build() {
            return Commands.argument(argName, this.argumentType);
        }

        public T get(final CommandContext<CommandSource> context) {
            return context.getArgument(argName, this.clazz);
        }
    }

    public static class ContainerClassArgument implements ArgumentType<ResourceLocation> {
        private static final List<String> EXAMPLES = Collections.singletonList("minecraft:chest");
        private final Supplier<Stream<String>> containerSuggestions;

        ContainerClassArgument(Supplier<Stream<String>> suggestions) {
            containerSuggestions = suggestions;
        }

        @Override
        public ResourceLocation parse(final StringReader reader) throws CommandSyntaxException {
            return ResourceLocation.read(reader);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
            return ISuggestionProvider.suggest(containerSuggestions.get(), builder);
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }


    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }

}
