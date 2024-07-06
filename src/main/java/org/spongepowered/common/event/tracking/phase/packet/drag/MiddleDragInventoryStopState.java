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
package org.spongepowered.common.event.tracking.phase.packet.drag;

import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.phase.packet.inventory.InventoryPacketContext;
import org.spongepowered.common.util.Constants;

import java.util.List;
import java.util.Optional;

public final class MiddleDragInventoryStopState extends DragInventoryStopState {

    public MiddleDragInventoryStopState() {
        super("MIDDLE_DRAG_INVENTORY_STOP", Constants.Networking.DRAG_MODE_MIDDLE_BUTTON);
    }

    @Override
    public ClickContainerEvent createContainerEvent(
        final InventoryPacketContext context, final Cause cause, final ServerPlayer serverPlayer,
        final Container openContainer, final Transaction<ItemStackSnapshot> transaction,
        final List<SlotTransaction> slotTransactions, final List<Entity> capturedEntities, final int usedButton,
        @Nullable final Slot slot
    ) {
        if (!capturedEntities.isEmpty()) {
            SpongeCommon.logger().warn("Entities are being captured but not being processed");
        }
        return SpongeEventFactory.createClickContainerEventDragMiddle(
            cause,
            openContainer, transaction,
            Optional.ofNullable(slot), slotTransactions);
    }

}
