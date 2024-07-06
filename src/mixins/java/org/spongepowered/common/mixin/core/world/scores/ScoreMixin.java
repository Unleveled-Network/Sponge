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
package org.spongepowered.common.mixin.core.world.scores;

import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.world.scores.ScoreBridge;
import org.spongepowered.common.bridge.world.scores.ScoreboardBridge;
import org.spongepowered.common.scoreboard.SpongeScore;

import javax.annotation.Nullable;

@Mixin(Score.class)
public abstract class ScoreMixin implements ScoreBridge {

    @Shadow @Final private Scoreboard scoreboard;

    @Nullable private SpongeScore impl$spongeScore;

    @Override
    public SpongeScore bridge$getSpongeScore() {
        return this.impl$spongeScore;
    }

    @Override
    public void bridge$setSpongeScore(final SpongeScore score) {
        this.impl$spongeScore = score;
    }

    @Inject(method = "setScore", at = @At("HEAD"), cancellable = true)
    private void impl$sUpdateSpongeScore(final int points, final CallbackInfo ci) {
        if (this.scoreboard != null && ((ScoreboardBridge) this.scoreboard).bridge$isClient()) {
            return; // Let the normal logic take over.
        }
        if (this.impl$spongeScore == null) {
            SpongeCommon.logger().warn("Returning score because null score!");
            ci.cancel();
            return;
        }
        this.impl$spongeScore.setScore(points);
        ci.cancel();
    }

    @Inject(method = "setLocked", at = @At("HEAD"), cancellable = true)
    private void impl$sUpdateSpongeScoreLocked(final boolean locked, final CallbackInfo ci) {
        if (this.scoreboard != null && ((ScoreboardBridge) this.scoreboard).bridge$isClient()) {
            return; // Let the normal logic take over.
        }
        if (this.impl$spongeScore == null) {
            SpongeCommon.logger().warn("Returning score because null score!");
            ci.cancel();
            return;
        }
        this.impl$spongeScore.setLocked(locked);
        ci.cancel();
    }

}
