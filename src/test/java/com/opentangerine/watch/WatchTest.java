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

import com.opentangerine.watch.internal.Native;
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
     *     Basic example how to use Watch.Native.
     * </readme>
     * @throws Exception If test fails.
     */
    @Test
    public void notifiesOnFileContentChange() throws Exception {
        final File directory = folder.newFolder();
        final Path file = createSampleFile(directory);
        final CountDownLatch done = new CountDownLatch(1);
        try (Watch watch = new Native(directory.toPath())) {
            watch.start().await().listen(
                    change -> {
                        assertThat(change.filename(), equalTo(SAMPLE_FILENAME));
                        done.countDown();
                    }
            );
            changeFile(file);
            if (!done.await(3, TimeUnit.SECONDS)) {
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
        try (Watch watch = new Native(directory.toPath())) {
            watch.start().await().listen(e -> {
                assertThat(e.filename(), equalTo(SAMPLE_FILENAME));
                done.countDown();
            });
            recreate(file);
            if (!done.await(1, TimeUnit.SECONDS)) {
                assertThat("Change not spotted withing the timeframe", false);
            }
        }
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
        try (Watch watch = new Native(content)) {
            watch.start().listen(e -> {
                assertThat(e.filename(), equalTo(SAMPLE_FILENAME));
                done.countDown();
            });
            FileUtils.forceMkdir(content.toFile());
            Path file = temp.toPath().resolve("content").resolve(SAMPLE_FILENAME);
            watch.await();
            changeFile(file);
            if (!done.await(1, TimeUnit.SECONDS)) {
                assertThat("Change not spotted withing the timeframe", false);
            }
        }
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
        for(int i=0; i<10 || !directory.exists(); i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                FileUtils.forceMkdir(directory);
            } catch (Exception ex) {
                // Let's try again
            }
        }
        changeFile(file);
    }

    // FIXME GG: in progress, add Objects.require etc.
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
    // FIXME GG: in progress, test exception on double start
}