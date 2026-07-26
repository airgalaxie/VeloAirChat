VeloAirChat supports a number of community-sourced translations of the plugin locales into different languages. The default language is `en-gb` (English). Message files are formatted using MiniMessage, with legacy formatting compatibility.

You can change which preset language option to use by changing the top-level `language` setting in the plugin `config.yml` file. At runtime, VeloAirChat reads and creates files named `messages-<locale>.yml` in its plugin data directory. Bundled source translations are located in `common/src/main/resources/locales`.

## Contributing Locales
You can contribute locales by submitting a pull request with a yaml file containing translations of the default locale into your language. Here's a few pointers for doing this:
* Do not translate the locale keys themselves (e.g. `channel_switched`)
* Add the translation to `common/src/main/resources/locales` using the `messages-<locale>.yml` naming scheme
* Do not translate MiniMessage tags, placeholders, commands, or command parameters; only translate the interface text
* Keep the same YAML structure, key order and placeholders as `en-gb.yml`
* Use the correct ISO 639-1 [locale code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) for your language and dialect
* If translator credits are restored in this fork, locale contributors can be credited there

Thank you for your interest in making VeloAirChat more accessible around the world!
