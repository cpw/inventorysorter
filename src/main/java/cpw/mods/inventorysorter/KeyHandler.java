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
import net.minecraftforge.fml.common.eventhandler.*;
import org.apache.commons.lang3.*;
import org.apache.logging.log4j.*;
import org.lwjgl.input.*;

import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Created by cpw on 08/01/16.
 */
public class KeyHandler
{
    private final Map<KeyBinding, Action> keyBindingMap;

    KeyHandler() {
        keyBindingMap = Stream.of(Action.values())
                .map(a -> new AbstractMap.SimpleEntry<>(a, new KeyBinding(a.getKeyBindingName(), KeyConflictContext.GUI, a.getDefaultKeyCode(), "keygroup.inventorysorter") {
                    @Override
                    @Nonnull
                    public String getDisplayName() {
                        if (getKeyCode() == -200) return I18n.format("key.inventorysorter.mousewheelup");
                        else if (getKeyCode() == -201) return I18n.format("key.inventorysorter.mousewheeldown");
                        else return super.getDisplayName();
                    }
                })).collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        Minecraft.getMinecraft().gameSettings.keyBindings = ArrayUtils.addAll(Minecraft.getMinecraft().gameSettings.keyBindings, keyBindingMap.keySet().toArray(new KeyBinding[keyBindingMap.size()]));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre evt) {
        onInputEvent(evt, this::tryKeys);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre evt) {
        onInputEvent(evt, this::tryMouseButtons);
    }

    private void onInputEvent(GuiScreenEvent evt, Supplier<Action> actionSupplier) {
        final GuiScreen gui = evt.getGui();
        if (!(gui instanceof GuiContainer && !(gui instanceof GuiContainerCreative))) {
            return;
        }
        final GuiContainer guiContainer = (GuiContainer) gui;
        Slot slot = guiContainer.getSlotUnderMouse();
        if (!ContainerContext.validSlot(slot)) {
            InventorySorter.INSTANCE.log.log(Level.DEBUG, "Skipping action handling for blacklisted slot");
            return;
        }
        Action triggered = actionSupplier.get();
        if (triggered == null) return;

        if (triggered.isActive())
        {
            if (guiContainer.inventorySlots != null && guiContainer.inventorySlots.inventorySlots != null && guiContainer.inventorySlots.inventorySlots.contains(slot))
            {
                InventorySorter.INSTANCE.log.log(Level.DEBUG, "Sending action {} slot {}", triggered, slot.slotNumber);
                InventorySorter.INSTANCE.channel.sendToServer(triggered.message(slot));
                evt.setCanceled(true);
            }
        }

    }

    private Action tryMouseButtons() {
        int mouse = Mouse.getEventButton() - 100;
        // fake wheel as keycodes
        if (mouse == -101) {
            int mwheel = Mouse.getDWheel();
            mouse = mwheel > 0 ?  -201 : mwheel < 0 ? -200 : -101;
            // reject mouseup
        } else if (!Mouse.getEventButtonState()) return null;

        Action triggered = null;
        for (Map.Entry<KeyBinding, Action> entry : keyBindingMap.entrySet()) {
            if (entry.getKey().isActiveAndMatches(mouse)) {
                triggered = entry.getValue();
                break;
            }
        }
        return triggered;
    }

    private Action tryKeys() {
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
    }
}