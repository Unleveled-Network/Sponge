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
package org.spongepowered.common.mixin.api.minecraft.world.entity.projectile;

import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.projectile.explosive.fireball.ExplosiveFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.entity.projectile.LargeFireballBridge;

import java.util.Set;

@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin_API extends FireballMixin_API implements ExplosiveFireball {

    @Shadow public int explosionPower;

    @Override
    public void detonate() {
        final boolean flag = this.shadow$getCommandSenderWorld().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        final Explosion.BlockInteraction mode = flag ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE;
        ((LargeFireballBridge) this).bridge$throwExplosionEventAndExplode(this.shadow$getCommandSenderWorld(), null, this.shadow$getX(),
                this.shadow$getY(), this.shadow$getZ(), this.explosionPower, flag, mode);
        this.shadow$remove();
    }

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        values.add(this.requireValue(Keys.CAN_GRIEF).asImmutable());

        this.getValue(Keys.EXPLOSION_RADIUS).map(Value::asImmutable).ifPresent(values::add);

        return values;
    }

}
