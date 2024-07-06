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
package org.spongepowered.common.applaunch.config.common;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public final class CommandsCategory {

    @Setting
    @Comment("Command aliases will resolve conflicts when multiple plugins request a specific command, \n"
                     + "Correct syntax is <unqualified command>=<plugin name> e.g. \"sethome=homeplugin\"")
    public final Map<String, String> aliases = new HashMap<>();

    @Setting("enforce-permission-checks-on-non-sponge-commands")
    @Comment("Some mods may not trigger a permission check when running their command. Setting this to\n"
             + "true will enforce a check of the Sponge provided permission (\"<modid>.command.<commandname>\").\n"
             + "Note that setting this to true may cause some commands that are generally accessible to all to\n"
             + "require a permission to run.\n\n"
             + "Setting this to true will enable greater control over whether a command will appear in\n"
             + "tab completion and Sponge's help command.\n\n"
             + "If you are not using a permissions plugin, it is highly recommended that this is set to false\n"
             + "(as it is by default).")
    public boolean enforcePermissionChecksOnNonSpongeCommands = false;

    @Setting("commands-hidden")
    @Comment("Defines how Sponge should act when a user tries to access a command they do not have\n"
                     + "permission for")
    public final CommandsHiddenCategory commandsHidden = new CommandsHiddenCategory();
}
