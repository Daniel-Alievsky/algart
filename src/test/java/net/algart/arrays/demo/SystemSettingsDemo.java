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

package net.algart.arrays.demo;

import net.algart.arrays.Arrays;

/**
 * <p>Shows the system settings, loaded via {@link net.algart.arrays.Arrays.SystemSettings} methods.</p>
 *
 * @author Daniel Alievsky
 */
public class SystemSettingsDemo {
    public static void main(String[] args) {
        System.out.printf("AlgART version: %s%n", Arrays.SystemSettings.version());
        System.out.printf("availableProcessors: %s%n", Arrays.SystemSettings.availableProcessors());
        System.out.printf("cpuCount: %s%n", Arrays.SystemSettings.cpuCount());
        System.out.printf("globalMemoryModel: %s%n", Arrays.SystemSettings.globalMemoryModel());
        System.out.printf("maxTempJavaMemory: %s%n", Arrays.SystemSettings.maxTempJavaMemory());
        System.out.printf("maxMultithreadingMemory: %s%n", Arrays.SystemSettings.maxMultithreadingMemory());
        System.out.printf("isJava32: %s%n", Arrays.SystemSettings.isJava32());
        System.out.printf("profilingMode: %s%n", Arrays.SystemSettings.profilingMode());
    }
}
