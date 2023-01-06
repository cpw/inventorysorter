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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
    private final Map<KeyMapping, Action> keyBindingMap;

    KeyHandler() {
        // Custom input mapping for wheel up (-1)
        InputConstants.Type.MOUSE.getOrCreate(99);
        // Custom input mapping for wheel down (-1)
        InputConstants.Type.MOUSE.getOrCreate(101);

        keyBindingMap = Stream.of(Action.values())
                .map(a -> new AbstractMap.SimpleEntry<>(a, new KeyMapping(a.getKeyBindingName(), KeyConflictContext.GUI,
                        InputConstants.Type.MOUSE, a.getDefaultKeyCode(), "keygroup.inventorysorter")))
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onKeyMappingEvent);

        var eh = new ScreenEventHandler();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, eh::onKey);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, eh::onMouse);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, eh::onScroll);
    }

    static void init() {
        keyHandler = new KeyHandler();
    }

    public void onKeyMappingEvent(RegisterKeyMappingsEvent evt) {
        keyBindingMap.keySet().forEach(evt::register);
    }
    private class ScreenEventHandler {
        private void onKey(ScreenEvent.KeyPressed.Pre evt) {
            onInputEvent(evt, KeyHandler.this::keyEvaluate);
        }

        private void onMouse(ScreenEvent.MouseButtonPressed.Pre evt) {
            onInputEvent(evt, KeyHandler.this::mouseClickEvaluate);
        }

        private void onScroll(ScreenEvent.MouseScrolled.Pre evt) {
            onInputEvent(evt, KeyHandler.this::mouseScrollEvaluate);
        }
    }
    private boolean keyEvaluate(final KeyMapping kb, final ScreenEvent.KeyPressed.Pre evt) {
        return kb.matches(evt.getKeyCode(), evt.getScanCode());
    }

    private boolean mouseClickEvaluate(final KeyMapping kb, final ScreenEvent.MouseButtonPressed.Pre evt) {
        return kb.matchesMouse(evt.getButton());
    }

    private boolean mouseScrollEvaluate(final KeyMapping kb, final ScreenEvent.MouseScrolled.Pre evt) {
        int dir = (int) Math.signum(evt.getScrollDelta());
        int keycode = dir + 100;
        return kb.matchesMouse(keycode);
    }

    private <T extends ScreenEvent> void onInputEvent(T evt, BiPredicate<KeyMapping, T> kbTest) {
        // Don't sort on spectator
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null && gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        final Screen gui = evt.getScreen();
        if (!(gui instanceof final AbstractContainerScreen<?> guiContainer && !(gui instanceof CreativeModeInventoryScreen))) {
            return;
        }
        Slot slot = guiContainer.getSlotUnderMouse();
        if (!ContainerContext.validSlot(slot)) {
            InventorySorter.LOGGER.debug("Skipping action handling for blacklisted slot");
            return;
        }

        final Optional<Action> action = keyBindingMap.entrySet().stream().filter(e -> kbTest.test(e.getKey(), evt)).
                map(Map.Entry::getValue).findFirst();
        if (action.isEmpty()) return;

        final Action triggeredAction = action.get();
        if (triggeredAction.isActive())
        {
            if (guiContainer.getMenu() != null && guiContainer.getMenu().slots != null && guiContainer.getMenu().slots.contains(slot))
            {
                InventorySorter.LOGGER.debug("Sending action {} slot {}", triggeredAction, slot.index);
                Network.channel.sendToServer(triggeredAction.message(slot));
                evt.setCanceled(true);
            }
        }

    }
}