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

package de.airgalaxie.veloairchat.context;

import de.airgalaxie.veloairchat.VeloAirChat;
import de.airgalaxie.veloairchat.channel.Channel;
import de.airgalaxie.veloairchat.getter.DataGetter;
import de.airgalaxie.veloairchat.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ChatContextFactory {

    private ChatContextFactory() {
    }

    @NotNull
    public static ChatContext create(@NotNull VeloAirChat plugin, @NotNull OnlineUser sender,
                                     @NotNull Channel channel, @NotNull String message) {
        return create(plugin, sender, channel, message, new Metadata(
                UUID.randomUUID(),
                plugin.getPlatform(),
                sender.getServerName(),
                "PROXY_CONNECTION",
                "UNKNOWN",
                "PROXY_VERIFIED",
                "",
                0L,
                System.currentTimeMillis(),
                message
        ));
    }

    @NotNull
    public static ChatContext create(@NotNull VeloAirChat plugin, @NotNull OnlineUser sender,
                                     @NotNull Channel channel, @NotNull String message,
                                     @NotNull Metadata metadata) {
        final DataGetter data = plugin.getDataGetter();
        final String backend = metadata.backend();
        final String server = plugin.getSettings().getServerNameReplacement().getOrDefault(backend, backend);
        return new ChatContext(
                metadata.messageId(),
                metadata.platform(),
                scope(channel.getBroadcastScope()),
                server,
                backend,
                data.getPlayerName(sender),
                sender.getUuid(),
                data.getPlayerFullName(sender),
                data.getPlayerPrefix(sender).orElse(""),
                data.getPlayerSuffix(sender).orElse(""),
                data.getPlayerGroupName(sender).orElse(""),
                data.getPlayerGroupDisplayName(sender).orElse(""),
                message,
                metadata.originalMessage(),
                channel.getId(),
                metadata.identityProvider(),
                metadata.signedState(),
                metadata.trustState(),
                metadata.signedAdapter(),
                metadata.signedTimestamp(),
                metadata.timestamp(),
                sender.getPing(),
                sender.getPlayersOnServer()
        );
    }

    private static String scope(Channel.BroadcastScope scope) {
        return scope.isOneOf(
                Channel.BroadcastScope.LOCAL,
                Channel.BroadcastScope.LOCAL_PASSTHROUGH,
                Channel.BroadcastScope.PASSTHROUGH
        ) ? "L" : "G";
    }

    public record Metadata(
            UUID messageId,
            String platform,
            String backend,
            String identityProvider,
            String signedState,
            String trustState,
            String signedAdapter,
            long signedTimestamp,
            long timestamp,
            String originalMessage
    ) {
    }
}
