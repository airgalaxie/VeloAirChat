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

package de.airgalaxie.veloairchat.moderation.analyzer;

import de.airgalaxie.veloairchat.moderation.AnalysisRequest;
import de.airgalaxie.veloairchat.moderation.AnalysisResult;
import de.airgalaxie.veloairchat.moderation.ChatAnalyzer;
import de.airgalaxie.veloairchat.moderation.StandardFindingTypes;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardAnalyzersTest {

    @Test
    void duplicateAnalyzerOnlyFlagsRepeatedNormalizedMessages() {
        final AtomicLong time = new AtomicLong(1_000L);
        final DuplicateAnalyzer analyzer = new DuplicateAnalyzer(30_000L, time::get);
        final UUID sender = UUID.randomUUID();

        assertTrue(analyze(analyzer, request(sender, "Hallo Welt")).findings().isEmpty());
        assertEquals(StandardFindingTypes.DUPLICATE,
                analyze(analyzer, request(sender, "  hallo   welt ")).findings().getFirst().type());
    }

    @Test
    void floodAnalyzerFlagsTheFourthMessageInItsWindow() {
        final AtomicLong time = new AtomicLong(1_000L);
        final FloodAnalyzer analyzer = new FloodAnalyzer(4, 4_000L, time::get);
        final UUID sender = UUID.randomUUID();

        for (int index = 0; index < 3; index++) {
            assertTrue(analyze(analyzer, request(sender, "message-" + index)).findings().isEmpty());
            time.addAndGet(500L);
        }

        assertEquals(StandardFindingTypes.FLOOD,
                analyze(analyzer, request(sender, "message-3")).findings().getFirst().type());
    }

    @Test
    void capsAnalyzerOnlyReportsPredominantlyUppercaseText() {
        final CapsAnalyzer analyzer = new CapsAnalyzer();

        assertTrue(analyze(analyzer, request(UUID.randomUUID(), "Hello World")).findings().isEmpty());
        assertEquals(StandardFindingTypes.EXCESSIVE_CAPS,
                analyze(analyzer, request(UUID.randomUUID(), "HELLO WORLD")).findings().getFirst().type());
    }

    @Test
    void urlAndAdvertisementAnalyzersReportIndependentFindings() {
        final AnalysisRequest request = request(
                UUID.randomUUID(), "Join my server at example.net and come play!");

        assertEquals(StandardFindingTypes.URL,
                analyze(new UrlAnalyzer(), request).findings().getFirst().type());
        assertEquals(StandardFindingTypes.ADVERTISEMENT_PATTERN,
                analyze(new AdvertisementPatternAnalyzer(), request).findings().getFirst().type());
    }

    private AnalysisResult analyze(ChatAnalyzer analyzer, AnalysisRequest request) {
        return analyzer.analyze(request).toCompletableFuture().join();
    }

    private AnalysisRequest request(UUID senderId, String message) {
        return new AnalysisRequest(
                UUID.randomUUID(),
                senderId,
                "Spieler",
                "global",
                "lobby",
                message,
                message,
                "JAVA_PROFILE",
                "SIGNED",
                "JAVA_SIGNATURE_PRESENT",
                1L
        );
    }
}
