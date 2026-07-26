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

package de.airgalaxie.veloairchat.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import de.airgalaxie.veloairchat.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for loading and storing plugin settings
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Settings {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃       VeloAirChat - Config   ┃
            ┃    Maintained by AirGalaxie   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://static-mc.airgalaxie.de/
            ┣╸ Config Help: https://static-mc.airgalaxie.de/
            ┗╸ Original project: HuskChat by William278""";

    @Comment("Locale of the default language file to use.")
    private String language = Locales.DEFAULT_LOCALE;

    @Comment("Message command settings")
    private MessageSettings messageCommand = new MessageSettings();

    @Comment("PlaceholderAPI bridge settings. Requires a VeloAirChat backend bridge on backend servers.")
    private PlaceholderApiBridgeSettings placeholderApiBridge = new PlaceholderApiBridgeSettings();

    @Comment("Backend chat bridge settings. When enabled, backend ChatEnvelope packets are the only public chat input.")
    private BackendBridgeSettings backendBridge = new BackendBridgeSettings();

    @Comment("Debug settings.")
    private DebugSettings debug = new DebugSettings();

    @Comment("Optional integrations configured centrally on Velocity.")
    private IntegrationSettings integrations = new IntegrationSettings();

    @Comment("Main/Velocity audit signature settings. This does not replace Mojang signed chat.")
    private ChatAuditSignatureSettings chatAuditSignature = new ChatAuditSignatureSettings();

    @Comment("Main/Velocity chat delivery settings")
    private ChatDeliverySettings chatDelivery = new ChatDeliverySettings();

    @Comment("Main/Velocity chat intercept settings")
    private ChatInterceptSettings chatIntercept = new ChatInterceptSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PlaceholderApiBridgeSettings {
        private boolean enabled = false;
        private int timeoutMilliseconds = 500;
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BackendBridgeSettings {
        private boolean enabled = true;
        private boolean debug = false;
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DebugSettings {
        private boolean bridge = false;
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class IntegrationSettings {
        private DynmapSettings dynmap = new DynmapSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class DynmapSettings {
            private boolean enabled = false;
            private boolean publishGlobal = true;
            private boolean publishLocal = false;
            private String format = "<platform> <scope> <server> <player>: <message>";
        }
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ChatAuditSignatureSettings {
        private boolean enabled = true;
        @Comment("Generated automatically. Keep private; this signs VeloAirChat audit log entries.")
        private String secret = "";

        public boolean ensureSecret() {
            if (secret != null && !secret.isBlank()) {
                return false;
            }
            final byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            secret = Base64.getUrlEncoder().withoutPadding().encodeToString(key);
            return true;
        }
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ChatDeliverySettings {
        @Comment({
                "Deprecated: VeloAirChat now always sends one canonical formatted chat message to all qualified recipients.",
                "Backend bridge plugins cancel their local platform chat output instead of relying on source-server suppression."
        })
        private boolean suppressProxyEchoOnSourceServer = false;
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ChatInterceptSettings {
        private Mode mode = Mode.EVENT;
        private boolean fallbackToEventListener = true;

        public enum Mode {
            PACKET,
            EVENT
        }
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MessageSettings {
        @Comment("Whether to enable the /msg command")
        private boolean enabled = true;

        @Comment("List of command aliases for /msg")
        @Getter(AccessLevel.NONE)
        private List<String> msgAliases = List.of("/msg", "/m", "/tell", "/whisper", "/w", "/pm");

        @Comment("List of command aliases for /reply")
        @Getter(AccessLevel.NONE)
        private List<String> replyAliases = List.of("/reply", "/r");

        @Comment("Whether to apply censorship filters on private messages")
        private boolean censor = false;

        @Comment("Whether to log private messages to the console")
        private boolean logToConsole = true;

        @Comment("Logging format for private messages")
        private String logFormat = "[MSG] [%sender% -> %receiver%]: ";

        @Comment("Group private message settings")
        private GroupSettings groupMessages = new GroupSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class GroupSettings {
            @Comment("Whether to enable group private messages (/msg Player1,Player2,...)")
            private boolean enabled = true;

            @Comment("Maximum amount of players in a group message")
            private int maxSize = 10;
        }

        @Comment("Formats for private messages (uses MiniMessage; legacy & color codes are supported)")
        private MessageFormat format = new MessageFormat();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class MessageFormat {
            private String inbound = "<yellow><bold>%name%</bold> <dark_gray>→</dark_gray> <yellow><bold>You</bold><dark_gray>: </dark_gray><white>";
            private String outbound = "<yellow><bold>You</bold> <dark_gray>→</dark_gray> <yellow><bold>%name%</bold><dark_gray>: </dark_gray><white>";
            private String groupInbound = "<yellow><bold>%name%</bold> <dark_gray>→</dark_gray> <yellow><bold>You</bold><gray><hover:show_text:'%group_members%'>[₍₊%group_amount_subscript%₎]</hover></gray><dark_gray>: </dark_gray><white>";
            private String groupOutbound = "<yellow><bold>You</bold> <dark_gray>→</dark_gray> <yellow><bold>%name%</bold><gray><hover:show_text:'%group_members%'>[₍₊%group_amount_subscript%₎]</hover></gray><dark_gray>: </dark_gray><white>";
        }

        @Comment("List of servers where private messages cannot be sent")
        private List<String> restrictedServers = List.of();

        @NotNull
        public List<String> getMsgAliases() {
            return formatCommands(msgAliases);
        }

        @NotNull
        public List<String> getReplyAliases() {
            return formatCommands(replyAliases);
        }


    }

    @Comment("Social spy settings (see other users' private messages)")
    private SocialSpySettings socialSpy = new SocialSpySettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SocialSpySettings {
        private boolean enabled = true;
        private String format = "<yellow>[Spy]</yellow> <gray>%name% <dark_gray>→</dark_gray> %receiver_name%:</gray>%spy_color% ";
        private String groupFormat = "<yellow>[Spy]</yellow> <gray>%name% <dark_gray>→</dark_gray> %receiver_name% <hover:show_text:'%group_members%'><click:suggest_command:'/msg %group_members_comma_separated%'>[₍₊%group_amount_subscript%₎]</click></hover>:</gray>%spy_color% ";
        @Getter(AccessLevel.NONE)
        private List<String> socialspyAliases = List.of("/socialspy", "/ss");

        @NotNull
        public List<String> getSocialspyAliases() {
            return formatCommands(socialspyAliases);
        }
    }


    @Comment("Local spy settings (see local messages on other servers)")
    private LocalSpySettings localSpy = new LocalSpySettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LocalSpySettings {
        private boolean enabled = true;
        private String format = "<yellow>[Spy]</yellow> <gray>[%channel%] %name%<dark_gray>:</dark_gray></gray>%spy_color% ";
        @Getter(AccessLevel.NONE)
        private List<String> localspyAliases = List.of("/localspy", "/ls");
        @Comment("List of channels to exclude from local spy")
        private List<String> excludedLocalChannels = List.of();

        @NotNull
        public List<String> getLocalspyAliases() {
            return formatCommands(localspyAliases);
        }
    }

    @Comment("Broadcast command settings")
    private BroadcastSettings broadcastCommand = new BroadcastSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BroadcastSettings {
        private boolean enabled = true;
        @Getter(AccessLevel.NONE)
        private List<String> broadcastAliases = List.of("/broadcast", "/alert");
        private String format = "<gold>[Broadcast]</gold><yellow> ";
        private boolean logToConsole = true;
        private String logFormat = "[BROADCAST]: ";

        @NotNull
        public List<String> getBroadcastAliases() {
            return formatCommands(broadcastAliases);
        }
    }

    @Comment("Join and quit message settings")
    private JoinQuitSettings joinAndQuitMessages = new JoinQuitSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JoinQuitSettings {

        @Comment({"Use the \"veloairchat.join_message.[text]\" permission to override this.",
                "Use the \"veloairchat.silent_join\" permission to silence for a user."})
        private ConnectionMessage join = new ConnectionMessage(true, "<yellow>%name% joined the network</yellow>");

        @Comment({"Use the \"veloairchat.quit_message.[text]\" permission to override this.",
                "Use the \"veloairchat.silent_quit\" permission to silence for a user."})
        private ConnectionMessage quit = new ConnectionMessage(true, "<yellow>%name% left the network</yellow>");

        @Comment("Note that PASSTHROUGH modes won't cancel backend join/quit messages")
        private Channel.BroadcastScope broadcastScope = Channel.BroadcastScope.GLOBAL;

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static class ConnectionMessage {
            private boolean enabled;
            private String format;
        }
    }
    @Comment("Custom names to display wherever you use the \"%server%\" placeholder instead of their default name")
    private Map<String, String> serverNameReplacement = new HashMap<>(
            Map.of("very-long-server-name", "VLSN")
    );

    public boolean applyServerNameReplacements(@NotNull List<String> serverNames) {
        final Map<String, String> updated = new LinkedHashMap<>(serverNameReplacement);
        boolean changed = updated.remove("very-long-server-name") != null;
        for (String serverName : serverNames) {
            if (!updated.containsKey(serverName)) {
                updated.put(serverName, serverName);
                changed = true;
            }
        }
        if (changed) {
            serverNameReplacement = updated;
        }
        return changed;
    }

    @NotNull
    public static List<String> formatCommands(@NotNull List<String> rawCommands) {
        return rawCommands.stream().map(c -> c.startsWith("/") ? c.substring(1) : c).toList();
    }
}
