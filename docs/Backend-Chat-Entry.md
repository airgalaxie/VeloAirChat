This page covers backend plugins that rely on raw chat input while VeloAirChat is running on Velocity.

## Background
VeloAirChat normally handles player chat on the proxy. If a backend plugin expects the backend server to receive raw chat input, that plugin may not see messages handled by VeloAirChat.

Prefer command, sign, book, anvil or GUI input for backend data entry where possible.

## Passthrough Channels
For plugins that still need backend chat input, create a temporary passthrough channel:

1. Create a channel with `PASSTHROUGH`, `LOCAL_PASSTHROUGH`, or `GLOBAL_PASSTHROUGH`.
2. Add a shortcut command such as `/input`.
3. Ask players to switch to that channel while entering backend plugin data.
4. Configure backend chat cancellation separately if needed, because passthrough means the backend can also process and broadcast the message.

See [Channels](Channels.md) for broadcast scope behavior.
