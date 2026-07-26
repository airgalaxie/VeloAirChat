/*
 * This file is part of VeloAirChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) AirGalaxie/VeloAirChat contributors
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

package de.airgalaxie.veloairchat.moderation;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, platform-neutral input to chat analysis.
 */
public record AnalysisRequest(
        UUID messageId,
        UUID senderId,
        String senderName,
        String channelId,
        String sourceServer,
        String originalMessage,
        String message,
        String identityProvider,
        String signedState,
        String trustState,
        long timestamp
) {

    public AnalysisRequest {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(senderId, "senderId");
        Objects.requireNonNull(senderName, "senderName");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(sourceServer, "sourceServer");
        Objects.requireNonNull(originalMessage, "originalMessage");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(identityProvider, "identityProvider");
        Objects.requireNonNull(signedState, "signedState");
        Objects.requireNonNull(trustState, "trustState");
    }
}
