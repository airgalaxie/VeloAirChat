VeloAirChat provides a number of commands, for switching channels, sending broadcasts and messaging players and groups of players. Channels can also be restricted behind send and receive permission nodes.

## List of Commands
| Command           | Usage                        | Aliases                                            | Description                                                      | Permission                                                  |
|-------------------|------------------------------|----------------------------------------------------|------------------------------------------------------------------|-------------------------------------------------------------|
| `/channel`        | `/channel <name> [message]`  | `/c`                                               | Send a message or switch to a chat channel                       | `veloairchat.command.channel`                                  |
| `/veloairchat`    | `/veloairchat <about\|reload>` | `/huskchat`                                        | View plugin information and reload                               | `veloairchat.command.veloairchat`                              |
| `/msg`            | `/msg <player(s)> <message>` | `/m`, `/tell`, `/w`, `/whisper`, `/message`, `/pm` | Send a private message to a player                               | `veloairchat.command.msg`                                      |
| `/reply`          | `/reply <message>`           | `/r`                                               | Quickly reply to a private message                               | `veloairchat.command.msg.reply`                                |
| `/socialspy`      | `/socialspy [color]`         | `/ss`                                              | Lets you view other users' private messages                      | `veloairchat.command.socialspy`                                |
| `/localspy`       | `/localspy [color]`          | `/ls`                                              | Lets you view messages sent in other local chat channels         | `veloairchat.command.localspy`                                 |
| `/broadcast`      | `/broadcast <message>`       | `/alert`                                           | Lets you send a broadcast across the server                      | `veloairchat.command.broadcast`                                |
| `/optoutmsg`      | `/optoutmsg`                 | N/A                                                | Lets you "opt-out" of a group private message you are in         | `veloairchat.command.optoutmsg`                                |
| Shortcut commands | `/<command> <message>`       | N/A                                                | Quickly send a message in or switch to a chat channel            | Channel send permission, e.g. `veloairchat.channel.staff.send` |

## Channel send and receive permissions
Channels also have their own permission to send and receive to.

You can configure these in the channel config file, but by default they are `veloairchat.channel.<channel>.receive`. Channels without permissions set do not require the permission node to talk in.

## Chat formatting permissions
Formatting messages also has its own permission.

You can apply the node `veloairchat.formatted_chat` to allow players to use MiniMessage-compatible formatting in their chat messages. Without this node, user messages are sent as plain text and only the configured channel/message formats are rendered.
