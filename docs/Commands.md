VeloAirChat is a Velocity-centered chat system. Velocity centrally handles chat processing, routing, formatting, and permission checks, while the backend modules serve only as bridges. The commands below provide channel switching, broadcasts, and private or group messaging.

## List of Commands
| Command           | Usage                             | Aliases                                            | Description                                                                 | Permission                                                  |
|-------------------|-----------------------------------|----------------------------------------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------|
| `/channel`        | `/channel <name> [message]`       | `/c`                                               | Send a message to a chat channel or switch the active channel               | `veloairchat.command.channel`                               |
| `/veloairchat`    | `/veloairchat [about\|reload]`    | `/vac`                                             | View plugin information or reload the current configuration                 | `veloairchat.command.veloairchat`                           |
| `/msg`            | `/msg <player(s)> <message>`      | `/m`, `/tell`, `/whisper`, `/w`, `/pm`             | Send a private message to one or more players                               | `veloairchat.command.msg`                                  |
| `/reply`          | `/reply <message>`                | `/r`                                               | Reply to the most recent private-message conversation                       | `veloairchat.command.msg.reply`                            |
| `/socialspy`      | `/socialspy [color]`              | `/ss`                                              | Toggle visibility of other users' private messages                          | `veloairchat.command.socialspy`                            |
| `/localspy`       | `/localspy [color]`               | `/ls`                                              | Toggle visibility of messages in other local chat channels                  | `veloairchat.command.localspy`                             |
| `/broadcast`      | `/broadcast <message>`            | `/alert`                                           | Send a broadcast across the network                                         | `veloairchat.command.broadcast`                            |
| `/optoutmsg`      | `/optoutmsg`                      | N/A                                                | Leave the current group private-message conversation                        | `veloairchat.command.optoutmsg`                            |
| Shortcut commands | `/<command> [message]`            | N/A                                                | Send a message to a configured channel or switch the active channel         | Configured channel send permission                         |

## Channel send and receive permissions
Channel send and receive access can be restricted with permission nodes configured for each channel. Velocity evaluates these permissions centrally for senders and recipients.

Channels without a configured send or receive permission do not require one. The concrete nodes are defined in the channel configuration.

## Chat formatting permissions
The `veloairchat.formatted_chat` permission allows players to use MiniMessage-compatible formatting in their chat messages.

Without this permission, player input is treated as plain text. In both cases, Velocity remains responsible for the final centralized rendering of the configured channel and message formats.
