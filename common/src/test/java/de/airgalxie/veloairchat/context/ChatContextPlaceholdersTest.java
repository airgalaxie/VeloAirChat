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

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatContextPlaceholdersTest {

    private static final ChatContext CONTEXT = new ChatContext(
            UUID.fromString("12345678-1234-1234-1234-123456789012"),
            "[J]", "G", "Lobby", "server1", "airgalaxie",
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            "<gold>[Admin]</gold> airgalaxie", "<gold>[Admin]</gold> ", "",
            "admin", "Administrator", "Hallo", "Hallo", "global",
            "JAVA_PROFILE", "SIGNED", "JAVA_SIGNATURE_PRESENT", "paper-v1",
            1_700_000_000_000L, 1_700_000_000_000L, 42, 7
    );

    @Test
    void exposesTheSameCanonicalInformationToEveryFormat() {
        final String minecraft = ChatContextPlaceholders.replaceAll(
                "<platform> <prefix><player>: <message>", CONTEXT, "Hallo");
        final String dynmap = ChatContextPlaceholders.replaceAll(
                "<scope> <server> <displayName>: <message>", CONTEXT, "Hallo");

        assertEquals("[J] <gold>[Admin]</gold> airgalaxie: Hallo", minecraft);
        assertEquals("G Lobby <gold>[Admin]</gold> airgalaxie: Hallo", dynmap);
    }

    @Test
    void keepsLegacyPercentAliasesCompatible() {
        final String rendered = ChatContextPlaceholders.replaceAll(
                "%fullname% | %name% | %roleprefix% | %serverplayercount% | %message%",
                CONTEXT, "Hallo");

        assertEquals(
                "<gold>[Admin]</gold> airgalaxie | airgalaxie | <gold>[Admin]</gold>  | 7 | Hallo",
                rendered);
    }

    @Test
    void leavesMessageUnresolvedUntilTheRendererChoosesItsRepresentation() {
        assertEquals("airgalaxie: <message>",
                ChatContextPlaceholders.replaceInformation("<player>: <message>", CONTEXT));
        assertTrue(ChatContextPlaceholders.hasMessagePlaceholder("<message>"));
        assertTrue(ChatContextPlaceholders.hasMessagePlaceholder("%message%"));
        assertFalse(ChatContextPlaceholders.hasMessagePlaceholder("<player>: "));
    }

    @Test
    void exposesIdentityAndProvenanceInformation() {
        assertEquals(
                "JAVA_PROFILE/SIGNED/JAVA_SIGNATURE_PRESENT/paper-v1/server1",
                ChatContextPlaceholders.replaceAll(
                        "<identityProvider>/<signedState>/<trustState>/<signedAdapter>/<backend>",
                        CONTEXT, "ignored"));
    }

    @Test
    void appendsMessageToLegacyFormatsButPreservesExplicitPlacement() {
        assertEquals("%fullname%: <message>", ChatContextRenderer.prepareTemplate("%fullname%: "));
        assertEquals("<message> — <player>", ChatContextRenderer.prepareTemplate("<message> — <player>"));
        assertEquals("%message% — %name%", ChatContextRenderer.prepareTemplate("%message% — %name%"));
    }
}
