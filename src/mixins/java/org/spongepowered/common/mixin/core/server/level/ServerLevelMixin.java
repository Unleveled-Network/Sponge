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
package org.spongepowered.common.mixin.core.server.level;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.action.LightningEvent;
import org.spongepowered.api.event.sound.PlaySoundEvent;
import org.spongepowered.api.event.world.ChangeWeatherEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.WeatherTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.ResourceKeyBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.bridge.world.level.PlatformServerLevelBridge;
import org.spongepowered.common.bridge.world.level.border.WorldBorderBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.general.GeneralPhase;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.mixin.core.world.level.LevelMixin;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends LevelMixin implements ServerLevelBridge, PlatformServerLevelBridge, ResourceKeyBridge {

    // @formatter:off
    @Shadow @Final private ServerLevelData serverLevelData;
    @Shadow private int emptyTime;

    @Shadow @Nonnull public abstract MinecraftServer shadow$getServer();
    @Shadow protected abstract void shadow$saveLevelData();
    // @formatter:on

    private final long[] impl$recentTickTimes = new long[100];

    private LevelStorageSource.LevelStorageAccess impl$levelSave;
    private CustomBossEvents impl$bossBarManager;
    private ChunkProgressListener impl$chunkStatusListener;
    private Weather impl$prevWeather;
    private boolean impl$isManualSave = false;
    private long impl$preTickTime = 0L;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void impl$cacheLevelSave(final MinecraftServer p_i241885_1_, final Executor p_i241885_2_, final LevelStorageSource.LevelStorageAccess p_i241885_3_,
                                     final ServerLevelData p_i241885_4_, final net.minecraft.resources.ResourceKey<Level> p_i241885_5_, final DimensionType p_i241885_6_, final ChunkProgressListener p_i241885_7_,
                                     final ChunkGenerator p_i241885_8_, final boolean p_i241885_9_, final long p_i241885_10_, final List<CustomSpawner> p_i241885_12_, final boolean p_i241885_13_,
                                     final CallbackInfo ci) {
        this.impl$levelSave = p_i241885_3_;
        this.impl$chunkStatusListener = p_i241885_7_;
        this.impl$prevWeather = ((ServerWorld) this).weather();
    }

    @Redirect(method = "getSeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/WorldData;worldGenSettings()Lnet/minecraft/world/level/levelgen/WorldGenSettings;"))
    public WorldGenSettings impl$onGetSeed(final WorldData iServerConfiguration) {
        return ((PrimaryLevelData) this.serverLevelData).worldGenSettings();
    }

    @Override
    public LevelStorageSource.LevelStorageAccess bridge$getLevelSave() {
        return this.impl$levelSave;
    }

    @Override
    public ChunkProgressListener bridge$getChunkStatusListener() {
        return this.impl$chunkStatusListener;
    }

    @Override
    public boolean bridge$isLoaded() {
        if (((LevelBridge) this).bridge$isFake()) {
            return false;
        }

        final ServerLevel world = this.shadow$getServer().getLevel(this.shadow$dimension());
        if (world == null) {
            return false;
        }

        return world == (Object) this;
    }

    @Override
    public void bridge$adjustDimensionLogic(final DimensionType dimensionType) {
        if (this.bridge$isFake()) {
            return;
        }

        super.bridge$adjustDimensionLogic(dimensionType);

        // TODO Minecraft 1.16.2 - Rebuild level stems, get generator from type, set generator
        // TODO ...or cache generator on type?

        this.impl$setWorldOnBorder();
    }

    @Override
    public CustomBossEvents bridge$getBossBarManager() {
        if (this.impl$bossBarManager == null) {
            if (Level.OVERWORLD.equals(this.shadow$dimension()) || this.bridge$isFake()) {
                this.impl$bossBarManager = this.shadow$getServer().getCustomBossEvents();
            } else {
                this.impl$bossBarManager = new CustomBossEvents();
            }
        }

        return this.impl$bossBarManager;
    }

    @Override
    public void bridge$triggerExplosion(Explosion explosion) {
        // Sponge start
        // Set up the pre event
        if (ShouldFire.EXPLOSION_EVENT_PRE) {
            final ExplosionEvent.Pre
                    event =
                    SpongeEventFactory.createExplosionEventPre(PhaseTracker.getCauseStackManager().currentCause(),
                            explosion, (org.spongepowered.api.world.server.ServerWorld) this);
            if (SpongeCommon.post(event)) {
                return;
            }
            explosion = event.explosion();
        }

        final net.minecraft.world.level.Explosion mcExplosion = (net.minecraft.world.level.Explosion) explosion;

        try (final PhaseContext<?> ignored = GeneralPhase.State.EXPLOSION.createPhaseContext(PhaseTracker.SERVER)
                .explosion((net.minecraft.world.level.Explosion) explosion)
                .source(explosion.sourceExplosive().isPresent() ? explosion.sourceExplosive() : this)) {
            ignored.buildAndSwitch();
            final boolean shouldBreakBlocks = explosion.shouldBreakBlocks();
            // Sponge End

            mcExplosion.explode();
            mcExplosion.finalizeExplosion(explosion.shouldPlaySmoke());

            if (!shouldBreakBlocks) {
                mcExplosion.clearToBlow();
            }

            // Sponge Start - end processing
        }
        // Sponge End
    }

    @Override
    public void bridge$setManualSave(final boolean state) {
        this.impl$isManualSave = state;
    }

    @Override
    public BlockSnapshot bridge$createSnapshot(final int x, final int y, final int z) {
        final BlockPos pos = new BlockPos(x, y, z);

        if (!Level.isInWorldBounds(pos)) {
            return BlockSnapshot.empty();
        }

        if (!this.hasChunk(x >> 4, z >> 4)) {
            return BlockSnapshot.empty();
        }
        final SpongeBlockSnapshot.BuilderImpl builder = SpongeBlockSnapshot.BuilderImpl.pooled();
        builder.world((ServerLevel) (Object) this).position(new Vector3i(x, y, z));
        final net.minecraft.world.level.chunk.LevelChunk chunk = this.shadow$getChunkAt(pos);
        final net.minecraft.world.level.block.state.BlockState state = chunk.getBlockState(pos);
        builder.blockState(state);
        final net.minecraft.world.level.block.entity.BlockEntity blockEntity = chunk.getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
        if (blockEntity != null) {
            TrackingUtil.addTileEntityToBuilder(blockEntity, builder);
        }
        ((LevelChunkBridge) chunk).bridge$getBlockCreatorUUID(pos).ifPresent(builder::creator);
        ((LevelChunkBridge) chunk).bridge$getBlockNotifierUUID(pos).ifPresent(builder::notifier);

        builder.flag(BlockChangeFlags.NONE);
        return builder.build();
    }

    @Override
    public ResourceKey bridge$getKey() {
        return (ResourceKey) (Object) this.shadow$dimension().location();
    }

    @Override
    public long[] bridge$recentTickTimes() {
        return this.impl$recentTickTimes;
    }

    @Redirect(method = "saveLevelData", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private WorldData impl$usePerWorldLevelDataForDragonFight(final MinecraftServer server) {
        return (WorldData) this.shadow$getLevelData();
    }

    @Redirect(method = "setDefaultSpawnPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;addRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V"))
    private void impl$respectKeepSpawnLoaded(final ServerChunkCache serverChunkProvider, final TicketType<Object> p_217228_1_, final ChunkPos p_217228_2_,
            final int p_217228_3_, final Object p_217228_4_) {
        if ((((ServerWorldProperties) this.shadow$getLevelData()).performsSpawnLogic())) {
            serverChunkProvider.addRegionTicket(p_217228_1_, p_217228_2_, p_217228_3_, p_217228_4_);
        }
    }

    /**
     * @author zidane - December 17th, 2020 - Minecraft 1.16.4
     * @reason Honor our serialization behavior in performing saves
     */
    @Overwrite
    public void save(@Nullable final ProgressListener progress, final boolean flush, final boolean skipSave) {

        final boolean isManualSave = this.impl$isManualSave;

        this.impl$isManualSave = false;

        final Cause currentCause = Sponge.server().causeStackManager().currentCause();

        if (Sponge.eventManager().post(SpongeEventFactory.createSaveWorldEventPre(currentCause, ((ServerWorld) this)))) {
            return; // cancelled save
        }

        final PrimaryLevelData levelData = (PrimaryLevelData) this.shadow$getLevelData();

        final ServerChunkCache chunkProvider = ((ServerLevel) (Object) this).getChunkSource();

        if (!skipSave) {

            final SerializationBehavior behavior = ((PrimaryLevelDataBridge) levelData).bridge$serializationBehavior().orElse(SerializationBehavior.AUTOMATIC);

            if (progress != null) {
                progress.progressStartNoAbort(new TranslatableComponent("menu.savingLevel"));
            }

            // We always save the metadata unless it is NONE
            if (behavior != SerializationBehavior.NONE) {

                this.shadow$saveLevelData();

                // Sponge Start - We do per-world WorldInfo/WorldBorders/BossBars

                levelData.setWorldBorder(this.getWorldBorder().createSettings());

                levelData.setCustomBossEvents(((ServerLevelBridge) this).bridge$getBossBarManager().save());

                ((ServerLevelBridge) this).bridge$getLevelSave().saveDataTag(SpongeCommon.server().registryAccess()
                    , (PrimaryLevelData) this.shadow$getLevelData(), this.shadow$dimension() == Level.OVERWORLD ? SpongeCommon.server().getPlayerList()
                        .getSingleplayerData() : null);

                // Sponge End
            }
            if (progress != null) {
                progress.progressStage(new TranslatableComponent("menu.savingChunks"));
            }

            if (behavior == SerializationBehavior.AUTOMATIC || (isManualSave && behavior == SerializationBehavior.MANUAL)) {
                chunkProvider.save(flush);
            }

            Sponge.eventManager().post(SpongeEventFactory.createSaveWorldEventPost(currentCause, ((ServerWorld) this)));
        }
    }

    @Inject(method = "tick",
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;oRainLevel:F", shift = At.Shift.BEFORE, ordinal = 1))
    public void impl$onSetWeatherParameters(final BooleanSupplier param0, final CallbackInfo ci, final ProfilerFiller var0, final boolean wasRaining) {
        final boolean isRaining = this.shadow$isRaining();
        if (this.oRainLevel != this.rainLevel || this.oThunderLevel != this.thunderLevel || wasRaining != isRaining) {
            Weather newWeather = ((ServerWorld) this).properties().weather();
            final Cause currentCause = Sponge.server().causeStackManager().currentCause();
            final Transaction<Weather> weatherTransaction = new Transaction<>(this.impl$prevWeather, newWeather);
            final ChangeWeatherEvent event = SpongeEventFactory.createChangeWeatherEvent(currentCause, ((ServerWorld) this), weatherTransaction);
            if (Sponge.eventManager().post(event)) {
                newWeather = event.weather().original();
            } else {
                newWeather = event.weather().finalReplacement();
            }

            // Set event results
            this.impl$prevWeather = newWeather;
            if (newWeather.type() == WeatherTypes.CLEAR.get()) {
                this.serverLevelData.setThunderTime(0);
                this.serverLevelData.setRainTime(0);
                this.serverLevelData.setClearWeatherTime((int) newWeather.remainingDuration().ticks());
                this.serverLevelData.setThundering(false);
                this.serverLevelData.setRaining(false);
            } else {
                final int newTime = (int) newWeather.remainingDuration().ticks();
                this.serverLevelData.setRaining(true);
                this.serverLevelData.setClearWeatherTime(0);
                this.serverLevelData.setRainTime(newTime);
                if (newWeather.type() == WeatherTypes.THUNDER.get()) {
                    this.serverLevelData.setThunderTime(newTime);
                    this.serverLevelData.setThundering(true);
                } else {
                    this.serverLevelData.setThunderTime(0);
                    this.serverLevelData.setThundering(false);
                }
            }
        }

    }

    @Redirect(method = "tickChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean impl$onBeforeThunder(final ServerLevel serverLevel, final BlockPos param0) {
        final boolean rainingAt = serverLevel.isRainingAt(param0);
        if (rainingAt) {
            final LightningEvent.Pre strike = SpongeEventFactory.createLightningEventPre(Sponge.server().causeStackManager().currentCause());
            if (Sponge.eventManager().post(strike)) {
                return false;
            }
        }
        return rainingAt;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void impl$capturePreTickTime(final BooleanSupplier param0, final CallbackInfo ci) {
        this.impl$preTickTime = Util.getNanos();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void impl$capturePostTickTime(final BooleanSupplier param0, final CallbackInfo ci) {
        final long postTickTime = Util.getNanos();

        this.impl$recentTickTimes[this.shadow$getServer().getTickCount() % 100] = postTickTime - this.impl$preTickTime;
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/level/ServerLevel;emptyTime:I",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
        )
    )
    @SuppressWarnings("SuspiciousMethodCalls")
    private void impl$unloadBlockEntities(final BooleanSupplier param0, final CallbackInfo ci) {
        // This code fixes block entity memory leak
        // https://github.com/SpongePowered/Sponge/pull/3689
        if (this.emptyTime >= 300 && !this.bridge$blockEntitiesToUnload().isEmpty()) {
            this.tickableBlockEntities.removeAll(this.bridge$blockEntitiesToUnload());
            this.blockEntityList.removeAll(this.bridge$blockEntitiesToUnload());
            this.bridge$blockEntitiesToUnload().clear();
        }
    }

    private void impl$setWorldOnBorder() {
        ((WorldBorderBridge) this.shadow$getWorldBorder()).bridge$setAssociatedWorld(this.bridge$getKey());
    }

    @Redirect(method = "updateSleepingPlayerList", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean impl$handleSleepingIgnored(final net.minecraft.server.level.ServerPlayer serverPlayer) {
        return serverPlayer.isSpectator() || ((ServerPlayerBridge) serverPlayer).bridge$sleepingIgnored();
    }

    @Inject(method = "globalLevelEvent", at = @At("HEAD"), cancellable = true)
    private void impl$throwBroadcastGlobalEvent(int effectID, BlockPos pos, int pitch, CallbackInfo ci) {
        if (!this.bridge$isFake() && ShouldFire.PLAY_SOUND_EVENT_BROADCAST) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.SERVER.pushCauseFrame()) {
                final PlaySoundEvent.Broadcast event = SpongeCommonEventFactory.callPlaySoundBroadcastEvent(frame, this, pos, effectID);
                if (event != null && event.isCancelled()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void impl$throwBroadcastEvent(final Player player, final int eventID, final BlockPos pos, final int dataID, CallbackInfo ci) {
        if(eventID == Constants.WorldEvents.PLAY_RECORD_EVENT && ShouldFire.PLAY_SOUND_EVENT_RECORD) {
            try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                final BlockEntity tileEntity = this.shadow$getBlockEntity(pos);
                if(tileEntity instanceof JukeboxBlockEntity) {
                    final JukeboxBlockEntity jukebox = (JukeboxBlockEntity) tileEntity;
                    final ItemStack record = jukebox.getRecord();
                    frame.pushCause(jukebox);
                    frame.addContext(EventContextKeys.USED_ITEM, ItemStackUtil.snapshotOf(record));
                    if (!record.isEmpty()) {
                        final Optional<MusicDisc> recordProperty = ((org.spongepowered.api.item.inventory.ItemStack) (Object) record).get(Keys.MUSIC_DISC);
                        if(!recordProperty.isPresent()) {
                            //Safeguard for https://github.com/SpongePowered/SpongeCommon/issues/2337
                            return;
                        }
                        final MusicDisc recordType = recordProperty.get();
                        final PlaySoundEvent.Record event = SpongeCommonEventFactory.callPlaySoundRecordEvent(frame.currentCause(), jukebox, recordType, dataID);
                        if (event.isCancelled()) {
                            ci.cancel();
                        }
                    }
                }
            }
        }
    }

    @Redirect(method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;add(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/ai/village/poi/PoiType;)V"
        )
    )
    private void impl$avoidAddingPoiUpdatesOnUnloadedWorld(final PoiManager manager, final BlockPos pos, final PoiType type) {
        // Unloaded worlds should not notify PoiManager of changes
        if (!SpongeCommon.server().levelKeys().contains(this.shadow$dimension())) {
            return;
        }
        manager.add(pos, type);
    }

    @Inject(method = "add", at = @At("HEAD"))
    private void impl$constructPostEventForAdd(final Entity entity, final CallbackInfo cir) {
        this.impl$constructPostEventForEntityAdd(entity, null);
    }

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void impl$constructPostEventForEntityAdd(final Entity entity, final CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof EntityBridge)) {
            return;
        }
        if (!((EntityBridge) entity).bridge$isConstructing()) {
            return;
        }
        ((EntityBridge) entity).bridge$fireConstructors();
        final Vec3 position = entity.position();
        final ServerLocation location = ServerLocation.of((ServerWorld) this, position.x(), position.y(), position.z());
        final Vec2 rotationVector = entity.getRotationVector();
        final Vector3d rotation = new Vector3d(rotationVector.x, rotationVector.y, 0);
        try (final CauseStackManager.StackFrame frame = PhaseTracker.SERVER.pushCauseFrame()) {
            frame.pushCause(entity);
            final Event construct = SpongeEventFactory.createConstructEntityEventPost(frame.currentCause(), (org.spongepowered.api.entity.Entity) entity, location, rotation,
                ((EntityType<?>) entity.getType()));
            SpongeCommon.post(construct);
        }
    }

    @Override
    public String toString() {
        final Optional<ResourceKey> worldTypeKey = RegistryTypes.WORLD_TYPE.get().findValueKey((WorldType) this.shadow$dimensionType());
        return new StringJoiner(",", ServerLevel.class.getSimpleName() + "[", "]")
                .add("key=" + this.shadow$dimension())
                .add("worldType=" + worldTypeKey.map (ResourceKey::toString).orElse("inline"))
                .toString();
    }
}
