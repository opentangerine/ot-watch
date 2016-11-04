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
package com.opentangerine.watch.internal;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jooq.lambda.Unchecked;

/**
 * Helper methods to simplify waiting operations.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
public interface Await {
    /**
     * Default amount of milliseconds for the 'moment'.
     */
    long MOMENT = 250L;
    /**
     * Default unit of waiting.
     */
    TimeUnit UNIT = TimeUnit.MILLISECONDS;

    /**
     * Block the current thread while condition is true.
     * @param condition Condition.
     */
    static void whileTrue(Supplier<Boolean> condition) {
        while (condition.get()) {
            Await.moment();
        }
    }

    /**
     * Block current thread using default amount of time.
     */
    static void moment() {
        Unchecked.runnable(() -> UNIT.sleep(MOMENT)).run();
    }
}
