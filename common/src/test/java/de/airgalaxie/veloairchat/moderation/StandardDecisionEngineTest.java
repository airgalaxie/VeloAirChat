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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardDecisionEngineTest {

    private final StandardDecisionEngine engine = new StandardDecisionEngine();

    @Test
    void allowsMessagesWithoutFindings() {
        assertEquals(ChatDecision.Action.ALLOW, engine.decide(result()).action());
    }

    @Test
    void blocksExplicitFloodAndDuplicateFindings() {
        assertEquals(ChatDecision.Action.BLOCK,
                engine.decide(result(finding(StandardFindingTypes.FLOOD))).action());
        assertEquals(ChatDecision.Action.BLOCK,
                engine.decide(result(finding(StandardFindingTypes.DUPLICATE))).action());
    }

    @Test
    void blocksRepeatedFindingsOfTheSameType() {
        assertEquals(ChatDecision.Action.BLOCK, engine.decide(result(
                finding(StandardFindingTypes.URL),
                finding(StandardFindingTypes.URL)
        )).action());
    }

    @Test
    void blocksAnAdvertisementPatternCombinedWithAUrl() {
        assertEquals(ChatDecision.Action.BLOCK, engine.decide(result(
                finding(StandardFindingTypes.URL),
                finding(StandardFindingTypes.ADVERTISEMENT_PATTERN)
        )).action());
    }

    @Test
    void allowsSingleBorderlineFindingsAndNeverUsesHold() {
        assertEquals(ChatDecision.Action.ALLOW,
                engine.decide(result(finding(StandardFindingTypes.URL))).action());
        assertEquals(ChatDecision.Action.ALLOW,
                engine.decide(result(finding(StandardFindingTypes.EXCESSIVE_CAPS, 0.8d))).action());
    }

    @Test
    void blocksUnambiguousCapsFindings() {
        assertEquals(ChatDecision.Action.BLOCK,
                engine.decide(result(finding(StandardFindingTypes.EXCESSIVE_CAPS, 0.95d))).action());
    }

    private AnalysisResult result(AnalysisFinding... findings) {
        return new AnalysisResult(UUID.randomUUID(), List.of(findings));
    }

    private AnalysisFinding finding(String type) {
        return finding(type, 1.0d);
    }

    private AnalysisFinding finding(String type, double score) {
        return new AnalysisFinding("test", type, score, Map.of());
    }
}
