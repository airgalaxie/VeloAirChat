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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import de.airgalaxie.veloairchat.VeloAirChat;
import de.airgalaxie.veloairchat.VelocityVeloAirChat;
import de.airgalaxie.veloairchat.user.VelocityUser;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class VelocityPlayerListener extends PlayerListener {

    private final VelocityVeloAirChat velocityPlugin;

    public VelocityPlayerListener(@NotNull VelocityVeloAirChat plugin) {
        super(plugin);
        this.velocityPlugin = plugin;
    }

    @Subscribe
    public void onPlayerChangeServer(ServerConnectedEvent e) {
        final String server = e.getServer().getServerInfo().getName();
        final boolean firstServer = e.getPreviousServer().isEmpty();

        velocityPlugin.getProxyServer().getScheduler().buildTask(velocityPlugin, () -> {
            e.getPlayer().getCurrentServer()
                    .filter(current -> current.getServerInfo().getName().equals(server))
                    .ifPresent(current -> {
                        final VelocityUser player = VelocityUser.adapt(e.getPlayer(), plugin);
                        if (firstServer) {
                            handlePlayerJoin(player);
                        } else {
                            handlePlayerSwitchServer(player, server);
                        }
                    });
        }).delay(250L, TimeUnit.MILLISECONDS).schedule();
    }

    @Subscribe
    public void onPlayerQuitNetwork(DisconnectEvent e) {
        if (e.getLoginStatus() == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {
            handlePlayerQuit(VelocityUser.adapt(e.getPlayer(), plugin));
        }
    }

}
