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

package de.airgalaxie.veloairchat.protocol;

import java.util.Objects;
import java.util.UUID;

/**
 * Untrusted platform observation. Velocity resolves the authoritative identity.
 */
public record ChatIdentityObservation(UUID connectionUuid, String observedName, String adapter) {

    public ChatIdentityObservation {
        Objects.requireNonNull(connectionUuid, "connectionUuid");
        Objects.requireNonNull(observedName, "observedName");
        Objects.requireNonNull(adapter, "adapter");
        if (observedName.length() > 64 || adapter.isBlank() || adapter.length() > 128) {
            throw new IllegalArgumentException("Invalid identity observation");
        }
    }
}
