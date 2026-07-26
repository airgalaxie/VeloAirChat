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

package de.airgalaxie.veloairchat.placeholders;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import de.airgalaxie.veloairchat.VelocityVeloAirChat;
import de.airgalaxie.veloairchat.bridge.VelocityBackendBridge;
import de.airgalaxie.veloairchat.protocol.PlaceholderRequest;
import de.airgalaxie.veloairchat.protocol.PlaceholderResponse;
import de.airgalaxie.veloairchat.protocol.VeloAirChatBridgeProtocol;
import de.airgalaxie.veloairchat.user.OnlineUser;
import de.airgalaxie.veloairchat.user.VelocityUser;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.logging.Level;

public class VelocityPlaceholderApiBridge implements PlaceholderReplacer {

    private final VelocityVeloAirChat plugin;
    private final ConcurrentMap<UUID, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public VelocityPlaceholderApiBridge(@NotNull VelocityVeloAirChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<String> formatPlaceholders(@NotNull String message, @NotNull OnlineUser player) {
        if (!(player instanceof VelocityUser velocityUser) || !message.contains("%")) {
            return CompletableFuture.completedFuture(message);
        }

        final UUID requestId = UUID.randomUUID();
        final CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        final boolean sent = velocityUser.getPlayer().getCurrentServer()
                .map(server -> server.sendPluginMessage(VelocityBackendBridge.CHANNEL,
                        createRequest(requestId, player, message)))
                .orElse(false);
        if (!sent) {
            pendingRequests.remove(requestId);
            return CompletableFuture.completedFuture(message);
        }

        final int timeout = Math.max(1, plugin.getSettings().getPlaceholderApiBridge().getTimeoutMilliseconds());
        return future.orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    pendingRequests.remove(requestId);
                    return message;
                });
    }

    @Subscribe
    public void handlePluginMessage(@NotNull PluginMessageEvent event) {
        if (!event.getIdentifier().equals(VelocityBackendBridge.CHANNEL)
                || !(event.getSource() instanceof ServerConnection)) {
            return;
        }

        try {
            if (VeloAirChatBridgeProtocol.messageType(event.getData())
                    != VeloAirChatBridgeProtocol.MessageType.PLACEHOLDER_RESPONSE) {
                return;
            }
            event.setResult(PluginMessageEvent.ForwardResult.handled());
            final PlaceholderResponse response = VeloAirChatBridgeProtocol.decodePlaceholderResponse(event.getData());
            final CompletableFuture<String> future = pendingRequests.remove(response.requestId());
            if (future != null) {
                future.complete(response.result());
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            plugin.log(Level.WARNING, "Received an invalid PlaceholderAPI bridge response", exception);
        }
    }

    private byte @NotNull [] createRequest(@NotNull UUID requestId, @NotNull OnlineUser player, @NotNull String message) {
        try {
            return VeloAirChatBridgeProtocol.encodePlaceholderRequest(
                    new PlaceholderRequest(requestId, player.getUuid(), message)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode placeholder request", exception);
        }
    }

}
