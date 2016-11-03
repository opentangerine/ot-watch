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

import org.jooq.lambda.Unchecked;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Each of the method is returning Watch object so it's easy to do
 * chain.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
// FIXME GG: in progress, extract interface
public interface Watch extends Closeable {

    Watch start();
    Watch await();
    Watch onChange(Consumer<Change> supplier);

    final class Native implements Watch {
        private Consumer<Change> supplier = change -> {};
        private boolean running = false;
        private Latch registration = new Latch();
        private Latch disposed = new Latch();
        private final Thread thread;

        public Native(File dir) {
            this.thread = new Thread(
                Unchecked.runnable(() -> {
                    try (Eye eye = new Eye()) {
                        eye.registerAll(dir.toPath());
                        registration.done();
                        while (running) {
                            eye.accept().forEach(it -> this.supplier.accept(it));
                        }
                    }
                    disposed.done();
                })
            );
        }

        @Override
        public Watch start() {
            thread.start();
            this.running = true;
            return this;
        }

        @Override
        public Watch await() {
            registration.await();
            return this;
        }

        @Override
        public Watch onChange(Consumer<Change> supplier) {
            this.supplier = supplier.andThen(supplier);
            return this;
        }

        @Override
        public void close() throws IOException {
            running = false;
            Unchecked.runnable(this.thread::join).run();
        }
    }
}

