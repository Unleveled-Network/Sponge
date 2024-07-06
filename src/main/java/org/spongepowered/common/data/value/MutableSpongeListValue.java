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
package org.spongepowered.common.data.value;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.common.data.key.SpongeKey;
import org.spongepowered.common.util.CopyHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MutableSpongeListValue<E> extends MutableSpongeCollectionValue<E, List<E>, ListValue.Mutable<E>, ListValue.Immutable<E>>
        implements ListValue.Mutable<E> {

    public MutableSpongeListValue(Key<? extends ListValue<E>> key, List<E> element) {
        super(key, element);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SpongeKey<? extends ListValue<E>, List<E>> key() {
        return (SpongeKey<? extends ListValue<E>, List<E>>) super.key();
    }

    @Override
    public E get(int index) {
        return this.element.get(index);
    }

    @Override
    public int indexOf(E element) {
        return this.element.indexOf(element);
    }

    @Override
    public ListValue.Mutable<E> add(int index, E value) {
        return this.modifyCollection(list -> list.add(index, value));
    }

    @Override
    public ListValue.Mutable<E> add(int index, Iterable<E> values) {
        return this.modifyCollection(list -> {
            int offset = 0;
            for (final E value : values) {
                list.add(index + offset++, value);
            }
        });
    }

    @Override
    public ListValue.Mutable<E> remove(int index) {
        return this.modifyCollection(list -> list.remove(index));
    }

    @Override
    public ListValue.Mutable<E> set(int index, E element) {
        return this.modifyCollection(list -> list.set(index, element));
    }

    @Override
    public ListValue.Immutable<E> asImmutable() {
        return this.key().getValueConstructor().getImmutable(this.element).asImmutable();
    }

    @Override
    public ListValue.Mutable<E> copy() {
        return new MutableSpongeListValue<>(this.key(), CopyHelper.copy(this.element));
    }

    @Override
    protected ListValue.Mutable<E> modifyCollection(Consumer<List<E>> consumer) {
        final List<E> list = this.element;
        if (list instanceof ImmutableList) {
            final List<E> copy = new ArrayList<>(list);
            consumer.accept(copy);
            this.set(ImmutableList.copyOf(copy));
        } else {
            consumer.accept(list);
        }
        return this;
    }
}
