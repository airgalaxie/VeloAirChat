/*
 * This file is part of VeloAirChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) AirGalxie/VeloAirChat contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.airgalxie.veloairchat.context;

import java.util.Objects;
import java.util.UUID;

/**
 * Complete, platform-neutral information available while rendering one chat message.
 *
 * <p>This record deliberately contains no templates, colors or presentation rules.</p>
 */
public record ChatContext(
        UUID messageId,
        String platform,
        String scope,
        String server,
        String backend,
        String player,
        UUID playerUuid,
        String displayName,
        String prefix,
        String suffix,
        String role,
        String roleDisplayName,
        String message,
        String originalMessage,
        String channel,
        String identityProvider,
        String signedState,
        String trustState,
        String signedAdapter,
        long signedTimestamp,
        long timestamp,
        int ping,
        int localPlayersOnline
) {

    public ChatContext {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(playerUuid, "playerUuid");
        platform = nonNull(platform);
        scope = nonNull(scope);
        server = nonNull(server);
        backend = nonNull(backend);
        player = nonNull(player);
        displayName = nonNull(displayName);
        prefix = nonNull(prefix);
        suffix = nonNull(suffix);
        role = nonNull(role);
        roleDisplayName = nonNull(roleDisplayName);
        message = nonNull(message);
        originalMessage = nonNull(originalMessage);
        channel = nonNull(channel);
        identityProvider = nonNull(identityProvider);
        signedState = nonNull(signedState);
        trustState = nonNull(trustState);
        signedAdapter = nonNull(signedAdapter);
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
