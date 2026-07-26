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

package de.airgalxie.veloairchat.bridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.airgalxie.veloairchat.VelocityVeloAirChat;
import de.airgalxie.veloairchat.channel.Channel;
import de.airgalxie.veloairchat.config.Settings;
import de.airgalxie.veloairchat.context.ChatContext;
import de.airgalxie.veloairchat.context.ChatContextFactory;
import de.airgalxie.veloairchat.core.SignedChatPolicy;
import de.airgalxie.veloairchat.core.ChatIdentityContext;
import de.airgalxie.veloairchat.core.ChatIdentityResolver;
import de.airgalxie.veloairchat.core.FloodgateIdentityProvider;
import de.airgalxie.veloairchat.core.GeyserIdentityProvider;
import de.airgalxie.veloairchat.protocol.ChatEnvelope;
import de.airgalxie.veloairchat.protocol.RenderedChatMessage;
import de.airgalxie.veloairchat.protocol.VeloAirChatBridgeProtocol;
import de.airgalxie.veloairchat.user.OnlineUser;
import de.airgalxie.veloairchat.user.VelocityUser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private final SignedChatPolicy signedChatPolicy = new SignedChatPolicy();
    private final ChatIdentityResolver identityResolver;

    public VelocityBackendBridge(@NotNull VelocityVeloAirChat plugin) {
        this.plugin = plugin;
        this.identityResolver = new ChatIdentityResolver(
                new FloodgateIdentityProvider(plugin), new GeyserIdentityProvider(plugin));
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
        final boolean local = channel.getBroadcastScope().isOneOf(
                Channel.BroadcastScope.LOCAL,
                Channel.BroadcastScope.LOCAL_PASSTHROUGH,
                Channel.BroadcastScope.PASSTHROUGH
        );
        plugin.signChatMessage(effectiveSender, channel, effectiveMessage);
        final String platformLabel = identity.provider() == ChatIdentityContext.IdentityProvider.FLOODGATE
                || identity.provider() == ChatIdentityContext.IdentityProvider.GEYSER
                ? "[B]" : "[J]";
        final ChatContext context = ChatContextFactory.create(
                plugin,
                effectiveSender,
                channel,
                effectiveMessage,
                new ChatContextFactory.Metadata(
                        envelope.messageId(),
                        platformLabel,
                        sourceServer,
                        identity.provider().name(),
                        envelope.signedChat().state().name(),
                        signedChatAssessment.trust().name(),
                        envelope.signedChat().adapter(),
                        envelope.signedChat().signedTimestamp(),
                        envelope.timestamp(),
                        envelope.signedChat().originalPlainMessage()
                )
        );
        final String rendered = miniMessage.serialize(plugin.getLocales()
                .createChannelMessage(effectiveSender, context, channel.getFormat(), plugin).join());
        final DynmapPayload dynmapPayload = createDynmapPayload(context, effectiveSender, local);
        final ProxyServer proxy = plugin.getProxyServer();

        for (RegisteredServer server : proxy.getAllServers()) {
            final List<UUID> recipients = server.getPlayersConnected().stream()
                    .filter(player -> !local || player.getCurrentServer()
                            .map(connection -> connection.getServer().equals(source.getServer()))
                            .orElse(false))
                    .map(player -> VelocityUser.adapt(player, plugin))
                    .filter(player -> player.getUuid().equals(effectiveSender.getUuid())
                            || channel.canUserReceive(player))
                    .filter(player -> !channel.isServerRestricted(player.getServerName()))
                    .map(OnlineUser::getUuid)
                    .toList();
            if (recipients.isEmpty()) {
                continue;
            }
            send(server, new RenderedChatMessage(envelope.messageId(), sourceServer, recipients, rendered,
                    dynmapPayload.publish(), dynmapPayload.server(), dynmapPayload.name(), dynmapPayload.message()));
        }
        if (channel.isLogToConsole()) {
            final String logFormat = plugin.getChannels().getChannelLogFormat()
                    .replace("%channel%", channel.getId().toUpperCase(Locale.ROOT))
                    .replace("%sender%", effectiveSender.getName());
            plugin.log(Level.INFO, logFormat + effectiveMessage);
        }
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

    private void send(RegisteredServer server, RenderedChatMessage message) {
        try {
            server.sendPluginMessage(CHANNEL, VeloAirChatBridgeProtocol.encodeRenderedChat(message));
        } catch (IOException exception) {
            plugin.log(Level.WARNING, "Failed to send rendered chat to backend "
                    + server.getServerInfo().getName(), exception);
        }
    }

    private void debug(String message) {
        plugin.debugBridge(message);
    }

    private DynmapPayload createDynmapPayload(ChatContext context, OnlineUser sender, boolean local) {
        final Settings.IntegrationSettings.DynmapSettings settings = plugin.getSettings().getIntegrations().getDynmap();
        final boolean publish = settings.isEnabled()
                && ((local && settings.isPublishLocal()) || (!local && settings.isPublishGlobal()));
        if (!publish) {
            return DynmapPayload.disabled(context.backend());
        }

        final String message = plainText.serialize(plugin.getLocales()
                .createChannelMessage(sender, context, settings.getFormat(), plugin).join());
        return new DynmapPayload(true, context.backend(), context.player(), message);
    }

    private record DynmapPayload(boolean publish, String server, String name, String message) {
        static DynmapPayload disabled(String server) {
            return new DynmapPayload(false, server, "", "");
        }
    }
}
