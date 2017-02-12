/*
 *     Copyright © 2016 cpw
 *     This file is part of Inventorysorter.
 *
 *     Inventorysorter is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Inventorysorter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Inventorysorter.  If not, see <http://www.gnu.org/licenses/>.
 */

package cpw.mods.inventorysorter;

import net.minecraftforge.fml.common.FMLCommonHandler;
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
    public IMessage onMessage(final Network.ActionMessage message, final MessageContext ctx)
    {
        FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable() {
            @Override public void run()
            {
                InventorySorter.INSTANCE.log.log(Level.DEBUG, "Got action {} slot {}", message.action, message.slotIndex);
                Slot slot = ctx.getServerHandler().playerEntity.openContainer.getSlot(message.slotIndex);
                message.action.execute(new Action.ActionContext(slot, ctx.getServerHandler().playerEntity));
            }
        });
        return null;
    }
}
