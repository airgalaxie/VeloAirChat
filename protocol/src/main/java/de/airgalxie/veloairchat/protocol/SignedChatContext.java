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

package de.airgalxie.veloairchat.protocol;

import java.util.Objects;

/**
 * Signed-chat provenance captured at the platform's official chat event.
 *
 * <p>The fingerprint identifies signature bytes for diagnostics and correlation.
 * It is deliberately not a replacement for Mojang signature verification.</p>
 */
public record SignedChatContext(
        SignedChatState state,
        String adapter,
        String originalPlainMessage,
        String signatureFingerprint,
        long signedTimestamp,
        boolean chatSessionEvidencePresent,
        String publicKeyFingerprint
) {

    public SignedChatContext {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(originalPlainMessage, "originalPlainMessage");
        Objects.requireNonNull(signatureFingerprint, "signatureFingerprint");
        Objects.requireNonNull(publicKeyFingerprint, "publicKeyFingerprint");
        if (adapter.length() > 128 || originalPlainMessage.length() > VeloAirChatBridgeProtocol.MAX_MESSAGE_CHARS
                || signatureFingerprint.length() > 128 || publicKeyFingerprint.length() > 128) {
            throw new IllegalArgumentException("Signed-chat context field is too long");
        }
        if (state == SignedChatState.SIGNED && signatureFingerprint.isBlank()) {
            throw new IllegalArgumentException("Signed chat requires a signature fingerprint");
        }
    }

    public static SignedChatContext unsigned(String adapter, String originalPlainMessage) {
        return new SignedChatContext(SignedChatState.UNSIGNED, adapter, originalPlainMessage, "", 0L, false, "");
    }

    public static SignedChatContext notApplicable(String adapter, String originalPlainMessage) {
        return new SignedChatContext(SignedChatState.NOT_APPLICABLE, adapter, originalPlainMessage, "", 0L, false, "");
    }

    public boolean contentWasModified(String processedPlainMessage) {
        return !originalPlainMessage.equals(processedPlainMessage);
    }
}
