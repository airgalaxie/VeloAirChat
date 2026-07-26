# VeloAirChat Architecture Contract and Porting Status

> Binding baseline for future work. “Implemented” means present in source and
> covered by the verification described here. Runtime integrations not executed
> in this workspace are explicitly marked unverified.

```yaml
architecture_status:
  schema_version: 2
  last_verified: "2026-07-26"
  authority: velocity
  protocol:
    channel: "veloairchat:bridge"
    version: 4
    previous_version_compatible: false
  identity:
    backend_assigns_identity: false
    backend_assigns_trust: false
    velocity_resolution: implemented
    java_uuid_separate: true
    floodgate_uuid_separate: true
    bedrock_xuid_separate: true
  optional_velocity_integrations:
    floodgate:
      implemented: true
      runtime_verified: false
    geyser:
      implemented: true
      runtime_verified: false
  trust_states:
    - JAVA_SIGNATURE_PRESENT
    - JAVA_UNSIGNED
    - FLOODGATE_VERIFIED
    - BEDROCK_IDENTIFIED_UNVERIFIED
    - PROXY_VERIFIED
    - REJECTED
  adapters:
    paper:
      common_core: implemented
      bukkit_spigot_compatibility_path: implemented_not_runtime_verified
      paper_extension: implemented_compile_verified
      folia: structurally_allowed_not_runtime_verified
    fabric:
      implemented: true
  cryptography:
    mojang_signature_presence_captured: true
    signature_bytes_forwarded: false
    signature_fingerprint_forwarded: true
    mojang_signature_reverified_by_velocity: false
    end_to_end_signed_delivery: false
  verification:
    tests: 40
    failures: 0
    errors: 0
    clean_build: passed
    clean_build_tasks: 58
```

## Central authority

Velocity is the sole authority for chat identity, identity provider selection,
trust, Signed-Chat policy, acceptance, permissions, channels, routing,
prefix/suffix, formatting, recipients, and platform labeling.

Backend modules only capture platform events and observations, encode Protocol
v4, resolve platform data requested by Velocity, and execute
`RenderedChatMessage`. They contain no Floodgate/Geyser trust decision.

## Analyzed starting state and corrected deviations

The previous design had these identity defects:

- `SignedChatContext` acted mainly as a Java-signature container.
- `ChatEnvelope` transported a backend-selected `ChatPlatform`.
- Paper queried Floodgate locally and classified the player as Bedrock.
- Velocity trust was derived from backend platform/signature flags.
- connection UUID, Java UUID, Floodgate UUID, and XUID had no explicit model.
- the Bukkit main class directly referenced Paper `AsyncChatEvent` and
  Adventure component delivery.

These paths were removed or separated. No backend now queries Floodgate or
Geyser, assigns trust, or labels Java/Bedrock.

## Protocol v4

All proxy/backend traffic still uses the single `veloairchat:bridge` channel,
but the wire version is now 4.

`ChatEnvelope` contains:

- a message ID;
- `ChatIdentityObservation`;
- mandatory `SignedChatContext`;
- processed plain message;
- capture timestamp.

`ChatIdentityObservation` contains only the backend connection UUID, observed
name, and adapter ID. It is explicitly untrusted. It has no `ChatPlatform`,
Java UUID, Floodgate UUID, XUID, provider, or trust field. Velocity validates
the connection UUID against its online player and the actual
`ServerConnection`.

Protocol v4 rejects other versions. Protocol v3 and v4 nodes cannot be mixed;
all proxy and backend artifacts must be upgraded together.

No Paper, Fabric, Geyser, Floodgate, or Velocity API object enters the protocol.

## Central identity model

`ChatIdentityContext` exists only in the Velocity core and separates:

- VeloAirChat/Velocity connection UUID;
- selected `IdentityProvider`;
- optional Java UUID;
- optional Floodgate UUID;
- optional Bedrock XUID;
- normalized evidence with source, type, and value.

Implemented providers:

- `JAVA_PROFILE`: Velocity player identity when no Bedrock provider identifies
  the connection;
- `FLOODGATE`: Floodgate confirmed the player and supplied Floodgate data;
- `GEYSER`: Geyser identified a Bedrock connection without Floodgate evidence;
- `PROXY_CONNECTION`: reserved in the model for connection-only identity.

No UUID is converted or inferred from another identifier. A Floodgate UUID is
never placed in the Java UUID field. For linked Floodgate players only,
Floodgate’s documented `getCorrectUniqueId()` result is recorded as Java UUID;
the Floodgate-generated UUID remains separately recorded from
`getJavaUniqueId()`. XUID remains a string from `getXuid()`.

## Floodgate and Geyser integration

Both integrations are optional Velocity-side adapters with no hard runtime
dependency.

`FloodgateIdentityProvider` uses the documented Floodgate 2 entry points:

- `FloodgateApi.getInstance()`;
- `isFloodgatePlayer(UUID)`;
- `getPlayer(UUID)`;
- `FloodgatePlayer.getJavaUniqueId()`;
- `getCorrectUniqueId()`;
- `getXuid()`;
- `isLinked()`.

When Floodgate confirms a connection, Velocity selects provider `FLOODGATE`.
The trust policy may then assign `FLOODGATE_VERIFIED`. This is an identity
assertion from Floodgate, not a Mojang chat signature.

`GeyserIdentityProvider` uses documented `GeyserApi.api()` and
`isBedrockPlayer(UUID)`. Geyser-only detection selects provider `GEYSER`, but
does not invent Floodgate UUID, XUID, or Floodgate evidence. Its trust is
`BEDROCK_IDENTIFIED_UNVERIFIED`, not `FLOODGATE_VERIFIED`.

Optional dependency metadata ensures Velocity orders these plugins when
present. Reflection confines foreign types and permits operation when absent.
Actual runtime calls were not integration-tested here.

## Signed Chat and trust

`SignedChatContext` remains platform-neutral and now represents:

- `SIGNED`, `UNSIGNED`, `NOT_APPLICABLE`, or `UNKNOWN`;
- original signed/plain body;
- signature fingerprint;
- signed timestamp;
- whether ChatSession evidence was available;
- optional public-key fingerprint;
- adapter that observed the evidence.

The current official Paper/Fabric capture APIs used by this project provide
message/signature observations but this implementation does not obtain a
ChatSession or public key from them. Those two fields are therefore currently
`false`/empty, never fabricated.

Velocity’s `SignedChatPolicy` assigns:

- `JAVA_SIGNATURE_PRESENT`: Java identity plus observed signature metadata;
- `JAVA_UNSIGNED`: Java identity without usable signature;
- `FLOODGATE_VERIFIED`: Floodgate-confirmed Bedrock identity;
- `BEDROCK_IDENTIFIED_UNVERIFIED`: Geyser detected Bedrock without Floodgate;
- `PROXY_VERIFIED`: connection-only/unknown signature state;
- `REJECTED`: inconsistent evidence.

A `SIGNED` context combined with Floodgate or Geyser identity is rejected.
Therefore Bedrock cannot receive an invented Java signature status.

`JAVA_SIGNATURE_PRESENT` deliberately does not mean cryptographically verified
by Velocity. Velocity receives a non-reversible SHA-256 fingerprint, not raw
signature bytes, public key, or complete ChatSession chain. No Mojang signature
reverification takes place in Velocity.

Final backend delivery remains a system/component message, not an end-to-end
Mojang-signed player-chat packet.

## Paper module

The `paper` module entry point is `VeloAirChatPaperBridge`.

The common `VeloAirChatPaperBridge` no longer imports Paper or Adventure API
types.

Common Bukkit/Spigot functionality:

- plugin channel transport;
- legacy `AsyncPlayerChatEvent` capture;
- deduplication and suppression;
- PlaceholderAPI translation;
- UUID-targeted delivery;
- optional Dynmap execution;
- plain Bukkit string delivery fallback.

Optional Paper extensions:

- `PaperChatCaptureListener` for `AsyncChatEvent`;
- `PaperSignedChatAdapter` for Adventure `SignedMessage`;
- `PaperMessageDelivery` for rich Adventure components.

Paper classes are loaded only after runtime detection. This preserves current
Paper behavior while providing a Bukkit/Spigot compatibility path. The project
was compiled against Paper and was not started on Bukkit, Spigot, or Folia in
this workspace. Folia runtime compatibility is therefore not claimed.

Fabric remains a separate adapter with version-specific Minecraft access
isolated in `FabricSignedChatAdapter`.

## Verification

Executed successfully:

```text
./gradlew test
40 tests, 0 failures, 0 errors

./gradlew clean build
BUILD SUCCESSFUL
58 actionable tasks, 58 executed
all license checks passed
```

Generated artifacts:

- `target/VeloAirChat-Velocity-1.0.0-beta.1.jar`
- `target/VeloAirChat-Paper-1.0.0-beta.1.jar`
- `target/VeloAirChat-Fabric-1.0.0-beta.1.jar`

Static checks confirmed:

- no backend Floodgate/Geyser query remains;
- no `ChatPlatform` or backend `ChatTrust` remains in the protocol;
- the common Bukkit bridge has no Paper/Adventure type import;
- no patched Velocity API source path exists.

## Remaining limitations and open decisions

1. Floodgate and Geyser adapters need live Velocity integration tests.
2. Bukkit, Spigot, Paper, Folia, and Fabric need runtime matrix tests; only
   compilation and core tests ran here.
3. Decide whether raw signature/session/public-key material may be transported
   safely if a future official API exposes enough data for central
   cryptographic verification.
4. Decide the long-term client-visible delivery model: signed body with
   unsigned decoration or explicitly unsigned system component.
5. The Bukkit/Spigot fallback strips MiniMessage tags and loses rich behavior.
6. Fabric also strips MiniMessage tags and has an unbounded delivered-message
   cache.
7. Paper legacy event compatibility and Gradle 10 deprecations remain.

## Rule for future adapters

A new Bukkit-family extension, Sponge adapter, or other platform module may
only create `ChatIdentityObservation` and `SignedChatContext`, send Protocol v4,
and execute Velocity output. It must not select identity provider, trust,
Java/Bedrock label, channel, permission, routing, or format.
