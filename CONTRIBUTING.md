# Contributing

## Build

You need JDK 21 and Gradle 8.10 (the GitHub Actions install it; locally you can use the same versions).

```bash
gradle test --stacktrace
gradle build --stacktrace
```

`test` is a plain JUnit suite: scan radius, farm flood-fill, inventories, sorcerer weights, farmer task order. It does not start Minecraft.

`build` downloads NeoForge, Minecraft, Kotlin for Forge, Ribbits, GeckoLib and YUNG's API, then produces `build/libs/reallyusefulribbits-*.jar`.

The repository does not commit `gradle-wrapper.jar`. CI uses `gradle/actions/setup-gradle` with Gradle 8.10. If you generate the wrapper locally and commit the jar, you can switch the workflows to `./gradlew`.

## Layout

- `src/main/kotlin/com/reallyusefulribbits/mod/logic` — rules that JUnit can run without the game
- `src/main/kotlin/com/reallyusefulribbits/mod` — NeoForge / Ribbits wiring
- `src/main/java/com/reallyusefulribbits/mod/mixin` — mixins into Ribbits and the player
- `src/test/kotlin` — JUnit 5

## Config

Gameplay radius is a NeoForge `SERVER` config (`ServerConfig`). In multiplayer only the server file matters. See the README.
