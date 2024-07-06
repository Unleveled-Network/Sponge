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
package org.spongepowered.common.event.tracking.phase.general;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.entity.SpawnType;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.world.BlockChange;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

final class CommandState extends GeneralState<CommandPhaseContext> {

    private final BiConsumer<CauseStackManager.StackFrame, CommandPhaseContext> COMMAND_MODIFIER = super.getFrameModifier()
        .andThen((frame, ctx) -> {
            if (ctx.commandMapping != null) {
                frame.pushCause(ctx.commandMapping);
            }
        });

    @Override
    public CommandPhaseContext createNewContext(final PhaseTracker tracker) {
        return new CommandPhaseContext(this, tracker);
    }

    @Override
    public BiConsumer<CauseStackManager.StackFrame, CommandPhaseContext> getFrameModifier() {
        return this.COMMAND_MODIFIER;
    }

    @Override
    public void postBlockTransactionApplication(
        final CommandPhaseContext context, final BlockChange blockChange,
        final BlockTransactionReceipt transaction
    ) {
        // We want to investigate if there is a user on the cause stack
        // and if possible, associate the notiifer/owner based on the change flag
        // We have to check if there is a player, because command blocks can be triggered
        // without player interaction.
        // Fixes https://github.com/SpongePowered/SpongeForge/issues/2442
        PhaseTracker.getCauseStackManager().currentCause().first(Player.class).ifPresent(user -> {
            TrackingUtil.associateTrackerToTarget(blockChange, transaction, user.uniqueId());
        });
   }

    @Override
    public void associateNeighborStateNotifier(final CommandPhaseContext context, final @Nullable BlockPos sourcePos, final Block block,
        final BlockPos notifyPos, final ServerLevel minecraftWorld, final PlayerTracker.Type notifier) {
        context.getSource(Player.class)
            .ifPresent(player -> ((LevelChunkBridge) minecraftWorld.getChunkAt(notifyPos))
                .bridge$addTrackedBlockPosition(block, notifyPos, player.uniqueId(), PlayerTracker.Type.NOTIFIER));
    }

    @Override
    public void unwind(final CommandPhaseContext phaseContext) {
        TrackingUtil.processBlockCaptures(phaseContext);
    }

    @Override
    public Supplier<ResourceKey> attemptWorldKey(CommandPhaseContext context) {
        final Optional<net.minecraft.world.entity.player.Player> playerSource = context.getSource(net.minecraft.world.entity.player.Player.class);
        if (playerSource.isPresent()) {
            return () -> (ResourceKey) (Object) playerSource.get().level.dimension().location();
        }
        return super.attemptWorldKey(context);
    }

    @Override
    public Supplier<SpawnType> getSpawnTypeForTransaction(
        final CommandPhaseContext context, final Entity entityToSpawn
    ) {
        return SpawnTypes.PLACEMENT;
    }
}
