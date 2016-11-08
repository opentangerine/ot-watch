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
     * Callback with the filechange payload.
     */
    private Consumer<Change> onchange;
    /**
     * Callback with the exception payload.
     */
    private Consumer<Exception> onerror;

    /**
     * Ctor.
     * @param dir Directory to watch.
     * @checkstyle IllegalCatchCheck (50 lines)
     */
    @SuppressWarnings(
        {
            "PMD.ConstructorOnlyInitializesOrCallOtherConstructors",
            "PMD.AvoidCatchingGenericException"
        }
    )
    public Native(final Path dir) {
        final Latch disposed = new Latch();
        this.registration = new Latch();
        this.onchange = it -> { };
        this.onerror = it -> {
            throw new IllegalStateException(
                "No `.error` handler is provided",
                it
            );
        };
        this.thread = new Thread(
            () -> {
                try (Eye eye = new Eye()) {
                    eye.register(dir);
                    this.registration.done();
                    while (!Thread.interrupted()) {
                        eye.accept().forEach(it -> this.onchange.accept(it));
                    }
                } catch (final InterruptedException inter) {
                } catch (final Exception exc) {
                    this.onerror.accept(exc);
                }
                disposed.done();
            }
        );
    }

    @Override
    public Watch start() {
        this.thread.start();
        return this;
    }

    @Override
    public Watch await() {
        this.registration.await();
        return this;
    }

    @Override
    public Watch change(final Consumer<Change> callback) {
        this.onchange = callback;
        return this;
    }

    @Override
    public Watch error(final Consumer<Exception> callback) {
        this.onerror = callback;
        return this;
    }

    @Override
    public void close() throws IOException {
        this.thread.interrupt();
        Unchecked.runnable(this.thread::join).run();
    }
}
