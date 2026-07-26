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

import java.util.Objects;
import java.util.UUID;

/**
 * A policy decision made independently from its platform-specific execution.
 */
public record ChatDecision(UUID messageId, Action action, String reason) {

    public ChatDecision {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(reason, "reason");
    }

    public static ChatDecision allow(UUID messageId) {
        return new ChatDecision(messageId, Action.ALLOW, "allowed");
    }

    public enum Action {
        ALLOW,
        BLOCK,
        HOLD
    }
}
