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

package de.airgalxie.veloairchat.event;

import com.velocitypowered.api.proxy.ProxyServer;
import de.airgalxie.veloairchat.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface VelocityEventProvider extends EventProvider {

    @Override
    default CompletableFuture<ChatMessageEvent> fireChatMessageEvent(@NotNull OnlineUser player,
                                                                     @NotNull String message,
                                                                     @NotNull String channelId) {
        return getProxyServer().getEventManager().fire(new VelocityChatMessageEvent(player, message, channelId));
    }

    @Override
    default CompletableFuture<PrivateMessageEvent> firePrivateMessageEvent(@NotNull OnlineUser sender,
                                                                           @NotNull List<OnlineUser> receivers,
                                                                           @NotNull String message) {
        return getProxyServer().getEventManager().fire(new VelocityPrivateMessageEvent(sender, receivers, message));
    }

    @Override
    default CompletableFuture<BroadcastMessageEvent> fireBroadcastMessageEvent(@NotNull OnlineUser sender,
                                                                               @NotNull String message) {
        return getProxyServer().getEventManager().fire(new VelocityBroadcastMessageEvent(sender, message));
    }

    @NotNull
    ProxyServer getProxyServer();

}
