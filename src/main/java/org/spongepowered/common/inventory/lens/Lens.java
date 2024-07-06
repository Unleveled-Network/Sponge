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
package org.spongepowered.common.inventory.lens;

import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.inventory.fabric.Fabric;
import org.spongepowered.common.inventory.lens.slots.SlotLens;

import java.util.List;

/**
 * Base Lens interface. A lens presents an indexed view of a number of child
 * lenses, the leaf nodes in this structure being {@link SlotLens}es. Slots can
 * be accessed directly via this lens, and their index within the lens is the
 * slot <tt>ordinal</tt>.
 *
 * <p>Internally, a lens will always attempt to fetch a particular slot via
 * whichever <em>child</em> lens can provide access to the target slot which is
 * part of the lens's spanning set. For query purposes, the lens may have other
 * child lenses which provide other ways of looking at the same collection of
 * slots, however most of the time access is via the spanning set.</p>
 */
public interface Lens extends LensCollection {

    /**
     * Returns the <em>primary</em> parent lens of this lens. Can be null.
     */
    Lens getParent();

    /**
     * Get the corresponding adapter type for this lens
     *
     * @return class of the adapter which corresponds to this specific lens type
     */
    Class<? extends Inventory> getAdapterType();

    /**
     * Get an instance of the corresponding adapter type for this lens
     *
     * @return adapter for this lens
     */
    Inventory getAdapter(Fabric fabric, Inventory parent);

    /**
     * Get the number of slots referenced by this lens
     *
     * @return
     */
    int slotCount();

    /**
     * Get the maximum stack size from the target inventory
     *
     * @param fabric
     * @return
     */
    int getMaxStackSize(Fabric fabric);

    /**
     * Get child lenses of this lens
     *
     * @return
     */
    List<Lens> getChildren();

    /**
     * Get child lenses of this lens
     *
     * @return
     */
    List<Lens> getSpanningChildren();

    /**
     * Set the stack at the specified offset
     *
     * @param fabric
     * @param ordinal
     * @param stack
     * @return
     */
    default boolean setStack(Fabric fabric, int ordinal, ItemStack stack) {
        SlotLens slot = this.getSlotLens(fabric, ordinal);
        if (slot != null) {
            return slot.setStack(fabric, stack);
        }
        return false;
    }

    /**
     * Gets the itemstack for the specified slot ordinal. Returns null if
     * the specified ordinal is outside the range of this lens.
     *
     * @param fabric inventory
     * @param ordinal slot ordinal
     * @return the item stack in the specified slot
     */
    default @Nullable ItemStack getStack(Fabric fabric, int ordinal) {
        SlotLens slot = this.getSlotLens(fabric, ordinal);
        if (slot != null) {
            return slot.getStack(fabric);
        }
        return null;
    }

    List<SlotLens> getSlots(Fabric fabric);
    SlotLens getSlotLens(Fabric fabric, int ordinal);

    String toString(int deep);

    default int base() {
        return 0;
    }

}
