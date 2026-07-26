VeloAirChat began as a fork of the original HuskChat project by William278
and is now maintained as an independent project.

Files that still contain copyrightable source code from HuskChat retain their
copyright attribution to William278 and contributors. Independently written
VeloAirChat files are attributed to AirGalxie/VeloAirChat contributors.

The historical origin of VeloAirChat, and ideas or architecture shared with
HuskChat, are recorded as project attribution. They do not by themselves
establish William278 copyright in newly written files.

## Source attribution audit

The 103 Java source files present at VeloAirChat revision
`02dcf6d1a83404b2161a9f0423855f34305c2c2a` were compared individually with
the final archived HuskChat source revision
`d31180b0c127ace47e33848d82ad1c533fe9de35`.

License comments, package renames and the systematic `HuskChat` to
`VeloAirChat` rename were ignored during comparison. Attribution was retained
when concrete method bodies, control flow, data structures or other
copyrightable implementation remained. Imports, declarations, annotations,
short conventional expressions, API usage, equivalent behavior and shared
architecture were not treated as evidence of source-code authorship.

### Files containing HuskChat source

The following 64 files retain both the William278 and VeloAirChat contributor
attributions:

- `common/src/main/java/de/airgalxie/veloairchat/VeloAirChat.java`
- every Java file present at the audited revision in
  `common/src/main/java/de/airgalxie/veloairchat/api/`,
  `channel/`, `command/`, `config/`, `event/`, `filter/`, `getter/`,
  `listener/`, `message/`, `placeholders/` and `user/`
- `common/src/main/java/de/airgalxie/veloairchat/util/AudiencesProvider.java`
- every Java file present at the audited revision in
  `common/src/test/java/de/airgalxie/veloairchat/channel/`, `filter/` and
  `user/`
- `velocity/src/main/java/de/airgalxie/veloairchat/VelocityVeloAirChat.java`
- every Java file present at the audited revision in
  `velocity/src/main/java/de/airgalxie/veloairchat/api/`, `command/`, `event/`,
  `listener/` and `user/`

These files have direct HuskChat counterparts and retain concrete
implementation from them. This includes the renamed pairs `VeloAirChat` /
`HuskChat`, `VeloAirChatCommand` / `HuskChatCommand`,
`VelocityVeloAirChat` / `VelocityHuskChat`,
`VelocityVeloAirChatAPI` / `VelocityHuskChatAPI`, and
`VelocityPacketChatInterceptor` / `VelocityPacketChatListener`. Later
refactoring or added VeloAirChat behavior does not remove the attribution
while copyrightable HuskChat implementation remains in the file.

### Independently written VeloAirChat files

The following 39 files contain no copyrightable HuskChat source and carry
only the VeloAirChat contributor attribution:

- every Java file present at the audited revision in
  `common/src/main/java/de/airgalxie/veloairchat/context/`
- `common/src/main/java/de/airgalxie/veloairchat/util/MessageFormatter.java`
- `common/src/test/java/de/airgalxie/veloairchat/context/ChatContextPlaceholdersTest.java`
- every Java file present at the audited revision under `fabric/src/`
- every Java file present at the audited revision under `paper/src/`
- every Java file present at the audited revision under `protocol/src/`
- every Java file present at the audited revision in
  `velocity/src/main/java/de/airgalxie/veloairchat/bridge/`, `core/`,
  `placeholders/` and `security/`
- `velocity/src/test/java/de/airgalxie/veloairchat/core/SignedChatPolicyTest.java`

These files have no HuskChat counterpart. A cross-file comparison against all
HuskChat Java sources found only language and API boilerplate or short
conventional constructs, not copied method bodies or other copyrightable
implementation. Their protocol, signed-chat, identity, backend-bridge,
rendering, Fabric and Paper implementations are independently written
VeloAirChat source.

The directory descriptions above enumerate only files present at the stated
audited revision. They do not automatically classify files added later; every
future file must be assessed from its own source.

The project remains licensed under the Apache License 2.0.
