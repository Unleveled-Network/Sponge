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
package org.spongepowered.common.item.recipe.crafting.custom;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.SpecialCraftingRecipe;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.util.AbstractResourceKeyedBuilder;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class SpongeSpecialCraftingRecipeBuilder extends AbstractResourceKeyedBuilder<RecipeRegistration, SpecialCraftingRecipe.Builder>
        implements SpecialCraftingRecipe.Builder, SpecialCraftingRecipe.Builder.ResultStep, SpecialCraftingRecipe.Builder.EndStep {

    private BiPredicate<CraftingGridInventory, ServerWorld> biPredicate;
    private Function<CraftingGridInventory, List<ItemStack>> remainingItemsFunction;
    private Function<CraftingGridInventory, ItemStack> resultFunction;

    @Override
    public ResultStep matching(BiPredicate<CraftingGridInventory, ServerWorld> biPredicate) {
        this.biPredicate = biPredicate;
        return this;
    }

    @Override
    public ResultStep remainingItems(Function<CraftingGridInventory, List<ItemStack>> remainingItemsFunction) {
        this.remainingItemsFunction = remainingItemsFunction;
        return this;
    }

    @Override
    public EndStep result(Function<CraftingGridInventory, ItemStack> resultFunction) {
        this.resultFunction = resultFunction;
        return this;
    }

    @Override
    public EndStep result(ItemStack result) {
        final ItemStack copy = result.copy();
        this.resultFunction = inv -> copy;
        return this;
    }

    @Override
    public RecipeRegistration build0() {
        final ResourceLocation resourceLocation = (ResourceLocation) (Object) this.key;
        return new SpongeSpecialCraftingRecipeRegistration(resourceLocation, this.biPredicate, this.remainingItemsFunction, this.resultFunction);
    }

    @Override
    public SpecialCraftingRecipe.Builder reset() {
        super.reset();
        this.biPredicate = null;
        this.remainingItemsFunction = null;
        this.resultFunction = null;
        return this;
    }

}
