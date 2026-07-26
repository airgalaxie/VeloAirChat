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

package de.airgalxie.veloairchat.security;

import de.airgalxie.veloairchat.VelocityVeloAirChat;
import de.airgalxie.veloairchat.channel.Channel;
import de.airgalxie.veloairchat.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

public class VelocityChatAuditSigner {

    private static final String VERSION = "v1";
    private static final String TRUSTED_SOURCE = "velocity_registered_backend";
    private static final String MINECRAFT_CHAT_FORWARDING = "unchanged";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();
    private final VelocityVeloAirChat plugin;

    public VelocityChatAuditSigner(@NotNull VelocityVeloAirChat plugin) {
        this.plugin = plugin;
    }

    public void sign(@NotNull OnlineUser sender, @NotNull Channel channel, @NotNull String message) {
        if (!plugin.getSettings().getChatAuditSignature().isEnabled()) {
            return;
        }

        try {
            final long timestamp = Instant.now().toEpochMilli();
            final String nonce = nonce();
            final String messageHash = hash(message);
            final boolean backendRegistered = plugin.getProxyServer().getServer(sender.getServerName()).isPresent();
            final String payload = String.join("\n",
                    VERSION,
                    Long.toString(timestamp),
                    nonce,
                    sender.getUuid().toString(),
                    sender.getName(),
                    sender.getServerName(),
                    Boolean.toString(backendRegistered),
                    TRUSTED_SOURCE,
                    MINECRAFT_CHAT_FORWARDING,
                    channel.getId(),
                    channel.getBroadcastScope().name(),
                    message
            );
            final String signature = hmac(payload, plugin.getSettings().getChatAuditSignature().getSecret());
            plugin.log(Level.INFO, "[CHAT-AUDIT] version=" + VERSION
                    + " ts=" + timestamp
                    + " nonce=" + nonce
                    + " channel=" + channel.getId()
                    + " server=" + sender.getServerName()
                    + " backend_registered=" + backendRegistered
                    + " trusted_source=" + TRUSTED_SOURCE
                    + " minecraft_chat_forwarding=" + MINECRAFT_CHAT_FORWARDING
                    + " uuid=" + sender.getUuid()
                    + " name=" + sender.getName()
                    + " message_hash=" + messageHash
                    + " signature=" + signature);
        } catch (RuntimeException exception) {
            plugin.log(Level.WARNING, "Failed to create VeloAirChat audit signature", exception);
        }
    }

    @NotNull
    private static String hmac(@NotNull String payload, @NotNull String secret) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getUrlDecoder().decode(secret), "HmacSHA256"));
            return BASE64.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create chat audit signature", exception);
        }
    }

    @NotNull
    private static String hash(@NotNull String message) {
        try {
            return BASE64.encodeToString(MessageDigest.getInstance("SHA-256").digest(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @NotNull
    private static String nonce() {
        return UUID.randomUUID() + "-" + Long.toUnsignedString(RANDOM.nextLong(), 36);
    }
}
