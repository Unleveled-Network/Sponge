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
package org.spongepowered.common.inventory.lens.impl;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.accessor.world.inventory.SlotAccessor;
import org.spongepowered.common.inventory.adapter.impl.BasicInventoryAdapter;
import org.spongepowered.common.inventory.fabric.Fabric;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.impl.slot.SlotLensProvider;
import org.spongepowered.common.inventory.lens.slots.SlotLens;

import java.util.ArrayList;
import java.util.List;

/**
 * A delegating Lens used in Containers. Provides ordered inventory access.
 */
@SuppressWarnings("rawtypes")
public class DelegatingLens extends AbstractLens {

    private Lens delegate;

    public DelegatingLens(final int base, final Lens lens) {
        super(base, lens.slotCount(), BasicInventoryAdapter.class);
        this.delegate = lens;
        this.addSpanningChild(lens);
    }

    public DelegatingLens(final int base, final Lens lens, final SlotLensProvider slots) {
        super(base, lens.slotCount(), BasicInventoryAdapter.class);
        this.delegate = lens;
        this.init(slots);
    }
    // TODO check if this is still working as intended
    public DelegatingLens(final int base, final List<Slot> containerSlots, final Lens lens, final SlotLensProvider slots) {
        super(base, containerSlots.size(), BasicInventoryAdapter.class);
        this.delegate = lens;
        final CustomSlotProvider slotProvider = new CustomSlotProvider();
        for (final Slot slot : containerSlots) {
            // Get slots from original slot provider and add them to custom slot provider in order of actual containerSlots.
            slotProvider.add(slots.getSlotLens(((SlotAccessor) slot).accessor$index()));
        }
        // Provide indexed access over the Container to the slots in the base inventory
        this.addSpanningChild(new DefaultIndexedLens(0, containerSlots.size(), slotProvider));
        this.addChild(this.delegate);
    }

    protected void init(final SlotLensProvider slots) {
        this.addSpanningChild(new DefaultIndexedLens(this.base, this.size, slots));
        this.addChild(this.delegate);
    }

    public Lens getDelegate() {
        return this.delegate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Inventory getAdapter(final Fabric fabric, final Inventory parent) {
        return this.delegate.getAdapter(fabric.fabric$offset(this.base), parent);
    }

    public static class CustomSlotProvider implements SlotLensProvider {

        private List<SlotLens> lenses = new ArrayList<>();

        public void add(final SlotLens toAdd) {
            this.lenses.add(toAdd);
        }

        @Override
        public SlotLens getSlotLens(final int index) {
            return this.lenses.get(index);
        }
    }
}
