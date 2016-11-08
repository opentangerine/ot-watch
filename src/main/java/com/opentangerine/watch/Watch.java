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

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Watch directory for changes.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
public interface Watch extends Closeable {
    /**
     * Start watching (can be used only one time).
     * @return Self.
     */
    Watch start();
    /**
     * Wait for successful filesystem events registration.
     * @return Self.
     */
    Watch await();

    /**
     * Overrides callback executed on file change.
     * @param change Consumer executed on change.
     * @return Self.
     */
    Watch change(Consumer<Change> change);

    /**
     * Overrides callback executed in exceptional case.
     * @param callback Consumer executed on exceptional state.
     * @return Self.
     */
    Watch error(Consumer<Exception> callback);

}

