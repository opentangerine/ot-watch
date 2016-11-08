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
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Facade around {@link WatchService}.
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
final class Eye implements Closeable {
    /**
     * Native service instance.
     */
    private WatchService watcher;

    @Override
    public void close() throws IOException {
        if (this.watcher != null) {
            this.watcher.close();
        }
    }

    /**
     * Register the given directory, and all its sub-directories with the
     * {@link WatchService}.
     * @param dir Directory to watch.
     * @throws IOException When unable to register the service.
     */
    public void register(final Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        Await.whileTrue(() -> !dir.toFile().exists());
        Files.walkFileTree(
            dir,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(
                    final Path directory,
                    final BasicFileAttributes attrs
                )
                    throws IOException {
                    directory.register(
                        Eye.this.watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                    );
                    return FileVisitResult.CONTINUE;
                }
            }
        );
    }

    /**
     * Poll events from the watcher service and return stream of
     * new events (if any).
     * @return Simple changes stream.
     * @throws InterruptedException On interruption.
     */
    public Stream<Change.Simple> accept() throws InterruptedException {
        final WatchKey key = this.watcher.poll(Await.MOMENT, Await.UNIT);
        return Optional.ofNullable(key)
            .map(wk -> wk.pollEvents().stream().map(Change.Simple::new))
            .orElse(Stream.empty());
    }
}

