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
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DynmapWebChatPublisherTest {

    @Test
    void publishesAccountPlayerNameAndFullyFormattedMessage() {
        final AtomicReference<String[]> arguments = new AtomicReference<>();
        final DynmapCommonAPI dynmap = (DynmapCommonAPI) Proxy.newProxyInstance(
                DynmapCommonAPI.class.getClassLoader(),
                new Class<?>[]{DynmapCommonAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("postPlayerMessageToWeb")) {
                        arguments.set(new String[]{(String) args[0], (String) args[1], (String) args[2]});
                    }
                    return defaultValue(method.getReturnType());
                }
        );
        final RenderedChatMessage message = new RenderedChatMessage(
                UUID.randomUUID(), "server1", List.of(), "", true,
                "server1", "airgalaxie", "[J] G server1 airgalaxie: Hallo"
        );
        final DynmapWebChatPublisher publisher = new DynmapWebChatPublisher(
                LoggerFactory.getLogger(DynmapWebChatPublisherTest.class)
        );
        publisher.apiEnabled(dynmap);

        publisher.publish(message);

        assertArrayEquals(
                new String[]{"airgalaxie", "airgalaxie", "[J] G server1 airgalaxie: Hallo"},
                arguments.get()
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
