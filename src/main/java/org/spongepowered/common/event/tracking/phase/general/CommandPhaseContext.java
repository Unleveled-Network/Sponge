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
package org.spongepowered.common.event.tracking.phase.general;

import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.effect.BroadcastInventoryChangesEffect;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.util.PrettyPrinter;

public class CommandPhaseContext extends GeneralPhaseContext<CommandPhaseContext> {

    @Nullable String command;
    @Nullable CommandMapping commandMapping;

    CommandPhaseContext(final IPhaseState<CommandPhaseContext> state, final PhaseTracker tracker) {
        super(state, tracker);
    }

    @Override
    public boolean hasCaptures() {
        return super.hasCaptures();
    }

    @Override
    protected void reset() {
        super.reset();
        this.command = null;
        this.commandMapping = null;
    }

    public CommandPhaseContext command(final String command) {
        this.command = command;
        return this;
    }

    public CommandPhaseContext commandMapping(final CommandMapping mapping) {
        this.commandMapping = mapping;
        return this;
    }

    @Override
    public PrettyPrinter printCustom(final PrettyPrinter printer, final int indent) {
        final String s = String.format("%1$" + indent + "s", "");
        super.printCustom(printer, indent)
            .add(s + "- %s: %s", "Command", this.command == null ? "empty command" : this.command)
            .add(s + "- %s: %s", "Command Mapping", this.commandMapping == null ? "no mapping" : this.commandMapping.toString());
        return printer;
    }

    @Override
    public void close() {
        // Make sure to broadcast any changes to capture any inventory transactions for events.
        if (this.getSource() instanceof ServerPlayer) {
            final TransactionalCaptureSupplier transactor = this.getTransactor();
            transactor.logPlayerInventoryChange((ServerPlayer) this.getSource(), PlayerInventoryTransaction.EventCreator.STANDARD);
            try (EffectTransactor ignored = BroadcastInventoryChangesEffect.transact(transactor)) {
                ((ServerPlayer) this.getSource()).containerMenu.broadcastChanges();
            }
        }

        super.close();
    }

    // Maybe we could provide the command?
}
