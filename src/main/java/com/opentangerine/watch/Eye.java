package com.opentangerine.watch;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Grzegorz Gajos
 */
class Eye {
    private WatchService watcher;

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    void registerAll(final Path start) {
        // register directory and sub-directories
        try {
            watcher = FileSystems.getDefault().newWatchService();
            while(!start.toFile().exists()) {
                // wait
                try {
                    TimeUnit.SECONDS.sleep(1L);
                } catch (InterruptedException e1) {
                    throw new IllegalStateException("Interrupted", e1);
                }
            }
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir1, BasicFileAttributes attrs)
                        throws IOException {
                    dir1.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void accept(Consumer<Change> supplier) {
        try {
            final WatchKey wk = this.watcher.poll(1L, TimeUnit.SECONDS);
            wk.pollEvents().stream()
                    .map(Change.Simple::new)
                    .forEach(it -> supplier.accept(it));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read monitoring events", ex);
        }
    }
}
