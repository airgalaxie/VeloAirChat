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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Runs independent analyzers and combines their neutral findings.
 */
public final class CompositeChatAnalyzer implements ChatAnalyzer {

    private final List<ChatAnalyzer> analyzers;

    public CompositeChatAnalyzer(List<ChatAnalyzer> analyzers) {
        this.analyzers = List.copyOf(analyzers);
    }

    @Override
    public CompletionStage<AnalysisResult> analyze(AnalysisRequest request) {
        Objects.requireNonNull(request, "request");
        final List<CompletableFuture<AnalysisResult>> results = analyzers.stream()
                .map(analyzer -> analyzer.analyze(request).toCompletableFuture())
                .toList();
        return CompletableFuture.allOf(results.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> new AnalysisResult(
                        request.messageId(),
                        results.stream()
                                .map(CompletableFuture::join)
                                .peek(result -> validateMessageId(request, result))
                                .flatMap(result -> result.findings().stream())
                                .toList()
                ));
    }

    private void validateMessageId(AnalysisRequest request, AnalysisResult result) {
        if (!request.messageId().equals(result.messageId())) {
            throw new IllegalArgumentException("Analyzer returned findings for a different message");
        }
    }
}
