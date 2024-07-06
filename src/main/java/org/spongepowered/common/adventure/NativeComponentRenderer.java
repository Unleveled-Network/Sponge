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
package org.spongepowered.common.adventure;

import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.adventure.NativeComponentRenderer.SiblingConsumer;

import java.text.AttributedCharacterIterator;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

/**
 * An implementation of the functionality of {@link TranslatableComponentRenderer} on native MC chat components.
 *
 * <p>This will perform in-place modification of input components -- use
 * {@link #apply(Component, Locale)} to safely handle copying when
 * necessary.</p>
 *
 * @param <C>
 */
public abstract class NativeComponentRenderer<C> {

    static final NativeComponentRenderer<Locale> INSTANCE = new NativeComponentRenderer<Locale>() {
        @Override
        public MessageFormat translate(final @NonNull String key, final @NonNull Locale locale) {
            return GlobalTranslator.get().translate(key, locale);
        }
    };

    public static @NonNull Component apply(final Component input, final Locale locale) {
        if (input instanceof AdventureTextComponent) {
            return ((AdventureTextComponent) input).rendered(locale);
        } else {
            return NativeComponentRenderer.get().render(input.copy(), locale);
        }
    }

    /**
     * Gets the default translatable component renderer.
     *
     * @return a translatable component renderer
     * @see TranslationRegistry
     */
    public static @NonNull NativeComponentRenderer<Locale> get() {
        return NativeComponentRenderer.INSTANCE;
    }

    /**
     * Gets a message format from a key and context.
     *
     * @param key a translation key
     * @param context a context
     * @return a message format or {@code null} to skip translation
     */
    protected abstract @Nullable MessageFormat translate(final @NonNull String key, final @NonNull C context);

    public Component render(@NonNull MutableComponent component, final @NonNull C context) {
        if (component instanceof TranslatableComponent) {
            component = this.renderTranslatable((TranslatableComponent) component, context);
        } else {
            this.renderSiblings(component, context);
        }


        final net.minecraft.network.chat.HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover != null)  {
            component.setStyle(component.getStyle().withHoverEvent(this.renderHoverEvent(hover, context)));
        }
        return component;
    }

    private HoverEvent renderHoverEvent(final HoverEvent input, final @NonNull C context) {
        final HoverEvent.Action<?> action = input.getAction();
        if (action == HoverEvent.Action.SHOW_TEXT) {
            final Component original = input.getValue(HoverEvent.Action.SHOW_TEXT);
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, this.render(original.copy(), context));
        } else if (action == HoverEvent.Action.SHOW_ENTITY) {
           final HoverEvent.EntityTooltipInfo data = input.getValue(HoverEvent.Action.SHOW_ENTITY);
           if (data.name != null) {
               final Component rendered = this.render(data.name.copy(), context);
               return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(data.type, data.id, rendered));
           }
        }

        return input;
    }

    protected @NonNull MutableComponent renderTranslatable(final @NonNull TranslatableComponent component, final @NonNull C context) {
        final /* @Nullable */ MessageFormat format = this.translate(component.getKey(), context);
        if (format == null) {
            // we don't have a translation for this component, but the arguments or children
            // of this component might need additional rendering
            final Object[] args = component.getArgs();
            if (args.length > 0) {
                for (int i = 0, size = args.length; i < size; i++) {
                    if (args[i] instanceof Component) {
                        args[i] = this.render(((Component) args[i]).copy(), context);
                    }
                }
            }
            this.renderSiblings(component, context);
            return component;
        }

        final Object[] args = component.getArgs();
        final TextComponent result;
        // no arguments makes this render very simple
        if(args.length == 0) {
            result = new TextComponent(format.format(null, new StringBuffer(), null).toString());
        } else {
            result = new TextComponent("");

            final Object[] nulls = new Object[args.length];
            final StringBuffer sb = format.format(nulls, new StringBuffer(), null);
            final AttributedCharacterIterator it = format.formatToCharacterIterator(nulls);

            while (it.getIndex() < it.getEndIndex()) {
                final int end = it.getRunLimit();
                final Integer index = (Integer) it.getAttribute(MessageFormat.Field.ARGUMENT);
                if (index != null && args[index] instanceof Component) {
                    result.append(this.render(((Component) args[index]).copy(), context));
                } else {
                    result.append(new TextComponent(sb.substring(it.getIndex(), end)));
                }
                it.setIndex(end);
            }
        }

        result.setStyle(component.getStyle());
        this.renderSiblings(component, context, (idx, rendered) -> result.append(rendered));

        return result;
    }

    private void renderSiblings(final Component component, final C context) {
        this.renderSiblings(component, context, component.getSiblings()::set);
    }

    private void renderSiblings(final Component component, final C context, final SiblingConsumer consumer) {
        final List<Component> siblings = component.getSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            final Component original = siblings.get(i);
            final Component rendered = this.render(original.copy(), context);
            consumer.accept(i, rendered);
        }
    }

    @FunctionalInterface
    interface SiblingConsumer {

        // Receives every sibling
        void accept(final int idx, final Component rendered);

    }

}
