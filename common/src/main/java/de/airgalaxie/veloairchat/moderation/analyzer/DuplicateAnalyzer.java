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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Detects repeated equal messages from the same sender.
 */
public final class DuplicateAnalyzer implements ChatAnalyzer {

    static final long DEFAULT_WINDOW_MILLIS = 30_000L;
    private static final String SOURCE = "duplicate-analyzer";
    private static final int CLEANUP_INTERVAL = 1_024;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final ConcurrentMap<UUID, PreviousMessage> previousMessages = new ConcurrentHashMap<>();
    private final AtomicInteger analysisCount = new AtomicInteger();
    private final long windowMillis;
    private final LongSupplier clock;

    public DuplicateAnalyzer() {
        this(DEFAULT_WINDOW_MILLIS, System::currentTimeMillis);
    }

    DuplicateAnalyzer(long windowMillis, LongSupplier clock) {
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public CompletionStage<AnalysisResult> analyze(AnalysisRequest request) {
        final long now = clock.getAsLong();
        final String normalized = normalize(request.message());
        final PreviousMessage previous = previousMessages.put(
                request.senderId(), new PreviousMessage(normalized, now));
        cleanupExpiredEntries(now);
        final boolean duplicate = previous != null
                && previous.message().equals(normalized)
                && now - previous.timestamp() <= windowMillis;
        final List<AnalysisFinding> findings = duplicate
                ? List.of(new AnalysisFinding(
                        SOURCE,
                        StandardFindingTypes.DUPLICATE,
                        1.0d,
                        Map.of("window_millis", Long.toString(windowMillis))
                ))
                : List.of();
        return CompletableFuture.completedFuture(new AnalysisResult(request.messageId(), findings));
    }

    private String normalize(String message) {
        return WHITESPACE.matcher(message.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    private void cleanupExpiredEntries(long now) {
        if (analysisCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            previousMessages.entrySet().removeIf(
                    entry -> now - entry.getValue().timestamp() > windowMillis);
        }
    }

    private record PreviousMessage(String message, long timestamp) {
    }
}
