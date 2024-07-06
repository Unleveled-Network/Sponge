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

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.Tuple;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.WorldLike;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Interface.Remap;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.data.holder.SpongeLocationBaseDataHolder;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.MissingImplementationException;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.storage.SpongeChunkLayout;
import org.spongepowered.common.world.volume.VolumeStreamUtils;
import org.spongepowered.common.world.volume.buffer.biome.ObjectArrayMutableBiomeBuffer;
import org.spongepowered.common.world.volume.buffer.block.ArrayMutableBlockBuffer;
import org.spongepowered.common.world.volume.buffer.blockentity.ObjectArrayMutableBlockEntityBuffer;
import org.spongepowered.common.world.volume.buffer.entity.ObjectArrayMutableEntityBuffer;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(net.minecraft.world.level.chunk.LevelChunk.class)
@Implements(@Interface(iface = WorldChunk.class, prefix = "worldChunk$", remap = Remap.NONE))
public abstract class LevelChunkMixin_API implements WorldChunk, SpongeLocationBaseDataHolder {

    //@formatter:off
    @Shadow @Final private ChunkPos chunkPos;
    @Shadow @Final private Level level;
    @Shadow @Final private ClassInstanceMultiMap<net.minecraft.world.entity.Entity>[] entitySections;

    @Shadow public abstract boolean shadow$isEmpty();
    @Shadow public abstract int shadow$getHeight(Heightmap.Types param0, int param1, int param2);
    @Shadow public abstract void shadow$setUnsaved(boolean unsaved);
    @Shadow public abstract <T extends net.minecraft.world.entity.Entity> void shadow$getEntitiesOfClass(Class<? extends T> param0,
            net.minecraft.world.phys.AABB param1,
            List<T> param2, @org.jetbrains.annotations.Nullable Predicate<? super T> param3);
    @Shadow public abstract void shadow$getEntities(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity param0,
            net.minecraft.world.phys.AABB param1,
            List<net.minecraft.world.entity.Entity> param2,
            @org.jetbrains.annotations.Nullable Predicate<? super net.minecraft.world.entity.Entity> param3);
    @Shadow public abstract Map<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> shadow$getBlockEntities();
    @Shadow public abstract void shadow$setBlockEntity(BlockPos param0, net.minecraft.world.level.block.entity.BlockEntity param1);
    @Shadow public abstract void shadow$removeBlockEntity(BlockPos param0);
    //@formatter:on

    private @Nullable Vector3i api$blockMin;
    private @Nullable Vector3i api$blockMax;

    @Override
    public Biome biome(final int x, final int y, final int z) {
        if (!this.contains(x, y, z)) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), Constants.World.BLOCK_MIN, Constants.World.BLOCK_MAX);
        }
        return (Biome) (Object) this.level.getBiome(new BlockPos(x, y, z));
    }

    @Override
    public double regionalDifficultyFactor() {
        return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(),
                this.inhabitedTime().ticks(), this.level.getMoonBrightness()).getEffectiveDifficulty();
    }

    @Override
    public double regionalDifficultyPercentage() {
        return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(),
                this.inhabitedTime().ticks(), this.level.getMoonBrightness()).getSpecialMultiplier();
    }

    @Override
    public org.spongepowered.api.world.World<@NonNull ?, @NonNull ?> world() {
        return ((org.spongepowered.api.world.World<@NonNull ?, @NonNull ?>) this.level);
    }

    @Intrinsic
    public boolean worldChunk$isEmpty() {
        return this.shadow$isEmpty();
    }

    @Override
    public VolumeStream<WorldChunk, Entity> entityStream(
        final Vector3i min, final Vector3i max, final StreamOptions options
    ) {
        VolumeStreamUtils.validateStreamArgs(
            Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableEntityBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableEntityBuffer(min, size);
        } else {
            backingVolume = null;
        }

        return VolumeStreamUtils.<WorldChunk, Entity, net.minecraft.world.entity.Entity, LevelChunk, UUID>generateStream(
            options,
            this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            (chunk) -> VolumeStreamUtils.getEntitiesFromChunk(min, max, chunk),
            // Entity Identity Function
            VolumeStreamUtils.getOrCloneEntityWithVolume(shouldCarbonCopy, backingVolume, this.level),
            (key, entity) -> entity.getUUID(),
            // Filtered Position Entity Accessor
            (entityUuid, chunk) -> {
                final net.minecraft.world.entity.@Nullable Entity entity = shouldCarbonCopy
                    ? (net.minecraft.world.entity.Entity) backingVolume.entity(entityUuid).orElse(null)
                    : (net.minecraft.world.entity.Entity) chunk.world().entity(entityUuid).orElse(null);
                if (entity == null) {
                    return null;
                }
                return new Tuple<>(entity.blockPosition(), entity);
            }
            );
    }

    @Override
    public VolumeStream<WorldChunk, BlockState> blockStateStream(
        final Vector3i min, final Vector3i max, final StreamOptions options
    ) {
        VolumeStreamUtils.validateStreamArgs(Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ArrayMutableBlockBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ArrayMutableBlockBuffer(min, size);
        } else {
            backingVolume = null;
        }

        return VolumeStreamUtils.<WorldChunk, BlockState, net.minecraft.world.level.block.state.BlockState, ChunkAccess, BlockPos>generateStream(
            options,
            // Ref
            (WorldChunk) this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            VolumeStreamUtils.getBlockStatesForSections(min, max),
            // IdentityFunction
            (pos, blockState) -> {
                if (shouldCarbonCopy) {
                    backingVolume.setBlock(pos, blockState);
                }
            },
            // Biome by block position
            (key, biome) -> key,
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final net.minecraft.world.level.block.state.BlockState tileEntity = shouldCarbonCopy
                    ? backingVolume.getBlock(blockPos)
                    : ((LevelReader) world).getBlockState(blockPos);
                return new Tuple<>(blockPos, tileEntity);
            }
        );
    }

    @Override
    public Collection<? extends BlockEntity> blockEntities() {
        return (Collection) Collections.unmodifiableCollection(this.shadow$getBlockEntities().values());
    }

    @Override
    public VolumeStream<WorldChunk, BlockEntity> blockEntityStream(
        final Vector3i min, final Vector3i max, final StreamOptions options
    ) {
        VolumeStreamUtils.validateStreamArgs(Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableBlockEntityBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableBlockEntityBuffer(min, size);
        } else {
            backingVolume = null;
        }

        return VolumeStreamUtils.<WorldChunk, BlockEntity, net.minecraft.world.level.block.entity.BlockEntity, ChunkAccess, BlockPos>generateStream(
            options,
            // Ref
            (WorldChunk) this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            this::impl$getBlockEntitiesStream,
            // IdentityFunction
            VolumeStreamUtils.getBlockEntityOrCloneToBackingVolume(shouldCarbonCopy, backingVolume, this.level),
            // Biome by block position
            (key, biome) -> key,
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final net.minecraft.world.level.block.entity.@Nullable BlockEntity tileEntity = shouldCarbonCopy
                    ? (net.minecraft.world.level.block.entity.BlockEntity) backingVolume.blockEntity(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                    .orElse(null)
                    : ((LevelReader) world).getBlockEntity(blockPos);
                return new Tuple<>(blockPos, tileEntity);
            }
        );
    }

    private Stream<Map.Entry<BlockPos, net.minecraft.world.level.block.entity.BlockEntity>> impl$getBlockEntitiesStream(final ChunkAccess chunk) {
        return chunk instanceof LevelChunk ? ((LevelChunk) chunk).getBlockEntities().entrySet().stream() : Stream.empty();
    }

    @Override
    public void addBlockEntity(int x, int y, int z, BlockEntity blockEntity) {
        this.world().addBlockEntity(x, y, z, blockEntity);
    }

    @Override
    public void removeBlockEntity(int x, int y, int z) {
        this.world().removeBlockEntity(x, y, z);
    }

    @Override
    public VolumeStream<WorldChunk, Biome> biomeStream(final Vector3i min, final Vector3i max, final StreamOptions options) {
        VolumeStreamUtils.validateStreamArgs(Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableBiomeBuffer backingVolume;
        if (shouldCarbonCopy) {
            final Registry<net.minecraft.world.level.biome.Biome> biomeRegistry = this.level.registryAccess().registry(Registry.BIOME_REGISTRY)
                .map(wr -> ((Registry<net.minecraft.world.level.biome.Biome>) wr))
                .orElse(BuiltinRegistries.BIOME);
            backingVolume = new ObjectArrayMutableBiomeBuffer(min, size, VolumeStreamUtils.nativeToSpongeRegistry(biomeRegistry));
        } else {
            backingVolume = null;
        }
        return VolumeStreamUtils.<WorldChunk, Biome, net.minecraft.world.level.biome.Biome, ChunkAccess, BlockPos>generateStream(
            options,
            // Ref
            (WorldChunk) this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            VolumeStreamUtils.getBiomesForChunkByPos((LevelReader) (Object) this, min, max),
            // IdentityFunction
            (pos, biome) -> {
                if (shouldCarbonCopy) {
                    backingVolume.setBiome(pos, biome);
                }
            },            // Biome by block position
            (key, biome) -> key,
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final net.minecraft.world.level.biome.Biome biome = shouldCarbonCopy
                    ? backingVolume.getNativeBiome(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                    : ((LevelReader) world.world()).getBiome(blockPos);
                return new Tuple<>(blockPos, biome);
            }
        );
    }

    @Override
    public Vector3i min() {
        if (this.api$blockMin == null) {
            this.api$blockMin = SpongeChunkLayout.INSTANCE.forceToWorld(this.chunkPosition());
        }
        return this.api$blockMin;
    }

    @Override
    public Vector3i max() {
        if (this.api$blockMax == null) {
            this.api$blockMax = this.min().add(SpongeChunkLayout.CHUNK_SIZE).sub(1, 1, 1);
        }
        return this.api$blockMax;
    }

    @Override
    public Vector3i size() {
        return SpongeChunkLayout.CHUNK_SIZE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<? extends Player> players() {
        return (Collection) this.level.players().stream()
                .filter(x -> x.inChunk && x.xChunk == this.chunkPos.x && x.zChunk == this.chunkPos.z)
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Optional<Entity> entity(final UUID uuid) {
        return (Optional) Arrays.stream(this.entitySections).flatMap(Collection::stream).filter(x -> x.getUUID().equals(uuid)).findAny();
    }

    @Override
    public Collection<? extends Entity> entities() {
        return (Collection<? extends Entity>) (Object) Arrays.stream(this.entitySections).flatMap(Collection::stream).collect(ImmutableList.toImmutableList());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T extends Entity> Collection<? extends T> entities(final Class<? extends T> entityClass, final AABB box, @Nullable final Predicate<? super T> predicate) {
        final List<T> entities = new ArrayList<>();
        this.shadow$getEntitiesOfClass((Class<? extends net.minecraft.world.entity.Entity>) entityClass,
                VecHelper.toMinecraftAABB(box),
                (List) entities,
                (Predicate) predicate);
        return entities;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<? extends Entity> entities(final AABB box, final Predicate<? super Entity> filter) {
        final List<Entity> entities = new ArrayList<>();
        this.shadow$getEntities(null, VecHelper.toMinecraftAABB(box), (List) entities, (Predicate) filter);
        return entities;
    }

    @Override
    public <E extends Entity> E createEntity(final EntityType<E> type, final Vector3d position) throws IllegalArgumentException, IllegalStateException {
        this.api$checkPositionInChunk(position);
        return ((LevelBridge) this.level).bridge$createEntity(type, position, false);
    }

    @Override
    public <E extends Entity> E createEntityNaturally(final EntityType<E> type, final Vector3d position)
            throws IllegalArgumentException, IllegalStateException {
        this.api$checkPositionInChunk(position);
        return ((LevelBridge) this.level).bridge$createEntity(type, position, true);
    }

    @Override
    public Optional<Entity> createEntity(final DataContainer container) {
        return Optional.ofNullable(((LevelBridge) this.level).bridge$createEntity(container, null,
                position -> VecHelper.inBounds(position, this.min(), this.max())));
    }

    @Override
    public Optional<Entity> createEntity(final DataContainer container, final Vector3d position) {
        this.api$checkPositionInChunk(position);
        return Optional.ofNullable(((LevelBridge) this.level).bridge$createEntity(container, position, null));
    }

    @Override
    public boolean spawnEntity(final Entity entity) {
        return ((LevelChunkBridge) this).bridge$spawnEntity(entity);
    }

    @Override
    public Collection<Entity> spawnEntities(final Iterable<? extends Entity> entities) {
        return EntityUtil.spawnEntities(entities,
                x -> this.api$isInBounds(x.position()),
                entity -> ((LevelChunkBridge) this).bridge$spawnEntity((Entity) entity));
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockState blockState, final BlockChangeFlag flag) {
        this.api$checkPositionInChunk(x, y, z);
        return ((WorldLike<@NonNull ?>) this.level).setBlock(x, y, z, blockState, flag);
    }

    private void api$checkPositionInChunk(final int x, final int y, final int z) {
        if (!VecHelper.inBounds(x, y, z, this.min(), this.max())) {
            throw new IllegalArgumentException("Supplied bounds are not within this chunk.");
        }
    }

    private void api$checkPositionInChunk(final Vector3d position) {
        if (!this.api$isInBounds(position)) {
            throw new IllegalArgumentException("Supplied bounds are not within this chunk.");
        }
    }

    private boolean api$isInBounds(final Vector3d position) {
        return VecHelper.inBounds(position, this.min(), this.max());
    }


    @Override
    public ServerLocation impl$dataholder(final int x, final int y, final int z) {
        this.api$checkPositionInChunk(x, y, z);
        if (this.level instanceof ServerWorld) {
            return ((ServerWorld) this.level).location(x, y ,z);
        }
        throw new MissingImplementationException("LevelChunk", "impl$dataholder");
    }
}
