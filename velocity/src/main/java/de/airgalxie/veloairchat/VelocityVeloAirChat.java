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

package de.airgalxie.veloairchat;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.miniplaceholders.api.MiniPlaceholders;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import de.airgalxie.veloairchat.api.VelocityVeloAirChatAPI;
import de.airgalxie.veloairchat.bridge.VelocityBackendBridge;
import de.airgalxie.veloairchat.command.ShortcutCommand;
import de.airgalxie.veloairchat.command.VelocityCommand;
import de.airgalxie.veloairchat.config.Channels;
import de.airgalxie.veloairchat.config.Filters;
import de.airgalxie.veloairchat.config.Locales;
import de.airgalxie.veloairchat.config.Settings;
import de.airgalxie.veloairchat.event.VelocityEventProvider;
import de.airgalxie.veloairchat.filter.ChatFilter;
import de.airgalxie.veloairchat.getter.DataGetter;
import de.airgalxie.veloairchat.getter.DefaultDataGetter;
import de.airgalxie.veloairchat.getter.LuckPermsDataGetter;
import de.airgalxie.veloairchat.listener.VelocityEventChatListener;
import de.airgalxie.veloairchat.listener.VelocityPacketChatInterceptor;
import de.airgalxie.veloairchat.listener.VelocityPlayerListener;
import de.airgalxie.veloairchat.placeholders.DefaultReplacer;
import de.airgalxie.veloairchat.placeholders.PlaceholderReplacer;
import de.airgalxie.veloairchat.placeholders.VelocityPlaceholderApiBridge;
import de.airgalxie.veloairchat.security.VelocityChatAuditSigner;
import de.airgalxie.veloairchat.channel.Channel;
import de.airgalxie.veloairchat.user.OnlineUser;
import de.airgalxie.veloairchat.user.UserCache;
import de.airgalxie.veloairchat.user.VelocityUser;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

@Plugin(id = "veloairchat")
@Getter
public class VelocityVeloAirChat implements VeloAirChat, VelocityEventProvider {

    // bStats ID
    private static final int METRICS_ID = 14187;

    // Plugin version
    private final PluginContainer container;
    private final Logger logger;
    private final Metrics.Factory metrics;
    private final Path configDirectory;
    private final ProxyServer server;
    private final List<ChatFilter> filtersAndReplacers = new ArrayList<>();
    private final List<PlaceholderReplacer> placeholderReplacers = new ArrayList<>();
    private TagResolver formattingTagResolver = TagResolver.empty();
    private VelocityPlaceholderApiBridge placeholderApiBridge;
    private VelocityChatAuditSigner chatAuditSigner;
    private VelocityBackendBridge backendBridge;

    @Setter
    private Settings settings;
    @Setter
    private Locales locales;
    @Setter
    private Channels channels;
    @Setter
    private Filters filterSettings;
    @Setter
    private UserCache.Editor userCache;
    private DataGetter dataGetter;

    @Inject
    public VelocityVeloAirChat(@NotNull ProxyServer server, @NotNull org.slf4j.Logger logger,
                            @DataDirectory Path configDirectory, @NotNull Metrics.Factory metrics,
                            @NotNull PluginContainer pluginContainer) {
        this.server = server;
        this.logger = logger;
        this.configDirectory = configDirectory;
        this.metrics = metrics;
        this.container = pluginContainer;
    }

    @Subscribe
    public void onProxyInitialization(@NotNull ProxyInitializeEvent event) {
        // Load config and locale files
        this.loadConfig();

        //load filters
        this.loadFilters();

        // Setup player data getter
        if (isPluginPresent("luckperms")) {
            this.dataGetter = new LuckPermsDataGetter();
        } else {
            this.dataGetter = new DefaultDataGetter();
        }

        // Setup built-in placeholders
        this.placeholderReplacers.add(new DefaultReplacer(this));
        this.chatAuditSigner = new VelocityChatAuditSigner(this);
        if (getSettings().getPlaceholderApiBridge().isEnabled()) {
            this.placeholderApiBridge = new VelocityPlaceholderApiBridge(this);
            this.placeholderReplacers.add(this.placeholderApiBridge);
            log(Level.INFO, "Enabled PlaceholderAPI bridge; install a VeloAirChat backend bridge on backend servers");
        }
        if (isPluginPresent("miniplaceholders")) {
            try {
                this.formattingTagResolver = TagResolver.resolver(
                        MiniPlaceholders.globalPlaceholders(),
                        MiniPlaceholders.audiencePlaceholders()
                );
                log(Level.INFO, "Hooked into MiniPlaceholders for MiniMessage tag placeholders");
            } catch (Throwable e) {
                log(Level.WARNING, "Failed to hook into MiniPlaceholders; built-in placeholders will still work", e);
            }
        }
        final boolean backendBridgeMode = getSettings().getBackendBridge().isEnabled();
        if (backendBridgeMode || placeholderApiBridge != null) {
            getProxyServer().getChannelRegistrar().register(VelocityBackendBridge.CHANNEL);
        }
        if (backendBridgeMode) {
            this.backendBridge = new VelocityBackendBridge(this);
            getProxyServer().getEventManager().register(this, backendBridge);
            log(Level.INFO, "Enabled backend chat bridge on " + VelocityBackendBridge.CHANNEL.getId());
        }

        // Register events
        getProxyServer().getEventManager().register(this, new VelocityPlayerListener(this));
        if (backendBridgeMode) {
            debugBridge("LEGACY_FLOODGATE_CHAT_REGISTERED false");
            log(Level.INFO, "Backend bridge mode active; Velocity PlayerChatEvent and packet chat inputs are disabled.");
        } else {
            getProxyServer().getEventManager().register(this, new VelocityEventChatListener(this));
            debugBridge("LEGACY_FLOODGATE_CHAT_REGISTERED true");
            if (getSettings().getChatIntercept().getMode() == Settings.ChatInterceptSettings.Mode.PACKET) {
                try {
                    new VelocityPacketChatInterceptor(this).register();
                    log(Level.INFO, "Enabled Velocity packet chat interceptor");
                } catch (Throwable throwable) {
                    log(Level.WARNING, "Failed to enable Velocity packet chat interceptor", throwable);
                }
            } else {
                log(Level.INFO, "Enabled Velocity PlayerChatEvent listener");
            }
        }
        if (placeholderApiBridge != null) {
            getProxyServer().getEventManager().register(this, placeholderApiBridge);
        }

        // Register commands & channel shortcuts
        VelocityCommand.Type.registerAll(this);
        getChannels().getChannels().forEach(channel -> channel.getShortcutCommands()
                .forEach(command -> new VelocityCommand(
                        new ShortcutCommand(command, channel.getId(), this), this
                )));
        getPlugin().log(Level.INFO, String.format("Loaded %s channels with %s associated shortcut commands",
                getChannels().getChannels().size(), getChannels().getChannels().stream()
                        .mapToInt(channel -> channel.getShortcutCommands().size()).sum()));

        VelocityVeloAirChatAPI.register(this);

        // Initialise metrics and log
        this.metrics.make(this, METRICS_ID);
        log(Level.INFO, "Enabled VeloAirChat version " + getVersion());
    }

    public boolean isBridgeDebugEnabled() {
        return getSettings().getBackendBridge().isDebug() || getSettings().getDebug().isBridge();
    }

    public void debugBridge(@NotNull String message) {
        if (isBridgeDebugEnabled()) {
            log(Level.INFO, message);
        }
    }

    @Override
    public void loadConfig() {
        VeloAirChat.super.loadConfig();
        if (getSettings().getChatAuditSignature().ensureSecret()) {
            saveSettings();
        }
        applyVelocityServerDefaults();
    }

    private void applyVelocityServerDefaults() {
        final List<String> serverNames = getProxyServer().getAllServers().stream()
                .map(RegisteredServer::getServerInfo)
                .map(info -> info.getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (serverNames.isEmpty()) {
            return;
        }

        if (getSettings().applyServerNameReplacements(serverNames)) {
            saveSettings();
        }
        if (getChannels().applyServerDefaultChannels(serverNames)) {
            saveChannels();
        }
    }

    @NotNull
    @Override
    public String getVersion() {
        return container.getDescription().getVersion()
                .orElseThrow(() -> new IllegalStateException("Could not fetch plugin version from container"));
    }

    @NotNull
    @Override
    public TagResolver getFormattingTagResolver() {
        return formattingTagResolver;
    }

    @NotNull
    @Override
    public String getPluginDescription() {
        return container.getDescription().getDescription().orElse("Unknown");
    }

    @NotNull
    @Override
    public String getPlatform() {
        return "Velocity";
    }


    @Override
    public Optional<OnlineUser> getPlayer(@NotNull UUID uuid) {
        return getProxyServer().getPlayer(uuid).map(player -> VelocityUser.adapt(player, this));
    }

    @Override
    @NotNull
    public Collection<OnlineUser> getOnlinePlayers() {
        return getProxyServer().getAllPlayers().stream()
                .map(player -> (OnlineUser) VelocityUser.adapt(player, this)).toList();
    }

    @Override
    @NotNull
    public Collection<OnlineUser> getOnlinePlayersOnServer(@NotNull OnlineUser user) {
        return ((VelocityUser) user).getPlayer().getCurrentServer()
                .map(conn -> conn.getServer().getPlayersConnected().stream()
                        .map(player -> (OnlineUser) VelocityUser.adapt(player, this)).toList())
                .orElseGet(Collections::emptyList);
    }

    @Override
    public Optional<OnlineUser> findPlayer(@NotNull String username) {
        if (username.isEmpty()) {
            return Optional.empty();
        }

        final Optional<OnlineUser> optionalPlayer;
        if (getProxyServer().getPlayer(username).isPresent()) {
            final com.velocitypowered.api.proxy.Player player = getProxyServer().getPlayer(username).get();
            optionalPlayer = Optional.of(VelocityUser.adapt(player, this));
        } else {
            final List<com.velocitypowered.api.proxy.Player> matchedPlayers = getProxyServer().matchPlayer(username)
                    .stream().filter(val -> val.getUsername().startsWith(username)).sorted().toList();
            if (!matchedPlayers.isEmpty()) {
                optionalPlayer = Optional.of(VelocityUser.adapt(matchedPlayers.get(0), this));
            } else {
                optionalPlayer = Optional.empty();
            }
        }
        return optionalPlayer;
    }

    @Nullable
    @Override
    public InputStream getResource(@NotNull String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public boolean isPluginPresent(@NotNull String dependency) {
        return getProxyServer().getPluginManager().getPlugin(dependency.toLowerCase(Locale.ENGLISH)).isPresent();
    }

    @Override
    public void signChatMessage(@NotNull OnlineUser sender, @NotNull Channel channel, @NotNull String message) {
        if (chatAuditSigner != null) {
            chatAuditSigner.sign(sender, channel, message);
        }
    }

    @NotNull
    public ProxyServer getProxyServer() {
        return server;
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... exceptions) {
        switch (level.getName()) {
            case "SEVERE" -> {
                if (exceptions.length > 0) {
                    logger.error(message, exceptions[0]);
                } else {
                    logger.error(message);
                }
            }
            case "WARNING" -> {
                if (exceptions.length > 0) {
                    logger.warn(message, exceptions[0]);
                } else {
                    logger.warn(message);
                }
            }
            default -> logger.info(message);
        }
    }

    @NotNull
    @Override
    public Audience getAudience(@NotNull UUID user) {
        return getProxyServer().getPlayer(user).map(player -> (Audience) player).orElse(Audience.empty());
    }

    @NotNull
    @Override
    public Audience getConsole() {
        return getProxyServer().getConsoleCommandSource();
    }

    @NotNull
    @Override
    public VeloAirChat getPlugin() {
        return this;
    }
}
