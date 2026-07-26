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

package de.airgalaxie.veloairchat.paper;

import de.airgalaxie.veloairchat.paper.signed.PaperSignedChatAdapter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Optional Paper extension. The Bukkit-family core never references Paper API types.
 */
public final class PaperChatCaptureListener implements Listener {

    private final VeloAirChatPaperBridge bridge;
    private final PaperSignedChatAdapter signedChat = new PaperSignedChatAdapter();

    public PaperChatCaptureListener(VeloAirChatPaperBridge bridge) {
        this.bridge = bridge;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        final String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        final boolean cancelledBefore = event.isCancelled();
        event.viewers().clear();
        event.setCancelled(true);
        bridge.capturePlatformChat(event.getPlayer(), plain, signedChat.capture(event),
                "PaperAsyncChatEvent", cancelledBefore);
    }
}
