/*
 * This file is part of VeloAirChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Original project: HuskChat by William278
 *  Modifications Copyright (c) AirGalxie/VeloAirChat contributors
 *  Copyright (c) contributors
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

package de.airgalxie.veloairchat.core;

import de.airgalxie.veloairchat.VelocityVeloAirChat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Optional, isolated adapter for the documented Floodgate 2 API.
 */
public final class FloodgateIdentityProvider {

    private final Object api;
    private final Method isFloodgatePlayer;
    private final Method getPlayer;

    public FloodgateIdentityProvider(VelocityVeloAirChat plugin) {
        Object resolvedApi = null;
        Method resolvedCheck = null;
        Method resolvedPlayer = null;
        if (plugin.isPluginPresent("floodgate")) {
            try {
                final Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                resolvedApi = apiClass.getMethod("getInstance").invoke(null);
                resolvedCheck = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                resolvedPlayer = apiClass.getMethod("getPlayer", UUID.class);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                resolvedApi = null;
            }
        }
        this.api = resolvedApi;
        this.isFloodgatePlayer = resolvedCheck;
        this.getPlayer = resolvedPlayer;
    }

    public Optional<ChatIdentityContext> resolve(UUID connectionUuid) {
        if (api == null) {
            return Optional.empty();
        }
        try {
            if (!Boolean.TRUE.equals(isFloodgatePlayer.invoke(api, connectionUuid))) {
                return Optional.empty();
            }
            final Object player = getPlayer.invoke(api, connectionUuid);
            if (player == null) {
                return Optional.empty();
            }
            final Class<?> type = Class.forName("org.geysermc.floodgate.api.player.FloodgatePlayer");
            final UUID floodgateUuid = (UUID) type.getMethod("getJavaUniqueId").invoke(player);
            final String xuid = (String) type.getMethod("getXuid").invoke(player);
            final boolean linked = Boolean.TRUE.equals(type.getMethod("isLinked").invoke(player));
            final UUID correctUuid = (UUID) type.getMethod("getCorrectUniqueId").invoke(player);
            return Optional.of(new ChatIdentityContext(
                    connectionUuid,
                    ChatIdentityContext.IdentityProvider.FLOODGATE,
                    linked ? Optional.of(correctUuid) : Optional.empty(),
                    Optional.ofNullable(floodgateUuid),
                    xuid == null || xuid.isBlank() ? Optional.empty() : Optional.of(xuid),
                    List.of(new ChatIdentityContext.IdentityEvidence(
                            "velocity-floodgate-api", "FLOODGATE_PLAYER", "confirmed"))
            ));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Optional.empty();
        }
    }
}
