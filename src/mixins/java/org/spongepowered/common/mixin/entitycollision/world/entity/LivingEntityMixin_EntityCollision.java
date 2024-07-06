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
package org.spongepowered.common.mixin.entitycollision.world.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.mixin.entitycollision.entity.EntityMixin_EntityCollision;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_EntityCollision extends EntityMixin_EntityCollision {

    @Shadow protected abstract void shadow$doPush(Entity entity);

    private boolean runningCollideWithNearby = false;

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;pushEntities()V"))
    private void collisions$canUpdateCollisions(final CallbackInfo ci) {
        this.runningCollideWithNearby = true;
    }

    @Inject(method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;pushEntities()V",
            shift = Shift.AFTER))
    private void collisions$resetCanUpdateCollisions(final CallbackInfo ci) {
        this.runningCollideWithNearby = false;
    }

    @Override
    public boolean collision$isRunningCollideWithNearby() {
        return this.runningCollideWithNearby;
    }

    // This injection allows maxEntityCramming to be applied first before checking for max collisions
    @Redirect(method = "pushEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", remap = false))
    private int collisions$pushEntities(final List<Entity> list) {
        for (final Entity entity: list) {
            // ignore players and entities with parts (ex. EnderDragon)
            if (this.shadow$getCommandSenderWorld().isClientSide() || entity == null || entity instanceof Player || entity instanceof EnderDragon) {
                continue;
            }

            if (this.collision$requiresCollisionsCacheRefresh()) {
                this.collision$initializeCollisionState(this.shadow$getCommandSenderWorld());
                this.collision$requiresCollisionsCacheRefresh(false);
            }

            if (this.collision$getMaxCollisions() >= 0 && list.size() >= this.collision$getMaxCollisions()) {
                // Don't process any more collisions
                break;
            }
            this.shadow$doPush(entity);
        }
        // We always return '0' to prevent the original loop from running.
        return 0;
    }
}
