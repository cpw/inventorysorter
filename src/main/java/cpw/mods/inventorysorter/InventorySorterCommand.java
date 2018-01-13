package cpw.mods.inventorysorter;

import net.minecraft.command.*;
import net.minecraft.server.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;

import javax.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class InventorySorterCommand extends CommandBase {

    public static final TextComponentTranslation NOOP_COMMAND = new TextComponentTranslation("inventorysorter.commands.inventorysorter.noop");

    private enum CommandAction {
        BLADD(InventorySorter::blackListAdd), BLREMOVE(InventorySorter::blackListRemove), SHOW(InventorySorter::showLast), LIST(InventorySorter::showBlacklist);

        private final Supplier<TextComponentTranslation> action;

        CommandAction(final Supplier<TextComponentTranslation> action) {
            this.action = action;
        }

        static final List<String> lowerNames;
        static {
            lowerNames = Stream.of(values()).map(Enum::name).map(s->s.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
        }
    }

    public InventorySorterCommand() {
    }

    @Override
    public String getName() {
        return "inventorysorter";
    }

    @Override
    public String getUsage(final ICommandSender sender) {
        return "inventorysorter.commands.inventorysorter.usage";
    }

    @Override
    public List<String> getTabCompletions(final MinecraftServer server, final ICommandSender sender, final String[] args, @Nullable final BlockPos targetPos) {
        if (args.length >=1) {
            return getListOfStringsMatchingLastWord(args, CommandAction.lowerNames);
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(final MinecraftServer server, final ICommandSender sender, final String[] args) throws CommandException {
        if (args.length >=1) {
            final String op = args[0];
            try {
                final CommandAction action = Enum.valueOf(CommandAction.class, op.toUpperCase(Locale.ROOT));
                sender.sendMessage(action.action.get());
                return;
            } catch (IllegalArgumentException iae) {
                // Noop (the command was missing)
            }
        }
        sender.sendMessage(NOOP_COMMAND);
    }
}
