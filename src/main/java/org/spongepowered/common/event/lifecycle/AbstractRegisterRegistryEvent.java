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
package org.spongepowered.common.event.lifecycle;

import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.Engine;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.lifecycle.RegisterRegistryEvent;
import org.spongepowered.api.registry.DuplicateRegistrationException;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.common.registry.RegistryLoader;
import org.spongepowered.common.registry.SpongeRegistryHolder;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractRegisterRegistryEvent extends AbstractLifecycleEvent implements RegisterRegistryEvent {

    public AbstractRegisterRegistryEvent(final Cause cause, final Game game) {
        super(cause, game);
    }

    @Override
    public <T> RegistryType<T> register(final ResourceKey key, final boolean isDynamic) throws DuplicateRegistrationException {
        Objects.requireNonNull(key, "key");

        final SpongeRegistryHolder holder = this.getHolder();
        final RegistryType<T> type = RegistryType.of(RegistryRoots.SPONGE, key);
        holder.createRegistry(type, (RegistryLoader<T>) null, isDynamic);
        return type;
    }

    @Override
    public <T> RegistryType<T> register(final ResourceKey key, final boolean isDynamic, final Supplier<Map<ResourceKey, T>> defaultValues)
            throws DuplicateRegistrationException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(defaultValues, "defaultValues");

        final SpongeRegistryHolder holder = this.getHolder();
        final RegistryType<T> type = RegistryType.of(RegistryRoots.SPONGE, key);
        holder.createRegistry(type, defaultValues, isDynamic);
        return type;
    }

    protected abstract SpongeRegistryHolder getHolder();

    public static final class GameScopedImpl extends AbstractRegisterRegistryEvent implements RegisterRegistryEvent.GameScoped {

        public GameScopedImpl(final Cause cause, final Game game) {
            super(cause, game);
        }

        @Override
        protected SpongeRegistryHolder getHolder() {
            return (SpongeRegistryHolder) this.game;
        }
    }

    public static final class EngineScopedImpl<E extends Engine> extends AbstractRegisterRegistryEvent implements RegisterRegistryEvent.EngineScoped<E> {

        private final TypeToken<E> token;
        private final E engine;

        public EngineScopedImpl(final Cause cause, final Game game, final E engine) {
            super(cause, game);

            this.token = TypeToken.get((Class<E>) engine.getClass());
            this.engine = engine;
        }

        @Override
        public TypeToken<E> paramType() {
            return this.token;
        }

        @Override
        protected SpongeRegistryHolder getHolder() {
            return (SpongeRegistryHolder) this.engine;
        }
    }

    public static final class WorldScopedImpl extends AbstractRegisterRegistryEvent implements RegisterRegistryEvent.WorldScoped {

        private final ResourceKey worldKey;

        public WorldScopedImpl(final Cause cause, final Game game, final ResourceKey worldKey) {
            super(cause, game);
            this.worldKey = worldKey;
        }

        @Override
        public ResourceKey worldKey() {
            return this.worldKey;
        }

        @Override
        protected SpongeRegistryHolder getHolder() {
            return (SpongeRegistryHolder) this.game.server().worldManager().world(this.worldKey).get();
        }
    }
}
