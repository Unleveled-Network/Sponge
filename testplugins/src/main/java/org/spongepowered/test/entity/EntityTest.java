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
package org.spongepowered.test.entity;

import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.HashSet;
import java.util.Set;

@Plugin("entitytest")
public class EntityTest {

    private final PluginContainer plugin;
    private final Set<EntityType<@NonNull ?>> blockedSpawn = new HashSet<>();

    @Inject
    public EntityTest(final PluginContainer plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onRegisterCommand(final RegisterCommandEvent<Parameterized> event) {
        final Parameter.Value<EntityType<@NonNull ?>> entityTypeParam =
                Parameter.registryElement(new TypeToken<EntityType<@NonNull ?>>() {}, RegistryTypes.ENTITY_TYPE, "minecraft").key("entityType").build();
        event.register(this.plugin, Command.builder()
                .addChild(Command.builder()
                        .addParameter(entityTypeParam)
                        .executor(ctx -> {
                            EntityType<@NonNull ?> type = ctx.requireOne(entityTypeParam);
                            blockedSpawn.remove(type);
                            ctx.sendMessage(Identity.nil(), Component.text("Entity type ").append(type).append(Component.text(" spawn is no longer blocked.")));
                            return CommandResult.success();
                        }).build(), "allow")
                .addChild(Command.builder()
                        .addParameter(entityTypeParam)
                        .executor(ctx -> {
                            EntityType<@NonNull ?> type = ctx.requireOne(entityTypeParam);
                            blockedSpawn.add(type);
                            ctx.sendMessage(Identity.nil(), Component.text("Entity type ").append(type).append(Component.text(" spawn is now blocked.")));
                            return CommandResult.success();
                        }).build(), "deny")
                .addChild(Command.builder()
                        .executor(ctx -> {
                            ctx.sendMessage(Identity.nil(), Component.join(JoinConfiguration.builder()
                                    .prefix(Component.text("Blocked entity types: ["))
                                    .separator(Component.text(", "))
                                    .suffix(Component.text("]."))
                                    .build(), this.blockedSpawn));
                            return CommandResult.success();
                        }).build(), "list")
                .build(), "spawnFilter");
    }

    @Listener
    public void onSpawnEntity(final SpawnEntityEvent event) {
        event.filterEntities(entity -> !this.blockedSpawn.contains(entity.type()));
    }
}
