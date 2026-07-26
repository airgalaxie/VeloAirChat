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
import java.util.concurrent.CompletionStage;

/**
 * Coordinates analysis and decision-making without executing the result.
 */
public final class ChatModerationPipeline {

    private final ChatAnalyzer analyzer;
    private final DecisionEngine decisionEngine;

    public ChatModerationPipeline(ChatAnalyzer analyzer, DecisionEngine decisionEngine) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
        this.decisionEngine = Objects.requireNonNull(decisionEngine, "decisionEngine");
    }

    public CompletionStage<ChatDecision> process(AnalysisRequest request) {
        return analyzer.analyze(request).thenApply(result -> {
            if (!request.messageId().equals(result.messageId())) {
                throw new IllegalArgumentException("Analysis result belongs to a different message");
            }
            final ChatDecision decision = decisionEngine.decide(result);
            if (!request.messageId().equals(decision.messageId())) {
                throw new IllegalArgumentException("Chat decision belongs to a different message");
            }
            return decision;
        });
    }
}
