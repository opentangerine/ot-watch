/**
 * Copyright (c) since 2012, Open Tangerine (http://opentangerine.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentangerine.watch;

import java.nio.file.WatchEvent;

/**
 * Change interface is able to understand disk changes from the system.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
public interface Change {

    /**
     * Filename.
     * @return Name of the file that changed.
     */
    String filename();

    /**
     * Simple implementation of the change interface that is able to read
     * basic information from {@link WatchEvent}.
     */
    class Simple implements Change {
        /**
         * Source event.
         */
        private final WatchEvent<?> source;

        /**
         * Ctor.
         * @param event Source event.
         */
        public Simple(final WatchEvent<?> event) {
            this.source = event;
        }

        @Override
        public String filename() {
            return this.source.toString();
        }

    }
}
