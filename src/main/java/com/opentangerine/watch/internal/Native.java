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

import com.opentangerine.watch.Change;
import com.opentangerine.watch.Watch;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.jooq.lambda.Unchecked;

/**
 * Native implementation of Watch which is using WatchEvents internally.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
public final class Native implements Watch {
    /**
     * Check when services are registered.
     */
    private final Latch registration;
    /**
     * Background thread for watching purposes.
     */
    private final Thread thread;
    /**
     * Notification function.
     */
    private Consumer<Change> notify;
    /**
     * True if watch is running.
     */
    private boolean running;

    /**
     * Ctor.
     * @param dir Directory to watch.
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public Native(final Path dir) {
        final Latch disposed = new Latch();
        this.registration = new Latch();
        this.notify = it -> { };
        this.thread = new Thread(
            Unchecked.runnable(
                () -> {
                    try (Eye eye = new Eye()) {
                        eye.register(dir);
                        this.registration.done();
                        while (this.running) {
                            eye.accept().forEach(it -> this.notify.accept(it));
                        }
                    }
                    disposed.done();
                }
            )
        );
    }

    @Override
    public Watch start() {
        this.thread.start();
        this.running = true;
        return this;
    }

    @Override
    public Watch await() {
        this.registration.await();
        return this;
    }

    @Override
    public Watch listen(final Consumer<Change> change) {
        this.notify = change.andThen(change);
        return this;
    }

    @Override
    public void close() throws IOException {
        this.running = false;
        Unchecked.runnable(this.thread::join).run();
    }
}
