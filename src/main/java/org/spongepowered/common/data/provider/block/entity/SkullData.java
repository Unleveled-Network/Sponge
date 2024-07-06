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
package org.spongepowered.common.data.provider.block.entity;

import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.accessor.world.level.block.entity.SkullBlockEntityAccessor;
import org.spongepowered.common.bridge.world.level.block.entity.SkullBlockEntityBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.profile.SpongeGameProfile;

public final class SkullData {

    private SkullData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(SkullBlockEntity.class)
                    .create(Keys.GAME_PROFILE)
                        .get(h -> {
                            final SkullBlockEntityAccessor accessor = (SkullBlockEntityAccessor) h;
                            return accessor.accessor$owner() == null ? null : SpongeGameProfile.of(accessor.accessor$owner());
                        })
                        .set((h, v) -> ((SkullBlockEntityBridge) h).bridge$setUnresolvedPlayerProfile(SpongeGameProfile.toMcProfile(v)))
                        .delete(h -> ((SkullBlockEntityBridge) h).bridge$setUnresolvedPlayerProfile(null));
    }
    // @formatter:on
}
