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
package org.spongepowered.common.item.recipe.ingredient;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class SpongePredicateItemList extends SpongeItemList {

    public static final String TYPE_PREDICATE = "sponge:predicate";
    public static final String INGREDIENT_PREDICATE = "sponge:predicate";

    private final String id;
    private final Predicate<ItemStack> predicate;

    public SpongePredicateItemList(String id, Predicate<ItemStack> predicate, ItemStack... stacks) {
        super(stacks);
        this.id = id;
        this.predicate = predicate;
    }

    @Override
    public JsonObject serialize() {
        final JsonObject jsonobject = super.serialize();
        jsonobject.addProperty(SpongeItemList.INGREDIENT_TYPE, SpongePredicateItemList.TYPE_PREDICATE);
        jsonobject.addProperty(SpongePredicateItemList.INGREDIENT_PREDICATE, this.id);
        return jsonobject;
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.predicate.test(stack);
    }
}
