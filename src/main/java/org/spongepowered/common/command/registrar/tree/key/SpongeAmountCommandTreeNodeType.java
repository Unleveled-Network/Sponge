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
package org.spongepowered.common.command.registrar.tree.key;

import com.mojang.brigadier.arguments.ArgumentType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.registrar.tree.CommandTreeNode;
import org.spongepowered.api.command.registrar.tree.CommandTreeNodeType;
import org.spongepowered.common.AbstractResourceKeyed;
import org.spongepowered.common.command.registrar.tree.builder.AmountCommandTreeNode;

public final class SpongeAmountCommandTreeNodeType extends AbstractResourceKeyed implements CommandTreeNodeType<CommandTreeNode.@NonNull Amount> {

    private final ArgumentType<?> ifSingle;
    private final ArgumentType<?> ifMultiple;

    public SpongeAmountCommandTreeNodeType(final ResourceKey key, final ArgumentType<?> ifSingle, final ArgumentType<?> ifMultiple) {
        super(key);

        this.ifSingle = ifSingle;
        this.ifMultiple = ifMultiple;
    }

    @Override
    public CommandTreeNode.@NonNull Amount createNode() {
        return new AmountCommandTreeNode(this, this.ifSingle, this.ifMultiple);
    }
}
