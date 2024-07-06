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
package org.spongepowered.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.spongepowered.api.Server;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.DamageFunction;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.cause.entity.damage.source.BlockDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.FallingBlockDamageSource;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.accessor.world.entity.LivingEntityAccessor;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Predicate;

public final class DamageEventUtil {

    public static final DoubleUnaryOperator HARD_HAT_FUNCTION = damage -> -(damage - (damage * 0.75F));

    private DamageEventUtil() {
    }

    public static DoubleUnaryOperator createResistanceFunction(final int resistanceAmplifier) {
        final int base = (resistanceAmplifier + 1) * 5;
        final int modifier = 25 - base;
        return damage -> -(damage - (Math.max(((damage * modifier) / 25.0F), 0.0f)));
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<DamageFunction> createHardHatModifier(final LivingEntity living, final DamageSource damageSource) {
        if ((damageSource instanceof FallingBlockDamageSource) && !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            final DamageModifier modifier = DamageModifier.builder()
                    // TODO - Event Context Key for Helmet
                    .cause(Cause.of(EventContext.empty(), ((ItemStack) (Object) living.getItemBySlot(EquipmentSlot.HEAD)).createSnapshot()))
                    .type(DamageModifierTypes.HARD_HAT)
                    .build();
            return Optional.of(new DamageFunction(modifier, DamageEventUtil.HARD_HAT_FUNCTION));
        }
        return Optional.empty();
    }

    public static Optional<DamageFunction> createArmorModifiers(final LivingEntity living, final DamageSource damageSource) {
        if (damageSource.isBypassArmor()) {
            return Optional.empty();
        }

        final DoubleUnaryOperator function = incomingDamage -> -(incomingDamage - CombatRules.getDamageAfterAbsorb((float) incomingDamage, living.getArmorValue(), (float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
        final DamageFunction armorModifier;
        try (final CauseStackManager.StackFrame frame = ((Server) living.getServer()).causeStackManager().pushCauseFrame()){
            frame.pushCause(living);
            frame.pushCause(Attributes.ARMOR_TOUGHNESS);
            armorModifier = DamageFunction.of(DamageModifier.builder()
                .cause(frame.currentCause())
                .type(DamageModifierTypes.ARMOR)
                .build(), function);
        }

        return Optional.of(armorModifier);
    }

    public static Optional<DamageFunction> createResistanceModifier(final LivingEntity living, final DamageSource damageSource) {
        final PotionEffect effect = ((PotionEffect) living.getEffect(MobEffects.DAMAGE_RESISTANCE));

        if (!damageSource.isBypassMagic() && effect != null && damageSource != DamageSource.OUT_OF_WORLD) {
            return Optional.of(new DamageFunction(DamageModifier.builder()
                    .cause(Cause.of(EventContext.empty(), effect))
                    .type(DamageModifierTypes.DEFENSIVE_POTION_EFFECT)
                    .build(), DamageEventUtil.createResistanceFunction(effect.amplifier())));
        }
        return Optional.empty();
    }

    public static Optional<List<DamageFunction>> createEnchantmentModifiers(final LivingEntity living, final DamageSource damageSource) {
        if (damageSource.isBypassMagic()) {
            return Optional.empty();
        }
        final Iterable<net.minecraft.world.item.ItemStack> inventory = living.getArmorSlots();
        final int damageProtection = EnchantmentHelper.getDamageProtection(inventory, damageSource);
        if (damageProtection <= 0) {
            return Optional.empty();
        }
        final List<DamageFunction> modifiers = new ArrayList<>();
        final DoubleUnaryOperator enchantmentFunction = incomingDamage -> -(incomingDamage - CombatRules.getDamageAfterMagicAbsorb((float) incomingDamage, damageProtection));
        try (final CauseStackManager.StackFrame frame = ((Server) living.getServer()).causeStackManager().pushCauseFrame()) {
            frame.pushCause(living);
            final DamageModifier enchantmentModifier = DamageModifier.builder()
                .cause(frame.currentCause())
                .type(DamageModifierTypes.ARMOR_ENCHANTMENT)
                .build();
            modifiers.add(new DamageFunction(enchantmentModifier, enchantmentFunction));
        }

        return Optional.of(modifiers);

    }

    public static Optional<DamageFunction> createAbsorptionModifier(final LivingEntity living) {
        final float absorptionAmount = living.getAbsorptionAmount();
        if (absorptionAmount > 0) {
            final DoubleUnaryOperator function = damage ->
                    -(Math.max(damage - Math.max(damage - absorptionAmount, 0.0F), 0.0F));
            final DamageModifier modifier = DamageModifier.builder()
                    .cause(Cause.of(EventContext.empty(), living))
                    .type(DamageModifierTypes.ABSORPTION)
                    .build();
            return Optional.of(new DamageFunction(modifier, function));
        }
        return Optional.empty();
    }

    public static ServerLocation findFirstMatchingBlock(final Entity entity, final AABB bb, final Predicate<BlockState> predicate) {
        final int i = Mth.floor(bb.minX);
        final int j = Mth.floor(bb.maxX + 1.0D);
        final int k = Mth.floor(bb.minY);
        final int l = Mth.floor(bb.maxY + 1.0D);
        final int i1 = Mth.floor(bb.minZ);
        final int j1 = Mth.floor(bb.maxZ + 1.0D);
        final ChunkSource chunkSource = entity.level.getChunkSource();
        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    final BlockPos blockPos = new BlockPos(k1, l1, i2);
                    final LevelChunk chunk = chunkSource.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, false);
                    if (chunk == null || chunk.isEmpty()) {
                        continue;
                    }
                    if (predicate.test(chunk.getBlockState(blockPos))) {
                        return ServerLocation.of((ServerWorld) entity.level, k1, l1, i2);
                    }
                }
            }
        }

        // Entity is source of fire
        return ((org.spongepowered.api.entity.Entity) entity).serverLocation();
    }

    /**
     * This applies various contexts based on the type of {@link DamageSource}, whether
     * it's provided by sponge or vanilla. This is not stack neutral, which is why it requires
     * a {@link CauseStackManager.StackFrame} reference to push onto the stack.
     */
    public static void generateCauseFor(final DamageSource damageSource, final CauseStackManager.StackFrame frame) {
        if (damageSource instanceof EntityDamageSource) {
            final net.minecraft.world.entity.Entity source = damageSource.getEntity();
            if (!(source instanceof Player) && source instanceof CreatorTrackedBridge) {
                final CreatorTrackedBridge creatorBridge = (CreatorTrackedBridge) source;
                creatorBridge.tracker$getCreatorUUID().ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
                creatorBridge.tracker$getNotifierUUID().ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
            }
        } else if (damageSource instanceof BlockDamageSource) {
            final ServerLocation location = ((BlockDamageSource) damageSource).location();
            final BlockPos blockPos = VecHelper.toBlockPos(location);
            final LevelChunkBridge chunkBridge = (LevelChunkBridge) ((net.minecraft.world.level.Level) location.world()).getChunkAt(blockPos);
            chunkBridge.bridge$getBlockCreatorUUID(blockPos).ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
            chunkBridge.bridge$getBlockNotifierUUID(blockPos).ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
        }
        frame.pushCause(damageSource);
    }

    public static List<DamageFunction> createAttackEnchantmentFunction(final net.minecraft.world.item.ItemStack heldItem,
            final MobType creatureAttribute, final float attackStrength) {
        if (heldItem.isEmpty()) {
            return Collections.emptyList();
        }

        final ListTag enchantmentCompounds = heldItem.getEnchantmentTags();
        if (enchantmentCompounds.isEmpty()) {
            return Collections.emptyList();
        }

        final Map<Enchantment, Collection<Integer>> enchantments = new HashMap<>();

        for (int i = 0; i < enchantmentCompounds.size(); ++i) {
            final String id = enchantmentCompounds.getCompound(i).getString("id");
            final int enchantmentLevel = enchantmentCompounds.getCompound(i).getInt("lvl");

            final Enchantment enchantment = Registry.ENCHANTMENT.get(new ResourceLocation(id));
            if (enchantment != null) {
                enchantments.computeIfAbsent(enchantment, k -> new ArrayList<>()).add(enchantmentLevel);
            }
        }

        if (enchantments.isEmpty()) {
            return Collections.emptyList();
        }

        final List<DamageFunction> damageModifierFunctions = new ArrayList<>();
        final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(heldItem);

        for (final Map.Entry<Enchantment, Collection<Integer>> enchantment : enchantments.entrySet()) {
            final DamageModifier enchantmentModifier = DamageModifier.builder()
                    .type(DamageModifierTypes.WEAPON_ENCHANTMENT)
                    .cause(Cause.of(EventContext.empty(), snapshot, enchantment))
                    .build();
            final DoubleUnaryOperator enchantmentFunction = (damage) -> {
                double totalDamage = 0;
                for (final int level : enchantment.getValue()) {
                    totalDamage += (double) enchantment.getKey().getDamageBonus(level, creatureAttribute) * attackStrength;
                }
                return totalDamage;
            };
            damageModifierFunctions.add(new DamageFunction(enchantmentModifier, enchantmentFunction));
        }

        return damageModifierFunctions;
    }

    public static DamageFunction provideCriticalAttackTuple(final Player player, double criticalModifier) {
        final DamageModifier modifier = DamageModifier.builder()
                .cause(Cause.of(EventContext.empty(), player))
                .type(DamageModifierTypes.CRITICAL_HIT)
                .build();
        final DoubleUnaryOperator function = (damage) -> damage * criticalModifier;
        return new DamageFunction(modifier, function);
    }

    public static DamageFunction provideCooldownAttackStrengthFunction(final Player player, final float attackStrength) {
        final DamageModifier modifier = DamageModifier.builder()
                .cause(Cause.of(EventContext.empty(), player))
                .type(DamageModifierTypes.ATTACK_COOLDOWN)
                .build();
        // The formula is as follows:
        // Since damage needs to be "multiplied", this needs to basically add negative damage but re-add the "reduced" damage.
        final DoubleUnaryOperator function = (damage) -> -damage + (damage * (0.2F + attackStrength * attackStrength * 0.8F));
        return new DamageFunction(modifier, function);
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<DamageFunction> createShieldFunction(final LivingEntity entity, final DamageSource source, final float amount) {
        if (entity.isBlocking() && amount > 0.0 && ((LivingEntityAccessor) entity).invoker$isDamageSourceBlocked(source)) {
            final DamageModifier modifier = DamageModifier.builder()
                    .cause(Cause.of(EventContext.empty(), entity, ((ItemStack) (Object) entity.getUseItem()).createSnapshot()))
                    .type(DamageModifierTypes.SHIELD)
                    .build();
            return Optional.of(new DamageFunction(modifier, (damage) -> -damage));
        }
        return Optional.empty();
    }
}
