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
package org.spongepowered.common.mixin.inventory.api.world.inventory;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.accessor.world.inventory.ItemCombinerMenuAccessor;
import org.spongepowered.common.inventory.DefaultSingleBlockCarrier;
import org.spongepowered.math.vector.Vector3i;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin_BlockCarrier_Inventory_API implements DefaultSingleBlockCarrier {

    @Override
    public ServerLocation location() {
        return ((ItemCombinerMenuAccessor) this).accessor$access().evaluate((world, pos) ->
                ServerLocation.of(((ServerWorld) world), new Vector3i(pos.getX(), pos.getY(), pos.getZ()))
        ).orElse(null);
    }

    @Override
    public World<?, ?> world() {
        return ((ItemCombinerMenuAccessor) this).accessor$access().evaluate((world, pos) -> (World<?, ?>) world).orElse(null);
    }

}
