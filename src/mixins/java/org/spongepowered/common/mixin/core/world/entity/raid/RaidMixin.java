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
package org.spongepowered.common.mixin.core.world.entity.raid;

import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.api.raid.RaidWave;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.world.entity.raid.RaidBridge;
import org.spongepowered.common.raid.SpongeRaidWave;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mixin(Raid.class)
public abstract class RaidMixin implements RaidBridge {

    @Final @Shadow private Map<Integer, Set<Raider>> groupRaiderMap;

    private final Map<Integer, RaidWave> impl$waves = new HashMap<>();

    // Minecraft's raids have no real concept of a wave object but instead have two maps containing raiders. We make a Wave object for the API.
    @Inject(method = "<init>*", at = @At("TAIL"))
    private void impl$createWaves(CallbackInfo ci) {
        for (Map.Entry<Integer, Set<Raider>> entry : this.groupRaiderMap.entrySet()) {
            this.impl$waves.put(entry.getKey(), new SpongeRaidWave((Raid) (Object) this, entry.getKey()));
        }
    }

    @Override
    public Map<Integer, RaidWave> bridge$getWaves() {
        return this.impl$waves;
    }
}
