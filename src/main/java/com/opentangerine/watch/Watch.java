package com.opentangerine.watch;

import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Each of the method is returning Watch object so it's easy to do
 * chain.
 *
 * @author Grzegorz Gajos
 */
// FIXME GG: in progress, extract interface
public interface Watch extends Closeable, Runnable {

    final class Default implements Watch {
        private final File dir;
        private Consumer<Change> supplier = event -> {};
        private boolean running = false;
        private WatchService watchService;

        public Default(File dir, Consumer<Change> onChange) {
            this.dir = dir;
            this.supplier = onChange;
        }

        /**
         * Register the given directory, and all its sub-directories, with the WatchService.
         */
        private void registerAll(final Path start, final WatchService watcher) throws IOException {
            // register directory and sub-directories

            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        @Override
        public void run() {
            synchronized (this) {
                if (running) {
                    throw new IllegalStateException("You cannot start watcher multiple times.");
                }
                running = true;
            }
            try {
                this.watchService = FileSystems.getDefault().newWatchService();
                registerAll(dir.toPath(), this.watchService);
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(() -> {
                try {
                    while (running) {
                        // wait for key to be signaled
                        final WatchKey wk = this.watchService.take();
                        wk.pollEvents().stream()
                                .map(Change.Simple::new)
                                .forEach(it -> this.supplier.accept(it));
                        // reset the key
                        boolean valid = wk.reset();
                        if (!valid) {
                            throw new IllegalStateException("The key has been unregistered");
                        }
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        }

        @Override
        public void close() throws IOException {
            running = false;
        }
    }


}
