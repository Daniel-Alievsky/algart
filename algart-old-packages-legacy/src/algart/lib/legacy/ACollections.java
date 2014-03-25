/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package algart.lib.legacy;

import java.util.*;
import java.lang.reflect.*;

/**
 * <p>Title:     <b>Algart Collections</b><br>
 * Description:  Library of tools for working with collections<br>
 *
 * @author Daniel Alievsky
 * @version 1.0
 * @deprecated
 */

public class ACollections {

    /**
     */
    public static boolean getBoolean(Map m, Object key, boolean defaultValue) {
        Boolean v = (Boolean) m.get(key);
        return v == null ? defaultValue : v.booleanValue();
    }

    /**
     */
    public static boolean getBoolean(Map m, int key, boolean defaultValue) {
        return getBoolean(m, new Integer(key), defaultValue);
    }

    /**
     */
    public static int getInt(Map m, Object key, int defaultValue) {
        Integer v = (Integer) m.get(key);
        return v == null ? defaultValue : v.intValue();
    }

    /**
     */
    public static int getInt(Map m, int key, int defaultValue) {
        return getInt(m, new Integer(key), defaultValue);
    }

    /**
     */
    public static float getFloat(Map m, Object key, float defaultValue) {
        Float v = (Float) m.get(key);
        return v == null ? defaultValue : v.floatValue();
    }

    /**
     */
    public static float getFloat(Map m, int key, float defaultValue) {
        return getFloat(m, new Integer(key), defaultValue);
    }

    /**
     */
    public static double getDouble(Map m, Object key, double defaultValue) {
        Double v = (Double) m.get(key);
        return v == null ? defaultValue : v.doubleValue();
    }

    /**
     */
    public static double getDouble(Map m, int key, double defaultValue) {
        return getDouble(m, new Integer(key), defaultValue);
    }

    /**
     * a can be an array (including arrays of primitive types) or Collection
     * In other case, an empty collection is returned
     * If a is a collection and an instance of collectionClass already, it is returned as a result
     */
    public static Collection toCollection(Object a, Class collectionClass)
            throws IllegalAccessException, InstantiationException
    {
        if (a instanceof Collection) {
            if (collectionClass.isAssignableFrom(a.getClass())) {
                return (Collection) a;
            }
            Collection result = (Collection) collectionClass.newInstance();
            result.addAll((Collection) a);
            return result;
        }
        Collection result = (Collection) collectionClass.newInstance();
        if (a != null && a.getClass().isArray()) {
            int len = Array.getLength(a);
            for (int k = 0; k < len; k++) {
                result.add(Array.get(a, k));
            }
        }
        return result;
    }

    /**
     */
    public static Collection toImmutableCollection(Object a, Class basicCollectionClass)
            throws IllegalAccessException, InstantiationException
    {
        Collection c = toCollection(a, basicCollectionClass);
        if (c instanceof List) {
            return Collections.unmodifiableList((List) c);
        }
        if (c instanceof SortedSet) {
            return Collections.unmodifiableSortedSet((SortedSet) c);
        }
        if (c instanceof Set) {
            return Collections.unmodifiableSet((Set) c);
        }
        return Collections.unmodifiableCollection(c);
    }

    /**
     */
    public static ArrayList toArrayList(Object a) {
        try {
            return (ArrayList) toCollection(a, ArrayList.class);
        } catch (IllegalAccessException e) {
            throw new InternalError(e.toString());
        } catch (InstantiationException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     */
    public static List toImmutableArrayList(Object a) {
        try {
            return (List) toImmutableCollection(a, ArrayList.class);
        } catch (IllegalAccessException e) {
            throw new InternalError(e.toString());
        } catch (InstantiationException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     */
    public static HashSet toHashSet(Object a) {
        try {
            return (HashSet) toCollection(a, HashSet.class);
        } catch (IllegalAccessException e) {
            throw new InternalError(e.toString());
        } catch (InstantiationException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     */
    public static Set toImmutableHashSet(Object a) {
        try {
            return (Set) toImmutableCollection(a, HashSet.class);
        } catch (IllegalAccessException e) {
            throw new InternalError(e.toString());
        } catch (InstantiationException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * a can be an array (including arrays of primitive types) or Collection
     * If a.length%2==1, the last element of a is ignored
     * If a==null, an empty map is returned
     */
    public static Map toMap(Object a, boolean reverse, boolean skipNullValues, Class mapClass)
            throws IllegalAccessException, InstantiationException
    {
        if (a instanceof Collection) {
            a = ATools.toA(a);
        }
        int len = ATools.length(a);
        Map result = (Map) mapClass.newInstance();
        if (!reverse) {
            for (int k = 0; k + 1 < len; k += 2) {
                Object key = Array.get(a, k);
                Object value = Array.get(a, k + 1);
                if (!skipNullValues || value != null) {
                    result.put(key, value);
                }
            }
        } else {
            for (int k = len & ~0x1; k > 0; k -= 2) {
                Object key = Array.get(a, k - 2);
                Object value = Array.get(a, k - 1);
                if (!skipNullValues || value != null) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     */
    public static Map toImmutableMap(Object a, boolean reverse, boolean skipNullValues, Class mapClass)
            throws IllegalAccessException, InstantiationException
    {
        return Collections.unmodifiableMap(toMap(a, reverse, skipNullValues, mapClass));
    }

    /**
     */
    public static LinkedHashMap toLinkedHashMap(Object a, boolean reverse, boolean skipNullValues) {
        try {
            return (LinkedHashMap) toMap(a, reverse, skipNullValues, LinkedHashMap.class);
        } catch (IllegalAccessException e) {
            throw new InternalError(e.toString());
        } catch (InstantiationException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     */
    public static Map toImmutableLinkedHashMap(Object a, boolean reverse, boolean skipNullValues) {
        return Collections.unmodifiableMap(toLinkedHashMap(a, reverse, skipNullValues));
    }

    /**
     * keys and values can be arrays (including arrays of primitive types) or Collection
     * If min(keys.length,value.length)%2==1, the last element is ignored
     */
    public static Map toMap(Object keys, Object values, boolean reverse, boolean skipNullValues, Class mapClass)
            throws IllegalAccessException, InstantiationException
    {
        if (keys instanceof Collection) {
            keys = ATools.toA(keys);
        }
        if (values instanceof Collection) {
            values = ATools.toA(values);
        }
        int len = ATools.min(ATools.length(keys), ATools.length(values));
        Map result = (Map) mapClass.newInstance();
        if (!reverse) {
            for (int k = 0; k < len; k++) {
                Object key = Array.get(keys, k);
                Object value = Array.get(values, k);
                if (!skipNullValues || value != null) {
                    result.put(key, value);
                }
            }
        } else {
            for (int k = len - 1; k >= 0; k--) {
                Object key = Array.get(keys, k);
                Object value = Array.get(values, k);
                if (!skipNullValues || value != null) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     */
    public static Map toImmutableMap(
            Object keys,
            Object values,
            boolean reverse,
            boolean skipNullValues,
            Class mapClass) throws IllegalAccessException, InstantiationException
    {
        return Collections.unmodifiableMap(toMap(keys, values, reverse, skipNullValues, mapClass));
    }

    /**
     */
    public static LinkedHashMap toLinkedHashMap(Object keys, Object values, boolean reverse, boolean skipNullValues) {
        try {
            return (LinkedHashMap) toMap(keys, values, reverse, skipNullValues, LinkedHashMap.class);
        } catch (IllegalAccessException e) {
            throw new InternalError(e.toString());
        } catch (InstantiationException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     */
    public static Map toImmutableLinkedHashMap(Object keys, Object values, boolean reverse, boolean skipNullValues) {
        return Collections.unmodifiableMap(toLinkedHashMap(keys, values, reverse, skipNullValues));
    }


    /**
     * <p>Title:     <b>Algart Immutable Map</b><br>
     * Description:  Immutable equivalent of standard Maps (for String type only)<br>
     */
    public static class ImmutableMap implements Cloneable, Map {

        private Map storage;

        private Map createStorage(Map m, Class storageMapClass) {
            Map result;
            try {
                result = (Map) storageMapClass.newInstance();
            } catch (InstantiationException e) {
                throw new InternalError(e.toString());
            } catch (IllegalAccessException e) {
                throw new InternalError(e.toString());
            }
            Set entries = m.entrySet();
            for (Iterator it = entries.iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = String.valueOf(entry.getKey());
                String value = ATools.toSOrNull(entry.getValue());
                result.put(key, value);
            }
            return result;
        }

        /**
         * Builds an immutable map with the same content as m parameter.
         * Will use storageMapClass for creating internal data storage,
         * for example, HasmMap.class; MUST BE one of the following classes:
         * HashMap, LinkedHashMap, TreeMap
         * (It allows to be ensured that the resulting object will be truly immutable.)
         * All non-String keys (including null) will be converted into String type.
         * All non-String values that are not equal to null will be converted into String type.
         */
        public ImmutableMap(Map m, Class storageMapClass) throws IllegalArgumentException {
            if (storageMapClass != HashMap.class &&
                storageMapClass != LinkedHashMap.class &&
                storageMapClass != TreeMap.class)
            {
                throw new IllegalArgumentException(
                        "Cannot create ImmutableMap with storage map class \"" +
                        storageMapClass +
                        "\": only HashMap, LinkedHashMap, TreeMap are allowed " +
                        "(to be ensured that the resulting map will be truly immutable)"
                );
            }
            this.storage = createStorage(m, storageMapClass);
            // - necessary to provide correct keySet, values and entrySet methods
        }

        /**
         * Equivalent of AImmutableMap(m,m.getClass())
         * m.getClass() MUST BE equal to
         * HashMap.class, LinkedHashMap.class or TreeMap.class
         */
        public ImmutableMap(Map m) throws IllegalArgumentException {
            this(m, m.getClass());
        }

        /**
         * keyValuePairs should contain even number of strings: key, value, key, value, ..., or can be null
         * In other case, the last element of the array is ignored.
         * If some key appears twice or several times, the first appearance is the most important.
         * Will use storageMapClass for creating internal data storage,
         * for example, HasmMap.class; MUST BE one of the following classes:
         * HashMap, LinkedHashMap, TreeMap
         * (It allows to be ensured that the resulting object will be truly immutable.)
         */
        public ImmutableMap(String[] keyValuePairs, boolean skipNullValues, Class storageMapClass)
                throws IllegalArgumentException
        {
            if (storageMapClass != HashMap.class &&
                storageMapClass != LinkedHashMap.class &&
                storageMapClass != TreeMap.class)
            {
                throw new IllegalArgumentException(
                        "Cannot create ImmutableMap with storage map class \"" +
                        storageMapClass +
                        "\": only HashMap, LinkedHashMap, TreeMap are allowed " +
                        "(to be ensured that the resulting map will be truly immutable)"
                );
            }
            try {
                this.storage = toMap(keyValuePairs, true, skipNullValues, storageMapClass);
            } catch (InstantiationException e) {
                throw new InternalError(e.toString());
            } catch (IllegalAccessException e) {
                throw new InternalError(e.toString());
            }
        }

        /**
         * Internal data storage will be LinkedHashMap
         */
        public ImmutableMap(String[] keyValuePairs, boolean skipNullValues) {
            this.storage = toLinkedHashMap(keyValuePairs, true, skipNullValues);
        }

        // Implementation of Map interface
        public final int size() {
            return storage.size();
        }

        public final boolean isEmpty() {
            return storage.isEmpty();
        }

        public final boolean containsKey(Object key) {
            return storage.containsKey(key);
        }

        public final boolean containsValue(Object val) {
            return storage.containsValue(val);
        }

        public final Object get(Object key) {
            return storage.get(key);
        }

        public final Object put(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public final Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public final void putAll(Map t) {
            throw new UnsupportedOperationException();
        }

        public final void clear() {
            throw new UnsupportedOperationException();
        }

        private transient Map storageUnmodifiable = null;

        public final Set keySet() {
            if (storageUnmodifiable == null) {
                storageUnmodifiable = Collections.unmodifiableMap(storage);
            }
            return storageUnmodifiable.keySet();
        }

        public final Set entrySet() {
            if (storageUnmodifiable == null) {
                storageUnmodifiable = Collections.unmodifiableMap(storage);
            }
            return storageUnmodifiable.entrySet();
        }

        public final Collection values() {
            if (storageUnmodifiable == null) {
                storageUnmodifiable = Collections.unmodifiableMap(storage);
            }
            return storageUnmodifiable.values();
        }

        public boolean equals(Object o) {
            return storage.equals(o);
        }

        public int hashCode() {
            return storage.hashCode();
        }

        public String toString() {
            return storage.toString();
        }

        // Implementation of Cloneable interface
        protected Object clone() {
            try {
                ImmutableMap result = (ImmutableMap) super.clone();
                result.storage = createStorage(this.storage, this.storage.getClass());
                return result;
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e.toString());
            }
        }

        // Additional functions
        public final String get(String key) {
            return (String) storage.get(key);
        }

        public final Map toMutableMap() {
            return createStorage(this.storage, this.storage.getClass());
        }

        public final ImmutableMap change(String key, String newValue, boolean skipNullValues) {
            ImmutableMap result = (ImmutableMap) this.clone();
            if (skipNullValues && newValue == null) {
                result.storage.remove(key);
            } else {
                result.storage.put(String.valueOf(key), newValue);
            }
            return result;
        }
    }
}