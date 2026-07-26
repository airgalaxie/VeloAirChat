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

package de.airgalaxie.veloairchat.paper.integrations;

import de.airgalaxie.veloairchat.protocol.RenderedChatMessage;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class DynmapWebChatPublisher implements WebChatPublisher {

    private static final int RECENT_MESSAGE_LIMIT = 4096;

    private final DynmapCommonAPI dynmap;
    private final Consumer<String> debugLogger;
    private final Set<UUID> publishedMessages = new HashSet<>();

    public DynmapWebChatPublisher(@NotNull Plugin dynmapPlugin, @NotNull Consumer<String> debugLogger) {
        this((DynmapCommonAPI) dynmapPlugin, debugLogger);
    }

    DynmapWebChatPublisher(@NotNull DynmapCommonAPI dynmap, @NotNull Consumer<String> debugLogger) {
        this.dynmap = dynmap;
        this.debugLogger = debugLogger;
    }

    @Override
    public void publish(@NotNull RenderedChatMessage message) {
        if (!message.dynmapPublish()) {
            debugLogger.accept("DYNMAP_SKIP messageId=" + message.messageId() + " reason=disabled");
            return;
        }
        if (!remember(message.messageId())) {
            debugLogger.accept("DYNMAP_SKIP messageId=" + message.messageId() + " reason=duplicate");
            return;
        }

        try {
            dynmap.postPlayerMessageToWeb(message.dynmapName(), message.dynmapName(), message.dynmapMessage());
            debugLogger.accept("DYNMAP_PUBLISH messageId=" + message.messageId()
                    + " backend=" + message.sourceServer()
                    + " success=true");
        } catch (RuntimeException exception) {
            debugLogger.accept("DYNMAP_ERROR messageId=" + message.messageId()
                    + " reason=" + exception.getClass().getSimpleName());
        }
    }

    private boolean remember(UUID messageId) {
        if (!publishedMessages.add(messageId)) {
            return false;
        }
        if (publishedMessages.size() > RECENT_MESSAGE_LIMIT) {
            final List<UUID> retained = new ArrayList<>(publishedMessages).subList(
                    publishedMessages.size() / 2,
                    publishedMessages.size()
            );
            publishedMessages.clear();
            publishedMessages.addAll(retained);
        }
        return true;
    }

}
