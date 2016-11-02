[![codecov](https://codecov.io/gh/opentangerine/ot-watch/branch/master/graph/badge.svg)](https://codecov.io/gh/opentangerine/ot-watch)

[![Build Status](https://travis-ci.org/opentangerine/ot-watch.svg?branch=master)](https://travis-ci.org/opentangerine/ot-watch)

* Watch is notifying even when directory is not yet created. It is going to wait

## Documentation

In order to start watching for the changes you have to create `new Watch.Default` instance.

```java
{
    // Create sample directory
    final File directory = folder.newFolder();
    // Create sample file
    final Path file = createSampleFile(directory);
    // Create counter in order to watch change in different thread
    final CountDownLatch done = new CountDownLatch(1);
    // watcher in the try-resource clause.
    try (Watch ignored = new Watch.Default(// Path we're monitoring
    directory, // Callback executed for each file change
     change -> {
        assertThat(change.filename(), equalTo(SAMPLE_FILENAME));
        done.countDown();
    }).start().await()) {
        // Change sample file
        changeFile(file);
        // Confirm that change was intercepted done
        if (!done.await(1, TimeUnit.SECONDS)) {
            assertThat("Change not spotted within the timeframe", false);
        }
    }
}
```

Even if directory deleted and created again, it's still listening.

```java
{
    final File directory = folder.newFolder();
    final Path file = createSampleFile(directory);
    final CountDownLatch done = new CountDownLatch(1);
    Watch watch = new Watch.Default(directory,  e -> {
        assertThat(e.filename(), equalTo(SAMPLE_FILENAME));
        done.countDown();
    }).start().await();
    recreate(file);
    if (!done.await(1, TimeUnit.SECONDS)) {
        assertThat("Change not spotted withing the timeframe", false);
    }
    watch.close();
}
```

If directory doesn't exists, it is going to be registered as soon as it is going to be created. Therefore we can listen for Paths which are going to be created in near future.

```java
{
    final CountDownLatch done = new CountDownLatch(1);
    final File temp = folder.newFolder();
    final Path content = temp.toPath().resolve("content");
    Watch watch = new Watch.Default(content.toFile(),  e -> {
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
```

