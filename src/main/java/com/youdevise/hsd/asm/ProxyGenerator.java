package com.youdevise.hsd.asm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.youdevise.hsd.EnumIndexedCursor;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

public class ProxyGenerator<E extends Enum<E>, T> {

    private static final String T_CURSOR = "com/youdevise/hsd/EnumIndexedCursor";
    private static final String T_LONG = "java/lang/Long";
    private static final String T_INTEGER = "java/lang/Integer";
    private static final String T_SHORT = "java/lang/Short";
    private static final String T_CHARACTER = "java/lang/Character";
    private static final String T_BYTE = "java/lang/Byte";
    private static final String T_BOOLEAN = "java/lang/Boolean";
    private static final String T_OBJECT = "java/lang/Object";
    private static final String L_OBJECT = "Ljava/lang/Object;";
    private static final String L_ENUM = "Ljava/lang/Enum;";
    private static final String L_CURSOR = "Lcom/youdevise/hsd/EnumIndexedCursor;";

    public static interface EnumBinding<E extends Enum<E>> {
        <T> ProxyGenerator<E, T> to(Class<T> proxyClass);
    }

    public static <E extends Enum<E>> ProxyGenerator.EnumBinding<E> mapping(final Class<E> enumClass) {
        return new ProxyGenerator.EnumBinding<E>() {
            @Override
            public <T> ProxyGenerator<E, T> to(Class<T> proxyClass) {
                return new ProxyGenerator<E, T>(enumClass, proxyClass);
            }
        };
    }

    private final Class<E> enumClass;
    private final Class<T> proxyClass;
    private final Class<? extends T> proxyImplementationClass;

    private ProxyGenerator(Class<E> enumClass, Class<T> proxyClass) {
        this.enumClass = enumClass;
        this.proxyClass = proxyClass;
        this.proxyImplementationClass = generateProxyImplementationClass();
    }

    public T generateView(EnumIndexedCursor<E> cursor) {
        try {
            return (T) proxyImplementationClass.getConstructor(EnumIndexedCursor.class).newInstance(cursor);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends T> generateProxyImplementationClass() {
        ClassWriter cw = new ClassWriter(false);
        String implName = implName(proxyClass);
        cw.visit(V1_6,
                 ACC_PUBLIC + ACC_SUPER,
                 implName,
                 null,
                 T_OBJECT,
                 new String[] { Type.getInternalName(proxyClass) });

        cw.visitField(0, "cursor", L_CURSOR, null, null).visitEnd();

        generateConstructor(cw, implName);
        for (E field : enumClass.getEnumConstants()) {
            generateGetter(cw, implName, field);
        }

        cw.visitEnd();

        ASMClassloader loader = new ASMClassloader();
        return (Class<? extends T>) loader.getClass(cw.toByteArray(), "asm1." + proxyClass.getName() + "Impl");
    }

    private class ASMClassloader extends ClassLoader {
        public Class<?> getClass(byte[] classBytes, String className) {
            Class<?> klass = defineClass(className, classBytes, 0, classBytes.length);
            resolveClass(klass);
            return klass;
        }
    }

    private void generateConstructor(ClassWriter cw, String implName) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lcom/youdevise/hsd/EnumIndexedCursor;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, T_OBJECT, "<init>", "()V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, T_CURSOR);
        mv.visitFieldInsn(PUTFIELD, implName, "cursor", L_CURSOR);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private void generateGetter(ClassWriter cw, String implName, E field) {
        String getterName = "get" + field.name().substring(0, 1).toUpperCase() + field.name().substring(1);
        Method method = getMethod(getterName);
        String descriptor = Type.getMethodDescriptor(method);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, getterName, descriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, implName, "cursor", L_CURSOR);
        mv.visitFieldInsn(GETSTATIC, Type.getInternalName(enumClass), field.name(), Type.getDescriptor(enumClass));
        mv.visitMethodInsn(INVOKEINTERFACE, T_CURSOR, "get", String.format("(%s)%s", L_ENUM, L_OBJECT));
        unboxReturnValue(mv, Type.getReturnType(method));
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    private void unboxReturnValue(MethodVisitor mv, Type returnType) {
        String T_FLOAT = "java/lang/Float";
        String T_DOUBLE = "java/lang/Double";
        switch (returnType.getSort()) {
            case Type.BOOLEAN:
                mv.visitTypeInsn(CHECKCAST, T_BOOLEAN);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_BOOLEAN, "booleanValue", "()Z");
                mv.visitInsn(IRETURN);
                break;
            case Type.BYTE:
                mv.visitTypeInsn(CHECKCAST, T_BYTE);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_BYTE, "byteValue", "()B");
                mv.visitInsn(IRETURN);
                break;
            case Type.CHAR:
                mv.visitTypeInsn(CHECKCAST, T_CHARACTER);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_CHARACTER, "charValue", "()C");
                mv.visitInsn(IRETURN);
                break;
            case Type.SHORT:
                mv.visitTypeInsn(CHECKCAST, T_SHORT);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_SHORT, "shortValue", "()S");
                mv.visitInsn(IRETURN);
                break;
            case Type.INT:
                mv.visitTypeInsn(CHECKCAST, T_INTEGER);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_INTEGER, "intValue", "()I");
                mv.visitInsn(IRETURN);
                break;
            case Type.FLOAT:
                mv.visitTypeInsn(CHECKCAST, T_FLOAT);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_FLOAT, "floatValue", "()F");
                mv.visitInsn(FRETURN);
                break;
            case Type.LONG:
                mv.visitTypeInsn(CHECKCAST, T_LONG);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_LONG, "longValue", "()J");
                mv.visitInsn(LRETURN);
                break;
            case Type.DOUBLE:
                mv.visitTypeInsn(CHECKCAST, T_DOUBLE);
                mv.visitMethodInsn(INVOKEVIRTUAL, T_DOUBLE, "doubleValue", "()D");
                mv.visitInsn(DRETURN);
                break;
            case Type.ARRAY:
                mv.visitTypeInsn(CHECKCAST, returnType.getDescriptor());
                mv.visitInsn(ARETURN);
                break;
            case Type.OBJECT:
                mv.visitTypeInsn(CHECKCAST, returnType.getInternalName());
                mv.visitInsn(ARETURN);
                break;
        }
    }

    private Method getMethod(String getterName) {
        try {
            return proxyClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private String implName(Class<?> klass) {
        return String.format("asm1/%sImpl", Type.getInternalName(klass));
    }
}