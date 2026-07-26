The channel `format` defines the complete rendered channel message. If it does
not contain `<message>` or `%message%`, VeloAirChat appends the message for
compatibility with existing prefix-only configurations. Private messages,
group private messages, broadcasts, join messages, quit messages, social spy
and local spy retain their existing specialized formats.

## MiniMessage
Formats use Adventure MiniMessage. Legacy `&` color codes and the old bracket color syntax are still accepted for compatibility, but new configs should use MiniMessage directly.

Examples:

```yaml
format: '<#00fb9a>[G]<reset><dark_gray>[</dark_gray><gray>%server%</gray><dark_gray>]</dark_gray><white> %fullname%<reset><white>: '
format: '<yellow>[Staff]</yellow> %name%: <gray>'
```

## Built-In Placeholders
These placeholders are resolved by VeloAirChat and do not require MiniPlaceholders.

Channel chat and integration formats are rendered from one platform-neutral
`ChatContext`. Minecraft chat and Dynmap therefore have the same information
available. Angle-bracket names are canonical; the historical percent forms
remain available for compatibility.

### Canonical ChatContext
* `<platform>` - platform label (`[J]` or `[B]` for backend bridge chat)
* `<scope>` - compact channel scope (`G` or `L`)
* `<server>` - configured display name of the source server
* `<backend>` - technical source backend name
* `<player>` - Minecraft account name
* `<playerUuid>` - Minecraft player UUID
* `<displayName>` - prefix, player name and suffix
* `<prefix>`, `<suffix>` - LuckPerms prefix and suffix
* `<role>`, `<roleDisplayName>` - LuckPerms group name and display name
* `<message>` - filtered chat message
* `<originalMessage>` - original message observed by the signed-chat adapter
* `<channel>` - channel ID
* `<identityProvider>` - resolved identity provider
* `<signedState>`, `<trustState>`, `<signedAdapter>`, `<signedTimestamp>`
* `<timestamp>` - message timestamp in epoch milliseconds
* `<messageId>` - VeloAirChat message ID
* `<ping>`, `<localPlayersOnline>`
* `<currentTime>`, `<currentTimeShort>`, `<currentDate>`, `<currentDateUk>`,
  `<currentDateDay>`, `<currentMonth>`, `<currentYear>`

Every canonical name also works as a percent placeholder, for example
`%player%`. Snake-case forms such as `<display_name>`, `<identity_provider>`
and `<signed_state>` are aliases.

For historical channel formats without `<message>` or `%message%`, the message
is appended automatically. New configurations can place `<message>` anywhere
in the format.

### Player
* `%name%`, `%username%` - username
* `%full_name%`, `%fullname%` - LuckPerms prefix, username, and LuckPerms suffix
* `%prefix%`, `%role_prefix%`, `%roleprefix%` - LuckPerms prefix
* `%suffix%`, `%role_suffix%`, `%rolesuffix%` - LuckPerms suffix
* `%role%`, `%role_name%`, `%rolename%` - primary group name
* `%role_display_name%`, `%roledisplayname%` - primary group display name
* `%ping%` - player ping
* `%uuid%` - player UUID
* `%server%`, `%server_name%`, `%servername%` - current server name
* `%local_players_online%`, `%server_player_count%`, `%serverplayercount%` - players on the current server

### Time
* `%timestamp%` - `yyyy/MM/dd HH:mm:ss`
* `%current_time%`, `%time%` - `HH:mm:ss`
* `%current_time_short%`, `%short_time%` - `HH:mm`
* `%current_date%`, `%date%` - `yyyy/MM/dd`
* `%current_date_uk%`, `%british_date%` - `dd/MM/yyyy`
* `%current_date_day%`, `%day%` - `dd`
* `%current_month%`, `%month%` - `MM`
* `%current_year%`, `%year%` - `yyyy`

## MiniPlaceholders
If MiniPlaceholders is installed on Velocity, VeloAirChat also resolves MiniPlaceholders MiniMessage tags in formats, for example `<some_tag>`.

MiniPlaceholders is optional. If it is not installed, VeloAirChat still resolves all built-in `%...%` placeholders above.

## PlaceholderAPI Bridge
VeloAirChat can resolve backend placeholder plugins through optional backend bridges.

Install `VeloAirChat-Velocity-*.jar` on Velocity and one matching backend bridge on every backend server where placeholders should be resolved:

* `VeloAirChat-Paper-*.jar` for Paper servers. The backend server also needs PlaceholderAPI and the expansions you want to use.
* `VeloAirChat-Fabric-*.jar` for Fabric servers. The backend server can additionally use Fabric Placeholder API; without it, the bridge returns the original text unchanged.

The bridge is optional and disabled by default in `config.yml`:

```yaml
placeholder_api_bridge:
  enabled: false
  timeout_milliseconds: 500
```

After the built-in placeholders are resolved, remaining `%...%` placeholders are sent to the player's current backend server and resolved by the installed backend bridge.

## Group Message Placeholders
Group private message formats can use:

* `%group_amount%`
* `%group_amount_subscript%`
* `%group_members_comma_separated%`
* `%group_members%`

## Sender And Receiver Context
Private message and social spy formats support sender/receiver-prefixed placeholders where the code path applies them, for example `%sender_name%` and `%receiver_name%`.
