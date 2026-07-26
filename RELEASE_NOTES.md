# VeloAirChat 1.0.0-beta.1

This is the first public beta release of VeloAirChat, an independently
developed, Velocity-centered chat system that began as a fork of HuskChat.

## Release information

```text
Version: 1.0.0-beta.1
Protocol: v4
Minecraft: 26.2
Git: generated at build time
Built: generated at build time (UTC)
```

The concrete Git short hash and build date are stored in every produced JAR
manifest. Builds without Git metadata use `Git: unknown`. The build date uses
`BUILD_DATE`, then `SOURCE_DATE_EPOCH`, and otherwise the current UTC build
date.

## Highlights

* central Velocity authority for identity, permissions, routing and rendering
* Protocol v4 transport through `ChatEnvelope`
* Velocity-side `ChatIdentityContext`
* platform-neutral `ChatContext` and central rendering pipeline
* Paper and Fabric backend bridges
* signed-chat evidence handling and centralized trust policy
* optional Floodgate and Geyser identity resolution
* optional Dynmap integration using the final rendered chat context

## Upgrade notice

Protocol v4 is not compatible with Protocol v3. The Velocity plugin and every
Paper or Fabric backend bridge must be upgraded together.

## Beta limitations

Floodgate and Geyser still require live integration testing. The complete
Bukkit, Spigot, Paper, Folia and Fabric runtime matrix has not been verified;
Folia runtime compatibility is not claimed.
