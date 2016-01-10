/*
 *     Copyright
 *     This file is part of inventorysorter.
 *
 *     Foobar is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with inventorysorter.  If not, see <http://www.gnu.org/licenses/>.
 */

package cpw.mods.inventorysorter;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Created by cpw on 08/01/16.
 */
public final class Network
{
    public static class ActionMessage implements IMessage
    {
        public Action action;
        public int slotIndex;

        public ActionMessage()
        {

        }

        public ActionMessage(Action action, int slotIndex)
        {
            this.action = action;
            this.slotIndex = slotIndex;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.action = Action.values()[buf.readByte()];
            this.slotIndex = buf.readInt();
        }
        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeByte(action.ordinal());
            buf.writeInt(slotIndex);
        }
    }
}
