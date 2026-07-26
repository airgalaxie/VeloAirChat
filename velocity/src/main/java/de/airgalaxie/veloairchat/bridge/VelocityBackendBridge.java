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

package de.airgalaxie.veloairchat.bridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.airgalaxie.veloairchat.VelocityVeloAirChat;
import de.airgalaxie.veloairchat.channel.Channel;
import de.airgalaxie.veloairchat.core.SignedChatPolicy;
import de.airgalaxie.veloairchat.core.ChatIdentityContext;
import de.airgalaxie.veloairchat.core.ChatIdentityResolver;
import de.airgalaxie.veloairchat.core.FloodgateIdentityProvider;
import de.airgalaxie.veloairchat.core.GeyserIdentityProvider;
import de.airgalaxie.veloairchat.protocol.ChatEnvelope;
import de.airgalaxie.veloairchat.protocol.VeloAirChatBridgeProtocol;
import de.airgalaxie.veloairchat.moderation.AnalysisRequest;
import de.airgalaxie.veloairchat.moderation.VelocityChatExecution;
import de.airgalaxie.veloairchat.moderation.VelocityChatExecutor;
import de.airgalaxie.veloairchat.user.OnlineUser;
import de.airgalaxie.veloairchat.user.VelocityUser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class VelocityBackendBridge {

    public static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(
            VeloAirChatBridgeProtocol.CHANNEL
    );
    private static final int RECENT_MESSAGE_LIMIT = 4096;

    private final VelocityVeloAirChat plugin;
    private final Set<UUID> processedMessages = new HashSet<>();
    private final SignedChatPolicy signedChatPolicy = new SignedChatPolicy();
    private final ChatIdentityResolver identityResolver;
    private final VelocityChatExecutor chatExecutor;

    public VelocityBackendBridge(@NotNull VelocityVeloAirChat plugin) {
        this.plugin = plugin;
        this.identityResolver = new ChatIdentityResolver(
                new FloodgateIdentityProvider(plugin), new GeyserIdentityProvider(plugin));
        this.chatExecutor = new VelocityChatExecutor(plugin);
    }

    @Subscribe
    public void onPluginMessage(@NotNull PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection source)) {
            return;
        }

        final ChatEnvelope envelope;
        try {
            if (VeloAirChatBridgeProtocol.messageType(event.getData())
                    != VeloAirChatBridgeProtocol.MessageType.CHAT_INPUT) {
                return;
            }
            envelope = VeloAirChatBridgeProtocol.decodeChatInput(event.getData());
        } catch (IOException | IllegalArgumentException exception) {
            plugin.log(Level.WARNING, "Rejected invalid backend chat bridge packet", exception);
            return;
        }
        if (!remember(envelope.messageId())) {
            return;
        }
        final Player sender = plugin.getProxyServer().getPlayer(envelope.identity().connectionUuid()).orElse(null);
        final boolean playerFound = sender != null;
        final boolean observationMatches = playerFound
                && sender.getUniqueId().equals(envelope.identity().connectionUuid());
        final boolean sourceMatches = playerFound && sender.getCurrentServer()
                .map(connection -> connection.getServer().equals(source.getServer()))
                .orElse(false);
        if (!playerFound || !observationMatches || !sourceMatches) {
            debug("BRIDGE_CHAT_DENIED"
                    + " messageId=" + envelope.messageId()
                    + " playerFound=" + playerFound
                    + " observationMatches=" + observationMatches
                    + " sourceMatches=" + sourceMatches);
            return;
        }
        final ChatIdentityContext identity = identityResolver.resolve(sender, envelope.identity());
        final SignedChatPolicy.Assessment signedChatAssessment = signedChatPolicy.assess(envelope, identity);
        if (!signedChatAssessment.accepted()) {
            plugin.log(Level.WARNING, "Rejected backend chat with inconsistent signed-chat metadata: "
                    + signedChatAssessment.reason());
            return;
        }
        debug("SIGNED_CHAT_CONTEXT"
                + " messageId=" + envelope.messageId()
                + " state=" + envelope.signedChat().state()
                + " adapter=" + envelope.signedChat().adapter()
                + " trust=" + signedChatAssessment.trust()
                + " contentModified=" + envelope.signedChat().contentWasModified(envelope.plainMessage())
                + " signatureFingerprint=" + envelope.signedChat().signatureFingerprint());

        debug("CHAT_IDENTITY"
                + " player=" + sender.getUsername()
                + " provider=" + identity.provider()
                + " javaUuid=" + identity.javaUuid().map(UUID::toString).orElse("absent")
                + " floodgateUuid=" + identity.floodgateUuid().map(UUID::toString).orElse("absent")
                + " bedrockXuidPresent=" + identity.bedrockXuid().isPresent());

        final String sourceServer = source.getServerInfo().getName();
        final VelocityUser senderUser = VelocityUser.adapt(sender, plugin);
        final Channel selectedChannel = plugin.getUserCache().getPlayerChannel(sender.getUniqueId())
                .flatMap(channelId -> plugin.getChannels().getChannel(channelId))
                .orElse(null);
        if (selectedChannel == null) {
            plugin.getLocales().sendMessage(senderUser, "error_no_channel");
            return;
        }
        if (!selectedChannel.canUserSend(senderUser)) {
            plugin.getLocales().sendMessage(senderUser, "error_no_permission_send", selectedChannel.getId());
            return;
        }
        if (selectedChannel.isServerRestricted(sourceServer)) {
            plugin.getLocales().sendMessage(senderUser, "error_channel_restricted_server", selectedChannel.getId());
            return;
        }
        final String filteredMessage = plugin.filter(senderUser, envelope.plainMessage(),
                plugin.getChannelFilters(selectedChannel)).orElse(null);
        if (filteredMessage == null) {
            return;
        }
        final var chatEvent = plugin.fireChatMessageEvent(senderUser, filteredMessage, selectedChannel.getId()).join();
        if (chatEvent.isCancelled()) {
            return;
        }
        final Channel channel = plugin.getChannels().getChannel(chatEvent.getChannelId()).orElse(selectedChannel);
        final OnlineUser effectiveSender = chatEvent.getSender();
        final String effectiveMessage = chatEvent.getMessage();
        final AnalysisRequest analysisRequest = new AnalysisRequest(
                envelope.messageId(),
                effectiveSender.getUuid(),
                effectiveSender.getName(),
                channel.getId(),
                sourceServer,
                envelope.signedChat().originalPlainMessage(),
                effectiveMessage,
                identity.provider().name(),
                envelope.signedChat().state().name(),
                signedChatAssessment.trust().name(),
                envelope.timestamp()
        );
        final VelocityChatExecution execution = new VelocityChatExecution(
                source, envelope, identity, signedChatAssessment, channel, effectiveSender, effectiveMessage
        );
        plugin.getModerationPipeline().process(analysisRequest)
                .thenAccept(decision -> chatExecutor.execute(execution, decision));
    }

    private boolean remember(UUID messageId) {
        if (!processedMessages.add(messageId)) {
            return false;
        }
        if (processedMessages.size() > RECENT_MESSAGE_LIMIT) {
            final List<UUID> retained = new ArrayList<>(processedMessages).subList(
                    processedMessages.size() / 2,
                    processedMessages.size()
            );
            processedMessages.clear();
            processedMessages.addAll(retained);
        }
        return true;
    }

    private void debug(String message) {
        plugin.debugBridge(message);
    }
}
