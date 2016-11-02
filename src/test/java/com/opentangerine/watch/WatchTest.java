/**
 * Copyright (c) since 2012, Open Tangerine (http://opentangerine.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentangerine.watch;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Grzegorz Gajos
 */
public class WatchTest {
    public static final String SAMPLE_FILENAME = "sample.file";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * <readme>
     *     In order to start watching for the changes you have to
     *     create `new Watch.Default` instance.
     * </readme>
     * @throws Exception If test fails.
     */
    @Test
    public void notifiesOnFileContentChange() throws Exception {
        // Create sample directory
        final File directory = folder.newFolder();
        // Create sample file
        final Path file = createSampleFile(directory);
        // Create counter in order to watch change in different thread
        final CountDownLatch done = new CountDownLatch(1);
        // In order to correctly close resources we're going to open
        // watcher in the try-resource clause.
        try (
                Watch ignored = new Watch.Default(
                        // Path we're monitoring
                        directory,
                        // Callback executed for each file change
                        change -> {
                            assertThat(change.filename(), equalTo(SAMPLE_FILENAME));
                            done.countDown();
                        }
                )
                        // Start listening for the changes
                        .start()
                        // Wait till all services are registered properly
                        .await()
        ) {
            // Change sample file
            changeFile(file);
            // Confirm that change was intercepted done
            if (!done.await(1, TimeUnit.SECONDS)) {
                assertThat("Change not spotted within the timeframe", false);
            }
        }
    }

    /**
     * <readme>
     *     Even if directory deleted and created again, it's still listening.
     * </readme>
     * @throws Exception If fails.
     */
    @Test
    public void notifiesOnDirectoryRecreation() throws Exception {
        final File directory = folder.newFolder();
        final Path file = createSampleFile(directory);
        final CountDownLatch done = new CountDownLatch(1);
        Watch watch = new Watch.Default(directory, e -> {
            assertThat(e.filename(), equalTo(SAMPLE_FILENAME));
            done.countDown();
        }).start().await();
        recreate(file);
        if (!done.await(1, TimeUnit.SECONDS)) {
            assertThat("Change not spotted withing the timeframe", false);
        }
        watch.close();
    }

    /**
     * <readme>
     *     If directory doesn't exists, it is going to be registered as soon
     *     as it is going to be created. Therefore we can listen for Paths
     *     which are going to be created in near future.
     * </readme>
     * @throws Exception If fails.
     */
    @Test
    public void notifiesEvenIfRegisterBeforeDirectoryCreation() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final File temp = folder.newFolder();
        final Path content = temp.toPath().resolve("content");
        Watch watch = new Watch.Default(content.toFile(), e -> {
            assertThat(e.filename(), equalTo(SAMPLE_FILENAME));
            done.countDown();
        });
        watch.start();
        FileUtils.forceMkdir(content.toFile());
        Path file = temp.toPath().resolve("content").resolve(SAMPLE_FILENAME);
        watch.await();
        changeFile(file);
        if (!done.await(1, TimeUnit.SECONDS)) {
            assertThat("Change not spotted withing the timeframe", false);
        }
        watch.close();
    }

    /**
     * Change content of the file.
     * @param file File that should be changed.
     * @throws IOException If fails.
     */
    private static void changeFile(Path file) throws IOException {
        FileUtils.writeStringToFile(file.toFile(), UUID.randomUUID().toString(), StandardCharsets.UTF_8);
    }

    /**
     * Create sample file.
     * @param directory Directory where sample file should be created.
     * @return Path of newly created file.
     * @throws IOException If fails.
     */
    private static Path createSampleFile(File directory) throws IOException {
        Path file = directory.toPath().resolve(SAMPLE_FILENAME);
        FileUtils.writeStringToFile(file.toFile(), "1", StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Delete directory along the file and then recreate. We have to sleep between
     * recreations as system doesn't allow to immediately recreate files.
     * @param file Sample file.
     * @throws Exception If fails.
     */
    private static void recreate(Path file) throws Exception {
        final File directory = file.getParent().toFile();
        FileUtils.cleanDirectory(directory);
        FileUtils.deleteDirectory(directory);
        TimeUnit.MILLISECONDS.sleep(500);
        FileUtils.forceMkdir(directory);
        changeFile(file);
    }

    // FIXME GG: in progress, Watch, even if directory doesn't exists.
    // FIXME GG: in progress, still watching after directory recreation
    // FIXME GG: in progress, register that new file was added
    // FIXME GG: in progress, register that file was edited in new directory
    // FIXME GG: in progress, register that file was deleted along with the different directory
    // FIXME GG: in progress, listen only to the specific filter (lambda)
    // FIXME GG: in progress, listen only to the specific file extensions
    // FIXME GG: in progress, listen wait certain period before notification (when multiple changes are happening)
    // FIXME GG: in progress, do not trigger more often than X seconds
    // FIXME GG: in progress, get rid of all FIXMEs
    // FIXME GG: in progress, create documentation
    // FIXME GG: in progress, 100% CC
    // FIXME GG: in progress, static analysis
}