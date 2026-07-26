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

package de.airgalaxie.veloairchat.fabric.integrations;

import de.airgalaxie.veloairchat.protocol.RenderedChatMessage;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.slf4j.Logger;

public final class DynmapWebChatPublisher extends DynmapCommonAPIListener {

    private final Logger logger;
    private volatile DynmapCommonAPI dynmap;

    public DynmapWebChatPublisher(Logger logger) {
        this.logger = logger;
    }

    public void register() {
        DynmapCommonAPIListener.register(this);
    }

    @Override
    public void apiEnabled(DynmapCommonAPI api) {
        dynmap = api;
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        if (dynmap == api) {
            dynmap = null;
        }
    }

    public void publish(RenderedChatMessage message) {
        if (!message.dynmapPublish()) {
            return;
        }
        final DynmapCommonAPI api = dynmap;
        if (api == null) {
            logger.debug("Skipping Dynmap message {} because its API is unavailable", message.messageId());
            return;
        }
        try {
            api.postPlayerMessageToWeb(
                    message.dynmapName(),
                    message.dynmapName(),
                    message.dynmapMessage()
            );
        } catch (RuntimeException exception) {
            logger.warn("Failed to publish VeloAirChat message {} to Dynmap", message.messageId(), exception);
        }
    }
}
