package com.opentangerine.watch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
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
        // FIXME GG: in progress, create proper interfaces for the standard i. add @FunctionalInterface
        private Consumer<Change> supplier;
        private Runnable onSuccess = () -> {};
        private boolean running = false;
        private WatchService watchService;

        public Default(File dir, Consumer<Change> onChange) {
            this(dir, onChange, () -> {});
        }

        public Default(File dir, Consumer<Change> onChange, Runnable onSuccess) {
            this.dir = dir;
            this.supplier = onChange;
            this.onSuccess = onSuccess;
        }

        /**
         * Register the given directory, and all its sub-directories, with the WatchService.
         */
        private void registerAll(final Path start, final WatchService watcher) {
            // register directory and sub-directories
            try {
                System.out.println("HERE");
                while(!start.toFile().exists()) {
                    System.out.println("Unable to register " + start + " retry." + start.toFile().isDirectory());
                    // wait
                    try {
                        TimeUnit.SECONDS.sleep(1L);
                    } catch (InterruptedException e1) {
                        throw new IllegalStateException("Interrupted", e1);
                    }
                    System.out.println("HERE 2");
                }
                Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir1, BasicFileAttributes attrs)
                            throws IOException {
                        dir1.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                        return FileVisitResult.CONTINUE;
                    }
                });
                this.onSuccess.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            synchronized (this) {
                if (running) {
                    throw new IllegalStateException("You cannot start watcher multiple times.");
                }
                running = true;
            }
            new Thread(() -> {
                try {
                    try {
                        this.watchService = FileSystems.getDefault().newWatchService();
                        registerAll(dir.toPath(), this.watchService);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    while (running) {
                        // wait for key to be signaled
                        final WatchKey wk = this.watchService.take();
                        wk.pollEvents().stream()
                                .map(Change.Simple::new)
                                .forEach(it -> this.supplier.accept(it));
                        System.out.println("VALIE: " + wk.reset());
                        // reset the key
//                        boolean valid = wk.reset();
//                        if (!valid) {
                            //throw new IllegalStateException("The key has been unregistered");
//                        }
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
