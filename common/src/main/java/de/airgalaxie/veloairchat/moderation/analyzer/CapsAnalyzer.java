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

import de.airgalaxie.veloairchat.moderation.AnalysisFinding;
import de.airgalaxie.veloairchat.moderation.AnalysisRequest;
import de.airgalaxie.veloairchat.moderation.AnalysisResult;
import de.airgalaxie.veloairchat.moderation.ChatAnalyzer;
import de.airgalaxie.veloairchat.moderation.StandardFindingTypes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Detects messages whose letters are predominantly uppercase.
 */
public final class CapsAnalyzer implements ChatAnalyzer {

    static final int MINIMUM_LETTERS = 6;
    static final double CAPS_RATIO = 0.7d;
    private static final String SOURCE = "caps-analyzer";

    @Override
    public CompletionStage<AnalysisResult> analyze(AnalysisRequest request) {
        final long letters = request.message().codePoints().filter(Character::isLetter).count();
        final long uppercase = request.message().codePoints().filter(Character::isUpperCase).count();
        final double ratio = letters == 0 ? 0.0d : (double) uppercase / letters;
        final List<AnalysisFinding> findings = letters >= MINIMUM_LETTERS && ratio >= CAPS_RATIO
                ? List.of(new AnalysisFinding(
                        SOURCE,
                        StandardFindingTypes.EXCESSIVE_CAPS,
                        ratio,
                        Map.of("letters", Long.toString(letters))
                ))
                : List.of();
        return CompletableFuture.completedFuture(new AnalysisResult(request.messageId(), findings));
    }
}
