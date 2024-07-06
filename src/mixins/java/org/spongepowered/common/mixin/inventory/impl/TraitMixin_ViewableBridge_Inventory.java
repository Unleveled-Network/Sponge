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
package org.spongepowered.common.mixin.inventory.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.bridge.world.inventory.ViewableInventoryBridge;
import org.spongepowered.common.inventory.custom.ViewableCustomInventory;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link org.spongepowered.common.mixin.inventory.api.TraitMixin_Viewable_Inventory_API}
 */
@Mixin(value = {
        // MenuProvider impls:
        AbstractMinecartContainer.class,
        LecternBlockEntity.class,
        BaseContainerBlockEntity.class,
        ViewableCustomInventory.class,
        // Merchant impls:
        AbstractVillager.class,
        ClientSideMerchant.class,
        // ChestBlock - DoubleSidedInventory
        CompoundContainer.class,
        PlayerEnderChestContainer.class
})
public abstract class TraitMixin_ViewableBridge_Inventory implements ViewableInventoryBridge {

    private final Set<ServerPlayer> impl$viewers = new HashSet<>();

    @Override
    public void viewableBridge$addPlayer(ServerPlayer container) {
        this.impl$viewers.add(container);
    }

    @Override
    public void viewableBridge$removePlayer(ServerPlayer container) {
        this.impl$viewers.remove(container);
    }

    @Override
    public Set<ServerPlayer> viewableBridge$getViewers() {
        return this.impl$viewers;
    }

    @Override
    public boolean viewableBridge$hasViewers() {
        return !this.impl$viewers.isEmpty();
    }
}
