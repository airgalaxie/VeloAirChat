Channels are what players talk in. Players can switch channels with `/channel` or use channel shortcut commands.

Default channels:

* `local` - sends messages to players on the same backend server.
* `global` - sends messages across the network.
* `staff` - staff channel with send/receive permissions.
* `helpop` - player-to-staff help channel.

## Channel Config Structure

```yaml
channels:
  - id: staff
    format: "<yellow>[Staff]</yellow> %name%: <gray>"
    broadcast_scope: GLOBAL
    log_to_console: true
    restricted_servers: []
    filtered: false
    permissions:
      send: veloairchat.channel.staff.send
      receive: veloairchat.channel.staff.receive
    shortcut_commands:
      - /staff
      - /sc
```

## Channel Scope
Channel scope defines whether VeloAirChat handles and/or passes the message through to the backend server.

* `GLOBAL` - broadcast globally through Velocity.
* `LOCAL` - broadcast only to players on the same backend server.
* `PASSTHROUGH` - do not handle through VeloAirChat; pass to the backend server.
* `GLOBAL_PASSTHROUGH` - broadcast globally through VeloAirChat and also pass to the backend server.
* `LOCAL_PASSTHROUGH` - broadcast locally through VeloAirChat and also pass to the backend server.

## Default Channels
Set `default_channel` in `channels.yml`. Players are placed into this channel unless a server-specific default applies.

```yaml
server_default_channels:
  lobby: global
  survival: local
```

Server names are matched as regular expressions, case-insensitively.

## Restricted Channels
Use `restricted_servers` on a channel to prevent players from sending or receiving that channel while connected to matching backend servers.

You can also restrict `/msg` and `/r` through `message_command.restricted_servers` in `config.yml`.
