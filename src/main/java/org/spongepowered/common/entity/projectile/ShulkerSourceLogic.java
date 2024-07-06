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
package org.spongepowered.common.entity.projectile;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.golem.Shulker;
import org.spongepowered.api.entity.projectile.Projectile;

import java.util.Optional;

public final class ShulkerSourceLogic implements ProjectileSourceLogic<Shulker> {

    @SuppressWarnings("unchecked")
    @Override
    public <P extends Projectile> Optional<P> launch(final ProjectileLogic<P> logic, final Shulker source,
            final EntityType<P> projectileType, final Object... args) {
        if (projectileType == EntityTypes.SHULKER_BULLET.get() && args.length == 1 && args[0] instanceof Entity) {
            final net.minecraft.world.entity.monster.Shulker shulker = (net.minecraft.world.entity.monster.Shulker) source;
            final ShulkerBullet bullet =
                    new ShulkerBullet(shulker.level, shulker, (Entity) args[0], shulker.getAttachFace().getAxis());
            shulker.level.addFreshEntity(bullet);
            shulker.playSound(SoundEvents.SHULKER_SHOOT,
                    2.0F, (shulker.level.random.nextFloat() - shulker.level.random.nextFloat()) * 0.2F + 1.0F);

            return Optional.of((P) bullet);
        }

        return ProjectileUtil.launch(projectileType, source, null);
    }
}
