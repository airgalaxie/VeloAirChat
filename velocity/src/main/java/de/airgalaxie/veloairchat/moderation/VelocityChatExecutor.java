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

package de.airgalaxie.veloairchat.moderation;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.airgalaxie.veloairchat.VelocityVeloAirChat;
import de.airgalaxie.veloairchat.channel.Channel;
import de.airgalaxie.veloairchat.config.Settings;
import de.airgalaxie.veloairchat.context.ChatContext;
import de.airgalaxie.veloairchat.context.ChatContextFactory;
import de.airgalaxie.veloairchat.core.ChatIdentityContext;
import de.airgalaxie.veloairchat.protocol.RenderedChatMessage;
import de.airgalaxie.veloairchat.protocol.VeloAirChatBridgeProtocol;
import de.airgalaxie.veloairchat.user.OnlineUser;
import de.airgalaxie.veloairchat.user.VelocityUser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Applies chat decisions through Velocity's existing rendering and bridge delivery path.
 */
public final class VelocityChatExecutor implements ChatExecutor<VelocityChatExecution> {

    private final VelocityVeloAirChat plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    public VelocityChatExecutor(VelocityVeloAirChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(VelocityChatExecution execution, ChatDecision decision) {
        if (decision.action() == ChatDecision.Action.BLOCK) {
            notifyBlocked(execution.sender(), decision.reason());
            return;
        }
        if (decision.action() == ChatDecision.Action.HOLD) {
            throw new IllegalStateException("HOLD execution is not enabled");
        }

        final Channel channel = execution.channel();
        final OnlineUser sender = execution.sender();
        final String message = execution.message();
        final String sourceServer = execution.source().getServerInfo().getName();
        final boolean local = channel.getBroadcastScope().isOneOf(
                Channel.BroadcastScope.LOCAL,
                Channel.BroadcastScope.LOCAL_PASSTHROUGH,
                Channel.BroadcastScope.PASSTHROUGH
        );
        plugin.signChatMessage(sender, channel, message);
        final String platformLabel = execution.identity().provider() == ChatIdentityContext.IdentityProvider.FLOODGATE
                || execution.identity().provider() == ChatIdentityContext.IdentityProvider.GEYSER
                ? "[B]" : "[J]";
        final ChatContext context = ChatContextFactory.create(
                plugin,
                sender,
                channel,
                message,
                new ChatContextFactory.Metadata(
                        execution.envelope().messageId(),
                        platformLabel,
                        sourceServer,
                        execution.identity().provider().name(),
                        execution.envelope().signedChat().state().name(),
                        execution.signedChatAssessment().trust().name(),
                        execution.envelope().signedChat().adapter(),
                        execution.envelope().signedChat().signedTimestamp(),
                        execution.envelope().timestamp(),
                        execution.envelope().signedChat().originalPlainMessage()
                )
        );
        final String rendered = miniMessage.serialize(plugin.getLocales()
                .createChannelMessage(sender, context, channel.getFormat(), plugin).join());
        final DynmapPayload dynmapPayload = createDynmapPayload(context, sender, local);
        final ProxyServer proxy = plugin.getProxyServer();

        for (RegisteredServer server : proxy.getAllServers()) {
            final List<UUID> recipients = server.getPlayersConnected().stream()
                    .filter(player -> !local || player.getCurrentServer()
                            .map(connection -> connection.getServer().equals(execution.source().getServer()))
                            .orElse(false))
                    .map(player -> VelocityUser.adapt(player, plugin))
                    .filter(player -> player.getUuid().equals(sender.getUuid())
                            || channel.canUserReceive(player))
                    .filter(player -> !channel.isServerRestricted(player.getServerName()))
                    .map(OnlineUser::getUuid)
                    .toList();
            if (recipients.isEmpty()) {
                continue;
            }
            send(server, new RenderedChatMessage(execution.envelope().messageId(), sourceServer, recipients, rendered,
                    dynmapPayload.publish(), dynmapPayload.server(), dynmapPayload.name(), dynmapPayload.message()));
        }
        if (channel.isLogToConsole()) {
            final String logFormat = plugin.getChannels().getChannelLogFormat()
                    .replace("%channel%", channel.getId().toUpperCase(Locale.ROOT))
                    .replace("%sender%", sender.getName());
            plugin.log(Level.INFO, logFormat + message);
        }
    }

    private void send(RegisteredServer server, RenderedChatMessage message) {
        try {
            server.sendPluginMessage(
                    de.airgalaxie.veloairchat.bridge.VelocityBackendBridge.CHANNEL,
                    VeloAirChatBridgeProtocol.encodeRenderedChat(message)
            );
        } catch (IOException exception) {
            plugin.log(Level.WARNING, "Failed to send rendered chat to backend "
                    + server.getServerInfo().getName(), exception);
        }
    }

    private void notifyBlocked(OnlineUser sender, String reason) {
        final String locale = switch (reason) {
            case StandardDecisionEngine.REASON_FLOOD -> "error_chat_filter_spam";
            case StandardDecisionEngine.REASON_DUPLICATE -> "error_chat_filter_repeat";
            case StandardDecisionEngine.REASON_CAPS -> "error_chat_filter_caps";
            case StandardDecisionEngine.REASON_ADVERTISEMENT,
                 StandardDecisionEngine.REASON_REPEATED_FINDING -> "error_chat_filter_advertising";
            default -> "error_chat_filter_spam";
        };
        plugin.getLocales().sendMessage(sender, locale);
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
