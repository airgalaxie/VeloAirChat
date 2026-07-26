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

package de.airgalxie.veloairchat.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import de.airgalxie.veloairchat.VeloAirChat;
import de.airgalxie.veloairchat.config.Settings;
import de.airgalxie.veloairchat.user.OnlineUser;
import de.airgalxie.veloairchat.util.MessageFormatter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents a broadcast message to be sent to everyone
 */
public class BroadcastMessage {

    private final Settings.BroadcastSettings settings;
    private final OnlineUser sender;
    private final VeloAirChat plugin;
    private String message;

    public BroadcastMessage(@NotNull OnlineUser sender, @NotNull String message, @NotNull VeloAirChat plugin) {
        this.sender = sender;
        this.plugin = plugin;
        this.settings = plugin.getSettings().getBroadcastCommand();
        this.message = message;
    }

    /**
     * Dispatch the broadcast message to be sent
     */
    public void dispatch() {
        plugin.fireBroadcastMessageEvent(sender, message).thenAccept(event -> {
            if (event.isCancelled()) {
                return;
            }
            message = event.getMessage();

            // If the message is to be filtered, then perform filter checks (unless they have the bypass permission)
            final Optional<String> filtered = plugin.filter(sender, message, plugin.getBroadcastFilters());
            if (filtered.isEmpty()) {
                return;
            }
            message = filtered.get();

            // Send the broadcast
            plugin.getOnlinePlayers().forEach(this::sendMessage);

            // Log to console
            if (settings.isLogToConsole()) {
                plugin.log(Level.INFO, settings.getLogFormat() + message);
            }
        });
    }
    public void sendMessage(@NotNull OnlineUser player) {
        final TextComponent.Builder componentBuilder = Component.text();
        componentBuilder.append(MessageFormatter.format(
                plugin.getSettings().getBroadcastCommand().getFormat(),
                sender.getAudience(),
                plugin.getFormattingTagResolver()
        ));
        componentBuilder.append(MessageFormatter.formatTrustedUserMessage(
                message,
                sender.getAudience(),
                plugin.getFormattingTagResolver()
        ));
        player.sendMessage(componentBuilder.build());
    }
}
