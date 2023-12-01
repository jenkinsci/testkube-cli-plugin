package io.testkube.plugins.api.manager;

public class Utils {

    public static void checkNotNull(String name, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(name + "cannot be null");
        }
    }
}
