package com.opentangerine.watch;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author Grzegorz Gajos
 */
public class WatchTest  {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void whenChangeAppearsTrigger() throws IOException, InterruptedException {
        File temp = folder.newFolder();
        Path file = temp.toPath().resolve("text.txt");
        CountDownLatch done = new CountDownLatch(1);
        FileUtils.writeStringToFile(file.toFile(), "1", StandardCharsets.UTF_8);
        Watch watch = new Watch.Default(temp, e -> {
            assertThat(e.filename(), equalTo("text.txt"));
            done.countDown();
        });
        watch.run();
        FileUtils.writeStringToFile(file.toFile(), "2", StandardCharsets.UTF_8);
        if(!done.await(1, TimeUnit.SECONDS)) {
            assertThat("Change not spotted withing the timeframe", false);
        }
        watch.close();

    }

    // Bomb bomb = new Watch(dir, Watch.FILTER, new Delayed(1 sec, 1 sec, 5 sec)).listen(() => {})
    // bomb.destroy();

    // FIXME GG: in progress, still watching after directory recreation
    // FIXME GG: in progress, register that new file was added
    // FIXME GG: in progress, register that file was edited in new directory
    // FIXME GG: in progress, register that file was deleted along with the different directory
    // FIXME GG: in progress, listen only to the specific filter (lambda)
    // FIXME GG: in progress, listen only to the specific file extensions
    // FIXME GG: in progress, listen wait certain period before notification (when multiple changes are happening)
    // FIXME GG: in progress, do not trigger more often than X seconds
    // FIXME GG: in progress, get rid of all FIXMEs
}