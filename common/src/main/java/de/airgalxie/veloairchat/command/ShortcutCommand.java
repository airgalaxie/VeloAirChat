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

package de.airgalxie.veloairchat.command;

import de.airgalxie.veloairchat.VeloAirChat;
import de.airgalxie.veloairchat.channel.Channel;
import de.airgalxie.veloairchat.message.ChatMessage;
import de.airgalxie.veloairchat.user.ConsoleUser;
import de.airgalxie.veloairchat.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class ShortcutCommand extends CommandBase {
    private final String channelId;

    public ShortcutCommand(@NotNull String command, @NotNull String channelId, @NotNull VeloAirChat plugin) {
        super(List.of(command), "[message]", plugin);
        this.channelId = channelId;
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length == 0) {
            // Console can't chat in the same way as players can, it can only use commands.
            // So no need to allow it to switch channels.
            if (player instanceof ConsoleUser) {
                plugin.getLocales().sendMessage(player, "error_console_switch_channels");
                return;
            }
            plugin.editUserCache(c -> c.switchPlayerChannel(player, channelId, plugin));
        } else {
            StringJoiner message = new StringJoiner(" ");
            for (String arg : args) {
                message.add(arg);
            }

            final Optional<Channel> optionalChannel = plugin.getChannels().getChannel(channelId);
            if (optionalChannel.isEmpty()) {
                plugin.getLocales().sendMessage(player, "error_no_channel");
                return;
            }

            final Channel channel = optionalChannel.get();
            if (channel.getBroadcastScope().isPassThrough()) {
                plugin.getLocales().sendMessage(player, "error_passthrough_shortcut_command");
                return;
            }
            new ChatMessage(channel, player, message.toString(), plugin).dispatch();
        }
    }

    @Override
    @Nullable
    public String getPermission(@NotNull String... args) {
        return plugin.getChannels().getChannel(channelId)
                .map(Channel::getPermissions)
                .flatMap(Channel.ChannelPermissions::getSend)
                .orElse(null);
    }

}
