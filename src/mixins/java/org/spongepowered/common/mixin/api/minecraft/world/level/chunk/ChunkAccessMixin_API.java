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
package org.spongepowered.common.mixin.api.minecraft.world.level.chunk;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.fluid.FluidType;
import org.spongepowered.api.scheduler.ScheduledUpdateList;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.HeightType;
import org.spongepowered.api.world.HeightTypes;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.chunk.Chunk;
import org.spongepowered.api.world.chunk.ChunkState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.world.level.chunk.ChunkBiomeContainerAccessor;
import org.spongepowered.common.util.MissingImplementationException;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.volume.VolumeStreamUtils;
import org.spongepowered.math.vector.Vector3i;

import javax.annotation.Nullable;

@Mixin(ChunkAccess.class)
public interface ChunkAccessMixin_API<P extends Chunk<P>> extends Chunk<P> {

    // @formatter:off
    @Shadow ChunkStatus shadow$getStatus();
    @Shadow @Nullable ChunkBiomeContainer shadow$getBiomes();
    @Shadow void shadow$addEntity(net.minecraft.world.entity.Entity entity);
    @Shadow void shadow$setUnsaved(boolean var1);
    @Shadow void shadow$setInhabitedTime(long var1);
    @Shadow long shadow$getInhabitedTime();

    @Shadow ChunkPos shadow$getPos();

    @Shadow int shadow$getHeight(Heightmap.Types var1, int var2, int var3);

    // @formatter:on


    @Override
    default void addEntity(final Entity entity) {
        this.shadow$addEntity((net.minecraft.world.entity.Entity) entity);
    }

    @Override
    default ChunkState state() {
        return (ChunkState) this.shadow$getStatus();
    }

    @Override
    default boolean isEmpty() {
        return this.shadow$getStatus() == ChunkStatus.EMPTY;
    }

    @Override
    default boolean setBiome(final int x, final int y, final int z, final Biome biome) {
        return VolumeStreamUtils.setBiomeOnNativeChunk(x, y, z, biome, () -> (ChunkBiomeContainerAccessor) this.shadow$getBiomes(), () -> this.shadow$setUnsaved(true));
    }

    @Override
    default Ticks inhabitedTime() {
        return new SpongeTicks(this.shadow$getInhabitedTime());
    }

    @Override
    default void setInhabitedTime(Ticks newInhabitedTime) {
        this.shadow$setInhabitedTime(newInhabitedTime.ticks());
    }

    @Override
    default Vector3i chunkPosition() {
        final ChunkPos chunkPos = this.shadow$getPos();
        return new Vector3i(chunkPos.x, 0, chunkPos.z);
    }

    @Override
    default boolean contains(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, this.min(), this.max());
    }

    @Override
    default boolean isAreaAvailable(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, this.min(), this.max());
    }

    @Override
    default int highestYAt(int x, int z) {
        return this.shadow$getHeight((Heightmap.Types) (Object) HeightTypes.WORLD_SURFACE.get(), x, z);
    }

    @Override
    default int height(HeightType type, int x, int z) {
        return this.shadow$getHeight((Heightmap.Types) (Object) HeightTypes.WORLD_SURFACE.get(), x, z);
    }

    @Override
    default ScheduledUpdateList<BlockType> scheduledBlockUpdates() {
        throw new MissingImplementationException("ChunkAccess", "scheduledBlockUpdates");
    }

    @Override
    default ScheduledUpdateList<FluidType> scheduledFluidUpdates() {
        throw new MissingImplementationException("ChunkAccess", "scheduledFluidUpdates");
    }


}
