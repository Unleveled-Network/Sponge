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
package org.spongepowered.common.data.provider.item.stack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.Constants;

public final class SpawnEggItemStackData {

    // @formatter:off
    @SuppressWarnings({"rawTypes", "rawtypes"})
    public static void register(final DataProviderRegistrator registrator) {
        registrator.asMutable(ItemStack.class)
                .create(Keys.ENTITY_TO_SPAWN)
                    .supports(stack -> !stack.isEmpty() && stack.getItem() instanceof SpawnEggItem)
                    .get(stack -> {
                        final Item item = stack.getItem();
                        final SpawnEggItem eggItem = (SpawnEggItem) item;
                        final @Nullable CompoundTag tag = stack.getTag();
                        final EntityType<?> type = eggItem.getType(tag);
                        final EntityArchetype.Builder builder = EntityArchetype.builder()
                            .type((org.spongepowered.api.entity.EntityType<?>) type);
                        if (tag != null && tag.contains(Constants.Item.ENTITY_TAG, Constants.NBT.TAG_COMPOUND)) {
                            final CompoundTag entityData = tag.getCompound(Constants.Item.ENTITY_TAG);
                            builder.entityData(NBTTranslator.INSTANCE.translate(entityData));
                        }
                        return builder.build();
                    })
            .asImmutable(ItemStack.class)
                .create(Keys.ENTITY_TYPE)
                    .supports(stack -> !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.SpawnEggItem)
                    .get(stack -> {
                        final Item item = stack.getItem();
                        final SpawnEggItem eggItem = (SpawnEggItem) item;
                        final @Nullable CompoundTag tag = stack.getTag();
                        final EntityType<?> type = eggItem.getType(tag);
                        return (org.spongepowered.api.entity.EntityType) type;
                    })
        ;
    }
    // @formatter:on

}
