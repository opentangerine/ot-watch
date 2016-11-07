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

import com.opentangerine.watch.internal.Await;
import com.opentangerine.watch.internal.Latch;
import com.opentangerine.watch.internal.Native;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.util.function.CheckedConsumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link Watch}.
 *
 * @author Grzegorz Gajos (grzegorz.gajos@opentangerine.com)
 * @version $Id$
 * @since 1.0
 */
public final class WatchTest {
    /**
     * Name of the sample file.
     */
    private static final String SAMPLE = "sample.file";

    /**
     * Create temp directory for each test.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Basic example how to use Watch.Native.
     * @throws Exception If test fails.
     */
    @Test
    public void notifiesOnFileContentChange() throws Exception {
        this.withLatch(
            changed -> {
                final File directory = this.folder.newFolder();
                final Path file = sampleFile(directory);
                try (Watch watch = new Native(directory.toPath())) {
                    watch.start().await().listen(
                        change -> {
                            MatcherAssert.assertThat(
                                change.filename(),
                                Matchers.equalTo(file.toFile().getName())
                            );
                            changed.done();
                        }
                    );
                    changeFile(file);
                }
            }
        );
    }

    /**
     * Even if directory deleted and created again, it's still listening.
     * @throws Exception If fails.
     */
    @Test
    public void notifiesOnDirectoryRecreation() throws Exception {
        this.withLatch(
            changed -> {
                final File directory = this.folder.newFolder();
                final Path file = sampleFile(directory);
                try (Watch watch = new Native(directory.toPath())) {
                    watch.start().await().listen(
                        change -> {
                            MatcherAssert.assertThat(
                                change.filename(),
                                Matchers.equalTo(file.toFile().getName())
                            );
                            changed.done();
                        }
                    );
                    recreate(file);
                }
            }
        );
    }

    /**
     * If directory doesn't exists, it is going to be registered as soon
     * as it is going to be created. Therefore we can listen for Paths
     * which are going to be created in near future.
     * @throws Exception If fails.
     */
    @Test
    public void notifiesEvenIfRegisterBeforeDirCreation() throws Exception {
        this.withLatch(
            changed -> {
                final File temp = this.folder.newFolder();
                final String dir = "content";
                final Path content = temp.toPath().resolve(dir);
                try (Watch watch = new Native(content)) {
                    watch.start().listen(
                        change -> {
                            MatcherAssert.assertThat(
                                change.filename(),
                                Matchers.equalTo(WatchTest.SAMPLE)
                            );
                            changed.done();
                        }
                    );
                    FileUtils.forceMkdir(content.toFile());
                    final Path file = temp.toPath()
                        .resolve(dir).resolve(WatchTest.SAMPLE);
                    watch.await();
                    changeFile(file);
                }
            }
        );
    }

    /**
     * Run the consumer and wait for the latch to be completed.
     *
     * @param consumer Function that should mark the latch as done.
     */
    private static void withLatch(final CheckedConsumer<Latch> consumer) {
        final Latch done = new Latch();
        Unchecked.consumer(consumer).accept(done);
        done.await();
    }

    /**
     * Change content of the file.
     * @param file File that should be changed.
     * @throws IOException If fails.
     */
    private static void changeFile(final Path file) throws IOException {
        FileUtils.writeStringToFile(
            file.toFile(),
            UUID.randomUUID().toString(),
            StandardCharsets.UTF_8
        );
    }

    /**
     * Create sample file.
     * @param directory Directory where sample file should be created.
     * @return Path of newly created file.
     * @throws IOException If fails.
     */
    private static Path sampleFile(final File directory) throws IOException {
        final Path file = directory.toPath().resolve(WatchTest.SAMPLE);
        FileUtils.writeStringToFile(file.toFile(), "1", StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Delete directory along the file and then recreate. We have to sleep
     * between recreations as system doesn't allow to immediately recreate
     * files.
     * @param file Sample file.
     * @throws Exception If fails.
     */
    private static void recreate(final Path file) throws Exception {
        final File dir = file.getParent().toFile();
        FileUtils.cleanDirectory(dir);
        FileUtils.deleteDirectory(dir);
        final int retries = 10;
        for (int retry = 0; retry < retries || !dir.exists(); retry += 1) {
            Await.moment();
            FileUtils.forceMkdir(dir);
        }
        changeFile(file);
    }

}
