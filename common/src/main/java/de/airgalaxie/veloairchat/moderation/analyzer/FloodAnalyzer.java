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

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Detects a sender exceeding a fixed message rate.
 */
public final class FloodAnalyzer implements ChatAnalyzer {

    static final int DEFAULT_MESSAGES_PER_WINDOW = 4;
    static final long DEFAULT_WINDOW_MILLIS = 4_000L;
    private static final String SOURCE = "flood-analyzer";
    private static final int CLEANUP_INTERVAL = 1_024;

    private final ConcurrentMap<UUID, ArrayDeque<Long>> messageTimes = new ConcurrentHashMap<>();
    private final AtomicInteger analysisCount = new AtomicInteger();
    private final int messagesPerWindow;
    private final long windowMillis;
    private final LongSupplier clock;

    public FloodAnalyzer() {
        this(DEFAULT_MESSAGES_PER_WINDOW, DEFAULT_WINDOW_MILLIS, System::currentTimeMillis);
    }

    FloodAnalyzer(int messagesPerWindow, long windowMillis, LongSupplier clock) {
        this.messagesPerWindow = messagesPerWindow;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public CompletionStage<AnalysisResult> analyze(AnalysisRequest request) {
        final long now = clock.getAsLong();
        final ArrayDeque<Long> times = messageTimes.computeIfAbsent(
                request.senderId(), ignored -> new ArrayDeque<>());
        final int count;
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > windowMillis) {
                times.removeFirst();
            }
            times.addLast(now);
            while (times.size() > messagesPerWindow) {
                times.removeFirst();
            }
            count = times.size();
        }
        cleanupExpiredEntries(now);
        final List<AnalysisFinding> findings = count >= messagesPerWindow
                ? List.of(new AnalysisFinding(
                        SOURCE,
                        StandardFindingTypes.FLOOD,
                        1.0d,
                        Map.of(
                                "messages", Integer.toString(count),
                                "window_millis", Long.toString(windowMillis)
                        )
                ))
                : List.of();
        return CompletableFuture.completedFuture(new AnalysisResult(request.messageId(), findings));
    }

    private void cleanupExpiredEntries(long now) {
        if (analysisCount.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        messageTimes.entrySet().removeIf(entry -> {
            final ArrayDeque<Long> times = entry.getValue();
            synchronized (times) {
                return times.isEmpty() || now - times.peekLast() > windowMillis;
            }
        });
    }
}
