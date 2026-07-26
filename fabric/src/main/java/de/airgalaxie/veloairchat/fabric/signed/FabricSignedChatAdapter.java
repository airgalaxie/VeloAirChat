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

package de.airgalaxie.veloairchat.fabric.signed;

import de.airgalaxie.veloairchat.protocol.SignedChatContext;
import de.airgalaxie.veloairchat.protocol.SignedChatAdapter;
import de.airgalaxie.veloairchat.protocol.SignedChatState;
import net.minecraft.network.chat.PlayerChatMessage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Contains all Minecraft-version-specific signed-message access used by Fabric.
 */
public final class FabricSignedChatAdapter implements SignedChatAdapter<PlayerChatMessage> {

    public static final String ID = "fabric-player-chat-v1";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public SignedChatContext capture(PlayerChatMessage message) {
        if (!message.hasSignature() || message.signature() == null) {
            return SignedChatContext.unsigned(id(), message.signedContent());
        }
        return new SignedChatContext(
                SignedChatState.SIGNED,
                id(),
                message.signedContent(),
                fingerprint(message.signature().bytes()),
                message.timeStamp().toEpochMilli(),
                false,
                ""
        );
    }

    private static String fingerprint(byte[] signature) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(signature);
            return "sha256:" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
