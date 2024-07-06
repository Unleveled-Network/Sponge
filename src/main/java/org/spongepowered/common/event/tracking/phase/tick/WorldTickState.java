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
package org.spongepowered.common.event.tracking.phase.tick;

import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

final class WorldTickState extends TickPhaseState<WorldTickState.WorldTickContext> {

    private final BiConsumer<CauseStackManager.StackFrame, WorldTickContext> WORLD_MODIFIER = super.getFrameModifier()
        .andThen((frame, context) -> {
            context.getSource(Object.class).ifPresent(frame::pushCause);
            final @Nullable ServerLevel serverWorld = context.serverWorld.get();
            if (serverWorld != null) {
                frame.pushCause(serverWorld);
            }
        });

    @Override
    protected WorldTickContext createNewContext(final PhaseTracker tracker) {
        return new WorldTickContext(this, tracker);
    }

    @Override
    public BiConsumer<CauseStackManager.StackFrame, WorldTickContext> getFrameModifier() {
        return this.WORLD_MODIFIER;
    }

    @Override
    public void unwind(final WorldTickContext phaseContext) {
        TrackingUtil.processBlockCaptures(phaseContext);
    }

    @Override
    public Supplier<ResourceKey> attemptWorldKey(
        final WorldTickContext context
    ) {
        return () -> (ResourceKey) (Object) Objects.requireNonNull(context.serverWorld.get(), "ServerWorld reference lost during tick").dimension().location();
    }

    public static class WorldTickContext extends TickContext<WorldTickContext> {

        @MonotonicNonNull WeakReference<ServerLevel> serverWorld;

        public WorldTickContext world(final ServerLevel server) {
            this.serverWorld = new WeakReference<>(server);
            return this;
        }


        WorldTickContext(final IPhaseState<WorldTickContext> phaseState, final PhaseTracker tracker) {
            super(phaseState, tracker);
        }
    }
}
