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

package de.airgalxie.veloairchat.context;

import de.airgalxie.veloairchat.VeloAirChat;
import de.airgalxie.veloairchat.user.OnlineUser;
import de.airgalxie.veloairchat.util.MessageFormatter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ChatContextRenderer {

    private static final String FORMATTED_CHAT_PERMISSION = "veloairchat.formatted_chat";

    private ChatContextRenderer() {
    }

    @NotNull
    public static CompletableFuture<Component> render(@NotNull ChatContext context,
                                                      @NotNull OnlineUser sender,
                                                      String configuredFormat,
                                                      @NotNull VeloAirChat plugin) {
        final String template = prepareTemplate(configuredFormat);
        final String contextTemplate = ChatContextPlaceholders.replaceInformation(template, context);
        return plugin.replacePlaceholders(sender, contextTemplate).thenApply(externalPlaceholders -> {
            final String renderedMessage = sender.hasPermission(FORMATTED_CHAT_PERMISSION, false)
                    ? context.message()
                    : MessageFormatter.escape(context.message());
            final String resolved = ChatContextPlaceholders.replaceAll(
                    externalPlaceholders, context, renderedMessage);
            return MessageFormatter.format(
                    resolved, sender.getAudience(), plugin.getFormattingTagResolver());
        });
    }

    static String prepareTemplate(String configuredFormat) {
        final String template = configuredFormat == null ? "" : configuredFormat;
        return ChatContextPlaceholders.hasMessagePlaceholder(template) ? template : template + "<message>";
    }
}
