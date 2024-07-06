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
package org.spongepowered.common.util.metric;

import com.google.inject.Singleton;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.metric.MetricsConfigManager;
import org.spongepowered.common.applaunch.config.core.ConfigHandle;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.config.metrics.MetricsConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.plugin.PluginContainer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public final class SpongeMetricsConfigManager implements MetricsConfigManager {

    private final ConfigHandle<MetricsConfig> metrics;

    public SpongeMetricsConfigManager() {
        // This needs to be loaded after the global-inheritable configuration
        // is loaded. That load is performed by the sponge non-inheritable
        // configuration which is loaded way earlier in the lifecycle since it
        // is used to configure mixin plugins.
        this.metrics = SpongeConfigs.create(MetricsConfig.class, MetricsConfig::transformation, MetricsConfig.FILE_NAME);
    }

    public CompletableFuture<CommentedConfigurationNode> savePluginsInConfig(final Map<String, Tristate> entries) {
        return this.metrics.updateSetting("plugin-states", entries, new TypeToken<Map<String, Tristate>>() {});
    }

    @Override
    public Tristate globalCollectionState() {
        return this.metrics.get().globalState;
    }

    @Override
    public Tristate collectionState(final PluginContainer container) {
        return this.metrics.get().getCollectionState(container);
    }
}
