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

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatModerationPipelineTest {

    @Test
    void defaultPipelinePreservesTheExistingAllowBehavior() {
        final AnalysisRequest request = request(UUID.randomUUID());
        final ChatModerationPipeline pipeline = new ChatModerationPipeline(
                new NoOpChatAnalyzer(), new AllowAllDecisionEngine());

        final ChatDecision decision = pipeline.process(request).toCompletableFuture().join();

        assertEquals(request.messageId(), decision.messageId());
        assertEquals(ChatDecision.Action.ALLOW, decision.action());
    }

    @Test
    void analyzerAndDecisionEngineRemainSeparateStages() {
        final AnalysisRequest request = request(UUID.randomUUID());
        final AnalysisFinding finding = new AnalysisFinding(
                "test-analyzer", "test-finding", 0.5d, java.util.Map.of("key", "value"));
        final ChatModerationPipeline pipeline = new ChatModerationPipeline(
                ignored -> java.util.concurrent.CompletableFuture.completedFuture(
                        new AnalysisResult(request.messageId(), java.util.List.of(finding))),
                result -> {
                    assertTrue(result.findings().contains(finding));
                    return ChatDecision.allow(result.messageId());
                });

        final ChatDecision decision = pipeline.process(request).toCompletableFuture().join();

        assertEquals(ChatDecision.Action.ALLOW, decision.action());
    }

    private AnalysisRequest request(UUID messageId) {
        return new AnalysisRequest(
                messageId,
                UUID.randomUUID(),
                "Spieler",
                "global",
                "lobby",
                "Hallo",
                "Hallo",
                "JAVA_PROFILE",
                "SIGNED",
                "JAVA_SIGNATURE_PRESENT",
                1L
        );
    }
}
