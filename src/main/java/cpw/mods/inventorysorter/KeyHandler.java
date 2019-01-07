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

import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.resources.*;
import net.minecraft.client.settings.*;
import net.minecraft.inventory.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;
import org.apache.commons.lang3.*;
import org.apache.logging.log4j.*;

import javax.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Created by cpw on 08/01/16.
 */
public class KeyHandler
{
//    private final Map<KeyBinding, Action> keyBindingMap;

    KeyHandler() {
//        keyBindingMap = Stream.of(Action.values())
//                .map(a -> new AbstractMap.SimpleEntry<>(a, new KeyBinding(a.getKeyBindingName(), KeyConflictContext.GUI, a.getDefaultKeyCode(), "keygroup.inventorysorter") {
//                    @Override
//                    @Nonnull
//                    public String func_197978_k() {
//                        if (getKey() == -200) return I18n.format("key.inventorysorter.mousewheelup");
//                        else if (getKey() == -201) return I18n.format("key.inventorysorter.mousewheeldown");
//                        else return super.func_197978_k();
//                    }
//                })).collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
//
//        Minecraft.getInstance().gameSettings.keyBindings = ArrayUtils.addAll(Minecraft.getInstance().gameSettings.keyBindings, keyBindingMap.keySet().toArray(new KeyBinding[0]));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onKey);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onMouse);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onScroll);
    }

    private void onKey(GuiScreenEvent.KeyboardKeyPressedEvent.Pre evt) {
        onInputEvent(evt, this::tryKeys);
    }

    private void onMouse(GuiScreenEvent.MouseClickedEvent.Pre evt) {
        onInputEvent(evt, this::tryMouseButtons);
    }

    private void onScroll(GuiScreenEvent.MouseScrollEvent.Post evt) {
        onInputEvent(evt, this::tryMouseScroll);
    }

    private Action tryMouseScroll(GuiScreenEvent.MouseScrollEvent.Post evt) {
        double dir = Math.signum(evt.getScrollDelta());
        if (dir < 0.0) {
            return Action.ONEITEMIN;
        } else if (dir > 0.0) {
            return Action.ONEITEMOUT;
        }
        return null;
    }

    private <T extends GuiScreenEvent> void onInputEvent(T evt, Function<T,Action> actionSupplier) {
        final GuiScreen gui = evt.getGui();
        if (!(gui instanceof GuiContainer && !(gui instanceof GuiContainerCreative))) {
            return;
        }
        final GuiContainer guiContainer = (GuiContainer) gui;
        Slot slot = guiContainer.getSlotUnderMouse();
        if (!ContainerContext.validSlot(slot)) {
            InventorySorter.LOGGER.log(Level.DEBUG, "Skipping action handling for blacklisted slot");
            return;
        }
        Action triggered = actionSupplier.apply(evt);
        if (triggered == null) return;

        if (triggered.isActive())
        {
            if (guiContainer.inventorySlots != null && guiContainer.inventorySlots.inventorySlots != null && guiContainer.inventorySlots.inventorySlots.contains(slot))
            {
                InventorySorter.LOGGER.log(Level.DEBUG, "Sending action {} slot {}", triggered, slot.slotNumber);
                Network.channel.sendToServer(triggered.message(slot));
                evt.setCanceled(true);
            }
        }

    }

    private Action tryMouseButtons(GuiScreenEvent.MouseClickedEvent.Pre evt) {
        if (evt.getButton() == 2) return Action.SORT;
        return null;
    }

    private Action tryKeys(GuiScreenEvent.KeyboardKeyPressedEvent.Pre evt) {
/*
        // Not on key up
        if (!Keyboard.getEventKeyState()) return null;
        int key = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();

        Action triggered = null;
        if (key != 0 && !Keyboard.isRepeatEvent()) {
            for (Map.Entry<KeyBinding, Action> entry : keyBindingMap.entrySet()) {
                if (entry.getKey().isActiveAndMatches(key)) {
                    triggered = entry.getValue();
                    break;
                }
            }
        }
        return triggered;
*/
        return null;
    }
}