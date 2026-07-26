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

package de.airgalaxie.veloairchat.core;

import de.airgalaxie.veloairchat.protocol.ChatEnvelope;
import de.airgalaxie.veloairchat.protocol.ChatIdentityObservation;
import de.airgalaxie.veloairchat.protocol.SignedChatContext;
import de.airgalaxie.veloairchat.protocol.SignedChatState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

class SignedChatPolicyTest {

    private final SignedChatPolicy policy = new SignedChatPolicy();

    @Test
    void velocityAssignsSignedTrust() {
        final var assessment = policy.assess(envelope(
                new SignedChatContext(SignedChatState.SIGNED, "paper", "Hallo", "sha256:x", 1L, false, "")),
                javaIdentity());

        Assertions.assertTrue(assessment.accepted());
        Assertions.assertEquals(SignedChatPolicy.Trust.JAVA_SIGNATURE_PRESENT, assessment.trust());
    }

    @Test
    void velocityRejectsImpossibleSignedPlatform() {
        final var assessment = policy.assess(envelope(
                new SignedChatContext(SignedChatState.SIGNED, "paper", "Hallo", "sha256:x", 1L, false, "")),
                floodgateIdentity());

        Assertions.assertFalse(assessment.accepted());
        Assertions.assertEquals(SignedChatPolicy.Trust.REJECTED, assessment.trust());
    }

    @Test
    void floodgateEvidenceHasDistinctTrustWithoutJavaSignature() {
        final var assessment = policy.assess(envelope(
                SignedChatContext.notApplicable("bukkit-family-v1", "Hallo")), floodgateIdentity());

        Assertions.assertTrue(assessment.accepted());
        Assertions.assertEquals(SignedChatPolicy.Trust.FLOODGATE_VERIFIED, assessment.trust());
    }

    @Test
    void geyserObservationIsNotFloodgateVerified() {
        final var assessment = policy.assess(envelope(
                SignedChatContext.notApplicable("bukkit-family-v1", "Hallo")),
                new ChatIdentityContext(UUID.randomUUID(), ChatIdentityContext.IdentityProvider.GEYSER,
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of()));

        Assertions.assertTrue(assessment.accepted());
        Assertions.assertEquals(SignedChatPolicy.Trust.BEDROCK_IDENTIFIED_UNVERIFIED, assessment.trust());
    }

    private ChatEnvelope envelope(SignedChatContext context) {
        return new ChatEnvelope(UUID.randomUUID(),
                new ChatIdentityObservation(UUID.randomUUID(), "Spieler", "test"), context, "Hallo", 2L);
    }

    private ChatIdentityContext javaIdentity() {
        final UUID uuid = UUID.randomUUID();
        return new ChatIdentityContext(uuid, ChatIdentityContext.IdentityProvider.JAVA_PROFILE,
                Optional.of(uuid), Optional.empty(), Optional.empty(), List.of());
    }

    private ChatIdentityContext floodgateIdentity() {
        return new ChatIdentityContext(UUID.randomUUID(), ChatIdentityContext.IdentityProvider.FLOODGATE,
                Optional.empty(), Optional.of(UUID.randomUUID()), Optional.of("123456789"), List.of());
    }
}
