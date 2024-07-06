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
package org.spongepowered.common.event.filter.delegate;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.common.event.manager.ListenerClassVisitor;

import java.lang.reflect.Method;
import java.util.Optional;

public class GetterFilterSourceDelegate implements ParameterFilterSourceDelegate {

    private final Getter anno;

    public GetterFilterSourceDelegate(final Getter a) {
        this.anno = a;
    }

    @Override
    public Tuple<Integer, Integer> write(
        final ClassWriter cw, final MethodVisitor mv, final ListenerClassVisitor.DiscoveredMethod method,
        final int paramIdx, int local, final int[] plocals, final ListenerClassVisitor.ListenerParameter[] params
    ) throws ClassNotFoundException {
        final Class<?> targetType = method.classByLoader(params[paramIdx].type().getClassName());
        final Class<?> eventClass = method.classByLoader(params[0].type().getClassName());
        final String targetMethod = this.anno.value();
        Method targetMethodObj = null;

        try {
            targetMethodObj = eventClass.getMethod(targetMethod);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Method %s specified by getter annotation was not found in type %s", targetMethod, eventClass.getName()));
        }

        if (targetMethodObj.getParameterCount() != 0) {
            throw new IllegalArgumentException(
                    "Method " + targetMethodObj.toGenericString() + " specified by getter annotation has non-zero parameter count");
        }
        if (!targetMethodObj.getReturnType().equals(Optional.class) && !targetMethodObj.getReturnType().isAssignableFrom(targetType)) {
            throw new IllegalArgumentException("Method " + targetMethodObj.toGenericString() + " does not return the correct type. Expected: "
                    + targetType.getName() + " Found: " + targetMethodObj.getReturnType().getName());
        }

        final Type returnType = Type.getReturnType(targetMethodObj);
        final Class<?> declaringClass = targetMethodObj.getDeclaringClass();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(declaringClass));
        final int op = declaringClass.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL;
        mv.visitMethodInsn(op, Type.getInternalName(declaringClass), targetMethod, "()" + returnType.getDescriptor(), declaringClass.isInterface());
        final int paramLocal = local++;
        mv.visitVarInsn(returnType.getOpcode(ISTORE), paramLocal);
        if (!targetMethodObj.getReturnType().isPrimitive()) {
            final Label failure = new Label();
            final Label success = new Label();
            if (Optional.class.equals(targetMethodObj.getReturnType()) && !Optional.class.equals(targetType)) {
                // Unwrap the optional
                mv.visitVarInsn(ALOAD, paramLocal);

                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z", false);
                mv.visitJumpInsn(IFEQ, failure);

                mv.visitVarInsn(ALOAD, paramLocal);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ASTORE, paramLocal);
            } else {
                mv.visitVarInsn(returnType.getOpcode(ILOAD), paramLocal);
            }
            mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(targetType));
            mv.visitJumpInsn(IFNE, success);
            mv.visitLabel(failure);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitLabel(success);
        }

        return new Tuple<>(local, paramLocal);
    }

}
