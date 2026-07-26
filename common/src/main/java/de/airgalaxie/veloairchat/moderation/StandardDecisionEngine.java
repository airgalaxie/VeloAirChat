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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transparent rules for the built-in local moderation.
 *
 * <p>The rules block an explicit flood or duplicate finding, two or more
 * findings of the same type, a message with at least 90 percent uppercase
 * letters, or the combination of a URL and an advertisement pattern. Every
 * other result is allowed. This engine never produces HOLD.</p>
 */
public final class StandardDecisionEngine implements DecisionEngine {

    public static final String REASON_FLOOD = "standard:flood";
    public static final String REASON_DUPLICATE = "standard:duplicate";
    public static final String REASON_CAPS = "standard:caps";
    public static final String REASON_REPEATED_FINDING = "standard:repeated_finding";
    public static final String REASON_ADVERTISEMENT = "standard:advertisement";

    @Override
    public ChatDecision decide(AnalysisResult result) {
        final Map<String, Long> findingsByType = result.findings().stream()
                .map(AnalysisFinding::type)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        if (findingsByType.containsKey(StandardFindingTypes.FLOOD)) {
            return block(result, REASON_FLOOD);
        }
        if (findingsByType.containsKey(StandardFindingTypes.DUPLICATE)) {
            return block(result, REASON_DUPLICATE);
        }
        if (result.findings().stream().anyMatch(finding ->
                finding.type().equals(StandardFindingTypes.EXCESSIVE_CAPS) && finding.score() >= 0.9d)) {
            return block(result, REASON_CAPS);
        }
        if (findingsByType.values().stream().anyMatch(count -> count >= 2L)) {
            return block(result, REASON_REPEATED_FINDING);
        }
        if (findingsByType.containsKey(StandardFindingTypes.URL)
                && findingsByType.containsKey(StandardFindingTypes.ADVERTISEMENT_PATTERN)) {
            return block(result, REASON_ADVERTISEMENT);
        }
        return ChatDecision.allow(result.messageId());
    }

    private ChatDecision block(AnalysisResult result, String reason) {
        return new ChatDecision(result.messageId(), ChatDecision.Action.BLOCK, reason);
    }
}
