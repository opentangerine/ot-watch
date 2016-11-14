[![codecov](https://codecov.io/gh/opentangerine/ot-watch/branch/master/graph/badge.svg)](https://codecov.io/gh/opentangerine/ot-watch)

[![Build Status](https://travis-ci.org/opentangerine/ot-watch.svg?branch=master)](https://travis-ci.org/opentangerine/ot-watch)

`ot-watch` - is a library which simplifies the process of monitoring directories.
It is watching dirs recursively and exposes all changes via simple `Change`
interface.

## Usage

Below you can find very simple example how to monitor specific file directory
for the changes. We're going to print all files which changed.

```java
try (Watch watch = new Native(Paths.get("/home"))) {
    for(List<Change.Simple> change : watch.changes()) {
        for(Change.Simple simple : change) {
            System.out.println(simple.filename());
        }
    }
}
```

For the performance reasons all spotted changes are aggregated into the list.

## Build

```
mvn clean install -Pcoverage,qulice
```

## Contribution

If you would like to contribute something to the project, feel free to create
PR or submit a ticket where we can start discussion.