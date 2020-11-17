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
package org.spongepowered.test.projectile;

import com.google.inject.Inject;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.carrier.Dispenser;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;
import org.spongepowered.test.LoadableModule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

@Plugin("projectiletest")
public class ProjectileTest implements LoadableModule {

    @Inject private PluginContainer plugin;
    private ProjectileTestListener listeners;

    @Override
    public void enable(CommandContext ctx) {
        this.listeners = new ProjectileTestListener();
        Sponge.getEventManager().registerListeners(this.plugin, this.listeners);
    }

    @Listener
    public void registerCommand(final RegisterCommandEvent<Command.Parameterized> event) {
        @SuppressWarnings("rawtypes") final Parameter.Value<EntityType> entityTypeParameter =
                Parameter.catalogedElement(EntityType.class)
                        .setKey("type")
                        .build();
        final Parameter.Value<Boolean> targetParameter = Parameter.bool().setKey("target").orDefault(false).build();
        final Command.Parameterized launchCommand = Command.builder()
                .parameters(entityTypeParameter, targetParameter)
                .setExecutor(context -> {
                    final Player player = context.getCause().first(Player.class)
                            .orElseThrow(() -> new CommandException(Component.text("Only a player can execute this command")));
                    final EntityType<?> entityType = context.requireOne(entityTypeParameter);
                    final Optional<Projectile> launched;
                    if (context.requireOne(targetParameter)) {
                        final Collection<? extends Entity> nearbyEntities = player.getNearbyEntities(10,
                                entity -> entity instanceof Living && entity != player);
                        if (nearbyEntities.isEmpty()) {
                            return CommandResult.error(Component.text("No entity to target nearby"));
                        }
                        final Entity target = nearbyEntities.iterator().next();
                        launched = player.launchProjectileTo((EntityType<Projectile>) entityType, target);
                        if (launched.isPresent()) {
                            player.sendMessage(Identity.nil(), Component.text("Launched projectile to " + target.getType().key().asString()));
                            return CommandResult.success();
                        }
                    } else {
                        launched = player.launchProjectile((EntityType<Projectile>) entityType);
                        if (launched.isPresent()) {
                            player.sendMessage(Identity.nil(), Component.text("Launched projectile"));
                            return CommandResult.success();
                        }
                    }

                    throw new CommandException(Component.text("Could not launch projectile"));
                })
                .build();
        event.register(this.plugin, launchCommand, "launch");

        final Command.Parameterized launchToMeCommand = Command.builder()
                .parameter(entityTypeParameter)
                .setExecutor(context -> {
                    final Player player = context.getCause().first(Player.class)
                            .orElseThrow(() -> new CommandException(Component.text("Only a player can execute this command")));
                    final Collection<? extends ProjectileSource> nearbyProjectileSources =
                            (Collection<? extends ProjectileSource>) player.getNearbyEntities(10, entity -> entity instanceof ProjectileSource);
                    if (nearbyProjectileSources.isEmpty()) {
                        return CommandResult.error(Component.text("No projectile source nearby"));
                    }

                    final ProjectileSource projectileSource = nearbyProjectileSources.iterator().next();
                    final EntityType<?> entityType = context.requireOne(entityTypeParameter);
                    final Optional<? extends Projectile> launched = projectileSource.launchProjectileTo((EntityType<Projectile>) entityType, player);
                    if (launched.isPresent()) {
                        player.sendMessage(Identity.nil(), Component.text()
                                .append(Component.text("You made a ")).append(Component.text(((Entity) projectileSource).getType().key().asString()))
                                .append(Component.text(" shoot a ")).append(Component.text(launched.get().getType().key().asString()))
                                .append(Component.text(" at you")).build()
                        );
                        return CommandResult.success();
                    }

                    throw new CommandException(Component.text()
                            .append(Component.text("Could not launch a ")).append(Component.text(entityType.key().asString()))
                            .append(Component.text(" from a ")).append(Component.text(((Entity) projectileSource).getType().key().asString()))
                            .append(Component.text(" at you")).build());
                })
                .build();
        event.register(this.plugin, launchToMeCommand, "launchtome");

        final Parameter.Value<ServerLocation> dispenserParameter = Parameter.location()
                .setKey("dispenser")
                .build();
        final Command.Parameterized triggerDispenserCommand = Command.builder()
                .parameters(dispenserParameter, entityTypeParameter)
                .setExecutor(context -> {
                    final Player player = context.getCause().first(Player.class)
                            .orElseThrow(() -> new CommandException(Component.text("Only a player can execute this command")));
                    final BlockEntity dispenser = context.requireOne(dispenserParameter).getBlockEntity().orElse(null);
                    if (dispenser == null) {
                        return CommandResult.error(Component.text("Could not find dispenser"));
                    }
                    final EntityType<?> entityType = context.requireOne(entityTypeParameter);
                    final Optional<? extends Projectile> launched = ((Dispenser) dispenser).launchProjectile((EntityType<Projectile>) entityType);
                    if (launched.isPresent()) {
                        launched.get().offer(Keys.SHOOTER, player);
                        player.sendMessage(Identity.nil(), Component.text()
                                .append(Component.text("The dispenser launched a ")).append(Component.text(launched.get().getType().key().asString()))
                                .build()
                        );
                        return CommandResult.success();
                    }

                    return CommandResult.error(Component.text()
                            .append(Component.text("Could not make the dispenser launch a ")).append(Component.text(entityType.key().asString()))
                            .build());
                })
                .build();
        event.register(this.plugin, triggerDispenserCommand, "triggerdispenser");
    }


    public static class ProjectileTestListener {

        private Queue<EntityType<? extends Projectile>> projectileTypes = new LinkedList<>();

        public ProjectileTestListener() {
            this.projectileTypes.add(EntityTypes.SPECTRAL_ARROW.get());
            this.projectileTypes.add(EntityTypes.ARROW.get());
            this.projectileTypes.add(EntityTypes.EGG.get());
            this.projectileTypes.add(EntityTypes.SMALL_FIREBALL.get());
            this.projectileTypes.add(EntityTypes.FIREWORK_ROCKET.get());
            this.projectileTypes.add(EntityTypes.SNOWBALL.get());
            this.projectileTypes.add(EntityTypes.EXPERIENCE_BOTTLE.get());
            this.projectileTypes.add(EntityTypes.ENDER_PEARL.get());
            this.projectileTypes.add(EntityTypes.FIREBALL.get());
            this.projectileTypes.add(EntityTypes.WITHER_SKULL.get());
            this.projectileTypes.add(EntityTypes.EYE_OF_ENDER.get());
            //             this.projectileTypes.add(EntityTypes.FISHING_BOBBER.get());
            this.projectileTypes.add(EntityTypes.POTION.get());
            this.projectileTypes.add(EntityTypes.LLAMA_SPIT.get());
            this.projectileTypes.add(EntityTypes.DRAGON_FIREBALL.get());
            this.projectileTypes.add(EntityTypes.SHULKER_BULLET.get());
        }

        @Listener
        public void onClickBlock(InteractBlockEvent.Secondary event, @First ServerPlayer player) {
            if (event.getInteractionPoint().isPresent()) {
                final Vector3d interactionPoint = event.getInteractionPoint().get();
                final ServerWorld world = player.getWorld();
                final EntityType<? extends Projectile> nextType = this.projectileTypes.poll();
                this.projectileTypes.offer(nextType);
                final Optional<? extends BlockEntity> blockEntity = world.getBlockEntity(interactionPoint.toInt());
                if (blockEntity.isPresent() && blockEntity.get() instanceof Dispenser) {
                    ((Dispenser) blockEntity.get()).launchProjectile(nextType);
                } else {
                    player.launchProjectile(nextType);
                }
                event.setCancelled(true);
                player.sendMessage(Identity.nil(), Component.text(nextType.key().toString()));
            }

        }

    }

}