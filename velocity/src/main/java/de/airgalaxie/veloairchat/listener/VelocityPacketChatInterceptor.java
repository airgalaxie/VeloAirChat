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

package de.airgalaxie.veloairchat.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import de.airgalaxie.veloairchat.VeloAirChat;
import de.airgalaxie.veloairchat.VelocityVeloAirChat;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

public class VelocityPacketChatInterceptor {

    private static final String HANDLER_KEY = "veloairchat-chat-interceptor";
    private static final String VELOCITY_HANDLER_KEY = "handler";
    private final VelocityVeloAirChat plugin;

    public VelocityPacketChatInterceptor(@NotNull VelocityVeloAirChat plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getProxyServer().getAllPlayers().forEach(this::injectPlayer);
        plugin.getProxyServer().getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onPostLogin(@NotNull PostLoginEvent event) {
        injectPlayer(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(@NotNull DisconnectEvent event) {
        removePlayer(event.getPlayer());
    }

    private void injectPlayer(@NotNull Player player) {
        if (usesSensitiveSignedChatProtocol(player.getProtocolVersion())) {
            return;
        }
        try {
            final Channel channel = getNettyChannel(player);
            removePlayer(player);
            channel.pipeline().addBefore(VELOCITY_HANDLER_KEY, HANDLER_KEY, new PlayerChannelHandler(plugin, player));
        } catch (RuntimeException exception) {
            plugin.log(Level.WARNING, "Could not inject chat interceptor for " + player.getUsername(), exception);
        }
    }

    private void removePlayer(@NotNull Player player) {
        if (usesSensitiveSignedChatProtocol(player.getProtocolVersion())) {
            return;
        }
        try {
            final Channel channel = getNettyChannel(player);
            if (channel.pipeline().get(HANDLER_KEY) != null) {
                channel.pipeline().remove(HANDLER_KEY);
            }
        } catch (RuntimeException exception) {
            plugin.log(Level.WARNING, "Could not remove chat interceptor for " + player.getUsername(), exception);
        }
    }

    private boolean usesSensitiveSignedChatProtocol(@NotNull ProtocolVersion protocolVersion) {
        return protocolVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_19_1.getProtocol();
    }

    @NotNull
    private Channel getNettyChannel(@NotNull Player player) {
        try {
            final Method getConnection = player.getClass().getMethod("getConnection");
            final Object connection = getConnection.invoke(player);
            final Method getChannel = connection.getClass().getMethod("getChannel");
            final Object channel = getChannel.invoke(connection);
            if (channel instanceof Channel nettyChannel) {
                return nettyChannel;
            }
            throw new IllegalStateException("Velocity connection did not expose a Netty channel");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Velocity internals are not compatible with packet chat interception", exception);
        }
    }

    private static final class PlayerChannelHandler extends ChannelDuplexHandler implements VelocityChatListener {

        private static final String LEGACY_COMMAND_PREFIX = "/";
        private final VelocityVeloAirChat plugin;
        private final Player player;

        private PlayerChannelHandler(@NotNull VelocityVeloAirChat plugin, @NotNull Player player) {
            this.plugin = plugin;
            this.player = player;
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object packet) throws Exception {
            final Optional<String> message = extractChatMessage(packet);
            if (message.isEmpty()) {
                super.channelRead(context, packet);
                return;
            }

            plugin.getProxyServer().getEventManager().fire(new PlayerChatEvent(player, message.get()))
                    .thenApply(event -> event.getResult().isAllowed())
                    .whenComplete((passThrough, throwable) -> {
                        if (throwable != null) {
                            plugin.log(Level.WARNING, "Failed to process intercepted chat packet", throwable);
                            context.fireChannelRead(packet);
                            return;
                        }
                        if (Boolean.TRUE.equals(passThrough)) {
                            context.fireChannelRead(packet);
                        }
                    });
        }

        @NotNull
        private Optional<String> extractChatMessage(@NotNull Object packet) {
            return getPacketMessage(packet).flatMap(message -> {
                if (isLegacyChatPacket(packet) && message.startsWith(LEGACY_COMMAND_PREFIX)) {
                    return Optional.empty();
                }
                return Optional.of(message);
            });
        }

        @NotNull
        private Optional<String> getPacketMessage(@NotNull Object packet) {
            final String packetClassName = packet.getClass().getName();
            if (!packetClassName.equals("com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket")
                    && !packetClassName.equals("com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket")
                    && !isLegacyChatPacket(packet)) {
                return Optional.empty();
            }
            try {
                final Object message = packet.getClass().getMethod("getMessage").invoke(packet);
                return message instanceof String text ? Optional.of(text) : Optional.empty();
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
                return Optional.empty();
            }
        }

        private boolean isLegacyChatPacket(@NotNull Object packet) {
            return packet.getClass().getName()
                    .equals("com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket");
        }

        @Override
        @NotNull
        public VeloAirChat plugin() {
            return plugin;
        }
    }
}
