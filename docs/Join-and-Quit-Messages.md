VeloAirChat supports displaying special **join and quit messages** whenever a player joins or leaves your network.

## Usage
To enable this feature, set `join_and_quit_messages.join.enabled` and/or `join_and_quit_messages.quit.enabled` to `true` in your `config.yml` file. You can then modify the `format` of either. Formats accept VeloAirChat placeholders and MiniMessage.

### Broadcast scopes
You can set the `broadcast_scope` of join and quit messages in a similar fashion to how you can do this for [channels](Channels.md). See [Channel Scope](Channels.md#channel-scope) for more details on the available scopes.

On Velocity, PASSTHROUGH scopes do not cancel backend join/quit messages. Backend servers can still broadcast their own join/leave messages unless configured separately.

<details>
<summary>Example config.yml</summary>

```yaml
# Options for customizing player join and quit messages
join_and_quit_messages:
  join:
    enabled: false
    # Use the veloairchat.join_message.[text] permission to override this per-group if needed
    format: '<yellow>%name% joined the network</yellow>'
  quit:
    enabled: false
    # Use the veloairchat.quit_message.[text] permission to override this per-group if needed
    format: '<yellow>%name% left the network</yellow>'
  broadcast_scope: GLOBAL
```
</details>

## Permission-based formats
You can set specific join/quit messages for groups by using `veloairchat.join_message.[text]` and `veloairchat.quit_message.[text]` permission metadata. For example: `veloairchat.join_message.<green>%name% has arrived with style!</green>`.
