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

import io.netty.buffer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.*;

import java.util.Objects;

/**
 * Created by cpw on 08/01/16.
 */
public final class Network
{
    private static ResourceLocation invsorter = new ResourceLocation("inventorysorter","net");

    public static void init() {

    }

    public static class ActionMessage
    {
        Action action;
        int slotIndex;

        ActionMessage(Action action, int slotIndex)
        {
            this.action = action;
            this.slotIndex = slotIndex;
        }

        static ActionMessage fromBytes(ByteBuf buf)
        {
            return new ActionMessage(Action.values()[buf.readByte()],buf.readInt());
        }

        void toBytes(ByteBuf buf)
        {
            buf.writeByte(action.ordinal());
            buf.writeInt(slotIndex);
        }
    }


    static SimpleChannel channel;
    static {
        channel = NetworkRegistry.ChannelBuilder.named(invsorter).
                clientAcceptedVersions(s -> Objects.equals(s, "1")).
                serverAcceptedVersions(s -> Objects.equals(s, "1")).
                networkProtocolVersion(() -> "1").
                simpleChannel();

        channel.messageBuilder(ActionMessage.class, 1).
                decoder(ActionMessage::fromBytes).
                encoder(ActionMessage::toBytes).
                consumer(ServerHandler::onMessage).
                add();
    }
}
