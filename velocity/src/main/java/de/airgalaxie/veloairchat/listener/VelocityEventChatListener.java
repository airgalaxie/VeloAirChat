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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import de.airgalaxie.veloairchat.VeloAirChat;
import org.jetbrains.annotations.NotNull;

public record VelocityEventChatListener(@NotNull VeloAirChat plugin) implements VelocityChatListener {

    @Subscribe(order = PostOrder.LATE)
    public void onPlayerChat(PlayerChatEvent e) {
        if (!e.getResult().isAllowed()) {
            return;
        }

        final boolean shouldStopOriginalChat = this.handlePlayerChat(e);
        if (shouldStopOriginalChat && !usesSensitiveSignedChatProtocol(e.getPlayer().getProtocolVersion())) {
            e.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }

    private boolean usesSensitiveSignedChatProtocol(@NotNull ProtocolVersion protocolVersion) {
        return protocolVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_19_1.getProtocol();
    }

    @Override
    @NotNull
    public VeloAirChat plugin() {
        return plugin;
    }

}
