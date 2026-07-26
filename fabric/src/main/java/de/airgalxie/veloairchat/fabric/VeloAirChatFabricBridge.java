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

package de.airgalxie.veloairchat.fabric;

import de.airgalxie.veloairchat.fabric.signed.FabricSignedChatAdapter;
import de.airgalxie.veloairchat.fabric.integrations.DynmapWebChatPublisher;
import de.airgalxie.veloairchat.protocol.ChatEnvelope;
import de.airgalxie.veloairchat.protocol.ChatIdentityObservation;
import de.airgalxie.veloairchat.protocol.PlaceholderRequest;
import de.airgalxie.veloairchat.protocol.PlaceholderResponse;
import de.airgalxie.veloairchat.protocol.RenderedChatMessage;
import de.airgalxie.veloairchat.protocol.SignedChatContext;
import de.airgalxie.veloairchat.protocol.VeloAirChatBridgeProtocol;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VeloAirChatFabricBridge implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("VeloAirChat Fabric Bridge");
    private static final Set<UUID> DELIVERED_MESSAGES = new HashSet<>();
    private static final FabricSignedChatAdapter SIGNED_CHAT_ADAPTER = new FabricSignedChatAdapter();
    private static DynmapWebChatPublisher dynmapWebChatPublisher;

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("dynmap")) {
            dynmapWebChatPublisher = new DynmapWebChatPublisher(LOGGER);
            dynmapWebChatPublisher.register();
        }
        PayloadTypeRegistry.serverboundPlay().register(BridgePayload.ID, BridgePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BridgePayload.ID, BridgePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BridgePayload.ID, (payload, context) -> {
            try {
                switch (VeloAirChatBridgeProtocol.messageType(payload.data())) {
                    case PLACEHOLDER_REQUEST -> handlePlaceholderRequest(context.player(), payload.data());
                    case RENDERED_CHAT -> handleRenderedChat(context.player(), payload.data());
                    default -> {
                    }
                }
            } catch (IOException | IllegalArgumentException exception) {
                LOGGER.warn("Received an invalid VeloAirChat bridge packet", exception);
            }
        });
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(VeloAirChatFabricBridge::forwardChatToVelocity);
        LOGGER.info("Enabled Fabric chat bridge; final chat formatting and routing remain on Velocity.");
    }

    private static boolean forwardChatToVelocity(PlayerChatMessage message, ServerPlayer sender,
                                                 net.minecraft.network.chat.ChatType.Bound parameters) {
        final SignedChatContext signedChat = SIGNED_CHAT_ADAPTER.capture(message);
        final ChatEnvelope envelope = new ChatEnvelope(
                UUID.randomUUID(),
                new ChatIdentityObservation(sender.getUUID(), sender.getGameProfile().name(), "fabric-v1"),
                signedChat,
                message.decoratedContent().getString(),
                Instant.now().toEpochMilli()
        );
        try {
            ServerPlayNetworking.send(sender, new BridgePayload(VeloAirChatBridgeProtocol.encodeChatInput(envelope)));
        } catch (IOException exception) {
            LOGGER.warn("Failed to send chat message to Velocity", exception);
        }
        return false;
    }

    private static void handlePlaceholderRequest(ServerPlayer transport, byte[] message) throws IOException {
        final PlaceholderRequest request = VeloAirChatBridgeProtocol.decodePlaceholderRequest(message);
        final ServerPlayer player = transport.level().getServer().getPlayerList().getPlayer(request.playerId());
        final String result = player == null ? request.template()
                : PlaceholderApiResolver.resolve(player, request.template());
        ServerPlayNetworking.send(transport, new BridgePayload(
                VeloAirChatBridgeProtocol.encodePlaceholderResponse(
                        new PlaceholderResponse(request.requestId(), result))));
    }

    private static void handleRenderedChat(ServerPlayer transportPlayer, byte[] message) throws IOException {
        final RenderedChatMessage rendered = VeloAirChatBridgeProtocol.decodeRenderedChat(message);
        if (!DELIVERED_MESSAGES.add(rendered.messageId())) {
            return;
        }
        if (dynmapWebChatPublisher != null) {
            dynmapWebChatPublisher.publish(rendered);
        }
        final Component component = Component.literal(stripMiniMessage(rendered.miniMessage()));
        if (rendered.recipients().isEmpty()) {
            transportPlayer.level().getServer().getPlayerList().getPlayers()
                    .forEach(player -> player.sendSystemMessage(component));
            return;
        }
        for (UUID recipientId : rendered.recipients()) {
            final ServerPlayer recipient = transportPlayer.level().getServer().getPlayerList().getPlayer(recipientId);
            if (recipient != null) {
                recipient.sendSystemMessage(component);
            }
        }
    }

    private static String stripMiniMessage(String input) {
        return input.replaceAll("<[^>]+>", "");
    }

    public record BridgePayload(byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BridgePayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath("veloairchat", "bridge")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, BridgePayload> CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeBytes(payload.data()),
                buffer -> {
                    final byte[] data = new byte[buffer.readableBytes()];
                    buffer.readBytes(data);
                    return new BridgePayload(data);
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    private static final class PlaceholderApiResolver {
        private static final Class<?> PLACEHOLDERS_CLASS = findClass("eu.pb4.placeholders.api.Placeholders");
        private static final Class<?> CONTEXT_CLASS = findClass("eu.pb4.placeholders.api.PlaceholderContext");
        private static final Method CONTEXT_FACTORY = findContextFactory();
        private static final Method PARSE_TEXT = findParseText();

        private static String resolve(ServerPlayer player, String format) {
            if (PLACEHOLDERS_CLASS == null || CONTEXT_FACTORY == null || PARSE_TEXT == null) {
                return format;
            }

            try {
                final Object context = CONTEXT_FACTORY.invoke(null, player);
                final Object parsed = PARSE_TEXT.invoke(null, Component.literal(format), context);
                return parsed instanceof Component text ? text.getString() : format;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.warn("Failed to resolve Fabric Placeholder API placeholders", exception);
                return format;
            }
        }

        private static Class<?> findClass(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException exception) {
                return null;
            }
        }

        private static Method findContextFactory() {
            if (CONTEXT_CLASS == null) {
                return null;
            }
            for (Method method : CONTEXT_CLASS.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals("of")
                        || method.getParameterCount() != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0].isAssignableFrom(ServerPlayer.class)) {
                    return method;
                }
            }
            return null;
        }

        private static Method findParseText() {
            if (PLACEHOLDERS_CLASS == null || CONTEXT_CLASS == null) {
                return null;
            }
            for (Method method : PLACEHOLDERS_CLASS.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals("parseText")
                        || method.getParameterCount() != 2) {
                    continue;
                }
                final Class<?>[] parameters = method.getParameterTypes();
                if (parameters[0].isAssignableFrom(Component.class) && parameters[1].isAssignableFrom(CONTEXT_CLASS)) {
                    return method;
                }
            }
            return null;
        }
    }

}
