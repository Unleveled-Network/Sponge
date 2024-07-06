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
package org.spongepowered.common.item;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.item.FireworkEffect;
import org.spongepowered.api.item.FireworkShape;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Color;
import org.spongepowered.common.util.Constants;

import java.util.List;
import java.util.Objects;

public class SpongeFireworkEffect implements FireworkEffect {

    private final boolean flicker;
    private final boolean trails;
    private final ImmutableList<Color> colors;
    private final ImmutableList<Color> fades;
    private final FireworkShape shape;

    SpongeFireworkEffect(final boolean flicker, final boolean trails, final Iterable<Color> colors, final Iterable<Color> fades, final FireworkShape shape) {
        this.flicker = flicker;
        this.trails = trails;
        this.colors = ImmutableList.copyOf(colors);
        this.fades = ImmutableList.copyOf(fades);
        this.shape = shape;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final SpongeFireworkEffect that = (SpongeFireworkEffect) o;
        return this.flicker == that.flicker &&
                this.trails == that.trails &&
                Objects.equals(this.colors, that.colors) &&
                Objects.equals(this.fades, that.fades) &&
                Objects.equals(this.shape, that.shape);
    }

    @Override
    public int hashCode() {

        return Objects.hash(this.flicker, this.trails, this.colors, this.fades, this.shape);
    }

    @Override
    public boolean flickers() {
        return this.flicker;
    }

    @Override
    public boolean hasTrail() {
        return this.trails;
    }

    @Override
    public List<Color> colors() {
        return this.colors;
    }

    @Override
    public List<Color> fadeColors() {
        return this.fades;
    }

    @Override
    public FireworkShape shape() {
        return this.shape;
    }

    @Override
    public int contentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        final ResourceKey resourceKey = Sponge.game().registry(RegistryTypes.FIREWORK_SHAPE).valueKey(this.shape);

        return DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, this.contentVersion())
                .set(Constants.Item.Fireworks.FIREWORK_SHAPE, resourceKey)
                .set(Constants.Item.Fireworks.FIREWORK_COLORS, this.colors)
                .set(Constants.Item.Fireworks.FIREWORK_FADE_COLORS, this.fades)
                .set(Constants.Item.Fireworks.FIREWORK_TRAILS, this.trails)
                .set(Constants.Item.Fireworks.FIREWORK_FLICKERS, this.flicker);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("shape", this.shape)
                .add("trails", this.trails)
                .add("flickers", this.flicker)
                .add("colors", this.colors)
                .add("fades", this.fades)
                .toString();
    }
}
