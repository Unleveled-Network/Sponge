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
package org.spongepowered.common.data.key;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.common.AbstractResourceKeyed;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.provider.EmptyDataProvider;
import org.spongepowered.common.data.value.ValueConstructor;
import org.spongepowered.common.data.value.ValueConstructorFactory;
import org.spongepowered.plugin.PluginContainer;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public final class SpongeKey<V extends Value<E>, E> extends AbstractResourceKeyed implements Key<V> {

    private final Type valueType;
    private final Type elementType;
    private final Comparator<? super E> elementComparator;
    private final BiPredicate<? super E, ? super E> elementIncludesTester;
    private final ValueConstructor<V, E> valueConstructor;
    private final Supplier<E> defaultValueSupplier;
    private final EmptyDataProvider<V, E> emptyDataProvider;

    public SpongeKey(final ResourceKey key, final Type valueType, final Type elementType,
            final Comparator<? super E> elementComparator,
            final BiPredicate<? super E, ? super E> elementIncludesTester, final Supplier<E> defaultValueSupplier) {
        super(key);

        this.valueType = valueType;
        this.elementType = elementType;
        this.elementComparator = elementComparator;
        this.elementIncludesTester = elementIncludesTester;
        this.defaultValueSupplier = defaultValueSupplier;
        this.emptyDataProvider = new EmptyDataProvider<>(this);
        this.valueConstructor = ValueConstructorFactory.getConstructor(this);
    }

    @Override
    public Type valueType() {
        return this.valueType;
    }

    @Override
    public Type elementType() {
        return this.elementType;
    }

    @Override
    public Comparator<? super E> elementComparator() {
        return this.elementComparator;
    }

    @Override
    public BiPredicate<? super E, ? super E> elementIncludesTester() {
        return this.elementIncludesTester;
    }

    @Override
    public <H extends DataHolder> void registerEvent(final PluginContainer plugin, final Class<H> holderFilter,
            final EventListener<ChangeDataHolderEvent.ValueChange> listener) {
        ((SpongeDataManager) Sponge.game().dataManager()).registerKeyListener(new KeyBasedDataListener<>(plugin, holderFilter, this, listener));
    }

    public ValueConstructor<V, E> getValueConstructor() {
        return this.valueConstructor;
    }

    public Supplier<E> getDefaultValueSupplier() {
        return this.defaultValueSupplier;
    }

    public EmptyDataProvider<V, E> getEmptyDataProvider() {
        return this.emptyDataProvider;
    }

    @Override
    public String toString() {
        return "SpongeKey{" +
            "key=" + this.key() +
            ", elementType=" + elementType +
            '}';
    }
}
