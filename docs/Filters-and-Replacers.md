The `filtered:` property of a channel lets you specify whether a message sent to a channel should be filtered first by the enabled filters and message replacers defined in the `chat_filters` and `message_replacers` section in the dedicated [`filters.yml`](Config-Files.md#filtersyml) file. To use a filter, ensure the channel you want to be filtered has `filtered` enabled and that the chat filters are correctly enabled and configured.

## Replacer
Message replacers will alter the contents of the message, such as by replacing certain character combinations with emoji.

* `emoji_replacer` - Replaces certain character strings with the correct Unicode emoji. Note that if you have the `ascii_filter` enabled, this will still work and display unicode emoji characters in chat.

## Filters
Chat filters will prevent a user from sending a message based on certain conditions.

* `advertising_filter` - Prevents players from sending messages that contain IP or web addresses.
* `caps_filter` - Prevents players from sending messages that are comprised of over a certain specifiable percentage (as a decimal number, 0.0 to 1.0 representing 0% to 100%)
* `spam_filter` - Prevents players from sending messages too fast in chat (i.e. rate limits them). Specify how many messages players should be able to send in a period.
* `repeat_filter` - Prevents players from sending repeat messages. Checks against a specifiable number of the players previous messages.
* `ascii_filter` - Prevents players from using non-ASCII (i.e. Unicode/UTF-8) characters in chat. If members of your server need to use non-latin characters when talking in your community's language, you probably want to turn this off.
* `regex_filter` - Prevents players from sending messages that match configured regular expressions. Use this for local word lists or project-specific moderation rules.

### Bypassing filters
You can use the `veloairchat.bypass_filters` permission to allow a user's messages to not be run through the filters (although messages will still be run through replacers).

In addition, you can use the `veloairchat.ignore_filters.<filter_name>` node to let users bypass specific filters. The bypass will work in all channels.
* `veloairchat.ignore_filters.advertising` - Advertising filter
* `veloairchat.ignore_filters.caps` - Caps filter
* `veloairchat.ignore_filters.spam` - Spam filter
* `veloairchat.ignore_filters.repeat` - Repeat messages filter
* `veloairchat.ignore_filters.ascii` - ASCII filter
* `veloairchat.ignore_filters.regex` - Regex filter

You can also disable individual types of replacers with the following permissions:
* `veloairchat.ignore_filters.emoji_replacer` - Emoji replacer

## Local Word Filtering
For project-specific moderation, use the `regex_filter` with configured patterns. This keeps filtering local to the proxy and avoids native dependencies.
