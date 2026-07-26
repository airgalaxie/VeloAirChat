# Contributing to VeloAirChat

Thank you for helping improve VeloAirChat.

## Before you start

* Use an issue or discussion for substantial changes before investing in an
  implementation.
* Keep Protocol v4 and the central Velocity authority model in mind. Backend
  bridges observe input and deliver rendered output; they do not own identity,
  trust, routing or formatting decisions.
* Keep changes focused and avoid unrelated formatting rewrites.

The architecture contract and currently known limitations are documented in
[PORTING_STATUS.md](PORTING_STATUS.md).

## Development setup

VeloAirChat requires JDK 25. Use the checked-in Gradle wrapper:

```bash
./gradlew clean build
```

Installable test artifacts are collected in `target/`. Build directories and
release artifacts are intentionally excluded from Git.

## Pull requests

Before opening a pull request:

1. Run `./gradlew clean build`.
2. Add or update tests for behavior changes.
3. Update the relevant documentation.
4. Confirm that no generated files, IDE settings, credentials or server logs
   are included.
5. Describe runtime compatibility and any testing that could not be performed.

Java source files must retain the project license header. The build checks
headers automatically.

## Translations

See [docs/Translations.md](docs/Translations.md). Locale keys, placeholders,
commands, colors and MiniMessage formatting must remain compatible with the
English source locale.

## Licensing

By contributing, you agree that your contribution is licensed under the
project's [Apache License 2.0](LICENSE).
