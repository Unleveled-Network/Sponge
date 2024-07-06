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
package org.spongepowered.common.item.merchant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.minecraft.world.item.trading.MerchantOffer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.merchant.TradeOffer;
import org.spongepowered.common.accessor.world.item.trading.MerchantOfferAccessor;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class SpongeTradeOfferBuilder extends AbstractDataBuilder<TradeOffer> implements TradeOffer.Builder, DataBuilder<TradeOffer> {

    private ItemStackSnapshot firstItem = ItemStackSnapshot.empty();
    private @Nullable ItemStackSnapshot secondItem;
    private ItemStackSnapshot sellingItem = ItemStackSnapshot.empty();
    private int useCount;
    private int maxUses;
    private boolean allowsExperience;
    private int merchantExperienceGranted;
    private double priceGrowthMultiplier;
    private int demandBonus;

    public SpongeTradeOfferBuilder() {
        super(TradeOffer.class, 1);
        this.reset();
    }

    @Override
    public TradeOffer.Builder firstBuyingItem(final ItemStack item) {
        checkNotNull(item, "Buying item cannot be null");
        this.firstItem = item.createSnapshot();
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public TradeOffer.Builder secondBuyingItem(final ItemStack item) {
        this.secondItem = item != null ? item.createSnapshot() : ItemStackSnapshot.empty();
        return this;
    }

    @Override
    public TradeOffer.Builder sellingItem(final ItemStack item) {
        this.sellingItem = item.createSnapshot();
        return this;
    }

    @Override
    public TradeOffer.Builder uses(final int uses) {
        checkArgument(uses >= 0, "Usage count cannot be negative");
        this.useCount = uses;
        return this;
    }

    @Override
    public TradeOffer.Builder maxUses(final int maxUses) {
        checkArgument(maxUses > 0, "Max usage count must be greater than 0");
        this.maxUses = maxUses;
        return this;
    }

    @Override
    public TradeOffer.Builder canGrantExperience(final boolean experience) {
        this.allowsExperience = experience;
        return this;
    }

    @Override
    public TradeOffer.Builder merchantExperienceGranted(final int experience) {
        this.merchantExperienceGranted = experience;
        return this;
    }

    @Override
    public TradeOffer.Builder priceGrowthMultiplier(final double priceGrowthMultiplier) {
        this.priceGrowthMultiplier = priceGrowthMultiplier;
        return this;
    }

    @Override
    public TradeOffer.Builder demandBonus(final int bonus) {
        this.demandBonus = bonus;
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public TradeOffer build() throws IllegalStateException {
        checkState(this.firstItem != null, "Trading item has not been set");
        checkState(this.sellingItem != null, "Selling item has not been set");
        checkState(this.useCount <= this.maxUses, "Usage count cannot be greater than the max usage count (%s)", this.maxUses);
        final ItemStack first = this.firstItem.createStack();
        final ItemStack second = this.secondItem == null ? null : this.secondItem.createStack();
        final ItemStack selling = this.sellingItem.createStack();
        final MerchantOffer merchantOffer = new MerchantOffer(ItemStackUtil.toNative(first), ItemStackUtil.toNative(second), ItemStackUtil.toNative(selling),
                        this.useCount, this.maxUses, this.merchantExperienceGranted, (float) this.priceGrowthMultiplier);
        ((MerchantOfferAccessor) merchantOffer).accessor$rewardExp(this.allowsExperience);
        ((MerchantOfferAccessor) merchantOffer).accessor$demand(this.demandBonus);
        return (TradeOffer) merchantOffer;
    }

    @Override
    public TradeOffer.Builder from(final TradeOffer offer) {
        checkNotNull(offer, "Trade offer cannot be null");
        // Assumes the offer's values don't need to be validated
        this.firstItem = offer.firstBuyingItem();
        this.secondItem = offer.secondBuyingItem().orElse(null);
        this.sellingItem = offer.sellingItem();
        this.useCount = offer.uses();
        this.maxUses = offer.maxUses();
        this.allowsExperience = offer.doesGrantExperience();
        this.merchantExperienceGranted = offer.experienceGrantedToMerchant();
        this.priceGrowthMultiplier = offer.priceGrowthMultiplier();
        this.demandBonus = offer.demandBonus();
        return this;
    }

    @Override
    public SpongeTradeOfferBuilder reset() {
        this.firstItem = ItemStackSnapshot.empty();
        this.secondItem = null;
        this.sellingItem = ItemStackSnapshot.empty();
        this.useCount = Constants.Item.TradeOffer.DEFAULT_USE_COUNT;
        this.maxUses = Constants.Item.TradeOffer.DEFAULT_MAX_USES;
        this.allowsExperience = true;
        this.merchantExperienceGranted = 0;
        this.priceGrowthMultiplier = 0.0D;
        this.demandBonus = 0;
        return this;
    }

    @Override
    protected Optional<TradeOffer> buildContent(final DataView container) throws InvalidDataException {
        if (!container.contains(
            Constants.Item.TradeOffer.FIRST_QUERY,
            Constants.Item.TradeOffer.SECOND_QUERY,
            Constants.Item.TradeOffer.EXPERIENCE_QUERY,
            Constants.Item.TradeOffer.MAX_QUERY,
            Constants.Item.TradeOffer.USES_QUERY,
            Constants.Item.TradeOffer.BUYING_QUERY)) {
            return Optional.empty();
        }

        final ItemStack firstItem = container.getSerializable(Constants.Item.TradeOffer.FIRST_QUERY, ItemStack.class).get();
        final ItemStack buyingItem = container.getSerializable(Constants.Item.TradeOffer.BUYING_QUERY, ItemStack.class).get();
        final ItemStack secondItem;
        final boolean secondPresent;

        if (container.getString(Constants.Item.TradeOffer.SECOND_QUERY).isPresent() && container.getString(Constants.Item.TradeOffer.SECOND_QUERY).get().equals("none")) {
            secondPresent = false;
            secondItem = null;
        } else {
            secondPresent = true;
            secondItem = container.getSerializable(Constants.Item.TradeOffer.SECOND_QUERY, ItemStack.class).get();
        }

        final TradeOffer.Builder builder = new SpongeTradeOfferBuilder();
        builder.firstBuyingItem(firstItem);

        if (secondPresent) {
            builder.secondBuyingItem(secondItem);
        }

        builder.sellingItem(buyingItem)
                .maxUses(container.getInt(Constants.Item.TradeOffer.MAX_QUERY).get())
                .uses(container.getInt(Constants.Item.TradeOffer.USES_QUERY).get())
                .merchantExperienceGranted(container.getInt(Constants.Item.TradeOffer.EXPERIENCE_GRANTED_TO_MERCHANT_QUERY).orElse(0))
                .priceGrowthMultiplier(container.getDouble(Constants.Item.TradeOffer.PRICE_GROWTH_MULTIPLIER_QUERY).orElse(0.0D))
                .demandBonus(container.getInt(Constants.Item.TradeOffer.DEMAND_BONUS_QUERY).orElse(0));
        return Optional.of(builder.build());
    }
}
