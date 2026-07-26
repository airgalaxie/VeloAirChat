/*
 * This file is part of VeloAirChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Original project: HuskChat by William278
 *  Modifications Copyright (c) AirGalxie/VeloAirChat contributors
 *  Copyright (c) contributors
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

package de.airgalxie.veloairchat.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageFormatter {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern MINE_DOWN_LINK = Pattern.compile("\\[([^\\[\\]]*)]\\(([^()]*)\\)");
    private static final Pattern MINE_DOWN_HEX = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)&?#([0-9a-f]{6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)&([0-9a-fk-or])");

    private MessageFormatter() {
    }

    @NotNull
    public static Component format(@NotNull String message) {
        return format(message, Audience.empty(), TagResolver.empty());
    }

    @NotNull
    public static Component format(@NotNull String message, @NotNull Audience audience, @NotNull TagResolver tagResolver) {
        if (message.indexOf('<') == -1 && !MINE_DOWN_LINK.matcher(message).find()) {
            return LEGACY_AMPERSAND.deserialize(message);
        }
        return MINI_MESSAGE.deserialize(toMiniMessage(message), audience, tagResolver);
    }

    @NotNull
    public static Component formatUserMessage(@NotNull String message) {
        return Component.text(message);
    }

    @NotNull
    public static Component formatTrustedUserMessage(@NotNull String message) {
        return formatTrustedUserMessage(message, Audience.empty(), TagResolver.empty());
    }

    @NotNull
    public static Component formatTrustedUserMessage(
            @NotNull String message,
            @NotNull Audience audience,
            @NotNull TagResolver tagResolver
    ) {
        return format(message, audience, tagResolver);
    }

    @NotNull
    public static String escape(@NotNull String message) {
        return MINI_MESSAGE.escapeTags(message);
    }

    @NotNull
    private static String toMiniMessage(@NotNull String message) {
        String converted = message.replace("</>", "<reset>");
        String previous;
        do {
            previous = converted;
            converted = convertBracketFormatting(converted);
        } while (!converted.equals(previous));
        return convertLegacyCodes(converted);
    }

    @NotNull
    private static String convertBracketFormatting(@NotNull String message) {
        final Matcher matcher = MINE_DOWN_LINK.matcher(message);
        final StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(toMiniMessageTag(
                    matcher.group(1), matcher.group(2)
            )));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    @NotNull
    private static String toMiniMessageTag(@NotNull String content, @NotNull String rawAttributes) {
        String result = escape(content);
        for (String attribute : rawAttributes.split("\\s+")) {
            if (attribute.isBlank()) {
                continue;
            }
            final String lower = attribute.toLowerCase(Locale.ENGLISH);
            if (MINE_DOWN_HEX.matcher(attribute).matches()) {
                result = tag(attribute, result);
            } else if (isColor(lower)) {
                result = tag(lower, result);
            } else if (lower.equals("bold") || lower.equals("b")) {
                result = tag("bold", result);
            } else if (lower.equals("italic") || lower.equals("i")) {
                result = tag("italic", result);
            } else if (lower.equals("underlined") || lower.equals("underline") || lower.equals("u")) {
                result = tag("underlined", result);
            } else if (lower.equals("strikethrough") || lower.equals("strike") || lower.equals("s")) {
                result = tag("strikethrough", result);
            } else if (lower.equals("obfuscated") || lower.equals("magic")) {
                result = tag("obfuscated", result);
            } else if (lower.startsWith("show_text=")) {
                result = "<hover:show_text:'" + quoteValue(attribute.substring("show_text=".length())) + "'>"
                        + result + "</hover>";
            } else if (lower.startsWith("suggest_command=")) {
                result = "<click:suggest_command:'" + quoteValue(attribute.substring("suggest_command=".length())) + "'>"
                        + result + "</click>";
            } else if (lower.startsWith("run_command=")) {
                result = "<click:run_command:'" + quoteValue(attribute.substring("run_command=".length())) + "'>"
                        + result + "</click>";
            } else if (lower.startsWith("open_url=")) {
                result = "<click:open_url:'" + quoteValue(attribute.substring("open_url=".length())) + "'>"
                        + result + "</click>";
            }
        }
        return result;
    }

    @NotNull
    private static String tag(@NotNull String tag, @NotNull String content) {
        return "<" + tag + ">" + content + "</" + tag + ">";
    }

    @NotNull
    private static String convertLegacyCodes(@NotNull String message) {
        final Matcher hexMatcher = LEGACY_HEX.matcher(message);
        final StringBuilder hexBuilder = new StringBuilder();
        while (hexMatcher.find()) {
            final boolean alreadyMiniMessageTag = isMiniMessageHexTag(message, hexMatcher.start());
            hexMatcher.appendReplacement(hexBuilder, Matcher.quoteReplacement(
                    alreadyMiniMessageTag ? hexMatcher.group() : "<#" + hexMatcher.group(1) + ">"
            ));
        }
        hexMatcher.appendTail(hexBuilder);

        final Matcher codeMatcher = LEGACY_CODE.matcher(hexBuilder.toString());
        final StringBuilder codeBuilder = new StringBuilder();
        while (codeMatcher.find()) {
            codeMatcher.appendReplacement(codeBuilder, Matcher.quoteReplacement(legacyTag(codeMatcher.group(1).charAt(0))));
        }
        codeMatcher.appendTail(codeBuilder);
        return codeBuilder.toString();
    }

    private static boolean isMiniMessageHexTag(@NotNull String message, int hexStart) {
        if (hexStart > 0 && message.charAt(hexStart - 1) == '<') {
            return true;
        }
        return hexStart > 1 && message.charAt(hexStart - 1) == '/' && message.charAt(hexStart - 2) == '<';
    }

    @NotNull
    private static String legacyTag(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }

    private static boolean isColor(@NotNull String value) {
        return switch (value) {
            case "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
                 "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white" -> true;
            default -> false;
        };
    }

    @NotNull
    private static String quoteValue(@NotNull String value) {
        return escape(value).replace("\\", "\\\\").replace("'", "\\'");
    }
}
