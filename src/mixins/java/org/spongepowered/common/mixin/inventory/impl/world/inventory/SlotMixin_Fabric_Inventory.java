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
package org.spongepowered.common.mixin.inventory.impl.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.inventory.InventoryBridge;
import org.spongepowered.common.inventory.fabric.Fabric;

import java.util.Collection;
import java.util.Collections;

@Mixin(Slot.class)
public abstract class SlotMixin_Fabric_Inventory implements Fabric, InventoryBridge {
    @Shadow @Final public Container container;
    @Shadow public abstract ItemStack shadow$getItem();
    @Shadow public abstract void shadow$set(ItemStack stack);
    @Shadow public abstract int shadow$getMaxStackSize();
    @Shadow public abstract void shadow$setChanged();

    @Override
    public Collection<InventoryBridge> fabric$allInventories() {
        return Collections.emptyList();
    }

    @Override
    public InventoryBridge fabric$get(final int index) {
        if (this.container != null) {
            return (InventoryBridge) this.container;
        }

        throw new UnsupportedOperationException("Unable to access slot at " + index + " for delegating fabric of " + this.getClass());
    }

    @Override
    public ItemStack fabric$getStack(final int index) {
        return this.shadow$getItem();
    }

    @Override
    public void fabric$setStack(final int index, final ItemStack stack) {
        this.shadow$set(stack);
    }

    @Override
    public int fabric$getMaxStackSize() {
        return this.shadow$getMaxStackSize();
    }

    @Override
    public int fabric$getSize() {
        return 1;
    }

    @Override
    public void fabric$clear() {
        this.shadow$set(ItemStack.EMPTY);
    }

    @Override
    public void fabric$markDirty() {
        this.shadow$setChanged();
    }
}
