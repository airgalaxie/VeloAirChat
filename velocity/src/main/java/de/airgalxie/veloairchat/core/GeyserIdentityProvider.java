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

package de.airgalxie.veloairchat.core;

import de.airgalxie.veloairchat.VelocityVeloAirChat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Optional adapter for Geyser's documented player-presence API.
 */
public final class GeyserIdentityProvider {

    private final Object api;
    private final Method isBedrockPlayer;

    public GeyserIdentityProvider(VelocityVeloAirChat plugin) {
        Object resolvedApi = null;
        Method resolvedCheck = null;
        if (plugin.isPluginPresent("geyser")) {
            try {
                final Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                resolvedApi = apiClass.getMethod("api").invoke(null);
                resolvedCheck = apiClass.getMethod("isBedrockPlayer", UUID.class);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                resolvedApi = null;
            }
        }
        this.api = resolvedApi;
        this.isBedrockPlayer = resolvedCheck;
    }

    public Optional<ChatIdentityContext> resolve(UUID connectionUuid) {
        if (api == null) {
            return Optional.empty();
        }
        try {
            if (!Boolean.TRUE.equals(isBedrockPlayer.invoke(api, connectionUuid))) {
                return Optional.empty();
            }
            return Optional.of(new ChatIdentityContext(
                    connectionUuid,
                    ChatIdentityContext.IdentityProvider.GEYSER,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(new ChatIdentityContext.IdentityEvidence(
                            "velocity-geyser-api", "BEDROCK_CONNECTION", "confirmed"))
            ));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Optional.empty();
        }
    }
}
