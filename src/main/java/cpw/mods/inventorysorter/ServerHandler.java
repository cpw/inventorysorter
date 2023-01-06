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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Created by cpw on 08/01/16.
 */
public class ServerHandler
{
    static boolean onMessage(Network.ActionMessage message, Supplier<NetworkEvent.Context> ctx)
    {
        final ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            ctx.get().enqueueWork(() -> {
                InventorySorter.LOGGER.debug("Got action {} slot {}", message.action, message.slotIndex);
                Slot slot = sender.containerMenu.getSlot(message.slotIndex);
                message.action.execute(new ContainerContext(slot, sender));
            });
            return true;
        } else {
            return false;
        }
    }
}
