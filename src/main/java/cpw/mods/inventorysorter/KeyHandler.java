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

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.apache.logging.log4j.*;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by cpw on 08/01/16.
 */
public class KeyHandler
{
    private static KeyHandler keyHandler;
    private final Map<KeyBinding, Action> keyBindingMap;

    KeyHandler() {
        // Custom input mapping for wheel up (-1)
        InputMappings.Type.MOUSE.getOrMakeInput(99);
        // Custom input mapping for wheel down (-1)
        InputMappings.Type.MOUSE.getOrMakeInput(101);

        keyBindingMap = Stream.of(Action.values())
                .map(a -> new AbstractMap.SimpleEntry<>(a, new KeyBinding(a.getKeyBindingName(), KeyConflictContext.GUI,
                        InputMappings.Type.MOUSE, a.getDefaultKeyCode(), "keygroup.inventorysorter")))
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        keyBindingMap.keySet().forEach(ClientRegistry::registerKeyBinding);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onKey);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onMouse);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onScroll);
    }

    static void init() {
        keyHandler = new KeyHandler();
    }

    private void onKey(GuiScreenEvent.KeyboardKeyPressedEvent.Pre evt) {
        onInputEvent(evt, this::keyEvaluate);
    }

    private void onMouse(GuiScreenEvent.MouseClickedEvent.Pre evt) {
        onInputEvent(evt, this::mouseClickEvaluate);
    }

    private void onScroll(GuiScreenEvent.MouseScrollEvent.Post evt) {
        onInputEvent(evt, this::mouseScrollEvaluate);
    }

    private boolean keyEvaluate(final KeyBinding kb, final GuiScreenEvent.KeyboardKeyPressedEvent.Pre evt) {
        return kb.matchesKey(evt.getKeyCode(), evt.getScanCode());
    }

    private boolean mouseClickEvaluate(final KeyBinding kb, final GuiScreenEvent.MouseClickedEvent.Pre evt) {
        return kb.matchesMouseKey(evt.getButton());
    }

    private boolean mouseScrollEvaluate(final KeyBinding kb, final GuiScreenEvent.MouseScrollEvent.Post evt) {
        int dir = (int) Math.signum(evt.getScrollDelta());
        int keycode = dir + 100;
        return kb.matchesMouseKey(keycode);
    }

    private <T extends GuiScreenEvent> void onInputEvent(T evt, BiPredicate<KeyBinding, T> kbTest) {
        final Screen gui = evt.getGui();
        if (!(gui instanceof ContainerScreen && !(gui instanceof CreativeScreen))) {
            return;
        }
        final ContainerScreen guiContainer = (ContainerScreen) gui;
        Slot slot = guiContainer.getSlotUnderMouse();
        if (!ContainerContext.validSlot(slot)) {
            InventorySorter.LOGGER.log(Level.DEBUG, "Skipping action handling for blacklisted slot");
            return;
        }
        final Optional<Action> action = keyBindingMap.entrySet().stream().filter(e -> kbTest.test(e.getKey(), evt)).
                map(Map.Entry::getValue).findFirst();
        if (!action.isPresent()) return;

        final Action triggeredAction = action.get();
        if (triggeredAction.isActive())
        {
            if (guiContainer.getContainer() != null && guiContainer.getContainer().inventorySlots != null && guiContainer.getContainer().inventorySlots.contains(slot))
            {
                InventorySorter.LOGGER.debug("Sending action {} slot {}", triggeredAction, slot.slotNumber);
                Network.channel.sendToServer(triggeredAction.message(slot));
                evt.setCanceled(true);
            }
        }

    }
}