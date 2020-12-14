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
package org.spongepowered.common.data.provider.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.math.Rotations;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.BodyPart;
import org.spongepowered.api.data.type.BodyParts;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.common.accessor.entity.item.ArmorStandEntityAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ArmorStandData {

    private ArmorStandData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ArmorStandEntity.class)
                    .create(Keys.BODY_ROTATIONS)
                        .get(h -> {
                            final Map<BodyPart, Vector3d> values = new HashMap<>();
                            values.put(BodyParts.HEAD.get(), VecHelper.toVector3d(h.getHeadPose()));
                            values.put(BodyParts.CHEST.get(), VecHelper.toVector3d(h.getBodyPose()));
                            values.put(BodyParts.LEFT_ARM.get(), VecHelper.toVector3d(((ArmorStandEntityAccessor) h).accessor$leftArmPose()));
                            values.put(BodyParts.RIGHT_ARM.get(), VecHelper.toVector3d(((ArmorStandEntityAccessor) h).accessor$rightArmPose()));
                            values.put(BodyParts.LEFT_LEG.get(), VecHelper.toVector3d(((ArmorStandEntityAccessor) h).accessor$leftLegPose()));
                            values.put(BodyParts.RIGHT_LEG.get(), VecHelper.toVector3d(((ArmorStandEntityAccessor) h).accessor$rightLegPose()));
                            return values;
                        })
                        .set((h, v) -> {
                            ArmorStandData.apply(v, BodyParts.HEAD.get(), h::setHeadPose);
                            ArmorStandData.apply(v, BodyParts.CHEST.get(), h::setBodyPose);
                            ArmorStandData.apply(v, BodyParts.LEFT_ARM.get(), h::setLeftArmPose);
                            ArmorStandData.apply(v, BodyParts.RIGHT_ARM.get(), h::setRightArmPose);
                            ArmorStandData.apply(v, BodyParts.LEFT_LEG.get(), h::setLeftLegPose);
                            ArmorStandData.apply(v, BodyParts.RIGHT_LEG.get(), h::setRightLegPose);
                        })
                    .create(Keys.CHEST_ROTATION)
                        .get(h -> VecHelper.toVector3d(h.getBodyPose()))
                        .set((h, v) -> h.setBodyPose(VecHelper.toRotation(v)))
                    .create(Keys.HAS_ARMS)
                        .get(ArmorStandEntity::isShowArms)
                        .set((h, v) -> ((ArmorStandEntityAccessor) h).invoker$setShowArms(v))
                    .create(Keys.HAS_BASE_PLATE)
                        .get(h -> !h.isNoBasePlate())
                        .set((h, v) -> ((ArmorStandEntityAccessor) h).invoker$setNoBasePlate(!v))
                    .create(Keys.HAS_MARKER)
                        .get(ArmorStandEntity::isMarker)
                        .set((h, v) -> ((ArmorStandEntityAccessor) h).invoker$setMarker(v))
                    .create(Keys.HEAD_ROTATION)
                        .get(h -> VecHelper.toVector3d(h.getHeadPose()))
                        .set((h, v) -> h.setHeadPose(VecHelper.toRotation(v)))
                    .create(Keys.IS_PLACING_DISABLED)
                        .get(h -> Sponge.getRegistry().getCatalogRegistry()
                            .streamAllOf(EquipmentType.class)
                            .filter(t -> (Object) t instanceof EquipmentSlotType)
                            .collect(Collectors.toMap(t -> t, t -> ((ArmorStandEntityAccessor) h).invoker$isDisabled((EquipmentSlotType) (Object) t))))
                        .set((h, v) -> {
                            int chunk = 0;

                            int disabledSlots = ((ArmorStandEntityAccessor) h).accessor$disabledSlots();
                            // try and keep the all chunk empty
                            final int allChunk = disabledSlots & 0b1111_1111;
                            if (allChunk != 0) {
                                disabledSlots |= (allChunk << 16);
                                disabledSlots ^= 0b1111_1111;
                            }

                            if (v.get(EquipmentTypes.FEET.get())) chunk |= 1 << 1;
                            if (v.get(EquipmentTypes.LEGS.get())) chunk |= 1 << 2;
                            if (v.get(EquipmentTypes.CHEST.get())) chunk |= 1 << 3;
                            if (v.get(EquipmentTypes.HEAD.get())) chunk |= 1 << 4;

                            disabledSlots |= (chunk << 16);
                            ((ArmorStandEntityAccessor) h).accessor$disabledSlots(disabledSlots);
                        })
                    .create(Keys.IS_SMALL)
                        .get(ArmorStandEntity::isSmall)
                        .set((h, v) -> ((ArmorStandEntityAccessor) h).invoker$setSmall(v))
                    .create(Keys.IS_TAKING_DISABLED)
                        .get(h -> {
                            // include all chunk
                            final int disabled = ((ArmorStandEntityAccessor) h).accessor$disabledSlots();
                            final int resultantChunk = ((disabled >> 16) & 0b1111_1111) | (disabled & 0b1111_1111);

                            return ImmutableMap.of(
                                    EquipmentTypes.FEET.get(), (resultantChunk & (1 << 1)) != 0,
                                    EquipmentTypes.LEGS.get(), (resultantChunk & (1 << 2)) != 0,
                                    EquipmentTypes.CHEST.get(), (resultantChunk & (1 << 3)) != 0,
                                    EquipmentTypes.HEAD.get(), (resultantChunk & (1 << 4)) != 0);
                        })
                        .set((h, v) -> {
                            int chunk = 0;

                            int disabledSlots = ((ArmorStandEntityAccessor) h).accessor$disabledSlots();
                            // try and keep the all chunk empty
                            final int allChunk = disabledSlots & 0b1111_1111;
                            if (allChunk != 0) {
                                disabledSlots |= (allChunk << 16);
                                disabledSlots ^= 0b1111_1111;
                                ((ArmorStandEntityAccessor) h).accessor$disabledSlots(disabledSlots);
                            }

                            if (v.get(EquipmentTypes.FEET.get())) chunk |= 1 << 1;
                            if (v.get(EquipmentTypes.LEGS.get())) chunk |= 1 << 2;
                            if (v.get(EquipmentTypes.CHEST.get())) chunk |= 1 << 3;
                            if (v.get(EquipmentTypes.HEAD.get())) chunk |= 1 << 4;

                            disabledSlots |= (chunk << 8);
                            ((ArmorStandEntityAccessor) h).accessor$disabledSlots(disabledSlots);
                        })
                    .create(Keys.LEFT_ARM_ROTATION)
                        .get(h -> VecHelper.toVector3d(h.getLeftArmPose()))
                        .set((h, v) -> h.setLeftArmPose(VecHelper.toRotation(v)))
                    .create(Keys.LEFT_LEG_ROTATION)
                        .get(h -> VecHelper.toVector3d(h.getLeftLegPose()))
                        .set((h, v) -> h.setLeftLegPose(VecHelper.toRotation(v)))
                    .create(Keys.RIGHT_ARM_ROTATION)
                        .get(h -> VecHelper.toVector3d(h.getRightArmPose()))
                        .set((h, v) -> h.setRightArmPose(VecHelper.toRotation(v)))
                    .create(Keys.RIGHT_LEG_ROTATION)
                        .get(h -> VecHelper.toVector3d(h.getRightLegPose()))
                        .set((h, v) -> h.setRightLegPose(VecHelper.toRotation(v)));
    }
    // @formatter:on

    private static void apply(final Map<BodyPart, Vector3d> value, final BodyPart part, final Consumer<Rotations> consumer) {
        final Vector3d vec = value.get(part);
        if (vec == null) {
            return;
        }
        consumer.accept(VecHelper.toRotation(vec));
    }
}