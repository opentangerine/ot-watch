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
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Facade around {@link WatchService}.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
class Eye implements Closeable {
    /**
     * Native service instance.
     */
    private WatchService watcher;

    /**
     * Register the given directory, and all its sub-directories with the
     * {@link WatchService}.
     */
    void registerAll(final Path start) {
        Unchecked.runnable(() -> {
            watcher = FileSystems.getDefault().newWatchService();
            Await.on(() -> start.toFile().exists());
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs)
                        throws IOException {
                    directory.register(
                            watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                    );
                    return FileVisitResult.CONTINUE;
                }
            });
        }).run();
    }

    /**
     * Poll events from the watcher service and return stream of
     * new events (if any).
     *
     * @return Simple changes stream.
     */
    Stream<Change.Simple> accept() {
        return Optional.ofNullable(Unchecked.supplier(() -> this.watcher.poll(Await.MOMENT, Await.UNIT)).get())
                .map(wk -> wk.pollEvents().stream().map(Change.Simple::new))
                .orElse(Stream.empty());
    }

    @Override
    public void close() throws IOException {
        this.watcher.close();
    }
}

