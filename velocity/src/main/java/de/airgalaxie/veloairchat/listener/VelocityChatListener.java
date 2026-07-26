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

import com.velocitypowered.api.event.player.PlayerChatEvent;
import de.airgalaxie.veloairchat.VeloAirChat;
import de.airgalaxie.veloairchat.channel.Channel;
import de.airgalaxie.veloairchat.message.ChatMessage;
import de.airgalaxie.veloairchat.user.VelocityUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface VelocityChatListener {

    default boolean handlePlayerChat(PlayerChatEvent e) {
        final VelocityUser player = VelocityUser.adapt(e.getPlayer(), plugin());
        final Optional<Channel> channel = plugin().getUserCache().getPlayerChannel(player.getUuid())
                .flatMap(channelId -> plugin().getChannels().getChannel(channelId));
        if (channel.isEmpty()) {
            plugin().getLocales().sendMessage(player, "error_no_channel");
            return true;
        }

        return new ChatMessage(channel.get(), player, e.getMessage(), plugin()).dispatch();
    }

    @NotNull
    VeloAirChat plugin();

}
