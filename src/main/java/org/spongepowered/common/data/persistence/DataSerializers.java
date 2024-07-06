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
package org.spongepowered.common.data.persistence;

import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.util.Direction;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.util.Constants;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.math.imaginary.Complexd;
import org.spongepowered.math.imaginary.Complexf;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.imaginary.Quaternionf;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector2f;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vector2l;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.math.vector.Vector3l;
import org.spongepowered.math.vector.Vector4d;
import org.spongepowered.math.vector.Vector4f;
import org.spongepowered.math.vector.Vector4i;
import org.spongepowered.math.vector.Vector4l;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.UUID;
import java.util.function.Supplier;

public final class DataSerializers {

    public static final DataTranslator<Component> COMPONENT_DATA_SERIALIZER;
    public static final DataTranslator<UUID> UUID_DATA_SERIALIZER;
    public static final DataTranslator<Vector2d> VECTOR_2_D_DATA_SERIALIZER;
    public static final DataTranslator<Vector2f> VECTOR_2_F_DATA_SERIALIZER;
    public static final DataTranslator<Vector2i> VECTOR_2_I_DATA_SERIALIZER;
    public static final DataTranslator<Vector2l> VECTOR_2_L_DATA_SERIALIZER;
    public static final DataTranslator<Vector3d> VECTOR_3_D_DATA_SERIALIZER;
    public static final DataTranslator<Vector3f> VECTOR_3_F_DATA_SERIALIZER;
    public static final DataTranslator<Vector3i> VECTOR_3_I_DATA_SERIALIZER;
    public static final DataTranslator<Vector3l> VECTOR_3_L_DATA_SERIALIZER;
    public static final DataTranslator<Vector4d> VECTOR_4_D_DATA_SERIALIZER;
    public static final DataTranslator<Vector4f> VECTOR_4_F_DATA_SERIALIZER;
    public static final DataTranslator<Vector4i> VECTOR_4_I_DATA_SERIALIZER;
    public static final DataTranslator<Vector4l> VECTOR_4_L_DATA_SERIALIZER;
    public static final DataTranslator<Complexd> COMPLEXD_DATA_SERIALIZER;
    public static final DataTranslator<Complexf> COMPLEXF_DATA_SERIALIZER;
    public static final DataTranslator<Quaterniond> QUATERNIOND_DATA_SERIALIZER;
    public static final DataTranslator<Quaternionf> QUATERNIONF_DATA_SERIALIZER;
    public static final DataTranslator<LocalTime> LOCAL_TIME_DATA_SERIALIZER;
    public static final DataTranslator<LocalDate> LOCAL_DATE_DATA_SERIALIZER;
    public static final DataTranslator<LocalDateTime> LOCAL_DATE_TIME_DATA_SERIALIZER;
    public static final DataTranslator<Instant> INSTANT_DATA_SERIALIZER;
    public static final DataTranslator<ZonedDateTime> ZONED_DATE_TIME_DATA_SERIALIZER;
    public static final DataTranslator<Month> MONTH_DATA_SERIALIZER;
    public static final DataTranslator<Direction> DIRECTION_SERIALIZER;


    static {
        COMPONENT_DATA_SERIALIZER = new DataTranslator<Component>() {
            final TypeToken<Component> token = TypeToken.get(Component.class);

            @Override
            public TypeToken<Component> token() {
                return this.token;
            }

            // Translate via node

            @Override
            public Component translate(final DataView view) throws InvalidDataException {
                final ConfigurationNode node = ConfigurateTranslator.instance().translate(view);
                try {
                    return node.get(this.token);
                } catch (final SerializationException e) {
                    throw new InvalidDataException(e);
                }
            }

            @Override
            public DataContainer translate(final Component obj) throws InvalidDataException {
                final ConfigurationNode node = SpongeAdventure.CONFIGURATE.serialize(obj);
                return ConfigurateTranslator.instance().translate(node);
            }
        };
        UUID_DATA_SERIALIZER = new DataTranslator<UUID>() {

            final TypeToken<UUID> token = TypeToken.get(UUID.class);

            @Override
            public TypeToken<UUID> token() {
                return this.token;
            }

            @Override
            public UUID translate(DataView view) throws InvalidDataException {
                // fix for API7 notifier/creator uuids
                if (view.contains(Constants.UUID_LEAST_QUERY) && view.contains(Constants.UUID_MOST_QUERY)) {
                    final long most = view.getLong(Constants.UUID_MOST_QUERY).get();
                    final long least = view.getLong(Constants.UUID_LEAST_QUERY).get();
                    return new UUID(most, least);
                }

                final long most = view.getLong(Queries.UUID_MOST).orElseThrow(DataSerializers.invalidDataQuery(Queries.UUID_MOST));
                final long least = view.getLong(Queries.UUID_LEAST).orElseThrow(DataSerializers.invalidDataQuery(Queries.UUID_LEAST));
                return new UUID(most, least);
            }

            @Override
            public DataContainer translate(UUID obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Queries.UUID_MOST, obj.getMostSignificantBits())
                        .set(Queries.UUID_LEAST, obj.getLeastSignificantBits());
            }

            @Override
            public DataView addTo(UUID obj, DataView dataView) {
                return dataView
                    .set(Queries.UUID_LEAST, obj.getLeastSignificantBits())
                    .set(Queries.UUID_MOST, obj.getMostSignificantBits());
            }
        };
        VECTOR_2_D_DATA_SERIALIZER = new DataTranslator<Vector2d>() {
            final TypeToken<Vector2d> token = TypeToken.get(Vector2d.class);

            @Override
            public TypeToken<Vector2d> token() {
                return this.token;
            }

            @Override
            public Vector2d translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                return new Vector2d(x, y);
            }

            @Override
            public DataContainer translate(Vector2d obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y());
            }

            @Override
            public DataView addTo(Vector2d obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y());
            }
        };
        VECTOR_2_F_DATA_SERIALIZER = new DataTranslator<Vector2f>() {
            final TypeToken<Vector2f> token = TypeToken.get(Vector2f.class);

            @Override
            public TypeToken<Vector2f> token() {
                return this.token;
            }

            @Override
            public Vector2f translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                return new Vector2f(x, y);
            }

            @Override
            public DataContainer translate(Vector2f obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y());
            }

            @Override
            public DataView addTo(Vector2f obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y());
            }
        };
        VECTOR_2_I_DATA_SERIALIZER = new DataTranslator<Vector2i>() {
            final TypeToken<Vector2i> token = TypeToken.get(Vector2i.class);

            @Override
            public TypeToken<Vector2i> token() {
                return this.token;
            }

            @Override
            public Vector2i translate(DataView view) throws InvalidDataException {
                final int x = view.getInt(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final int y = view.getInt(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                return new Vector2i(x, y);
            }

            @Override
            public DataContainer translate(Vector2i obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y());
            }

            @Override
            public DataView addTo(Vector2i obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    ;
            }
        };
        VECTOR_2_L_DATA_SERIALIZER = new DataTranslator<Vector2l>() {
            final TypeToken<Vector2l> token = TypeToken.get(Vector2l.class);

            @Override
            public TypeToken<Vector2l> token() {
                return this.token;
            }

            @Override
            public Vector2l translate(DataView view) throws InvalidDataException {
                final long x = view.getLong(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final long y = view.getLong(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                return new Vector2l(x, y);
            }

            @Override
            public DataContainer translate(Vector2l obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y());
            }

            @Override
            public DataView addTo(Vector2l obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y());
            }
        };
        VECTOR_3_D_DATA_SERIALIZER = new DataTranslator<Vector3d>() {
            final TypeToken<Vector3d> token = TypeToken.get(Vector3d.class);

            @Override
            public TypeToken<Vector3d> token() {
                return this.token;
            }

            @Override
            public Vector3d translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final double z = view.getDouble(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                return new Vector3d(x, y, z);
            }

            @Override
            public DataContainer translate(Vector3d obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z());
            }

            @Override
            public DataView addTo(Vector3d obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    ;
            }
        };
        VECTOR_3_F_DATA_SERIALIZER = new DataTranslator<Vector3f>() {
            final TypeToken<Vector3f> token = TypeToken.get(Vector3f.class);

            @Override
            public TypeToken<Vector3f> token() {
                return this.token;
            }

            @Override
            public Vector3f translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final double z = view.getDouble(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                return new Vector3f(x, y, z);
            }

            @Override
            public DataContainer translate(Vector3f obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z());
            }

            @Override
            public DataView addTo(Vector3f obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    ;
            }
        };
        VECTOR_3_I_DATA_SERIALIZER = new DataTranslator<Vector3i>() {
            final TypeToken<Vector3i> token = TypeToken.get(Vector3i.class);

            @Override
            public TypeToken<Vector3i> token() {
                return this.token;
            }

            @Override
            public Vector3i translate(DataView view) throws InvalidDataException {
                final int x = view.getInt(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final int y = view.getInt(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final int z = view.getInt(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                return new Vector3i(x, y, z);
            }

            @Override
            public DataContainer translate(Vector3i obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z());
            }

            @Override
            public DataView addTo(Vector3i obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    ;
            }
        };
        VECTOR_3_L_DATA_SERIALIZER = new DataTranslator<Vector3l>() {
            final TypeToken<Vector3l> token = TypeToken.get(Vector3l.class);

            @Override
            public TypeToken<Vector3l> token() {
                return this.token;
            }

            @Override
            public Vector3l translate(DataView view) throws InvalidDataException {
                final long x = view.getLong(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final long y = view.getLong(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final long z = view.getLong(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                return new Vector3l(x, y, z);
            }

            @Override
            public DataContainer translate(Vector3l obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z());
            }

            @Override
            public DataView addTo(Vector3l obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    ;
            }
        };
        VECTOR_4_F_DATA_SERIALIZER = new DataTranslator<Vector4f>() {
            final TypeToken<Vector4f> token = TypeToken.get(Vector4f.class);

            @Override
            public TypeToken<Vector4f> token() {
                return this.token;
            }

            @Override
            public Vector4f translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final double z = view.getDouble(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                final double w = view.getDouble(Constants.DataSerializers.W_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.W_POS));
                return new Vector4f(x, y, z, w);
            }

            @Override
            public DataContainer translate(Vector4f obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z())
                        .set(Constants.DataSerializers.W_POS, obj.w());
            }

            @Override
            public DataView addTo(Vector4f obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    .set(Constants.DataSerializers.W_POS, obj.w())
                    ;
            }
        };
        VECTOR_4_I_DATA_SERIALIZER = new DataTranslator<Vector4i>() {
            final TypeToken<Vector4i> token = TypeToken.get(Vector4i.class);

            @Override
            public TypeToken<Vector4i> token() {
                return this.token;
            }

            @Override
            public Vector4i translate(DataView view) throws InvalidDataException {
                final int x = view.getInt(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final int y = view.getInt(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final int z = view.getInt(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                final int w = view.getInt(Constants.DataSerializers.W_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.W_POS));
                return new Vector4i(x, y, z, w);
            }

            @Override
            public DataContainer translate(Vector4i obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z())
                        .set(Constants.DataSerializers.W_POS, obj.w());
            }

            @Override
            public DataView addTo(Vector4i obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    .set(Constants.DataSerializers.W_POS, obj.w())
                    ;
            }
        };
        VECTOR_4_L_DATA_SERIALIZER = new DataTranslator<Vector4l>() {
            final TypeToken<Vector4l> token = TypeToken.get(Vector4l.class);

            @Override
            public TypeToken<Vector4l> token() {
                return this.token;
            }

            @Override
            public Vector4l translate(DataView view) throws InvalidDataException {
                final long x = view.getLong(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final long y = view.getLong(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final long z = view.getLong(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                final long w = view.getLong(Constants.DataSerializers.W_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.W_POS));
                return new Vector4l(x, y, z, w);
            }

            @Override
            public DataContainer translate(Vector4l obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z())
                        .set(Constants.DataSerializers.W_POS, obj.w());
            }

            @Override
            public DataView addTo(Vector4l obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    .set(Constants.DataSerializers.W_POS, obj.w())
                    ;
            }
        };
        VECTOR_4_D_DATA_SERIALIZER = new DataTranslator<Vector4d>() {
            final TypeToken<Vector4d> token = TypeToken.get(Vector4d.class);

            @Override
            public TypeToken<Vector4d> token() {
                return this.token;
            }

            @Override
            public Vector4d translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final double z = view.getDouble(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                final double w = view.getDouble(Constants.DataSerializers.W_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.W_POS));
                return new Vector4d(x, y, z, w);
            }

            @Override
            public DataContainer translate(Vector4d obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z())
                        .set(Constants.DataSerializers.W_POS, obj.w());
            }

            @Override
            public DataView addTo(Vector4d obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    .set(Constants.DataSerializers.W_POS, obj.w())
                    ;
            }
        };
        COMPLEXD_DATA_SERIALIZER = new DataTranslator<Complexd>() {
            final TypeToken<Complexd> token = TypeToken.get(Complexd.class);

            @Override
            public TypeToken<Complexd> token() {
                return this.token;
            }

            @Override
            public Complexd translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                return new Complexd(x, y);
            }

            @Override
            public DataContainer translate(Complexd obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y());
            }

            @Override
            public DataView addTo(Complexd obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    ;
            }
        };
        COMPLEXF_DATA_SERIALIZER = new DataTranslator<Complexf>() {
            final TypeToken<Complexf> token = TypeToken.get(Complexf.class);

            @Override
            public TypeToken<Complexf> token() {
                return this.token;
            }

            @Override
            public Complexf translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                return new Complexf(x, y);
            }

            @Override
            public DataContainer translate(Complexf obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y());
            }

            @Override
            public DataView addTo(Complexf obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y());
            }
        };
        QUATERNIOND_DATA_SERIALIZER = new DataTranslator<Quaterniond>() {
            final TypeToken<Quaterniond> token = TypeToken.get(Quaterniond.class);

            @Override
            public TypeToken<Quaterniond> token() {
                return this.token;
            }

            @Override
            public Quaterniond translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final double z = view.getDouble(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                final double w = view.getDouble(Constants.DataSerializers.W_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.W_POS));
                return new Quaterniond(x, y, z, w);
            }

            @Override
            public DataContainer translate(Quaterniond obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z())
                        .set(Constants.DataSerializers.W_POS, obj.w());
            }

            @Override
            public DataView addTo(Quaterniond obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    .set(Constants.DataSerializers.W_POS, obj.w());
            }
        };
        QUATERNIONF_DATA_SERIALIZER = new DataTranslator<Quaternionf>() {
            final TypeToken<Quaternionf> token = TypeToken.get(Quaternionf.class);

            @Override
            public TypeToken<Quaternionf> token() {
                return this.token;
            }

            @Override
            public Quaternionf translate(DataView view) throws InvalidDataException {
                final double x = view.getDouble(Constants.DataSerializers.X_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.X_POS));
                final double y = view.getDouble(Constants.DataSerializers.Y_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Y_POS));
                final double z = view.getDouble(Constants.DataSerializers.Z_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.Z_POS));
                final double w = view.getDouble(Constants.DataSerializers.W_POS).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.W_POS));
                return new Quaternionf(x, y, z, w);
            }

            @Override
            public DataContainer translate(Quaternionf obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.X_POS, obj.x())
                        .set(Constants.DataSerializers.Y_POS, obj.y())
                        .set(Constants.DataSerializers.Z_POS, obj.z())
                        .set(Constants.DataSerializers.W_POS, obj.w());
            }

            @Override
            public DataView addTo(Quaternionf obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.X_POS, obj.x())
                    .set(Constants.DataSerializers.Y_POS, obj.y())
                    .set(Constants.DataSerializers.Z_POS, obj.z())
                    .set(Constants.DataSerializers.W_POS, obj.w());
            }
        };
        LOCAL_TIME_DATA_SERIALIZER = new DataTranslator<LocalTime>() {
            final TypeToken<LocalTime> token = TypeToken.get(LocalTime.class);

            @Override
            public TypeToken<LocalTime> token() {
                return this.token;
            }

            @Override
            public LocalTime translate(DataView view) throws InvalidDataException {
                final int hour = view.getInt(Constants.DataSerializers.LOCAL_TIME_HOUR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_HOUR));
                final int minute = view.getInt(Constants.DataSerializers.LOCAL_TIME_MINUTE).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_MINUTE));
                final int second = view.getInt(Constants.DataSerializers.LOCAL_TIME_SECOND).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_SECOND));
                final int nano = view.getInt(Constants.DataSerializers.LOCAL_TIME_NANO).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_NANO));
                if (!ChronoField.HOUR_OF_DAY.range().isValidValue(hour)) {
                    throw new InvalidDataException("Invalid hour of day: " + hour);
                }
                if (!ChronoField.MINUTE_OF_HOUR.range().isValidValue(minute)) {
                    throw new InvalidDataException("Invalid minute of hour: " + minute);
                }
                if (!ChronoField.SECOND_OF_MINUTE.range().isValidValue(second)) {
                    throw new InvalidDataException("Invalid second of minute: " + second);
                }
                if (!ChronoField.NANO_OF_SECOND.range().isValidValue(nano)) {
                    throw new InvalidDataException("Invalid nanosecond of second: " + nano);
                }
                return LocalTime.of(hour, minute, second, nano);
            }

            @Override
            public DataContainer translate(LocalTime obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.LOCAL_TIME_HOUR, obj.getHour())
                        .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, obj.getMinute())
                        .set(Constants.DataSerializers.LOCAL_TIME_SECOND, obj.getSecond())
                        .set(Constants.DataSerializers.LOCAL_TIME_NANO, obj.getNano());
            }

            @Override
            public DataView addTo(LocalTime obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.LOCAL_TIME_HOUR, obj.getHour())
                    .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, obj.getMinute())
                    .set(Constants.DataSerializers.LOCAL_TIME_SECOND, obj.getSecond())
                    .set(Constants.DataSerializers.LOCAL_TIME_NANO, obj.getNano());
            }
        };
        LOCAL_DATE_DATA_SERIALIZER = new DataTranslator<LocalDate>() {
            final TypeToken<LocalDate> token = TypeToken.get(LocalDate.class);

            @Override
            public TypeToken<LocalDate> token() {
                return this.token;
            }

            @Override
            public LocalDate translate(DataView view) throws InvalidDataException {
                final int year = view.getInt(Constants.DataSerializers.LOCAL_DATE_YEAR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_YEAR));
                final int month = view.getInt(Constants.DataSerializers.LOCAL_DATE_MONTH).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_MONTH));
                final int day = view.getInt(Constants.DataSerializers.LOCAL_DATE_DAY).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_DAY));
                if (!ChronoField.YEAR.range().isValidValue(year)) {
                    throw new InvalidDataException("Invalid year: " + year);
                }
                if (!ChronoField.MONTH_OF_YEAR.range().isValidValue(month)) {
                    throw new InvalidDataException("Invalid month of year: " + month);
                }
                if (!ChronoField.DAY_OF_MONTH.range().isValidValue(day)) {
                    throw new InvalidDataException("Invalid day of month: " + day);
                }
                return LocalDate.of(year, month, day);
            }

            @Override
            public DataContainer translate(LocalDate obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.LOCAL_DATE_YEAR, obj.getYear())
                        .set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getMonthValue())
                        .set(Constants.DataSerializers.LOCAL_DATE_DAY, obj.getDayOfMonth());
            }

            @Override
            public DataView addTo(LocalDate obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.LOCAL_DATE_YEAR, obj.getYear())
                    .set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getMonthValue())
                    .set(Constants.DataSerializers.LOCAL_DATE_DAY, obj.getDayOfMonth());
            }
        };
        LOCAL_DATE_TIME_DATA_SERIALIZER = new DataTranslator<LocalDateTime>() {
            final TypeToken<LocalDateTime> token = TypeToken.get(LocalDateTime.class);

            @Override
            public TypeToken<LocalDateTime> token() {
                return this.token;
            }

            @Override
            public LocalDateTime translate(DataView view) throws InvalidDataException {
                final int year = view.getInt(Constants.DataSerializers.LOCAL_DATE_YEAR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_YEAR));
                final int month = view.getInt(Constants.DataSerializers.LOCAL_DATE_MONTH).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_MONTH));
                final int day = view.getInt(Constants.DataSerializers.LOCAL_DATE_DAY).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_DAY));
                final int hour = view.getInt(Constants.DataSerializers.LOCAL_TIME_HOUR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_HOUR));
                final int minute = view.getInt(Constants.DataSerializers.LOCAL_TIME_MINUTE).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_MINUTE));
                final int second = view.getInt(Constants.DataSerializers.LOCAL_TIME_SECOND).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_SECOND));
                final int nano = view.getInt(Constants.DataSerializers.LOCAL_TIME_NANO).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_NANO));
                if (!ChronoField.YEAR.range().isValidValue(year)) {
                    throw new InvalidDataException("Invalid year: " + year);
                }
                if (!ChronoField.MONTH_OF_YEAR.range().isValidValue(month)) {
                    throw new InvalidDataException("Invalid month of year: " + month);
                }
                if (!ChronoField.DAY_OF_MONTH.range().isValidValue(day)) {
                    throw new InvalidDataException("Invalid day of month: " + day);
                }
                if (!ChronoField.HOUR_OF_DAY.range().isValidValue(hour)) {
                    throw new InvalidDataException("Invalid hour of day: " + hour);
                }
                if (!ChronoField.MINUTE_OF_HOUR.range().isValidValue(minute)) {
                    throw new InvalidDataException("Invalid minute of hour: " + minute);
                }
                if (!ChronoField.SECOND_OF_MINUTE.range().isValidValue(second)) {
                    throw new InvalidDataException("Invalid second of minute: " + second);
                }
                if (!ChronoField.NANO_OF_SECOND.range().isValidValue(nano)) {
                    throw new InvalidDataException("Invalid nanosecond of second: " + nano);
                }
                return LocalDateTime.of(year, month, day, hour, minute, second, nano);
            }

            @Override
            public DataContainer translate(LocalDateTime obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.LOCAL_DATE_YEAR, obj.getYear())
                        .set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getMonthValue())
                        .set(Constants.DataSerializers.LOCAL_DATE_DAY, obj.getDayOfMonth())
                        .set(Constants.DataSerializers.LOCAL_TIME_HOUR, obj.getHour())
                        .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, obj.getMinute())
                        .set(Constants.DataSerializers.LOCAL_TIME_SECOND, obj.getSecond())
                        .set(Constants.DataSerializers.LOCAL_TIME_NANO, obj.getNano());
            }

            @Override
            public DataView addTo(LocalDateTime obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.LOCAL_DATE_YEAR, obj.getYear())
                    .set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getMonthValue())
                    .set(Constants.DataSerializers.LOCAL_DATE_DAY, obj.getDayOfMonth())
                    .set(Constants.DataSerializers.LOCAL_TIME_HOUR, obj.getHour())
                    .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, obj.getMinute())
                    .set(Constants.DataSerializers.LOCAL_TIME_SECOND, obj.getSecond())
                    .set(Constants.DataSerializers.LOCAL_TIME_NANO, obj.getNano());
            }
        };
        ZONED_DATE_TIME_DATA_SERIALIZER = new DataTranslator<ZonedDateTime>() {
            final TypeToken<ZonedDateTime> token = TypeToken.get(ZonedDateTime.class);

            @Override
            public TypeToken<ZonedDateTime> token() {
                return this.token;
            }

            @Override
            public ZonedDateTime translate(DataView view) throws InvalidDataException {
                final int year = view.getInt(Constants.DataSerializers.LOCAL_DATE_YEAR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_YEAR));
                final int month = view.getInt(Constants.DataSerializers.LOCAL_DATE_MONTH).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_MONTH));
                final int day = view.getInt(Constants.DataSerializers.LOCAL_DATE_DAY).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_DAY));
                final int hour = view.getInt(Constants.DataSerializers.LOCAL_TIME_HOUR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_HOUR));
                final int minute = view.getInt(Constants.DataSerializers.LOCAL_TIME_MINUTE).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_MINUTE));
                final int second = view.getInt(Constants.DataSerializers.LOCAL_TIME_SECOND).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_SECOND));
                final int nano = view.getInt(Constants.DataSerializers.LOCAL_TIME_NANO).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_NANO));
                final String zoneId = view.getString(Constants.DataSerializers.ZONE_TIME_ID).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.ZONE_TIME_ID));
                if (!ChronoField.YEAR.range().isValidValue(year)) {
                    throw new InvalidDataException("Invalid year: " + year);
                }
                if (!ChronoField.MONTH_OF_YEAR.range().isValidValue(month)) {
                    throw new InvalidDataException("Invalid month of year: " + month);
                }
                if (!ChronoField.DAY_OF_MONTH.range().isValidValue(day)) {
                    throw new InvalidDataException("Invalid day of month: " + day);
                }
                if (!ChronoField.HOUR_OF_DAY.range().isValidValue(hour)) {
                    throw new InvalidDataException("Invalid hour of day: " + hour);
                }
                if (!ChronoField.MINUTE_OF_HOUR.range().isValidValue(minute)) {
                    throw new InvalidDataException("Invalid minute of hour: " + minute);
                }
                if (!ChronoField.SECOND_OF_MINUTE.range().isValidValue(second)) {
                    throw new InvalidDataException("Invalid second of minute: " + second);
                }
                if (!ChronoField.NANO_OF_SECOND.range().isValidValue(nano)) {
                    throw new InvalidDataException("Invalid nanosecond of second: " + nano);
                }
                if (!ZoneId.getAvailableZoneIds().contains(zoneId)) {
                    throw new InvalidDataException("Unrecognized ZoneId: " + zoneId);
                }
                return ZonedDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second, nano), ZoneId.of(zoneId));
            }

            @Override
            public DataContainer translate(ZonedDateTime obj) throws InvalidDataException {
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.LOCAL_DATE_YEAR, obj.getYear())
                        .set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getMonthValue())
                        .set(Constants.DataSerializers.LOCAL_DATE_DAY, obj.getDayOfMonth())
                        .set(Constants.DataSerializers.LOCAL_TIME_HOUR, obj.getHour())
                        .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, obj.getMinute())
                        .set(Constants.DataSerializers.LOCAL_TIME_SECOND, obj.getSecond())
                        .set(Constants.DataSerializers.LOCAL_TIME_NANO, obj.getNano())
                        .set(Constants.DataSerializers.ZONE_TIME_ID, obj.getZone().getId());
            }

            @Override
            public DataView addTo(ZonedDateTime obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.LOCAL_DATE_YEAR, obj.getYear())
                    .set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getMonthValue())
                    .set(Constants.DataSerializers.LOCAL_DATE_DAY, obj.getDayOfMonth())
                    .set(Constants.DataSerializers.LOCAL_TIME_HOUR, obj.getHour())
                    .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, obj.getMinute())
                    .set(Constants.DataSerializers.LOCAL_TIME_SECOND, obj.getSecond())
                    .set(Constants.DataSerializers.LOCAL_TIME_NANO, obj.getNano())
                    .set(Constants.DataSerializers.ZONE_TIME_ID, obj.getZone().getId());
            }
        };
        INSTANT_DATA_SERIALIZER = new DataTranslator<Instant>() {
            final TypeToken<Instant> token = TypeToken.get(Instant.class);

            @Override
            public TypeToken<Instant> token() {
                return this.token;
            }

            @Override
            public Instant translate(DataView view) throws InvalidDataException {
                final int year = view.getInt(Constants.DataSerializers.LOCAL_DATE_YEAR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_YEAR));
                final int month = view.getInt(Constants.DataSerializers.LOCAL_DATE_MONTH).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_MONTH));
                final int day = view.getInt(Constants.DataSerializers.LOCAL_DATE_DAY).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_DAY));
                final int hour = view.getInt(Constants.DataSerializers.LOCAL_TIME_HOUR).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_HOUR));
                final int minute = view.getInt(Constants.DataSerializers.LOCAL_TIME_MINUTE).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_MINUTE));
                final int second = view.getInt(Constants.DataSerializers.LOCAL_TIME_SECOND).orElseThrow(DataSerializers.invalidDataQuery(
                    Constants.DataSerializers.LOCAL_TIME_SECOND));
                final int nano = view.getInt(Constants.DataSerializers.LOCAL_TIME_NANO).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_TIME_NANO));
                if (!ChronoField.YEAR.range().isValidValue(year)) {
                    throw new InvalidDataException("Invalid year: " + year);
                }
                if (!ChronoField.MONTH_OF_YEAR.range().isValidValue(month)) {
                    throw new InvalidDataException("Invalid month of year: " + month);
                }
                if (!ChronoField.DAY_OF_MONTH.range().isValidValue(day)) {
                    throw new InvalidDataException("Invalid day of month: " + day);
                }
                if (!ChronoField.HOUR_OF_DAY.range().isValidValue(hour)) {
                    throw new InvalidDataException("Invalid hour of day: " + hour);
                }
                if (!ChronoField.MINUTE_OF_HOUR.range().isValidValue(minute)) {
                    throw new InvalidDataException("Invalid minute of hour: " + minute);
                }
                if (!ChronoField.SECOND_OF_MINUTE.range().isValidValue(second)) {
                    throw new InvalidDataException("Invalid second of minute: " + second);
                }
                if (!ChronoField.NANO_OF_SECOND.range().isValidValue(nano)) {
                    throw new InvalidDataException("Invalid nanosecond of second: " + nano);
                }
                return LocalDateTime.of(year, month, day, hour, minute, second, nano).toInstant(ZoneOffset.UTC);
            }

            @Override
            public DataContainer translate(Instant obj) throws InvalidDataException {
                final LocalDateTime local = obj.atZone(ZoneOffset.UTC).toLocalDateTime();
                return DataContainer.createNew()
                        .set(Constants.DataSerializers.LOCAL_DATE_YEAR, local.getYear())
                        .set(Constants.DataSerializers.LOCAL_DATE_MONTH, local.getMonthValue())
                        .set(Constants.DataSerializers.LOCAL_DATE_DAY, local.getDayOfMonth())
                        .set(Constants.DataSerializers.LOCAL_TIME_HOUR, local.getHour())
                        .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, local.getMinute())
                        .set(Constants.DataSerializers.LOCAL_TIME_SECOND, local.getSecond())
                        .set(Constants.DataSerializers.LOCAL_TIME_NANO, local.getNano());
            }

            @Override
            public DataView addTo(Instant obj, DataView dataView) {
                final LocalDateTime local = obj.atZone(ZoneOffset.UTC).toLocalDateTime();
                return dataView.set(Constants.DataSerializers.LOCAL_DATE_YEAR, local.getYear())
                    .set(Constants.DataSerializers.LOCAL_DATE_MONTH, local.getMonthValue())
                    .set(Constants.DataSerializers.LOCAL_DATE_DAY, local.getDayOfMonth())
                    .set(Constants.DataSerializers.LOCAL_TIME_HOUR, local.getHour())
                    .set(Constants.DataSerializers.LOCAL_TIME_MINUTE, local.getMinute())
                    .set(Constants.DataSerializers.LOCAL_TIME_SECOND, local.getSecond())
                    .set(Constants.DataSerializers.LOCAL_TIME_NANO, local.getNano());
            }
        };
        MONTH_DATA_SERIALIZER = new DataTranslator<Month>() {
            final TypeToken<Month> token = TypeToken.get(Month.class);

            @Override
            public TypeToken<Month> token() {
                return this.token;
            }

            @Override
            public Month translate(DataView view) throws InvalidDataException {
                final int month = view.getInt(Constants.DataSerializers.LOCAL_DATE_MONTH).orElseThrow(DataSerializers.invalidDataQuery(Constants.DataSerializers.LOCAL_DATE_MONTH));
                if (!ChronoField.MONTH_OF_YEAR.range().isValidValue(month)) {
                    throw new InvalidDataException("Invalid month of year: " + month);
                }
                return Month.of(month);
            }

            @Override
            public DataContainer translate(Month obj) throws InvalidDataException {
                return DataContainer.createNew().set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getValue());
            }

            @Override
            public DataView addTo(Month obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.LOCAL_DATE_MONTH, obj.getValue());
            }
        };

        DIRECTION_SERIALIZER = new DataTranslator<Direction>() {
            final TypeToken<Direction> token = TypeToken.get(Direction.class);

            @Override
            public TypeToken<Direction> token() {
                return this.token;
            }

            @Override
            public Direction translate(DataView view) throws InvalidDataException {
                try {
                    return Direction.valueOf(view.getString(Constants.DataSerializers.DIRECTION).get());
                } catch (IllegalArgumentException e) {
                    throw new InvalidDataException("Invalid Direction" + view);
                }
            }

            @Override
            public DataContainer translate(Direction obj) throws InvalidDataException {
                return DataContainer.createNew().set(Constants.DataSerializers.DIRECTION, obj.name());
            }

            @Override
            public DataView addTo(Direction obj, DataView dataView) {
                return dataView.set(Constants.DataSerializers.DIRECTION, obj.name());
            }
        };

    }

    static Supplier<InvalidDataException> invalidDataQuery(final DataQuery query) {
        return () -> {
            throw new InvalidDataException("Invalid data located at: " + query.toString());
        };
    }
}
