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

package de.airgalaxie.veloairchat.moderation;

import java.util.Map;
import java.util.Objects;

/**
 * A backend-neutral observation produced while analyzing a chat message.
 */
public record AnalysisFinding(
        String source,
        String type,
        double score,
        Map<String, String> attributes
) {

    public AnalysisFinding {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        attributes = Map.copyOf(attributes);
    }
}
