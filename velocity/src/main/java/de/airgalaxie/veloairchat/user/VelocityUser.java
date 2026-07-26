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

package de.airgalaxie.veloairchat.user;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.util.TriState;
import de.airgalaxie.veloairchat.VeloAirChat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Velocity implementation of a cross-platform {@link OnlineUser}
 */
public class VelocityUser extends OnlineUser {

    private final com.velocitypowered.api.proxy.Player player;

    private VelocityUser(@NotNull Player player, @NotNull VeloAirChat plugin) {
        super(player.getUsername(), player.getUniqueId(), plugin);
        this.player = player;
    }

    @NotNull
    public static VelocityUser adapt(@NotNull Player player, @NotNull VeloAirChat plugin) {
        return new VelocityUser(player, plugin);
    }

    @Override
    public int getPing() {
        return (int) player.getPing();
    }

    @Override
    @NotNull
    public String getServerName() {
        final Optional<ServerConnection> connection = player.getCurrentServer();
        if (connection.isPresent()) {
            return connection.get().getServerInfo().getName();
        }
        return "";
    }

    @Override
    public int getPlayersOnServer() {
        return player.getCurrentServer().map(conn -> conn.getServer().getPlayersConnected().size()).orElse(0);
    }

    @Override
    public boolean hasPermission(@Nullable String permission, boolean allowByDefault) {
        if (permission == null) {
            return allowByDefault;
        }
        final TriState state = player.getPermissionValue(permission).toAdventureTriState();
        if (state != TriState.NOT_SET) {
            return state == TriState.TRUE;
        }
        if (permission.startsWith("veloairchat.")) {
            if (permission.equals("veloairchat.command.veloairchat")) {
                final TriState oldCommandState = player.getPermissionValue("huskchat.command.huskchat").toAdventureTriState();
                if (oldCommandState != TriState.NOT_SET) {
                    return oldCommandState == TriState.TRUE;
                }
            }
            final String legacyPermission = "huskchat." + permission.substring("veloairchat.".length());
            final TriState legacyState = player.getPermissionValue(legacyPermission).toAdventureTriState();
            if (legacyState != TriState.NOT_SET) {
                return legacyState == TriState.TRUE;
            }
        }
        return allowByDefault;
    }

    @NotNull
    @Override
    public Audience getAudience() {
        return player;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

}
