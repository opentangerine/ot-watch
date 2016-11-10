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
import com.opentangerine.watch.internal.Native;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.lang.CheckedRunnable;
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
@SuppressWarnings("PMD.TooManyMethods")
public final class WatchTest {

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
        final File directory = this.folder.newFolder();
        final Path file = sampleFile(directory);
        try (Watch watch = new Native(directory.toPath())) {
            changeFile(file);
            matchFirst(watch, file);
        }
    }

    /**
     * Even if directory deleted and created again, it's still listening.
     * @throws Exception If fails.
     */
    @Test
    public void notifiesOnDirectoryRecreation() throws Exception {
        final File directory = this.folder.newFolder();
        final Path file = sampleFile(directory);
        try (Watch watch = new Native(directory.toPath())) {
            recreate(file);
            matchFirst(watch, file);
        }
    }

    /**
     * If directory doesn't exists, it is going to be registered as soon
     * as it is going to be created. Therefore we can listen for Paths
     * which are going to be created in near future.
     * @throws Exception If fails.
     */
    @Test
    public void notifiesEvenIfRegisterBeforeDirCreation() throws Exception {
        final File temp = this.folder.newFolder();
        final String dir = "content";
        final Path content = temp.toPath().resolve(dir);
        final Path file = content.resolve("custom.file");
        final CountDownLatch started = new CountDownLatch(1);
        thread(
            () -> {
                FileUtils.forceMkdir(content.toFile());
                started.await();
                changeFile(file);
            }
        );
        try (Watch watch = new Native(content)) {
            started.countDown();
            matchFirst(watch, file);
        }
    }

    /**
     * When new file was added to the monitored directory we want to be notified
     * about it.
     * @throws Exception If fails.
     */
    @Test
    public void notifiesWhenNewFileWasAdded() throws Exception {
        final File directory = this.folder.newFolder();
        try (Watch watch = new Native(directory.toPath())) {
            final Path file = sampleFile(directory);
            FileUtils.touch(file.toFile());
            matchFirst(watch, file);
        }
    }

    /**
     * When file has been deleted then we want to be notified about it.
     * @throws Exception If fails.
     */
    @Test
    public void notifiesWhenExistingFileWasDeleted() throws Exception {
        final File directory = this.folder.newFolder();
        final Path file = sampleFile(directory);
        FileUtils.touch(file.toFile());
        try (Watch watch = new Native(directory.toPath())) {
            file.toFile().delete();
            matchFirst(watch, file);
        }
    }

    /**
     * Asset that first change is related with the specific file.
     * @param watch Watch service.
     * @param file File to check.
     */
    private static void matchFirst(final Watch watch, final Path file) {
        final Iterator<List<Change.Simple>> iter = watch.changes().iterator();
        MatcherAssert.assertThat(
            iter.hasNext(),
            Matchers.is(true)
        );
        MatcherAssert.assertThat(
            iter.next().get(0).filename(),
            Matchers.equalTo(file.toFile().getName())
        );
    }

    /**
     * Execute unchecked runnable.
     * @param runnable Unchecked runnable.
     */
    private static void thread(final CheckedRunnable runnable) {
        new Thread(Unchecked.runnable(runnable)).start();
    }

    /**
     * Change content of the file.
     * @param file File that should be changed.
     * @throws IOException If fails.
     */
    private static void changeFile(final Path file) throws IOException {
        FileUtils.writeStringToFile(
            file.toFile(),
            String.valueOf(System.currentTimeMillis()),
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
        final Path file = directory.toPath().resolve("sample.file");
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
        Await.moment();
        FileUtils.forceMkdir(dir);
        changeFile(file);
    }

}
