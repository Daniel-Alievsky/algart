package net.algart.arrays.demo;

import static net.algart.arrays.Arrays.SystemSettings.*;

/**
 * <p>Shows the system settings, loaded via {@link net.algart.arrays.Arrays.SystemSettings} methods.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
