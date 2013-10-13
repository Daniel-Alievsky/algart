package net.algart.matrices.morphology;

import net.algart.arrays.Arrays;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Iterator;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;
import java.lang.ref.Reference;

class HistogramCache<T> {
    private final Map<Long, Reference<T>> histogramCache;
    private final int histogramCacheCapacity;

    HistogramCache() {
        this.histogramCacheCapacity = 2 * Arrays.SystemSettings.cpuCount();
        // "2*" - to be on the safe side, when the we access an array in non-optimal order
        this.histogramCache = new LinkedHashMap<Long, Reference<T>>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Reference<T>> eldest) {
                return size() > histogramCacheCapacity;
            }

            private static final long serialVersionUID = 3203370748019883916L;
        };
    }

    public synchronized int size() {
        return histogramCache.size();
    }

    public synchronized T get(long position) {
        Reference<T> ref = histogramCache.remove(position);
        if (ref == null) {
            return null;
        }
        return ref.get();
    }

    public synchronized void put(long position, T histogram) {
        if (histogram == null)
            throw new NullPointerException("Null histogram");
        reap();
        Reference<T> ref = new SoftReference<T>(histogram);
        histogramCache.put(position, ref);
    }

    private void reap() {
        Set<Map.Entry<Long, Reference<T>>> entries = histogramCache.entrySet();
        for (Iterator<Map.Entry<Long, Reference<T>>> iterator = entries.iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, Reference<T>> entry = iterator.next();
            if (entry.getValue().get() == null) {
                iterator.remove();
            }
        }
    }
}
