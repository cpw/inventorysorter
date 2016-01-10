package cpw.mods.inventorysorter;

import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.logging.log4j.Level;

/**
 * Created by cpw on 08/01/16.
 */
public class ServerHandler implements IMessageHandler<Network.ActionMessage, IMessage>
{
    @Override
    public IMessage onMessage(Network.ActionMessage message, MessageContext ctx)
    {
        InventorySorter.INSTANCE.log.log(Level.DEBUG, "Got action %s slot %d", message.action, message.slotIndex);
        Slot slot = ctx.getServerHandler().playerEntity.openContainer.getSlot(message.slotIndex);
        message.action.execute(new Action.ActionContext(slot, ctx.getServerHandler().playerEntity));
        return null;
    }
}
