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
package org.spongepowered.common.world.storage;

import org.spongepowered.api.world.storage.ChunkLayout;
import org.spongepowered.math.vector.Vector3i;

public final class SpongeChunkLayout implements ChunkLayout {

    public static final SpongeChunkLayout INSTANCE = new SpongeChunkLayout();

    public static final Vector3i CHUNK_SIZE = new Vector3i(16, 256, 16);
    public static final Vector3i CHUNK_MASK = SpongeChunkLayout.CHUNK_SIZE.sub(1, 1, 1);
    private static final Vector3i SPACE_MAX = new Vector3i(30000000, 256, 30000000).sub(1, 1, 1).div(SpongeChunkLayout.CHUNK_SIZE);
    private static final Vector3i SPACE_MIN = new Vector3i(-30000000, 0, -30000000).div(SpongeChunkLayout.CHUNK_SIZE);
    private static final Vector3i SPACE_SIZE = SpongeChunkLayout.SPACE_MAX.sub(SpongeChunkLayout.SPACE_MIN).add(1, 1, 1);

    private SpongeChunkLayout() {
    }

    @Override
    public Vector3i chunkSize() {
        return SpongeChunkLayout.CHUNK_SIZE;
    }

    @Override
    public Vector3i spaceMax() {
        return SpongeChunkLayout.SPACE_MAX;
    }

    @Override
    public Vector3i spaceMin() {
        return SpongeChunkLayout.SPACE_MIN;
    }

    @Override
    public Vector3i spaceSize() {
        return SpongeChunkLayout.SPACE_SIZE;
    }

    @Override
    public Vector3i spaceOrigin() {
        return Vector3i.ZERO;
    }

    @Override
    public boolean isInChunk(final int x, final int y, final int z) {
        // no bits allowed outside the mask!
        return (x & ~SpongeChunkLayout.CHUNK_MASK.x()) == 0 && (y & ~SpongeChunkLayout.CHUNK_MASK.y()) == 0 && (z & ~SpongeChunkLayout.CHUNK_MASK
            .z()) == 0;
    }

    @Override
    public boolean isInChunk(final int wx, final int wy, final int wz, final int cx, final int cy, final int cz) {
        return this.isInChunk(wx - (cx << 4), wy - (cy << 8), wz - (cz << 4));
    }

    @Override
    public Vector3i forceToChunk(final int x, final int y, final int z) {
        return new Vector3i(x >> 4, y >> 8, z >> 4);
    }

    @Override
    public Vector3i forceToWorld(final int x, final int y, final int z) {
        return new Vector3i(x << 4, y << 8, z << 4);
    }

}
