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
package org.spongepowered.common.event.filter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.spongepowered.common.event.gen.DefineableClassLoader;
import org.spongepowered.common.event.manager.ListenerClassVisitor;

import java.util.concurrent.atomic.AtomicInteger;

public class FilterFactory {

    private final AtomicInteger id = new AtomicInteger();
    private final DefineableClassLoader classLoader;
    private final LoadingCache<ListenerClassVisitor.DiscoveredMethod, Class<? extends EventFilter>> cache = Caffeine.newBuilder()
            .weakValues().build(this::createClass);
    private final String targetPackage;

    public FilterFactory(final String targetPackage, final DefineableClassLoader classLoader) {
        checkNotNull(targetPackage, "targetPackage");
        checkArgument(!targetPackage.isEmpty(), "targetPackage cannot be empty");
        this.targetPackage = targetPackage + '.';
        this.classLoader = checkNotNull(classLoader, "classLoader");
    }

    public Class<? extends EventFilter> createFilter(final ListenerClassVisitor.DiscoveredMethod method) throws Exception {
        final Class<? extends EventFilter> clazz = this.cache.get(method);
        return clazz == EventFilter.class ? null : clazz;
    }

    Class<? extends EventFilter> createClass(final ListenerClassVisitor.DiscoveredMethod method) throws
        ClassNotFoundException {
        final Class<?> handle = method.declaringClass();
        final Class<?> eventClass = method.parameterTypes()[0].clazz();
        final String name = this.targetPackage + eventClass.getSimpleName() + "Filter_" + handle.getSimpleName() + '_'
                + method.methodName() + this.id.incrementAndGet();
        final byte[] cls = FilterGenerator.getInstance().generateClass(name, method);
        if (cls == null) {
            return EventFilter.class; // cache does not permit nulls
        }
        return this.classLoader.defineClass(name, cls);
    }

}
