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

package de.airgalxie.veloairchat.api;

import com.velocitypowered.api.proxy.Player;
import de.airgalxie.veloairchat.VeloAirChat;
import de.airgalxie.veloairchat.VelocityVeloAirChat;
import de.airgalxie.veloairchat.user.VelocityUser;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class VelocityVeloAirChatAPI extends VeloAirChatAPI {

    private VelocityVeloAirChatAPI(@NotNull VeloAirChat plugin) {
        super(plugin);
    }

    @NotNull
    public static VelocityVeloAirChatAPI getInstance() {
        return (VelocityVeloAirChatAPI) instance;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static void register(@NotNull VelocityVeloAirChat plugin) {
        VeloAirChatAPI.instance = new VelocityVeloAirChatAPI(plugin);
    }

    /**
     * Adapts a platform-specific Player object to a cross-platform Player object
     * @param player Must be a platform-specific Player object, e.g. a Velocity Player
     * @return {@link VelocityUser}
     */
    @NotNull
    public VelocityUser adaptPlayer(@NotNull Player player) {
        return VelocityUser.adapt(player, plugin);
    }
}
