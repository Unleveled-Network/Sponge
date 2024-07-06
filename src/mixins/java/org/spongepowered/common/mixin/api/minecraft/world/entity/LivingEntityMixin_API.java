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
package org.spongepowered.common.mixin.api.minecraft.world.entity;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.attribute.Attribute;
import org.spongepowered.api.entity.attribute.type.AttributeType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.entity.projectile.ProjectileUtil;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;
import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_API extends EntityMixin_API implements Living {

    // @formatter:off
    @Shadow public abstract float shadow$getHealth();
    @Shadow public abstract AttributeInstance shadow$getAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute);
    @Shadow public abstract boolean addEffect(MobEffectInstance p_195064_1_);
    // @formatter:on

    @Override
    public Component teamRepresentation() {
        return Component.text(this.shadow$getUUID().toString());
    }

    @Override
    public Optional<Attribute> attribute(final AttributeType type) {
        Preconditions.checkNotNull(type, "AttributeType cannot be null");
        return Optional.ofNullable((Attribute) this.shadow$getAttribute((net.minecraft.world.entity.ai.attributes.Attribute) type));
    }

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        values.add(this.requireValue(Keys.ABSORPTION).asImmutable());
        values.add(this.requireValue(Keys.ACTIVE_ITEM).asImmutable());
        values.add(this.requireValue(Keys.BODY_ROTATIONS).asImmutable());
        values.add(this.requireValue(Keys.CHEST_ROTATION).asImmutable());
        values.add(this.requireValue(Keys.HEAD_ROTATION).asImmutable());
        values.add(this.requireValue(Keys.HEALTH).asImmutable());
        values.add(this.requireValue(Keys.IS_ELYTRA_FLYING).asImmutable());
        values.add(this.requireValue(Keys.LAST_DAMAGE_RECEIVED).asImmutable());
        values.add(this.requireValue(Keys.MAX_HEALTH).asImmutable());
        values.add(this.requireValue(Keys.POTION_EFFECTS).asImmutable());
        values.add(this.requireValue(Keys.SCALE).asImmutable());
        values.add(this.requireValue(Keys.STUCK_ARROWS).asImmutable());
        values.add(this.requireValue(Keys.WALKING_SPEED).asImmutable());

        this.getValue(Keys.CAN_GRIEF).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.LAST_ATTACKER).map(Value::asImmutable).ifPresent(values::add);

        return values;
    }

    @Override
    public <T extends Projectile> Optional<T> launchProjectile(final EntityType<T> projectileType) {
        return ProjectileUtil.launch(requireNonNull(projectileType, "projectileType"), this, null);
    }

    @Override
    public <T extends Projectile> Optional<T> launchProjectile(final EntityType<T> projectileType, final Vector3d velocity) {
        return ProjectileUtil.launch(requireNonNull(projectileType, "projectileType"), this, requireNonNull(velocity, "velocity"));
    }

    @Override
    public <T extends Projectile> Optional<T> launchProjectileTo(final EntityType<T> projectileType, final Entity target) {
        // TODO implement this for all LivingEntities ?
        return Optional.empty();
    }
}
