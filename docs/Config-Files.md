This page describes the current VeloAirChat configuration layout.

## Configuration Structure
`plugins/VeloAirChat/`

* `config.yml` - general plugin configuration
* `channels.yml` - chat channel configuration
* `filters.yml` - chat filters and replacers
* `user_cache.yml` - generated social/local spy cache
* `messages-xx-xx.yml` - locale messages, formatted with MiniMessage compatibility

## config.yml

```yaml
language: en-gb

placeholder_api_bridge:
  enabled: false
  timeout_milliseconds: 500

backend_bridge:
  enabled: true
  debug: false

debug:
  bridge: false

integrations:
  dynmap:
    enabled: false
    publish_global: true
    publish_local: false
    format: "<platform> <scope> <server> <player>: <message>"

message_command:
  enabled: true
  msg_aliases:
    - /msg
    - /m
    - /tell
    - /whisper
    - /w
    - /pm
  reply_aliases:
    - /reply
    - /r
  censor: false
  log_to_console: true
  log_format: '[MSG] [%sender% -> %receiver%]: '
  group_messages:
    enabled: true
    max_size: 10
  format:
    inbound: "<yellow><bold>%name%</bold> <dark_gray>→</dark_gray> <yellow><bold>You</bold><dark_gray>: </dark_gray><white>"
    outbound: "<yellow><bold>You</bold> <dark_gray>→</dark_gray> <yellow><bold>%name%</bold><dark_gray>: </dark_gray><white>"
    group_inbound: "<yellow><bold>%name%</bold> <dark_gray>→</dark_gray> <yellow><bold>You</bold><gray><hover:show_text:'%group_members%'>[₍₊%group_amount_subscript%₎]</hover></gray><dark_gray>: </dark_gray><white>"
    group_outbound: "<yellow><bold>You</bold> <dark_gray>→</dark_gray> <yellow><bold>%name%</bold><gray><hover:show_text:'%group_members%'>[₍₊%group_amount_subscript%₎]</hover></gray><dark_gray>: </dark_gray><white>"
  restricted_servers: []

social_spy:
  enabled: true
  format: "<yellow>[Spy]</yellow> <gray>%name% <dark_gray>→</dark_gray> %receiver_name%:</gray>%spy_color% "
  group_format: "<yellow>[Spy]</yellow> <gray>%name% <dark_gray>→</dark_gray> %receiver_name% <hover:show_text:'%group_members%'><click:suggest_command:'/msg %group_members_comma_separated%'>[₍₊%group_amount_subscript%₎]</click></hover>:</gray>%spy_color% "
  socialspy_aliases:
    - /socialspy
    - /ss

local_spy:
  enabled: true
  format: "<yellow>[Spy]</yellow> <gray>[%channel%] %name%<dark_gray>:</dark_gray></gray>%spy_color% "
  localspy_aliases:
    - /localspy
    - /ls
  excluded_local_channels: []

broadcast_command:
  enabled: true
  broadcast_aliases:
    - /broadcast
    - /alert
  format: "<gold>[Broadcast]</gold><yellow> "
  log_to_console: true
  log_format: '[BROADCAST]: '

join_and_quit_messages:
  join:
    enabled: true
    format: "<yellow>%name% joined the network</yellow>"
  quit:
    enabled: true
    format: "<yellow>%name% left the network</yellow>"
  broadcast_scope: GLOBAL

server_name_replacement:
  lobby: Lobby
  survival: Survival
```

## channels.yml

```yaml
default_channel: global
server_default_channels:
  lobby: global
  survival: local
channel_log_format: '[CHAT] [%channel%] %sender%: '
channel_command_aliases:
  - channel
  - c
channels:
  - id: local
    format: "<dark_gray>[</dark_gray><gray>%server%</gray><dark_gray>]</dark_gray> %fullname%<reset><white>: "
    broadcast_scope: LOCAL
    shortcut_commands:
      - /local
      - /l
  - id: global
    format: "<#00fb9a>[G]<reset><dark_gray>[</dark_gray><gray>%server%</gray><dark_gray>]</dark_gray><white> %fullname%<reset><white>: "
    broadcast_scope: GLOBAL
    shortcut_commands:
      - /global
      - /g
  - id: staff
    format: "<yellow>[Staff]</yellow> %name%: <gray>"
    broadcast_scope: GLOBAL
    filtered: false
    permissions:
      send: veloairchat.channel.staff.send
      receive: veloairchat.channel.staff.receive
    shortcut_commands:
      - /staff
      - /sc
  - id: helpop
    format: "<#00fb9a>[HelpOp]</#00fb9a> %name%:<gray>"
    broadcast_scope: GLOBAL
    filtered: false
    permissions:
      receive: veloairchat.channel.helpop.receive
    shortcut_commands:
      - /helpop
      - /helpme
```

## filters.yml
`filters.yml` is still generated from the filter definitions in code. See [Filters and Replacers](Filters-and-Replacers.md) for behavior and tuning.
