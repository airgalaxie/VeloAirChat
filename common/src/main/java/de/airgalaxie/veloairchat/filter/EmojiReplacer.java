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

package de.airgalaxie.veloairchat.filter;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import de.airgalaxie.veloairchat.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A {@link ReplacerFilter} that replaces chat emoji with the character emote
 */
public class EmojiReplacer extends ChatFilter.ReplacerFilter {

    public EmojiReplacer(@NotNull FilterSettings settings) {
        super(settings);
    }

    @Override
    @NotNull
    public String replace(@NotNull String message) {
        String[] words = message.split(" ");
        StringJoiner replacedMessage = new StringJoiner(" ");
        final EmojiReplacerSettings settings = (EmojiReplacerSettings) this.settings;
        for (String word : words) {
            for (String emoteFormat : settings.getEmoji().keySet()) {
                if (!settings.isCaseInsensitive()) {
                    if (word.equals(emoteFormat)) {
                        word = settings.getEmoji().get(emoteFormat);
                        break;
                    }
                } else {
                    if (word.toLowerCase(Locale.ROOT).equals(emoteFormat)) {
                        word = settings.getEmoji().get(emoteFormat);
                        break;
                    }
                }
            }
            replacedMessage.add(word);
        }
        return replacedMessage.toString();
    }

    @NotNull
    public static FilterSettings getDefaultSettings() {
        final EmojiReplacerSettings settings = new EmojiReplacerSettings();
        settings.enabled = false;
        return settings;
    }

    @Override
    public boolean isAllowed(@NotNull OnlineUser sender, @NotNull String message) {
        return true;
    }

    @Override
    @NotNull
    public String getDisallowedLocale() {
        throw new UnsupportedOperationException("EmojiReplacer does not support failure messages");
    }

    @Override
    @NotNull
    public String getIgnorePermission() {
        return "veloairchat.ignore_filters.emoji_replacer";
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class EmojiReplacerSettings extends FilterSettings {
        private boolean caseInsensitive = false;
        private Map<String, String> emoji = new HashMap<>(Map.of(
                ":)", "☺",
                ":smile:", "☺",
                ":-)", "☺",
                ":(", "☹",
                ":frown:", "☹",
                ":-(", "☹",
                ":fire:", "🔥",
                ":heart:", "❤",
                "<3", "❤",
                ":star:", "⭐"
        ));
    }

}
