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

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.*;

import java.util.function.Supplier;

/**
 * Created by cpw on 08/01/16.
 */
public class ServerHandler
{
    static boolean onMessage(Network.ActionMessage message, Supplier<NetworkEvent.Context> ctx)
    {
        final ServerPlayerEntity sender = ctx.get().getSender();
        if (sender != null) {
            ctx.get().enqueueWork(() -> {
                InventorySorter.LOGGER.log(Level.DEBUG, "Got action {} slot {}", message.action, message.slotIndex);
                Slot slot = sender.openContainer.getSlot(message.slotIndex);
                message.action.execute(new ContainerContext(slot, sender));
            });
            return true;
        } else {
            return false;
        }
    }
}
