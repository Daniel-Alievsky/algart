/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.morphology;

import net.algart.arrays.Arrays;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

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
        Objects.requireNonNull(histogram, "Null histogram");
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
