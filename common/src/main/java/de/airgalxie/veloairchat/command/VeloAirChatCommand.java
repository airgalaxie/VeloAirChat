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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import de.airgalxie.veloairchat.VeloAirChat;
import de.airgalxie.veloairchat.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class VeloAirChatCommand extends CommandBase {

    private final static String[] COMMAND_TAB_ARGUMENTS = {"about", "reload"};
    private final MiniMessage mm = MiniMessage.miniMessage();

    public VeloAirChatCommand(@NotNull VeloAirChat plugin) {
        // Velocity nutzt meist List.of für Aliase
        super(List.of("veloairchat", "huskchat"), "[about|reload]", plugin);
        this.operatorOnly = true;
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length >= 1) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "about", "info" -> sendAboutMenu(player);
                case "reload" -> {
                    plugin.loadConfig();
                    // MiniMessage output
                    player.sendMessage(mm.deserialize("<bold><#00fb9a>VeloAirChat</#00fb9a></bold> <gray>»</gray> <#00fb9a>Konfiguration und Nachrichten neu geladen."));
                }
                default -> plugin.getLocales().sendMessage(player, "error_invalid_syntax", getUsage());
            }
            return;
        }
        sendAboutMenu(player);
    }

    // Da Desertwell (AboutMenu) entfernt wurde, hier eine native Adventure-Lösung
    private void sendAboutMenu(@NotNull OnlineUser player) {
        Component about = mm.deserialize(
                "<separator><br>" +
                        "<gradient:#00fb9a:#007d4d><bold>VeloAirChat</bold></gradient><br>" +
                        "<gray>Version " + plugin.getVersion() + "</gray><br>" +
                        "<gray>Entwickelt von <white>AirGalxie</white></gray><br>" +
                        "<gray>Basierend auf HuskChat von <white>William278</white></gray><br><br>" +
                        "<click:open_url:'https://airgalxie.de'><hover:show_text:'airgalxie.de öffnen'>[Projekt]</hover></click><br>" +
                        "<separator>"
        );
        player.sendMessage(about);
    }

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length <= 1) {
            return Arrays.stream(COMMAND_TAB_ARGUMENTS)
                    .filter(i -> i.toLowerCase().startsWith((args.length == 1) ? args[0].toLowerCase() : ""))
                    .sorted().toList();
        }
        return List.of();
    }
}
