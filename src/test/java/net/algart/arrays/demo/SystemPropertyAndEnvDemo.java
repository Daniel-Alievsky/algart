/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import static net.algart.arrays.Arrays.SystemSettings.*;

/**
 * <p>Shows the system settings, loaded via {@link net.algart.arrays.Arrays.SystemSettings} methods.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class SystemPropertyAndEnvDemo {
    static final String PROPERTY_NAME = "net.algart.arrays.demo.test";
    static final String ENV_VAR_NAME = "NET_ALGART_ARRAYS_DEMO_TEST";
    public static void main(String[] args) {
        System.out.printf("Reading system property \"%s\"...%n", PROPERTY_NAME);
        System.out.printf("  getStringProperty:  %s%n", getStringProperty(PROPERTY_NAME, "def"));
        System.out.printf("  getIntProperty:     %s%n", getIntProperty(PROPERTY_NAME, 157));
        System.out.printf("  getLongProperty:    %s%n", getLongProperty(PROPERTY_NAME, 157));
        System.out.printf("  getBooleanProperty: %s (default: true)%n", getBooleanProperty(PROPERTY_NAME, true));
        System.out.printf("  getBooleanProperty: %s (default: false)%n", getBooleanProperty(PROPERTY_NAME, false));
        System.out.printf("Reading environment variable \"%s\"...%n", ENV_VAR_NAME);
        System.out.printf("  getStringEnv:  %s%n", getStringEnv(ENV_VAR_NAME, "def"));
        System.out.printf("  getIntEnv:     %s%n", getIntEnv(ENV_VAR_NAME, 157));
        System.out.printf("  getLongEnv:    %s%n", getLongEnv(ENV_VAR_NAME, 157));
        System.out.printf("  getBooleanEnv: %s (default: true)%n", getBooleanEnv(ENV_VAR_NAME, true));
        System.out.printf("  getBooleanEnv: %s (default: false)%n", getBooleanEnv(ENV_VAR_NAME, false));
    }
}
