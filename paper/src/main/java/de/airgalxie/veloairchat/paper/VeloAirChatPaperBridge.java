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

package de.airgalxie.veloairchat.paper;

import de.airgalxie.veloairchat.paper.integrations.DynmapWebChatPublisher;
import de.airgalxie.veloairchat.paper.integrations.NoOpWebChatPublisher;
import de.airgalxie.veloairchat.paper.integrations.WebChatPublisher;
import de.airgalxie.veloairchat.protocol.ChatEnvelope;
import de.airgalxie.veloairchat.protocol.ChatIdentityObservation;
import de.airgalxie.veloairchat.protocol.PlaceholderRequest;
import de.airgalxie.veloairchat.protocol.PlaceholderResponse;
import de.airgalxie.veloairchat.protocol.RenderedChatMessage;
import de.airgalxie.veloairchat.protocol.SignedChatContext;
import de.airgalxie.veloairchat.protocol.VeloAirChatBridgeProtocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class VeloAirChatPaperBridge extends JavaPlugin implements Listener, PluginMessageListener {

    private static final String BRIDGE_CHANNEL = VeloAirChatBridgeProtocol.CHANNEL;
    private static final long LEGACY_SUPPRESSION_WINDOW_MILLIS = 1000L;
    private final Set<UUID> deliveredMessages = new HashSet<>();
    private final Map<ChatInputKey, BridgeCapture> recentCaptures = new HashMap<>();
    private BukkitMessageDelivery messageDelivery = new LegacyBukkitMessageDelivery();
    private Method placeholderApiSetPlaceholders;
    private WebChatPublisher webChatPublisher;
    private boolean bridgeDebug;
    private boolean dynmapEnabled;

    @Override
    public void onEnable() {
        getServer().getMessenger().registerIncomingPluginChannel(this, BRIDGE_CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, BRIDGE_CHANNEL);
        loadTechnicalConfig();
        webChatPublisher = createWebChatPublisher();
        getServer().getPluginManager().registerEvents(this, this);
        registerPaperExtension();
        this.placeholderApiSetPlaceholders = findPlaceholderApiSetPlaceholders();
        getLogger().info("Enabled Bukkit-family chat bridge; final chat decisions remain on Velocity.");
        if (bridgeDebug) {
            getServer().getScheduler().runTask(this, this::logRegisteredChatListeners);
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, BRIDGE_CHANNEL, this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, BRIDGE_CHANNEL);
        webChatPublisher = null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLegacyAsyncChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final ChatInputKey inputKey = new ChatInputKey(player.getUniqueId(), messageHash(event.getMessage()));
        final BridgeCapture existingCapture = recentCaptures.get(inputKey);
        final UUID messageId = existingCapture == null ? UUID.randomUUID() : existingCapture.messageId();
        final boolean duplicateInput = wasRecentlyCaptured(inputKey);
        final boolean cancelledBefore = event.isCancelled();
        final int recipientsBefore = event.getRecipients().size();
        event.getRecipients().clear();
        event.setCancelled(true);
        debug("PAPER_CHAT_CAPTURED messageId=" + messageId
                + " eventType=AsyncPlayerChatEvent"
                + " cancelledBefore=" + cancelledBefore
                + " recipientsBefore=" + recipientsBefore
                + " messageHash=" + inputKey.messageHash()
                + " duplicateInput=" + duplicateInput
                + " formatHash=" + messageHash(event.getFormat()));
        if (duplicateInput) {
            debug("PAPER_CHAT_DEDUPED messageId=" + messageId + " eventType=AsyncPlayerChatEvent");
            return;
        }
        rememberCapture(inputKey, messageId);
        debug("PAPER_CHAT_CANCELLED messageId=" + messageId
                + " eventType=AsyncPlayerChatEvent"
                + " cancelledAfter=" + event.isCancelled()
                + " recipientsAfter=" + event.getRecipients().size());
        debug("PAPER_LEGACY_CHAT_CANCELLED messageId=" + messageId
                + " cancelledAfter=" + event.isCancelled()
                + " recipientsBefore=" + recipientsBefore
                + " recipientsAfter=" + event.getRecipients().size());
        if (!cancelledBefore) {
            sendChatToVelocity(player, messageId, event.getMessage(),
                    SignedChatContext.unsigned("bukkit-legacy-v1", event.getMessage()),
                    "AsyncPlayerChatEvent");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void monitorLegacyAsyncChat(AsyncPlayerChatEvent event) {
        logMonitorState("AsyncPlayerChatEvent", event.getPlayer().getUniqueId(), event.getMessage(),
                event.isCancelled(), event.getRecipients().size());
    }

    @Override
    public void onPluginMessageReceived(
            String channel,
            Player sender,
            byte[] message
    ) {
        if (!BRIDGE_CHANNEL.equals(channel)) {
            return;
        }

        try {
            switch (VeloAirChatBridgeProtocol.messageType(message)) {
                case RENDERED_CHAT -> handleBridgeMessage(message);
                case PLACEHOLDER_REQUEST -> handlePlaceholderRequest(sender, message);
                default -> {
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            getLogger().log(Level.WARNING, "Received an invalid VeloAirChat bridge packet", exception);
        }
    }

    private void handlePlaceholderRequest(Player transport, byte[] message) throws IOException {
        final PlaceholderRequest request = VeloAirChatBridgeProtocol.decodePlaceholderRequest(message);
        final Player player = Bukkit.getPlayer(request.playerId());
        final String result = player == null ? request.template()
                : applyPlaceholderApi(player, request.template());
        transport.sendPluginMessage(this, BRIDGE_CHANNEL,
                VeloAirChatBridgeProtocol.encodePlaceholderResponse(
                        new PlaceholderResponse(request.requestId(), result)));
    }

    private void handleBridgeMessage(byte[] message) {
        try {
            final RenderedChatMessage rendered = VeloAirChatBridgeProtocol.decodeRenderedChat(message);
            debug("PAPER_RENDERED_RECEIVED messageId=" + rendered.messageId());
            if (!deliveredMessages.add(rendered.messageId())) {
                return;
            }
            if (webChatPublisher != null) {
                webChatPublisher.publish(rendered);
            }
            if (rendered.recipients().isEmpty()) {
                Bukkit.getOnlinePlayers().forEach(player -> messageDelivery.deliver(player, rendered.miniMessage()));
                debug("PAPER_RENDERED_DISPLAYED messageId=" + rendered.messageId());
                return;
            }
            for (UUID recipientId : rendered.recipients()) {
                final Player recipient = Bukkit.getPlayer(recipientId);
                if (recipient != null) {
                    messageDelivery.deliver(recipient, rendered.miniMessage());
                }
            }
            debug("PAPER_RENDERED_DISPLAYED messageId=" + rendered.messageId());
        } catch (IOException | IllegalArgumentException exception) {
            getLogger().log(Level.WARNING, "Received an invalid VeloAirChat bridge message", exception);
        }
    }

    private void sendChatToVelocity(Player player, UUID messageId, String plainMessage,
                                    SignedChatContext signedChat, String eventType) {
        final ChatEnvelope envelope = new ChatEnvelope(
                messageId,
                new ChatIdentityObservation(player.getUniqueId(), player.getName(), "bukkit-family-v1"),
                signedChat,
                plainMessage,
                Instant.now().toEpochMilli()
        );
        try {
            player.sendPluginMessage(this, BRIDGE_CHANNEL, VeloAirChatBridgeProtocol.encodeChatInput(envelope));
            debug("PAPER_CHAT_SENT_TO_VELOCITY messageId=" + messageId + " eventType=" + eventType);
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Failed to send chat message to Velocity", exception);
        }
    }

    void capturePlatformChat(Player player, String plainMessage, SignedChatContext signedChat,
                             String eventType, boolean cancelledBefore) {
        final ChatInputKey inputKey = new ChatInputKey(player.getUniqueId(), messageHash(plainMessage));
        final BridgeCapture existingCapture = recentCaptures.get(inputKey);
        final UUID messageId = existingCapture == null ? UUID.randomUUID() : existingCapture.messageId();
        if (wasRecentlyCaptured(inputKey)) {
            debug("BUKKIT_FAMILY_CHAT_DEDUPED messageId=" + messageId + " eventType=" + eventType);
            return;
        }
        rememberCapture(inputKey, messageId);
        if (!cancelledBefore) {
            sendChatToVelocity(player, messageId, plainMessage, signedChat, eventType);
        }
    }

    private void registerPaperExtension() {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent", false, getClassLoader());
            getServer().getPluginManager().registerEvents(new PaperChatCaptureListener(this), this);
            messageDelivery = new PaperMessageDelivery();
            getLogger().info("Enabled optional Paper signed-chat capture extension.");
        } catch (ClassNotFoundException | LinkageError exception) {
            getLogger().info("Paper chat API not present; using the Bukkit/Spigot compatibility capture.");
        }
    }

    private void rememberCapture(ChatInputKey inputKey, UUID messageId) {
        recentCaptures.put(inputKey, new BridgeCapture(messageId, System.currentTimeMillis()));
    }

    private boolean wasRecentlyCaptured(ChatInputKey inputKey) {
        final BridgeCapture capture = recentCaptures.get(inputKey);
        if (capture == null) {
            return false;
        }
        final boolean recent = System.currentTimeMillis() - capture.capturedAt() <= LEGACY_SUPPRESSION_WINDOW_MILLIS;
        if (!recent) {
            recentCaptures.remove(inputKey);
        }
        return recent;
    }

    private void logMonitorState(String eventType, UUID playerId, String message, boolean cancelled, int audienceCount) {
        final ChatInputKey inputKey = new ChatInputKey(playerId, messageHash(message));
        debug("PAPER_CHAT_MONITOR"
                + " eventType=" + eventType
                + " cancelled=" + cancelled
                + " audienceCount=" + audienceCount
                + " messageHash=" + inputKey.messageHash()
                + " bridgeEnvelopeCreated=" + wasRecentlyCaptured(inputKey));
    }

    private String messageHash(String message) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((message == null ? "" : message).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException exception) {
            return "hash-unavailable";
        }
    }

    private void loadTechnicalConfig() {
        getConfig().addDefault("debug.bridge", false);
        getConfig().addDefault("dynmap.enabled", true);
        getConfig().options().copyDefaults(true);
        saveConfig();
        bridgeDebug = getConfig().getBoolean("debug.bridge", false);
        dynmapEnabled = getConfig().getBoolean("dynmap.enabled", true);
    }

    private void debug(String message) {
        if (bridgeDebug) {
            getLogger().info(message);
        }
    }

    private WebChatPublisher createWebChatPublisher() {
        if (!dynmapEnabled) {
            return new NoOpWebChatPublisher("disabled", this::debug);
        }
        final Plugin dynmapPlugin = getDynmapPlugin();
        if (dynmapPlugin == null || !dynmapPlugin.isEnabled()) {
            return new NoOpWebChatPublisher("not-installed", this::debug);
        }
        if (!isDynmapCommonApi(dynmapPlugin)) {
            getLogger().warning("Dynmap is installed but does not expose DynmapCommonAPI; webchat publishing disabled.");
            return new NoOpWebChatPublisher("not-installed", this::debug);
        }
        return new DynmapWebChatPublisher(dynmapPlugin, this::debug);
    }

    private Plugin getDynmapPlugin() {
        Plugin dynmapPlugin = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin == null) {
            dynmapPlugin = getServer().getPluginManager().getPlugin("Dynmap");
        }
        return dynmapPlugin;
    }

    private boolean isDynmapCommonApi(Plugin dynmapPlugin) {
        try {
            final Class<?> apiClass = Class.forName("org.dynmap.DynmapCommonAPI", false,
                    dynmapPlugin.getClass().getClassLoader());
            return apiClass.isInstance(dynmapPlugin);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private void logRegisteredChatListeners() {
        logRegisteredChatListeners("AsyncPlayerChatEvent", AsyncPlayerChatEvent.getHandlerList());
    }

    private void logRegisteredChatListeners(String eventName, HandlerList handlerList) {
        getLogger().info("PAPER_CHAT_LISTENERS_BEGIN event=" + eventName);
        for (RegisteredListener listener : handlerList.getRegisteredListeners()) {
            final String pluginName = listener.getPlugin().getName();
            final String listenerClass = listener.getListener().getClass().getName();
            getLogger().info("PAPER_CHAT_LISTENER event=" + eventName
                    + " plugin=" + pluginName
                    + " listener=" + listenerClass
                    + " priority=" + listener.getPriority()
                    + " ignoreCancelled=" + listener.isIgnoringCancelled());
        }
        getLogger().info("PAPER_CHAT_LISTENERS_END event=" + eventName);
    }

    private record ChatInputKey(UUID playerId, String messageHash) {
    }

    private record BridgeCapture(UUID messageId, long capturedAt) {
    }

    private String applyPlaceholderApi(Player player, String format) {
        if (placeholderApiSetPlaceholders == null || !getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return format;
        }

        try {
            final Object result = placeholderApiSetPlaceholders.invoke(null, player, format);
            return result instanceof String value ? value : format;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            getLogger().log(Level.WARNING, "Failed to resolve PlaceholderAPI placeholders", exception);
            return format;
        }
    }

    private Method findPlaceholderApiSetPlaceholders() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return null;
        }

        try {
            return Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                    .getMethod("setPlaceholders", Player.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            getLogger().log(Level.WARNING, "PlaceholderAPI is installed but no compatible setPlaceholders method was found", exception);
            return null;
        }
    }

}
