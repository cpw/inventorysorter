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
