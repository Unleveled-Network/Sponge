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

import net.minecraft.core.MappedRegistry;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.HorseStyle;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.common.accessor.world.entity.animal.horse.HorseAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

public final class HorseData {

    private HorseData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(Horse.class)
                    .create(Keys.HORSE_COLOR)
                        .get(h -> {
                            final MappedRegistry<HorseColor> registry = (MappedRegistry<HorseColor>) (Object) Sponge.game().registry(RegistryTypes.HORSE_COLOR);
                            return registry.byId(HorseData.getHorseColor(h));
                        })
                        .set((h, v) -> {
                            final int style = HorseData.getHorseStyle(h);
                            final MappedRegistry<HorseColor> registry = (MappedRegistry<HorseColor>) (Object) Sponge.game().registry(RegistryTypes.HORSE_COLOR);
                            final int metadata = registry.getId(v);
                            ((HorseAccessor) h).invoker$setTypeVariant(metadata | style);
                        })
                    .create(Keys.HORSE_STYLE)
                        .get(h -> {
                            final MappedRegistry<HorseStyle> registry = (MappedRegistry<HorseStyle>) (Object) Sponge.game().registry(RegistryTypes.HORSE_STYLE);
                            return registry.byId(HorseData.getHorseStyle(h));
                        })
                        .set((h, v) -> {
                            final int color = HorseData.getHorseColor(h);
                            final MappedRegistry<HorseStyle> registry = (MappedRegistry<HorseStyle>) (Object) Sponge.game().registry(RegistryTypes.HORSE_STYLE);
                            final int metadata = registry.getId(v);
                            ((HorseAccessor) h).invoker$setTypeVariant((color | (metadata << 8)));
                        });
    }
    // @formatter:on

    private static int getHorseColor(final Horse holder) {
        return ((HorseAccessor) holder).invoker$getTypeVariant() & 0xFF;
    }

    private static int getHorseStyle(final Horse holder) {
        return (((HorseAccessor) holder).invoker$getTypeVariant() & 0xFF00) >> 8;
    }
}
