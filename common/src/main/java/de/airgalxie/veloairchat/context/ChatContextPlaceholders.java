/*
 * This file is part of VeloAirChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) AirGalxie/VeloAirChat contributors
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

package de.airgalxie.veloairchat.context;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ChatContextPlaceholders {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter BRITISH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");

    private ChatContextPlaceholders() {
    }

    @NotNull
    public static String replaceInformation(@NotNull String template, @NotNull ChatContext context) {
        return replace(template, values(context, null));
    }

    @NotNull
    public static String replaceAll(@NotNull String template, @NotNull ChatContext context,
                                    @NotNull String renderedMessage) {
        return replace(template, values(context, renderedMessage));
    }

    public static boolean hasMessagePlaceholder(String template) {
        return template != null && (template.contains("<message>") || template.contains("%message%"));
    }

    private static Map<String, String> values(ChatContext context, String renderedMessage) {
        final Map<String, String> values = new LinkedHashMap<>();
        add(values, "platform", context.platform());
        add(values, "scope", context.scope());
        add(values, "server", context.server(), "server_name", "servername");
        add(values, "backend", context.backend());
        add(values, "player", context.player(), "name", "username");
        add(values, "playerUuid", context.playerUuid().toString(), "uuid", "player_uuid");
        add(values, "displayName", context.displayName(), "display_name", "full_name", "fullname");
        add(values, "prefix", context.prefix(), "role_prefix", "roleprefix");
        add(values, "suffix", context.suffix(), "role_suffix", "rolesuffix");
        add(values, "role", context.role(), "role_name", "rolename");
        add(values, "roleDisplayName", context.roleDisplayName(), "role_display_name", "roledisplayname");
        if (renderedMessage != null) {
            add(values, "message", renderedMessage);
        }
        add(values, "originalMessage", context.originalMessage(), "original_message");
        add(values, "channel", context.channel());
        add(values, "identityProvider", context.identityProvider(), "identity_provider");
        add(values, "signedState", context.signedState(), "signed_state");
        add(values, "trustState", context.trustState(), "trust_state");
        add(values, "signedAdapter", context.signedAdapter(), "signed_adapter");
        add(values, "signedTimestamp", Long.toString(context.signedTimestamp()), "signed_timestamp");
        add(values, "timestamp", Long.toString(context.timestamp()));
        add(values, "messageId", context.messageId().toString(), "message_id");
        add(values, "ping", Integer.toString(context.ping()));
        add(values, "localPlayersOnline", Integer.toString(context.localPlayersOnline()),
                "local_players_online", "server_player_count", "serverplayercount");

        final var time = Instant.ofEpochMilli(context.timestamp()).atZone(ZoneId.systemDefault());
        addPercentOnly(values, "timestamp", TIMESTAMP.format(time));
        add(values, "currentTime", TIME.format(time), "current_time", "time");
        add(values, "currentTimeShort", SHORT_TIME.format(time), "current_time_short", "short_time");
        add(values, "currentDate", DATE.format(time), "current_date", "date");
        add(values, "currentDateUk", BRITISH_DATE.format(time), "current_date_uk", "british_date");
        add(values, "currentDateDay", DAY.format(time), "current_date_day", "day");
        add(values, "currentMonth", MONTH.format(time), "current_month", "month");
        add(values, "currentYear", YEAR.format(time), "current_year", "year");
        return values;
    }

    private static void add(Map<String, String> values, String canonical, String value, String... aliases) {
        values.put("<" + canonical + ">", value);
        values.put("%" + canonical + "%", value);
        for (String alias : aliases) {
            values.put("<" + alias + ">", value);
            values.put("%" + alias + "%", value);
        }
    }

    private static void addPercentOnly(Map<String, String> values, String name, String value) {
        values.put("%" + name + "%", value);
    }

    private static String replace(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
