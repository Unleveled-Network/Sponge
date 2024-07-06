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
package org.spongepowered.common.event.tracking.context.transaction.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.common.world.BlockChange;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@DefaultQualifier(NonNull.class)
public final class ReplaceBlockEntity extends BlockEventBasedTransaction {

    final BlockEntity added;
    final @Nullable BlockEntity removed;
    private final Supplier<ServerLevel> worldSupplier;
    @MonotonicNonNull SpongeBlockSnapshot removedSnapshot;

    public ReplaceBlockEntity(final BlockPos pos, final @Nullable BlockEntity existing, final BlockEntity proposed, final Supplier<ServerLevel> worldSupplier) {
        super(pos, worldSupplier.get().getBlockState(pos), ((ServerWorld) worldSupplier.get()).key());
        this.added = proposed;
        this.removed = existing;
        this.worldSupplier = worldSupplier;
    }

    @Override
    protected void captureState() {
        super.captureState();
        final BlockState currentState = this.worldSupplier.get().getBlockState(this.affectedPosition);
        final SpongeBlockSnapshot snapshot = TrackingUtil.createPooledSnapshot(
            currentState,
            this.affectedPosition,
            BlockChangeFlags.NONE,
            Constants.World.DEFAULT_BLOCK_CHANGE_LIMIT,
            this.removed,
            this.worldSupplier,
            Optional::empty, Optional::empty
        );
        snapshot.blockChange = BlockChange.MODIFY;
        this.removedSnapshot = snapshot;
    }

    @Override
    public Optional<AbsorbingFlowStep> parentAbsorber() {
        return Optional.of((ctx, tx) -> tx.acceptTileReplacement(this.removed, this.added));
    }

    @Override
    public boolean acceptTileAddition(final BlockEntity tileEntity) {
        if (this.added == tileEntity) {
            return true;
        }
        return super.acceptTileAddition(tileEntity);
    }

    @Override
    public void restore(final PhaseContext<?> context, final ChangeBlockEvent.All event) {
        this.removedSnapshot.restore(true, BlockChangeFlags.NONE);
    }

    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
        @Nullable final GameTransaction<@NonNull ?> parent
    ) {
        return Optional.empty();
    }

    @Override
    public void addToPrinter(final PrettyPrinter printer) {
        printer.add("ReplaceTileEntity")
            .add(" %s : %s", "Position", this.affectedPosition)
            .add(" %s : %s", "Added", this.added)
            .add(" %s : %s", "Removed", this.removed == null ? "null" : this.removed)
        ;
    }

    @Override
    protected SpongeBlockSnapshot getResultingSnapshot() {
        return SpongeBlockSnapshot.BuilderImpl.pooled()
            .from(this.removedSnapshot)
            .tileEntity(this.added)
            .build()
            ;
    }

    @Override
    protected SpongeBlockSnapshot getOriginalSnapshot() {
        return this.removedSnapshot;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReplaceBlockEntity.class.getSimpleName() + "[", "]")
            .add("affectedPosition=" + this.affectedPosition)
            .add("originalState=" + this.originalState)
            .add("worldKey=" + this.worldKey)
            .add("cancelled=" + this.cancelled)
            .add("added=" + this.added)
            .add("removed=" + this.removed)
            .toString();
    }
}
