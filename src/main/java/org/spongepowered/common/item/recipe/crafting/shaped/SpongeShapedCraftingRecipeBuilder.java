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
package org.spongepowered.common.item.recipe.crafting.shaped;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.common.inventory.util.InventoryUtil;
import org.spongepowered.common.item.recipe.SpongeRecipeRegistration;
import org.spongepowered.common.item.recipe.ingredient.IngredientUtil;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.AbstractResourceKeyedBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SpongeShapedCraftingRecipeBuilder extends AbstractResourceKeyedBuilder<RecipeRegistration, ShapedCraftingRecipe.Builder> implements
        ShapedCraftingRecipe.Builder, ShapedCraftingRecipe.Builder.AisleStep.ResultStep,
        ShapedCraftingRecipe.Builder.RowsStep.ResultStep, ShapedCraftingRecipe.Builder.EndStep {

    private List<String> aisle = Lists.newArrayList();
    private final Map<Character, Ingredient> ingredientMap = new Char2ObjectArrayMap<>();
    private final Map<Ingredient, Character> reverseIngredientMap = new IdentityHashMap<>();

    private ItemStack result = ItemStack.empty();
    private Function<net.minecraft.world.inventory.CraftingContainer, NonNullList<net.minecraft.world.item.ItemStack>> remainingItemsFunction;
    private Function<net.minecraft.world.inventory.CraftingContainer, net.minecraft.world.item.ItemStack> resultFunction;

    private String group;

    @Override
    public AisleStep aisle(final String... aisle) {
        checkNotNull(aisle, "aisle");
        this.aisle.clear();
        this.ingredientMap.clear();
        this.reverseIngredientMap.clear();
        Collections.addAll(this.aisle, aisle);
        return this;
    }

    @Override
    public AisleStep.ResultStep where(final char symbol, final Ingredient ingredient) throws IllegalArgumentException {
        if (this.aisle.stream().noneMatch(row -> row.indexOf(symbol) >= 0)) {
            throw new IllegalArgumentException("The symbol '" + symbol + "' is not defined in the aisle pattern.");
        }
        this.ingredientMap.put(symbol, ingredient == null ? Ingredient.empty() : ingredient);
        this.reverseIngredientMap.put(ingredient, symbol);
        return this;
    }

    @Override
    public AisleStep.ResultStep where(final Map<Character, Ingredient> ingredientMap) throws IllegalArgumentException {
        for (final Map.Entry<Character, Ingredient> entry : ingredientMap.entrySet()) {
            this.where(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public RowsStep rows() {
        this.aisle.clear();
        this.ingredientMap.clear();
        return this;
    }

    @Override
    public RowsStep.ResultStep row(final int skip, final Ingredient... ingredients) {
        final int columns = ingredients.length + skip;
        if (!this.aisle.isEmpty()) {
            checkState(this.aisle.get(0).length() == columns, "The rows have an inconsistent width.");
        }
        final StringBuilder row = new StringBuilder();
        for (int i = 0; i < skip; i++) {
            row.append(" ");
        }

        int key = 'A' + (columns * this.aisle.size());
        for (final Ingredient ingredient : ingredients) {
            Character usedKey = this.reverseIngredientMap.get(ingredient);
            if (usedKey == null) {
                usedKey = (char) key;
                key++;
            }
            row.append(usedKey);
            this.ingredientMap.put(usedKey, ingredient);
            this.reverseIngredientMap.put(ingredient, usedKey);
        }
        this.aisle.add(row.toString());
        return this;
    }

    public ShapedCraftingRecipe.Builder.ResultStep shapedLike(ShapedCraftingRecipe recipe) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public ShapedCraftingRecipe.Builder.ResultStep remainingItems(Function<CraftingGridInventory, List<ItemStack>> remainingItemsFunction) {
        this.remainingItemsFunction = grid -> {
            final NonNullList<net.minecraft.world.item.ItemStack> mcList = NonNullList.create();
            remainingItemsFunction.apply(InventoryUtil.toSpongeInventory(grid)).forEach(stack -> mcList.add(ItemStackUtil.toNative(stack)));
            return mcList;
        };
        return this;
    }

    @Override
    public EndStep result(ItemStackSnapshot result) {
        checkNotNull(result, "result");
        return this.result(result.createStack());
    }

    @Override
    public EndStep result(final ItemStack result) {
        checkNotNull(result, "result");
        this.result = result.copy();
        this.resultFunction = null;
        return this;
    }

    @Override
    public EndStep result(Function<CraftingGridInventory, ItemStack> resultFunction, ItemStack exemplaryResult) {
        this.resultFunction = (inv) -> ItemStackUtil.toNative(resultFunction.apply(InventoryUtil.toSpongeInventory(inv)));
        this.result = exemplaryResult.copy();
        return this;
    }

    @Override
    public EndStep group(final @Nullable String name) {
        this.group = name;
        return this;
    }

    @Override
    public RecipeRegistration build0() {
        checkState(!this.aisle.isEmpty(), "aisle has not been set");
        checkState(!this.ingredientMap.isEmpty(), "no ingredients set");
        checkState(!this.result.isEmpty(), "no result set");

        final Iterator<String> aisleIterator = this.aisle.iterator();
        String aisleRow = aisleIterator.next();
        final int width = aisleRow.length();

        checkState(width > 0, "The aisle cannot be empty.");

        while (aisleIterator.hasNext()) {
            aisleRow = aisleIterator.next();
            checkState(aisleRow.length() == width, "The aisle has an inconsistent width.");
        }

        final Map<Character, net.minecraft.world.item.crafting.Ingredient> ingredientsMap = this.ingredientMap.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> IngredientUtil.toNative(e.getValue())));

        // Default space to Empty Ingredient
//        ingredientsMap.putIfAbsent(' ', net.minecraft.item.crafting.Ingredient.EMPTY);
        final net.minecraft.world.item.ItemStack resultStack = ItemStackUtil.toNative(this.result);
        final RecipeSerializer<?> serializer = SpongeRecipeRegistration.determineSerializer(resultStack, this.resultFunction, this.remainingItemsFunction, ingredientsMap,
                RecipeSerializer.SHAPED_RECIPE, SpongeShapedCraftingRecipeSerializer.SPONGE_CRAFTING_SHAPED);
        return new SpongeShapedCraftingRecipeRegistration((ResourceLocation)(Object) key, serializer, this.group, this.aisle, ingredientsMap, resultStack, this.resultFunction, this.remainingItemsFunction);
    }

    @Override
    public ShapedCraftingRecipe.Builder reset() {
        super.reset();
        this.aisle = new ArrayList<>();
        this.ingredientMap.clear();
        this.result = ItemStack.empty();
        this.resultFunction = null;
        this.group = null;
        this.remainingItemsFunction = null;
        return this;
    }

}
