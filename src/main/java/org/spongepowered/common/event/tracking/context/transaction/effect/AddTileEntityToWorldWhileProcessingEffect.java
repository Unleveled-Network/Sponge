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
package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.common.accessor.server.level.ServerLevelAccessor;
import org.spongepowered.common.accessor.world.level.LevelAccessor;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.BlockPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.PipelineCursor;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

public final class AddTileEntityToWorldWhileProcessingEffect implements ProcessingSideEffect {

    private static final class Holder {
        static final AddTileEntityToWorldWhileProcessingEffect INSTANCE = new AddTileEntityToWorldWhileProcessingEffect();
    }

    public static AddTileEntityToWorldWhileProcessingEffect getInstance() {
        return AddTileEntityToWorldWhileProcessingEffect.Holder.INSTANCE;
    }

    AddTileEntityToWorldWhileProcessingEffect() {}

    @Override
    public EffectResult processSideEffect(final BlockPipeline pipeline, final PipelineCursor oldState, final BlockState newState,
        final SpongeBlockChangeFlag flag,
        final int limit
    ) {
        final ServerLevel serverWorld = pipeline.getServerWorld();
        final BlockEntity tileEntity = oldState.tileEntity;
        if (tileEntity == null) {
            return EffectResult.NULL_RETURN;
        }
        if (((LevelAccessor) serverWorld).accessor$updatingBlockEntities()) {
            ServerLevelAccessor.accessor$LOGGER().error(
                "Adding block entity while ticking: {} @ {}",
                () -> Registry.BLOCK_ENTITY_TYPE.getKey(tileEntity.getType()),
                tileEntity::getBlockPos
            );
            final boolean add = ((LevelAccessor) serverWorld).accessor$pendingBlockEntities().add(tileEntity);
            if (add) {
                return new EffectResult(oldState.state, true);
            } else {
                return EffectResult.NULL_RETURN;
            }
        }
        return EffectResult.NULL_PASS;
    }
}
