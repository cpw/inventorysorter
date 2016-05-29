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

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Created by cpw on 08/01/16.
 */
public class KeyHandler
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onKey(GuiScreenEvent.MouseInputEvent.Pre evt)
    {
        Action action = Action.interpret(new KeyStates());
        if (action != null && action.isActive() && evt.getGui() instanceof GuiContainer && !(evt.getGui() instanceof GuiContainerCreative))
        {
            final GuiContainer guiContainer = (GuiContainer)evt.getGui();
            Slot slot = guiContainer.getSlotUnderMouse();
            if (slot == null) return;
            if (guiContainer.inventorySlots != null && guiContainer.inventorySlots.inventorySlots != null && guiContainer.inventorySlots.inventorySlots.contains(slot))
            {
                InventorySorter.INSTANCE.log.log(Level.DEBUG, "Sending action {} slot {}", action, slot.slotNumber);
                InventorySorter.INSTANCE.channel.sendToServer(action.message(slot));
                evt.setCanceled(true);
            }
        }
    }

    static class KeyStates
    {
        private final boolean leftMouse;
        private final boolean middleMouse;
        private final boolean rightMouse;
        private final boolean shiftDown;
        private final boolean ctrlDown;
        private final boolean altDown;
        private final boolean space;
        private final int mouseWheel;
        private final boolean isDownClick;

        KeyStates()
        {
            this.leftMouse = Mouse.getEventButton() == 0;
            this.middleMouse = Mouse.getEventButton() == 2;
            this.rightMouse = Mouse.getEventButton() == 1;
            this.isDownClick = Mouse.getEventButtonState();
            this.shiftDown = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
            this.ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            this.altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
            this.space = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
            this.mouseWheel = Mouse.getEventDWheel();
        }

        public boolean isLeftMouse()
        {
            return leftMouse;
        }

        public boolean isMiddleMouse()
        {
            return middleMouse;
        }

        public boolean isRightMouse()
        {
            return rightMouse;
        }

        public boolean isShiftDown()
        {
            return shiftDown;
        }

        public boolean isCtrlDown()
        {
            return ctrlDown;
        }

        public boolean isAltDown()
        {
            return altDown;
        }

        public boolean isSpace()
        {
            return space;
        }

        public int getMouseWheel()
        {
            return mouseWheel;
        }

        public boolean mouseWheelRollingUp()
        {
            return mouseWheel > 0;
        }

        public boolean mouseWheelRollingDown()
        {
            return mouseWheel < 0;
        }

        public boolean isDownClick() { return isDownClick; }
    }
}
