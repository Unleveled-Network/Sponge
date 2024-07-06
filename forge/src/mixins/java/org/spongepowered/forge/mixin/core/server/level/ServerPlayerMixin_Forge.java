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
package org.spongepowered.forge.mixin.core.server.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.ITeleporter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.world.portal.PortalLogic;
import org.spongepowered.forge.mixin.core.world.entity.LivingEntityMixin_Forge;

@Mixin(value = ServerPlayer.class, priority = 1300)
public abstract class ServerPlayerMixin_Forge extends LivingEntityMixin_Forge implements ServerPlayerBridge {

    @Override
    public double bridge$reachDistance() {
        return this.shadow$getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue();
    }

    /**
     * @author dualspiral - 18th December 2020 - 1.16.4
     * @reason Redirects the Forge changeDimension method to our own
     *         to support our event and other logic (see
     *         ServerPlayerEntityMixin on the common mixin sourceset for
     *         details).
     *
     *         This will get called on the nether dimension changes, as the
     *         end portal teleport call itself has been redirected to provide
     *         the correct type.
     */
    @Overwrite
    @Nullable // should be javax.annotations.Nullable
    public Entity changeDimension(final ServerLevel serverLevel, final ITeleporter teleporter) {
        return ((EntityBridge) this).bridge$changeDimension(serverLevel, (PortalLogic) teleporter);
    }

    // override from LivingEntityMixin_Forge
    @Override
    protected void forge$onElytraUse(final CallbackInfo ci) {
        final PhaseContext<?> context = PhaseTracker.SERVER.getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        final net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) (Object) this;
        try (final EffectTransactor ignored = transactor.logPlayerInventoryChangeWithEffect(player, PlayerInventoryTransaction.EventCreator.STANDARD)) {
            player.inventoryMenu.broadcastChanges(); // capture
        }
    }

}
