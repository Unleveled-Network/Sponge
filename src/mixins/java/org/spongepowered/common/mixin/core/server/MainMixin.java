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
package org.spongepowered.common.mixin.core.server;

import net.minecraft.server.Main;
import net.minecraft.server.ServerPropertiesProvider;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.storage.FolderName;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeBootstrap;
import org.spongepowered.common.SpongeLifecycle;
import org.spongepowered.common.server.BootstrapProperties;

import java.nio.file.Path;

@Mixin(Main.class)
public abstract class MainMixin {

    @Redirect(method = "main", at = @At(value = "NEW", target = "net/minecraft/server/ServerPropertiesProvider"))
    private static ServerPropertiesProvider impl$cacheBootstrapProperties(final DynamicRegistries p_i242100_1_, final Path p_i242100_2_) {
        final ServerPropertiesProvider provider = new ServerPropertiesProvider(p_i242100_1_, p_i242100_2_);
        BootstrapProperties.init(provider.getProperties(), p_i242100_1_);
        return provider;
    }

    @Redirect(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/SaveFormat$LevelSave;getLevelPath(Lnet/minecraft/world/storage/FolderName;)Ljava/nio/file/Path;"))
    private static Path impl$configurePackRepository(final SaveFormat.LevelSave levelSave, final FolderName folderName) {
        final Path datapackDir = levelSave.getLevelPath(folderName);
        final SpongeLifecycle lifecycle = SpongeBootstrap.getLifecycle();
        lifecycle.establishGlobalRegistries();
        lifecycle.establishDataProviders();
        lifecycle.callRegisterDataEvent();
        lifecycle.callRegisterDataPackValueEvent(datapackDir);
        return datapackDir;
    }
}
