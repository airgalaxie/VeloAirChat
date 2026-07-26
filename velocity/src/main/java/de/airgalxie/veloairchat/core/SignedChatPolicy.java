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

import de.airgalxie.veloairchat.protocol.ChatEnvelope;
import de.airgalxie.veloairchat.protocol.SignedChatState;

/**
 * Velocity-owned signed-chat policy. Backend adapters only report observations.
 */
public final class SignedChatPolicy {

    public Assessment assess(ChatEnvelope envelope, ChatIdentityContext identity) {
        final SignedChatState state = envelope.signedChat().state();
        if (state == SignedChatState.SIGNED
                && (identity.provider() == ChatIdentityContext.IdentityProvider.FLOODGATE
                || identity.provider() == ChatIdentityContext.IdentityProvider.GEYSER)) {
            return new Assessment(false, Trust.REJECTED, "bedrock-cannot-have-java-signature");
        }
        if (state == SignedChatState.SIGNED && envelope.signedChat().signedTimestamp() <= 0L) {
            return new Assessment(false, Trust.REJECTED, "signed-timestamp-missing");
        }
        final Trust trust;
        if (identity.provider() == ChatIdentityContext.IdentityProvider.FLOODGATE) {
            trust = Trust.FLOODGATE_VERIFIED;
        } else if (identity.provider() == ChatIdentityContext.IdentityProvider.GEYSER) {
            trust = Trust.BEDROCK_IDENTIFIED_UNVERIFIED;
        } else {
            trust = switch (state) {
                case SIGNED -> Trust.JAVA_SIGNATURE_PRESENT;
                case UNSIGNED, NOT_APPLICABLE -> Trust.JAVA_UNSIGNED;
                case UNKNOWN -> Trust.PROXY_VERIFIED;
            };
        }
        return new Assessment(true, trust, "accepted");
    }

    public enum Trust {
        JAVA_SIGNATURE_PRESENT,
        JAVA_UNSIGNED,
        FLOODGATE_VERIFIED,
        BEDROCK_IDENTIFIED_UNVERIFIED,
        PROXY_VERIFIED,
        REJECTED
    }

    public record Assessment(boolean accepted, Trust trust, String reason) {
    }
}
