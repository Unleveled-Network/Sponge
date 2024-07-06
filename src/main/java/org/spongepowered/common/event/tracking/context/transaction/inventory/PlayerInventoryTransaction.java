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
package org.spongepowered.common.event.tracking.context.transaction.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;

import java.util.List;
import java.util.Optional;

public class PlayerInventoryTransaction extends InventoryBasedTransaction {

    private final ServerPlayer player;
    private final EventCreator eventCreator;

    public PlayerInventoryTransaction(final Player player, final EventCreator eventCreator) {
        super((Inventory) player.inventory);
        this.player = (ServerPlayer) player;
        this.eventCreator = eventCreator;
    }

    @Override
    Optional<ChangeInventoryEvent> createInventoryEvent(final List<SlotTransaction> slotTransactions,
            final List<Entity> entities, final PhaseContext<@NonNull ?> context,
            final Cause cause) {
        if (!entities.isEmpty()) {
            SpongeCommon.logger().warn("Entities are being captured but not being processed");
        }
        if (slotTransactions.isEmpty()) {
            return Optional.empty();
        }
        final ChangeInventoryEvent event = this.eventCreator.create(cause, this.inventory, slotTransactions);
        return Optional.of(event);
    }

    @Override
    public void restore(final PhaseContext<@NonNull ?> context, final ChangeInventoryEvent event) {
        PacketPhaseUtil.handleSlotRestore(this.player, null, event.transactions(), event.isCancelled());
    }

    @Override
    public void postProcessEvent(final PhaseContext<@NonNull ?> context, final ChangeInventoryEvent event) {
        PacketPhaseUtil.handleSlotRestore(this.player, null, event.transactions(), event.isCancelled());
    }

    @Override
    public Optional<AbsorbingFlowStep> parentAbsorber() {
        return Optional.of((ctx, tx) -> tx.absorbPlayerInventoryChange(ctx, this));
    }

    @Override
    public boolean absorbPlayerInventoryChange(
        final PhaseContext<@NonNull ?> context, final PlayerInventoryTransaction playerInventoryTransaction
    ) {
        if (playerInventoryTransaction.player == this.player) {
            return this.eventCreator == playerInventoryTransaction.eventCreator;
        }
        return false;
    }

    @SuppressWarnings("NullableProblems")
    public interface EventCreator {
        EventCreator STANDARD = SpongeEventFactory::createChangeInventoryEvent;
        EventCreator PICKUP = SpongeEventFactory::createChangeInventoryEventPickup;
        EventCreator SWAP_HAND = SpongeEventFactory::createChangeInventoryEventSwapHand;
        ChangeInventoryEvent create(final Cause cause, final Inventory inventory, final List<SlotTransaction> slotTransactions);
    }

}
