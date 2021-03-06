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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Native implementation of Watch which is using WatchEvents internally.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
public final class Native implements Watch {
    /**
     * Native service instance.
     */
    private final WatchService watcher;

    /**
     * Ctor.
     * @param dir Directory to watch.
     * @throws IOException On error.
     */
    @SuppressWarnings(
        {
            "PMD.ConstructorOnlyInitializesOrCallOtherConstructors",
            "PMD.AvoidCatchingGenericException"
        }
    )
    public Native(final Path dir) throws IOException {
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
                        Native.this.watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                    );
                    return FileVisitResult.CONTINUE;
                }
            }
        );
    }

    @Override
    public void close() throws IOException {
        this.watcher.close();
    }

    @Override
    public Iterable<List<Change.Simple>> changes() {
        return () -> new Iterator<List<Change.Simple>>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public List<Change.Simple> next() {
                while (true) {
                    final List<Change.Simple> pack = Native.this.accept();
                    if (!pack.isEmpty()) {
                        return pack;
                    }
                    Await.moment();
                }
            }
        };
    }

    /**
     * Poll events from the watcher service and return stream of
     * new events (if any). Returning immediately if no changes.
     * @return Simple changes stream.
     */
    private List<Change.Simple> accept() {
        final WatchKey key = this.watcher.poll();
        return Optional.ofNullable(key)
            .map(wk -> wk.pollEvents().stream().map(Change.Simple::new))
            .orElse(Stream.empty())
            .collect(Collectors.toList());
    }
}
