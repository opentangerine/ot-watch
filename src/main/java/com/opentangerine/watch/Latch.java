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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jooq.lambda.Unchecked;

/**
 * Facade around the {@link CountDownLatch}.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
public final class Latch {
    /**
     * Default number of seconds to wait for the latch before giving up.
     */
    private static final int DEFAULT = 3;

    /**
     * Internal latch.
     */
    private CountDownLatch internal = new CountDownLatch(1);

    /**
     * Mark internal latch as done.
     */
    void done() {
        this.internal.countDown();
    }

    /**
     * Await default amount of time or throw Timeout exception.
     */
    void await() {
        Unchecked.runnable(
            () -> {
                if (!this.internal.await(DEFAULT, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout");
                }
            }
        ).run();
    }

}

