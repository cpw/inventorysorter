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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Created by cpw on 08/01/16.
 */
public final class Network {
    private static final String PROTOCOL_VERSION = Integer.toString(1);

    public static void init(IEventBus bus) {
        bus.addListener(Network::registerEvent);

    }

    public static void registerEvent(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar("inventorysorter").versioned(PROTOCOL_VERSION);
        registrar.play(ActionMessage.ID, ActionMessage::fromBytes, ServerHandler::onMessage);
    }

    public static class ActionMessage implements CustomPacketPayload
    {
        Action action;
        int slotIndex;
        public static ResourceLocation ID = new ResourceLocation("inventorysorter", "net");

        ActionMessage(Action action, int slotIndex)
        {
            this.action = action;
            this.slotIndex = slotIndex;
        }

        static ActionMessage fromBytes(FriendlyByteBuf buf)
        {
            return new ActionMessage(Action.values()[buf.readByte()], buf.readInt());
        }

        @Override
        public void write(FriendlyByteBuf buf)
        {
            buf.writeByte(action.ordinal());
            buf.writeInt(slotIndex);
        }

        @Override
        public @NotNull ResourceLocation id()
        {
            return ID;
        }
    }
}
