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
package org.spongepowered.common.mixin.api.minecraft.world.level.levelgen;

import net.minecraft.world.level.levelgen.NoiseSamplingSettings;
import org.spongepowered.api.world.generation.config.noise.SamplingConfig;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseSamplingSettings.class)
@Implements(@Interface(iface = SamplingConfig.class, prefix = "samplingConfig$"))
public abstract class NoiseSamplingSettingsMixin_API implements SamplingConfig {

    // @formatter:off
    @Shadow public abstract double shadow$xzScale();
    @Shadow public abstract double shadow$xzFactor();
    @Shadow public abstract double shadow$yScale();
    @Shadow public abstract double shadow$yFactor();
    // @formatter:on

    @Intrinsic
    public double samplingConfig$xzScale() {
        return this.shadow$xzScale();
    }

    @Intrinsic
    public double samplingConfig$xzFactor() {
        return this.shadow$xzFactor();
    }

    @Intrinsic
    public double samplingConfig$yScale() {
        return this.shadow$yScale();
    }

    @Intrinsic
    public double samplingConfig$yFactor() {
        return this.shadow$yFactor();
    }
}
