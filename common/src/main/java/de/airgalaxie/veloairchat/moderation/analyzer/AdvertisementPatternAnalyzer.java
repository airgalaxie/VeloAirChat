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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects explicit invitations to join or connect to another server.
 */
public final class AdvertisementPatternAnalyzer implements ChatAnalyzer {

    private static final String SOURCE = "advertisement-pattern-analyzer";
    private static final Pattern ADVERTISEMENT = Pattern.compile(
            "(?i)\\b(?:join(?:\\s+my)?|come\\s+play|play\\s+on|connect\\s+to|server\\s+ip"
                    + "|komm(?:t)?\\s+auf|spiel(?:e|t)?\\s+auf|besuch(?:e|t)?)\\b"
    );

    @Override
    public CompletionStage<AnalysisResult> analyze(AnalysisRequest request) {
        final Matcher matcher = ADVERTISEMENT.matcher(request.message());
        final ArrayList<AnalysisFinding> findings = new ArrayList<>();
        while (matcher.find()) {
            findings.add(new AnalysisFinding(
                    SOURCE,
                    StandardFindingTypes.ADVERTISEMENT_PATTERN,
                    1.0d,
                    Map.of()
            ));
        }
        return CompletableFuture.completedFuture(new AnalysisResult(request.messageId(), findings));
    }
}
