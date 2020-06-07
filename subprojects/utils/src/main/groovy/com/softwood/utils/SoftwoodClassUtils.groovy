package com.softwood.utils


import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class SoftwoodClassUtils {

    /**
     *
     * @param clazz , the class to check
     * @return List<String>  list of names of static properties in this class
     */
    static List<String> getStaticProperties(Class clazz) {
        assert clazz, "must provide provide instance of Class to assess"

        List<MetaProperty> props = clazz.metaClass.properties
        props?.stream()
                .filter { MetaProperty prop -> Modifier.isStatic(prop.modifiers) }
                .map { prop -> prop.name }
                .collect()
                .toList()
    }

    static List<String> hasPublicStaticProperty(Class clazz, String) {
        assert clazz, "must provide provide instance of Class to assess"

        List<MetaProperty> props = clazz.metaClass.properties
        props?.stream()
                .filter { MetaProperty prop -> Modifier.isStatic(prop.modifiers) && Modifier.isPublic(prop.modifiers) }
                .map { prop -> prop.name }
                .collect()
                .toList()
    }


    /**
     * uses latest approved Java way creating instance of class.  will default to finding the no arg constructor if present
     * @param clazz to create instance of
     * @param args , optional args to pass a constructor that matches
     * @return Optional<T>  creates instance of constructed object, or empty optional if cant find a match
     */
    static <T> Optional<T> instanceOf(Class<T> clazz, Object[] args) {
        assert clazz

        final Object<?>[] EMPTY_ARGS = [].toArray()

        if (args == null)
            args = EMPTY_ARGS

        //gets the same answer Class<?>[] parameterTypes2 = ClassUtils.toClass(args)

        Constructor<T> constructor

        //gets all constructors, then looks match on args length and param types
        //if arg isnt assignable to constructor arg then fail the match
        Constructor[] allConstructors = clazz.getDeclaredConstructors()
        for (int j = 0; j < allConstructors.length; j++) {
            //process the constructor types relative to provided arg types
            Class[] constructorTypes = allConstructors[j].parameterTypes
            if (constructorTypes.length != args.length)
                continue

            def match = true
            for (int i = 0; i < constructorTypes.length; i++) {
                Class requiredType = constructorTypes[i], argType = args[i].getClass()
                if (!isAssignableTo(argType, requiredType)) {
                    match = false
                    break
                }
            }
            if (match) {
                constructor = allConstructors[j]
                break  //call off the search
            }
        }

        //use of the spread operator seems to trigger the unboxing from wrapped to unwrapped classes as required
        Optional o = Optional.ofNullable(constructor?.newInstance(*args))
    }

    private static final Map<Class<?>, Class<?>> primitiveToWrapperMap =
            Map.of(boolean.class, Boolean.class,
                    byte.class, Byte.class,
                    char.class, Character.class,
                    double.class, Double.class,
                    float.class, Float.class,
                    int.class, Integer.class,
                    long.class, Long.class,
                    short.class, Short.class)

    private static final Map<Class<?>, Class<?>> wrapperToPrimativeMap =
            Map.of(Boolean.class, boolean.class,
                    Byte.class, byte.class,
                    Character.class, char.class,
                    Double.class, double.class,
                    Float.class, float.class,
                    Integer.class, int.class,
                    Long.class, long.class,
                    Short.class, short.class)


    /**
     * checks whether class instance is a wrapper for the primative type
     * @param targetClass
     * @param primitiveType
     * @return true or false
     */
    static boolean isPrimitiveWrapperOf(targetClass, primitiveType) {
        if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException("First argument has to be primitive type");
        }

        //returnm equality check
        primitiveToWrapperMap.get(primitiveType) == targetClass
    }

    /**
     * primative types are boxed to wrapped types when passing into a function
     *
     * this routine will determine if the wrapped class can be assigned to its primative type
     * @param from
     * @param to
     * @return
     */
    static boolean isAssignableTo(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (from.isPrimitive()) {
            return isPrimitiveWrapperOf(to, from);
        }
        if (to.isPrimitive()) {
            return isPrimitiveWrapperOf(from, to);
        }
        return false;
    }

    /**
     * Determine whether the method is declared public static
     * @param m
     * @return true if the method is declared public static
     */
    static boolean isPublicStaticMethod(Method m) {
        final int modifiers = m.getModifiers()
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
    }

    /**
     * Determine whether the field is declared public static
     * @param f
     * @return true if the field is declared public static
     */
    static boolean isPublicStaticField(Field f) {
        final int modifiers = f.getModifiers()
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
    }


}
