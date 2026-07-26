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

import com.velocitypowered.api.proxy.Player;
import de.airgalxie.veloairchat.protocol.ChatIdentityObservation;

import java.util.List;
import java.util.Optional;

public final class ChatIdentityResolver {

    private final FloodgateIdentityProvider floodgate;
    private final GeyserIdentityProvider geyser;

    public ChatIdentityResolver(FloodgateIdentityProvider floodgate, GeyserIdentityProvider geyser) {
        this.floodgate = floodgate;
        this.geyser = geyser;
    }

    public ChatIdentityContext resolve(Player player, ChatIdentityObservation observation) {
        return floodgate.resolve(player.getUniqueId())
                .or(() -> geyser.resolve(player.getUniqueId()))
                .orElseGet(() -> new ChatIdentityContext(
                player.getUniqueId(),
                ChatIdentityContext.IdentityProvider.JAVA_PROFILE,
                Optional.of(player.getUniqueId()),
                Optional.empty(),
                Optional.empty(),
                List.of(new ChatIdentityContext.IdentityEvidence(
                        "velocity-player-connection", "JAVA_PROFILE_UUID", player.getUniqueId().toString()))
        ));
    }
}
