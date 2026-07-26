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

package de.airgalxie.veloairchat.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

class VeloAirChatBridgeProtocolTest {

    @Test
    void chatInputRoundTrips() throws IOException {
        ChatEnvelope envelope = new ChatEnvelope(
                UUID.randomUUID(),
                new ChatIdentityObservation(UUID.randomUUID(), "Spieler", "paper-bukkit-family-v1"),
                new SignedChatContext(SignedChatState.SIGNED, "paper-adventure-v1",
                        "Hallo", "sha256:abc", 41L, false, ""),
                "Hallo",
                42L
        );

        Assertions.assertEquals(envelope,
                VeloAirChatBridgeProtocol.decodeChatInput(VeloAirChatBridgeProtocol.encodeChatInput(envelope)));
    }

    @Test
    void signedContextTracksModifiedContent() {
        SignedChatContext context = new SignedChatContext(
                SignedChatState.SIGNED, "paper-adventure-v1", "Original", "sha256:abc", 42L, false, ""
        );

        Assertions.assertFalse(context.contentWasModified("Original"));
        Assertions.assertTrue(context.contentWasModified("Gefiltert"));
    }

    @Test
    void signedContextRejectsMissingFingerprint() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SignedChatContext(
                SignedChatState.SIGNED, "paper-adventure-v1", "Hallo", "", 42L, false, ""
        ));
    }

    @Test
    void placeholderMessagesRoundTripOnSharedProtocol() throws IOException {
        PlaceholderRequest request = new PlaceholderRequest(UUID.randomUUID(), UUID.randomUUID(), "%player_name%");
        PlaceholderResponse response = new PlaceholderResponse(request.requestId(), "Spieler");

        Assertions.assertEquals(request, VeloAirChatBridgeProtocol.decodePlaceholderRequest(
                VeloAirChatBridgeProtocol.encodePlaceholderRequest(request)));
        Assertions.assertEquals(response, VeloAirChatBridgeProtocol.decodePlaceholderResponse(
                VeloAirChatBridgeProtocol.encodePlaceholderResponse(response)));
    }

    @Test
    void renderedChatRoundTrips() throws IOException {
        RenderedChatMessage message = new RenderedChatMessage(
                UUID.randomUUID(),
                "server1",
                List.of(UUID.randomUUID()),
                "[J] [G] [server1] Spieler: Hallo",
                true,
                "server1",
                "[J] [G] [server1] Spieler",
                "Hallo"
        );

        Assertions.assertEquals(message,
                VeloAirChatBridgeProtocol.decodeRenderedChat(VeloAirChatBridgeProtocol.encodeRenderedChat(message)));
    }
}
