## Runtime Prerequisites
Processing libraries:

- `core.jar`
- `gluegen-rt.jar`
- `jogl-all.jar`

and serial libraries

- `jssc.jar`
- `serial.jar`

which all are accompanied by processing installation.

## Development Prerequisites
- meson >= 1.1, < 2.0
- ninja

## Compilation

```
meson setup build/ -Dprocessing_path="$HOME/.local/share/processing-4.3/"
ninja -C build/
```

to get `Bno055Test.jar`.

## Run in Development Environment

```
meson devenv -C build java Bno055Test
```

## Run in Production
Get `Bno055Test.jar` then invoke:

```
java -cp "${PATH_TO_PROCESSING}"'/core/library/*:'"${PATH_TO_PROCESSING}"'/modes/java/libraries/serial/library/*:Bno055Test.jar' Bno055Test
```

### TODO
Make the jar file portable.
