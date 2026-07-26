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
import java.util.Map.Entry;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import de.airgalaxie.veloairchat.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class for loading and storing {@link Channel}s
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Channels {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      VeloAirChat - Channels  ┃
            ┃    Maintained by AirGalaxie   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://static-mc.airgalaxie.de/
            ┗╸ Original project: HuskChat by William278""";

    @Comment("The default chat channel players are placed in (can be overridden by server_default_channels)")
    private String defaultChannel = "global";

    @Comment("Map of server names to a channel players will be automatically moved into when they join that server")
    private Map<String, String> serverDefaultChannels = Map.of("example", "global");

    @Comment("The format of log messages (applies to channels with logging enabled)")
    private String channelLogFormat = "[CHAT] [%channel%] %sender%: ";

    @Comment("Aliases for the /channel command")
    @Getter(AccessLevel.NONE)
    private List<String> channelCommandAliases = List.of("channel", "c");

    @Comment("Channel definitions")
    private List<Channel> channels = List.of(
            // Local channel
            Channel.builder()
                    .id("local")
                    .format("<dark_gray>[</dark_gray><gray>%server%</gray><dark_gray>]</dark_gray> %fullname%<reset><white>: ")
                    .broadcastScope(Channel.BroadcastScope.LOCAL)
                    .shortcutCommands(List.of("/local", "/l"))
                    .build(),

            // Global channel
            Channel.builder()
                    .id("global")
                    .format("<#00fb9a>[G]<reset><dark_gray>[</dark_gray><gray>%server%</gray><dark_gray>]</dark_gray><white> %fullname%<reset><white>: ")
                    .broadcastScope(Channel.BroadcastScope.GLOBAL)
                    .shortcutCommands(List.of("/global", "/g"))
                    .build(),

            // Staff channel
            Channel.builder()
                    .id("staff")
                    .format("<yellow>[Staff]</yellow> %name%: <gray>")
                    .broadcastScope(Channel.BroadcastScope.GLOBAL)
                    .filtered(false)
                    .permissions(Channel.ChannelPermissions.builder()
                            .send("veloairchat.channel.staff.send")
                            .receive("veloairchat.channel.staff.receive")
                            .build())
                    .shortcutCommands(List.of("/staff", "/sc"))
                    .build(),

            // HelpOp channel
            Channel.builder()
                    .id("helpop")
                    .format("<#00fb9a>[HelpOp]</#00fb9a> %name%:<gray>")
                    .broadcastScope(Channel.BroadcastScope.GLOBAL)
                    .filtered(false)
                    .permissions(Channel.ChannelPermissions.builder()
                            .receive("veloairchat.channel.helpop.receive")
                            .build())
                    .shortcutCommands(List.of("/helpop", "/helpme"))
                    .build()
    );

    public Optional<Channel> getChannel(@Nullable String channelId) {
        if (channelId == null) {
            return Optional.empty();
        }
        return channels.stream().filter(channel -> channel.getId().equalsIgnoreCase(channelId)).findFirst();
    }

    /**
     * Gets the default channel for the given server. Falls back to the global default is a server-specific default does not exist.
     * @param server The server name
     * @return The default channel for the given server, if any
     */
    public Optional<String> getServerDefaultChannel(String server) {
        return getServerDefaultChannels().entrySet().stream().filter(
                defaultChannelEntry -> Pattern.compile(defaultChannelEntry.getKey(),
                    Pattern.CASE_INSENSITIVE).matcher(server).matches()).map(Entry::getValue)
            .findFirst();
    }

    @NotNull
    public List<String> getChannelCommandAliases() {
        return Settings.formatCommands(channelCommandAliases);
    }

    public boolean applyServerDefaultChannels(@NotNull List<String> serverNames) {
        final Map<String, String> updated = new LinkedHashMap<>(serverDefaultChannels);
        boolean changed = updated.remove("example") != null;
        for (String serverName : serverNames) {
            if (!updated.containsKey(serverName)) {
                updated.put(serverName, defaultChannel);
                changed = true;
            }
        }
        if (changed) {
            serverDefaultChannels = updated;
        }
        return changed;
    }

}
