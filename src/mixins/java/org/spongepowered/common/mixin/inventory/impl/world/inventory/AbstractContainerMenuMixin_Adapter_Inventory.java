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

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.world.inventory.InventoryBridge;
import org.spongepowered.common.bridge.world.inventory.LensGeneratorBridge;
import org.spongepowered.common.bridge.world.inventory.container.ContainerBridge;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.fabric.Fabric;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.impl.LensRegistrar;
import org.spongepowered.common.inventory.lens.impl.slot.SlotLensProvider;
import org.spongepowered.common.inventory.lens.slots.SlotLens;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin_Adapter_Inventory implements InventoryBridge, LensGeneratorBridge, InventoryAdapter, ContainerBridge {

    @Shadow @Final public List<Slot> slots;

    private boolean impl$isLensInitialized;
    private boolean impl$spectatorChest;

    @Override
    public void inventoryAdapter$setSpectatorChest(final boolean spectatorChest) {
        this.impl$spectatorChest = spectatorChest;
    }

    @Override
    public SlotLensProvider lensGeneratorBridge$generateSlotLensProvider() {
        return new LensRegistrar.BasicSlotLensProvider(this.slots.size());
    }

    @Inject(method = "addSlot", at = @At(value = "HEAD"))
    private void impl$onAddSlotToContainer(final Slot slotIn, final CallbackInfoReturnable<Slot> cir) {
        this.impl$isLensInitialized = false;
        this.impl$provider = null;
        this.impl$lens = null;
        this.impl$slots.clear();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Lens lensGeneratorBridge$generateLens(SlotLensProvider slotLensProvider) {
        if (this.impl$isLensInitialized) {
            return null; // Means that we've tried to generate a lens before, but it was null. And because the lens is null,
            // the generate will try again. So, we stop trying to generate it.
        }
        this.impl$isLensInitialized = true;

        if (this.impl$spectatorChest) { // TODO check if this is needed - why can we not provide a basic lens?
            return null;
        }

        return LensRegistrar.getLens(this, slotLensProvider, this.slots.size());
    }

    private final Map<Integer, org.spongepowered.api.item.inventory.Slot> impl$slots = new Int2ObjectArrayMap<>();

    @Override
    public Optional<org.spongepowered.api.item.inventory.Slot> inventoryAdapter$getSlot(int ordinal) {
        org.spongepowered.api.item.inventory.Slot slot = this.impl$slots.get(ordinal);
        if (slot == null) {
            Lens rootLens = this.inventoryAdapter$getRootLens();
            SlotLens slotLens = rootLens.getSlotLens(this.inventoryAdapter$getFabric(), ordinal);
            if (slotLens == null) {
                return Optional.empty();
            }
            slot = slotLens.getAdapter(this.inventoryAdapter$getFabric(), ((Inventory) this));
            this.impl$slots.put(ordinal, slot);
        }
        return Optional.of(slot);
    }

    @Nullable private SlotLensProvider impl$provider;
    @Nullable private Lens impl$lens;

    @Override
    public Fabric inventoryAdapter$getFabric() {
        return (Fabric) this;
    }

    @Override
    public SlotLensProvider inventoryAdapter$getSlotLensProvider() {
        if (this.impl$provider == null) {
            this.impl$provider = this.lensGeneratorBridge$generateSlotLensProvider();
        }
        return this.impl$provider;
    }

    @Override
    public Lens inventoryAdapter$getRootLens() {
        if (this.impl$lens == null) {
            this.impl$lens = this.lensGeneratorBridge$generateLens(this.inventoryAdapter$getSlotLensProvider());
        }
        return this.impl$lens;
    }

}
