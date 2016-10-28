[![codecov](https://codecov.io/gh/opentangerine/ot-watch/branch/master/graph/badge.svg)](https://codecov.io/gh/opentangerine/ot-watch)

[![Build Status](https://travis-ci.org/opentangerine/ot-watch.svg?branch=master)](https://travis-ci.org/opentangerine/ot-watch)

* Watch is notifying even when directory is not yet created. It is going to wait

## Documentation

Below you can see test case. Second line.

```
{
    File temp = folder.newFolder();
    Path file = temp.toPath().resolve("text.txt");
    CountDownLatch done = new CountDownLatch(1);
    FileUtils.writeStringToFile(file.toFile(), "1", StandardCharsets.UTF_8);
    Watch watch = new Watch.Default(temp,  e -> {
        assertThat(e.filename(), equalTo("text.txt"));
        done.countDown();
    });
    watch.run();
    FileUtils.writeStringToFile(file.toFile(), "2", StandardCharsets.UTF_8);
    if (!done.await(1, TimeUnit.SECONDS)) {
        assertThat("Change not spotted withing the timeframe", false);
    }
    watch.close();
}```

