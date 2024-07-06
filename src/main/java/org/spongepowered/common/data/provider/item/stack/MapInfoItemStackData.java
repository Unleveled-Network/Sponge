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
package org.spongepowered.common.data.provider.item.stack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.map.MapInfo;
import org.spongepowered.api.world.DefaultWorldKeys;
import org.spongepowered.common.bridge.world.storage.MapItemSavedDataBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.Constants;


public final class MapInfoItemStackData {

    private MapInfoItemStackData() {
    }

    // @formatter:off
	public static void register(final DataProviderRegistrator registrator) {
		registrator
				.asMutable(ItemStack.class)
					.create(Keys.MAP_INFO)
						.supports(item -> item.getItem() instanceof MapItem)
						.get(itemStack -> {
							if (itemStack.getTag() == null) {
								return null;
							}
							return (MapInfo) ((Level)Sponge.server().worldManager().world(DefaultWorldKeys.DEFAULT).get())
									.getMapData(Constants.Map.MAP_PREFIX + itemStack.getTag().getInt(Constants.Map.MAP_ID));
						}) // Nullable
						.set((itemStack, mapInfo) -> {
							@Nullable CompoundTag nbt = itemStack.getTag();
							if (nbt == null) {
								nbt = new CompoundTag();
							}
							nbt.putInt(Constants.Map.MAP_ID, ((MapItemSavedDataBridge)mapInfo).bridge$getMapId());
							itemStack.setTag(nbt);
						});
	}
	// @formatter:on
}
