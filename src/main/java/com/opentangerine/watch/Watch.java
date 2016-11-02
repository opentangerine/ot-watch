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
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Each of the method is returning Watch object so it's easy to do
 * chain.
 *
 * @author Grzegorz Gajos
 */
// FIXME GG: in progress, extract interface
public interface Watch extends Closeable {

    Watch start();
    Watch await();

    final class Default implements Watch {
        private final File dir;
        // FIXME GG: in progress, create proper interfaces for the standard i. add @FunctionalInterface
        private Consumer<Change> supplier;
        private boolean running = false;
        private CountDownLatch latch = new CountDownLatch(1);
        private CountDownLatch closed = new CountDownLatch(1);

        public Default(File dir, Consumer<Change> onChange) {
            this.dir = dir;
            this.supplier = onChange;
        }

        @Override
        public Watch start() {
            synchronized (this) {
                if (running) {
                    throw new IllegalStateException("You cannot start watcher multiple times.");
                }
                running = true;
            }
            new Thread(() -> {
                Eye s = new Eye();
                s.registerAll(dir.toPath());
                latch.countDown();
                while (running) {
                    s.accept(supplier);
                }
                closed.countDown();
            }).start();
            return this;
        }

        @Override
        public Watch await() {
            try {
                if (!latch.await(3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Unable to register messages.");
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("Unable to register event service.", e);
            }
            return this;
        }

        @Override
        public void close() throws IOException {
            running = false;
            try {
                closed.await(1L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Unable to close listener.", e);
            }
        }
    }
}

