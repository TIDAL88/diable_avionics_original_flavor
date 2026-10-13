# Build helpers

`Diable-helpers.jar` preserves the five compiled utility classes recovered unchanged
from `jars/Diable.jar` at Git commit `39cac413b3de97dee8d3197a74123d87abc66258`:

- `data.scripts.util.MiscUtilsKt`
- `data.scripts.util.ReflectionUtils` and its three nested classes

Their source files are absent from this repository. Add this helper JAR as a
Compile dependency, and extract its contents into the Diable artifact alongside
the module output. The local IntelliJ library named `Diable` now points here.
The game continues to load only `jars/Diable.jar`; the helpers are included in it.

Do not use the output `jars/Diable.jar` as a compilation dependency: rebuilding
that artifact can erase the only copy of classes that have no source.
