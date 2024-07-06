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
package org.spongepowered.common.mixin.core.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.common.bridge.world.entity.GrieferBridge;

import java.util.function.Predicate;

@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalMixin extends Goal {

    // @formatter:off
    @Shadow @Final private Mob mob;
    // @formatter:on

    /**
     * @author gabizou - April 13th, 2018
     * @reason - Due to Forge's changes, there's no clear redirect or injection
     * point where Sponge can add the griefer checks. The original redirect aimed
     * at the gamerule check, but this can suffice for now.
     */
    @Redirect(
        method = "tick()V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z",
            remap = false
        )
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean impl$onTallGrassApplyForGriefing(final Predicate predicate, final Object object) {
        return ((GrieferBridge) this.mob).bridge$canGrief() && predicate.test(object);
    }

    /**
     * @author gabizou - April 13th, 2018
     * @reason - Due to Forge's changes, there's no clear redirect or injection
     * point where Sponge can add the griefer checks. The original redirect aimed
     * at the gamerule check, but this can suffice for now.
     */
    @Redirect(
        method = "tick()V",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"
            ),
            to = @At(
                value = "FIELD",
                target = "Lnet/minecraft/world/level/block/Blocks;GRASS_BLOCK:Lnet/minecraft/world/level/block/Block;",
                opcode = Opcodes.GETSTATIC
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private BlockState impl$onSpongeGetBlockForGriefing(Level world, BlockPos pos) {
        return ((GrieferBridge) this.mob).bridge$canGrief() ? world.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }
}
