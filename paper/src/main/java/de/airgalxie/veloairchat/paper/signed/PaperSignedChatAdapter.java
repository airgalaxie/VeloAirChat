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

package de.airgalxie.veloairchat.paper.signed;

import de.airgalxie.veloairchat.protocol.SignedChatContext;
import de.airgalxie.veloairchat.protocol.SignedChatAdapter;
import de.airgalxie.veloairchat.protocol.SignedChatState;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.chat.SignedMessage;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Isolates Paper's official Adventure signed-message API from the bridge core.
 */
public final class PaperSignedChatAdapter implements SignedChatAdapter<AsyncChatEvent> {

    public static final String ID = "paper-adventure-v1";

    @Override
    public String id() {
        return ID;
    }

    @Override
    @NotNull
    public SignedChatContext capture(@NotNull AsyncChatEvent event) {
        final SignedMessage signedMessage = event.signedMessage();
        final SignedMessage.Signature signature = signedMessage.signature();
        if (signature == null) {
            return SignedChatContext.unsigned(id(), signedMessage.message());
        }
        return new SignedChatContext(
                SignedChatState.SIGNED,
                id(),
                signedMessage.message(),
                fingerprint(signature.bytes()),
                signedMessage.timestamp().toEpochMilli(),
                false,
                ""
        );
    }

    @NotNull
    private static String fingerprint(byte[] signature) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(signature);
            return "sha256:" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
