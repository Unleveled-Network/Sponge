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
package org.spongepowered.common.world.portal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import org.spongepowered.api.event.cause.entity.MovementType;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.world.portal.PortalType;
import org.spongepowered.api.world.portal.PortalTypes;

import java.util.function.Function;

public abstract class VanillaPortalLogic implements PortalLogic {

    public static VanillaPortalLogic getEndInstance() {
        return Holder.END_INSTANCE;
    }

    final static class Holder {
        static final VanillaPortalLogic END_INSTANCE = new VanillaPortalLogic.End();
    }

    private VanillaPortalLogic() {
    }

    @Override
    public PortalInfo getPortalInfo(final Entity entity, final ServerLevel targetWorld, final Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        return defaultPortalInfo.apply(targetWorld);
    }

    @Override
    public Entity placeEntity(final Entity entity, final ServerLevel currentWorld, final ServerLevel targetWorld, final float yRot,
            final Function<Boolean, Entity> entityRepositioner) {
        return entityRepositioner.apply(true);
    }

    @Override
    public boolean isVanilla() {
        return true;
    }

    @Override
    public MovementType getMovementType() {
        return MovementTypes.PORTAL.get();
    }

    public final static class End extends VanillaPortalLogic {

        @Override
        public PortalType getPortalType() {
            return PortalTypes.END.get();
        }

    }

}
