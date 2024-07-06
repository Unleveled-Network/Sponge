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
package org.spongepowered.common.mixin.api.minecraft.server.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.generation.GenerationChunk;
import org.spongepowered.api.world.generation.GenerationRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.storage.SpongeChunkLayout;
import org.spongepowered.math.vector.Vector3i;

import java.util.List;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin_API implements GenerationRegion {

    // @formatter:off
    @Shadow @Final private ChunkPos firstPos;
    @Shadow @Final private ChunkPos lastPos;
    @Shadow public abstract ChunkAccess shadow$getChunk(int param0, int param1);
    // @formatter:on

    private ResourceKey api$serverWorldKey;
    private @MonotonicNonNull Vector3i api$minBlock;
    private @MonotonicNonNull Vector3i api$maxBlock;
    private @MonotonicNonNull Vector3i api$size;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void api$getWorldKeyOnConstruction(final ServerLevel param0, final List<ChunkAccess> param1, final CallbackInfo ci) {
        this.api$serverWorldKey = (ResourceKey) (Object) param0.dimension().location();
    }

    @Override
    public @NonNull ResourceKey worldKey() {
        return this.api$serverWorldKey;
    }

    @Override
    public @NonNull Server engine() {
        return SpongeCommon.game().server();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public @NonNull GenerationChunk chunk(final int cx, final int cy, final int cz) {
        final ChunkAccess chunk;
        try {
            chunk = this.shadow$getChunk(cx, cz);
        } catch (final RuntimeException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
        if (chunk == null) {
            // This indicates someone has asked outside of the region
            throw new IllegalArgumentException(String.format("Chunk coordinates (%d, %d, %d) is out of bounds.", cx, cy, cz));
        } else if (chunk instanceof LevelChunk) {
            // If this strange circumstance occurs, we just use Mojang's imposter and be on our way.
            return (GenerationChunk) new ImposterProtoChunk((LevelChunk) chunk);
        }
        return (GenerationChunk) chunk;
    }

    @Override
    public @NonNull Vector3i chunkMin() {
        return VecHelper.toVector3i(this.firstPos).min(VecHelper.toVector3i(this.lastPos));
    }

    @Override
    public @NonNull Vector3i chunkMax() {
        return VecHelper.toVector3i(this.firstPos).max(VecHelper.toVector3i(this.lastPos));
    }

    @Override
    public @NonNull Vector3i min() {
        if (this.api$minBlock == null) {
            this.api$minBlock = this.convertToBlock(this.chunkMin(), false);
        }
        return this.api$minBlock;
    }

    @Override
    public @NonNull Vector3i max() {
        if (this.api$maxBlock == null) {
            this.api$maxBlock = this.convertToBlock(this.chunkMax(), true);
        }
        return this.api$maxBlock;
    }

    @Override
    public @NonNull Vector3i size() {
        if (this.api$size == null) {
            this.api$size = this.max().sub(this.min()).add(Vector3i.ONE);
        }
        return this.api$size;
    }

    @Override
    public boolean isAreaAvailable(final int x, final int y, final int z) {
        return VecHelper.inBounds(x, y, z, this.min(), this.max());
    }

    private Vector3i convertToBlock(final Vector3i chunk, final boolean isMax) {
        final Vector3i chunkMin = Sponge.server().chunkLayout().forceToWorld(chunk);
        if (isMax) {
            return chunkMin.add(SpongeChunkLayout.CHUNK_MASK);
        }
        return chunkMin;
    }

}
