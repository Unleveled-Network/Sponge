/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.forge.mixin.core.minecraftforge.event.entity.player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.forge.launch.bridge.event.ForgeEventBridge_Forge;
import org.spongepowered.forge.util.TristateUtil;

@Mixin(value = PlayerInteractEvent.RightClickBlock.class, remap = false)
public abstract class PlayerInteractEvent_RightClickBlockMixin_Forge implements ForgeEventBridge_Forge {

    @Override
    public void bridge$syncFrom(final Event event) {
        final InteractBlockEvent.Secondary spongeEvent = (InteractBlockEvent.Secondary) event;
        final PlayerInteractEvent.RightClickBlock forgeEvent = (PlayerInteractEvent.RightClickBlock) (Object) this;

        forgeEvent.setCanceled(spongeEvent.isCancelled());
        forgeEvent.setUseBlock(TristateUtil.toEventResult(spongeEvent.useBlockResult()));
        forgeEvent.setUseItem(TristateUtil.toEventResult(spongeEvent.useItemResult()));
    }

    @Override
    public void bridge$syncTo(final Event event) {
        final InteractBlockEvent.Secondary spongeEvent = (InteractBlockEvent.Secondary) event;
        final PlayerInteractEvent.RightClickBlock forgeEvent = (PlayerInteractEvent.RightClickBlock) (Object) this;

        spongeEvent.setCancelled(forgeEvent.isCanceled());
        spongeEvent.setUseBlockResult(TristateUtil.fromEventResult(forgeEvent.getUseBlock()));
        spongeEvent.setUseItemResult(TristateUtil.fromEventResult(forgeEvent.getUseItem()));
    }

    @Override
    public @Nullable Event bridge$createSpongeEvent(final CauseStackManager.StackFrame frame) {
        final PlayerInteractEvent.RightClickBlock forgeEvent = (PlayerInteractEvent.RightClickBlock) (Object) this;
        final Level world = forgeEvent.getWorld();
        if (world.isClientSide) {
            return null;
        }

        return SpongeCommonEventFactory.createInteractBlockEventSecondary(forgeEvent.getPlayer(), (ServerLevel) world, forgeEvent.getItemStack(),
                forgeEvent.getHand(), forgeEvent.getHitVec(), frame);
    }
}
