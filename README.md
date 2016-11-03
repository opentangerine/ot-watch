[![codecov](https://codecov.io/gh/opentangerine/ot-watch/branch/master/graph/badge.svg)](https://codecov.io/gh/opentangerine/ot-watch)

[![Build Status](https://travis-ci.org/opentangerine/ot-watch.svg?branch=master)](https://travis-ci.org/opentangerine/ot-watch)

* Watch is notifying even when directory is not yet created. It is going to wait

## Documentation

In order to start watching for the changes you have to create `new Watch.Default` instance.

```java
{
    final File directory = folder.newFolder();
    final Path file = createSampleFile(directory);
    final CountDownLatch done = new CountDownLatch(1);
    try (Watch watch = new Watch.Native(directory)) {
        watch.start().await().onChange( change -> {
            assertThat(change.filename(), equalTo(SAMPLE_FILENAME));
            done.countDown();
        });
        changeFile(file);
        if (!done.await(3, TimeUnit.SECONDS)) {
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
    try (Watch watch = new Watch.Native(directory)) {
        watch.start().await().onChange( e -> {
            assertThat(e.filename(), equalTo(SAMPLE_FILENAME));
            done.countDown();
        });
        recreate(file);
        if (!done.await(1, TimeUnit.SECONDS)) {
            assertThat("Change not spotted withing the timeframe", false);
        }
    }
}
```

If directory doesn't exists, it is going to be registered as soon as it is going to be created. Therefore we can listen for Paths which are going to be created in near future.

```java
{
    final CountDownLatch done = new CountDownLatch(1);
    final File temp = folder.newFolder();
    final Path content = temp.toPath().resolve("content");
    try (Watch watch = new Watch.Native(content.toFile())) {
        watch.start().onChange( e -> {
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
```

