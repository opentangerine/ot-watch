package com.opentangerine.watch;

import java.nio.file.WatchEvent;

/**
 * @author Grzegorz Gajos
 */
public interface Change {
    String filename();

    class Simple implements Change {
        private final String filename;

        public Simple(WatchEvent<?> event) {
            this.filename = event.context().toString();
        }

        @Override
        public String filename() {
            return filename;
        }

    }
}
