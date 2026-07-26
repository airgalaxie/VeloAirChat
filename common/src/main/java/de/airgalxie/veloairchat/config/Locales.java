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

package de.airgalxie.veloairchat.config;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import de.airgalxie.veloairchat.VeloAirChat;
import de.airgalxie.veloairchat.channel.Channel;
import de.airgalxie.veloairchat.context.ChatContext;
import de.airgalxie.veloairchat.context.ChatContextFactory;
import de.airgalxie.veloairchat.context.ChatContextRenderer;
import de.airgalxie.veloairchat.user.OnlineUser;
import de.airgalxie.veloairchat.user.UserCache;
import de.airgalxie.veloairchat.util.MessageFormatter;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Locales {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      VeloAirChat - Locales   ┃
            ┃    Maintained by AirGalxie   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ See plugin about menu for international locale credits
            ┣╸ Formatted in MiniMessage: https://docs.papermc.io/adventure/minimessage/
            ┗╸ Original project: HuskChat by William278""";

    private static final String SILENT_JOIN_PERMISSION = "veloairchat.silent_join";
    private static final String SILENT_QUIT_PERMISSION = "veloairchat.silent_quit";
    private static final String FORMATTED_CHAT_PERMISSION = "veloairchat.formatted_chat";
    static final String DEFAULT_LOCALE = "en-gb";

    // The raw set of locales loaded from yaml
    Map<String, String> locales = new TreeMap<>();

    @Nullable
    public String getRawLocale(@NotNull String id) {
        return locales.get(id);
    }

    public void sendMessage(@NotNull OnlineUser player, @NotNull String id, @NotNull String... replacements) {
        String locale = getRawLocale(id);

        // Don't send empty messages
        if (locale == null || locale.isEmpty()) {
            return;
        }

        // Replace placeholders
        int replacementIndexer = 1;
        for (String replacement : replacements) {
            String replacementString = "%" + replacementIndexer + "%";
            locale = locale.replace(replacementString, replacement);
            replacementIndexer = replacementIndexer + 1;
        }

        player.sendMessage(MessageFormatter.format(locale, player.getAudience(), TagResolver.empty()));
    }

    public void sendChannelMessage(@NotNull OnlineUser target, @NotNull OnlineUser sender, @NotNull Channel channel,
                                   @NotNull String message, @NotNull VeloAirChat plugin) {
        createChannelMessage(sender, channel, message, plugin).thenAccept(target::sendMessage);
    }

    @NotNull
    public CompletableFuture<Component> createChannelMessage(@NotNull OnlineUser sender, @NotNull Channel channel,
                                                             @NotNull String message, @NotNull VeloAirChat plugin) {
        return createChannelMessage(
                sender, ChatContextFactory.create(plugin, sender, channel, message), channel.getFormat(), plugin);
    }

    @NotNull
    public CompletableFuture<Component> createChannelMessage(@NotNull OnlineUser sender,
                                                             @NotNull ChatContext context,
                                                             String format,
                                                             @NotNull VeloAirChat plugin) {
        return ChatContextRenderer.render(context, sender, format, plugin);
    }

    public void sendOutboundPrivateMessage(@NotNull OnlineUser sender, @NotNull List<OnlineUser> recipients,
                                           @NotNull String message, @NotNull VeloAirChat plugin) {
        plugin.replacePlaceholders(recipients.get(0), recipients.size() == 1
                ? plugin.getSettings().getMessageCommand().getFormat().getOutbound()
                : plugin.getSettings().getMessageCommand().getFormat().getGroupOutbound()
        ).thenAccept(replaced -> {
            if (recipients.size() > 1) {
                replaced = replaced.replace("%group_amount_subscript%", superscriptNumber(recipients.size() - 1))
                        .replace("%group_amount%", Integer.toString(recipients.size() - 1))
                        .replace("%group_members_comma_separated%", getGroupMemberList(recipients, ","))
                        .replace("%group_members%", MessageFormatter.escape(getGroupMemberList(recipients, "\n")));
            }

            final TextComponent.Builder builder = Component.text();
            final Component format = MessageFormatter.format(
                    replaced, recipients.get(0).getAudience(), plugin.getFormattingTagResolver()
            );
            builder.append(format);
            if (sender.hasPermission(FORMATTED_CHAT_PERMISSION, false)) {
                builder.append(MessageFormatter.formatTrustedUserMessage(
                        message, sender.getAudience(), plugin.getFormattingTagResolver()
                ).color(getFormatColor(format)));
            } else {
                builder.append(MessageFormatter.formatUserMessage(message).color(getFormatColor(format)));
            }

            sender.sendMessage(builder.build());
        });
    }

    // Gets the last TextColor from a component
    @Nullable
    private TextColor getFormatColor(@NotNull Component component) {
        // get the last color in the format
        TextColor color = component.color();
        if (component.children().isEmpty()) {
            return color;
        }
        for (Component child : component.children()) {
            TextColor childColor = getFormatColor(child);
            if (childColor != null) {
                color = childColor;
            }
        }
        return color;
    }

    public void sendInboundPrivateMessage(@NotNull List<OnlineUser> recipients, @NotNull OnlineUser sender,
                                          @NotNull String message, @NotNull VeloAirChat plugin) {
        plugin.replacePlaceholders(sender, recipients.size() == 1
                ? plugin.getSettings().getMessageCommand().getFormat().getInbound()
                : plugin.getSettings().getMessageCommand().getFormat().getGroupInbound()
        ).thenAccept(replaced -> {
            if (recipients.size() > 1) {
                replaced = replaced.replace("%group_amount_subscript%", superscriptNumber(recipients.size() - 1))
                        .replace("%group_amount%", Integer.toString(recipients.size() - 1))
                        .replace("%group_members_comma_separated%", getGroupMemberList(recipients, ","))
                        .replace("%group_members%", MessageFormatter.escape(getGroupMemberList(recipients, "\n")));
            }

            final TextComponent.Builder builder = Component.text();
            final Component format = MessageFormatter.format(replaced, sender.getAudience(), plugin.getFormattingTagResolver());
            builder.append(format);
            if (sender.hasPermission(FORMATTED_CHAT_PERMISSION, false)) {
                builder.append(MessageFormatter.formatTrustedUserMessage(
                        message, sender.getAudience(), plugin.getFormattingTagResolver()
                ).color(getFormatColor(format)));
            } else {
                builder.append(MessageFormatter.formatUserMessage(message).color(getFormatColor(format)));
            }

            for (final OnlineUser recipient : recipients) {
                recipient.sendMessage(builder.build());
            }
        });
    }

    public void sendLocalSpy(@NotNull OnlineUser spy, @NotNull UserCache.SpyColor spyColor, @NotNull OnlineUser sender,
                             @NotNull Channel channel, @NotNull String message, @NotNull VeloAirChat plugin) {
        plugin.replacePlaceholders(sender, plugin.getSettings().getLocalSpy().getFormat())
                .thenAccept(replaced -> {
                    final TextComponent.Builder componentBuilder = Component.text()
                            .append(MessageFormatter.format(replaced.replace("%spy_color%", spyColor.colorCode)
                                    .replace("%channel%", channel.getId()) +
                                    MessageFormatter.escape(message), sender.getAudience(), plugin.getFormattingTagResolver()));
                    spy.sendMessage(componentBuilder.build());
                });
    }

    public void sendSocialSpy(@NotNull OnlineUser spy, @NotNull UserCache.SpyColor spyColor, @NotNull OnlineUser sender,
                              @NotNull List<OnlineUser> receivers, @NotNull String message, @NotNull VeloAirChat plugin) {
        plugin.replacePlaceholders(sender, receivers.size() == 1
                ? plugin.getSettings().getSocialSpy().getFormat()
                : plugin.getSettings().getSocialSpy().getGroupFormat()
                .replace("%sender_", "%")
        ).thenAccept(senderReplaced -> plugin.replacePlaceholders(receivers.get(0), senderReplaced
                .replace("%receiver_", "%")
        ).thenAccept(replaced -> {
            if (receivers.size() > 1) {
                replaced = replaced.replace("%group_amount_subscript%", superscriptNumber(receivers.size() - 1))
                        .replace("%group_amount%", Integer.toString(receivers.size() - 1))
                        .replace("%group_members_comma_separated%", getGroupMemberList(receivers, ","))
                        .replace("%group_members%", MessageFormatter.escape(getGroupMemberList(receivers, "\n")));
            }
            spy.sendMessage(MessageFormatter.format(
                    replaced.replace("%spy_color%", spyColor.colorCode) + MessageFormatter.escape(message),
                    sender.getAudience(),
                    plugin.getFormattingTagResolver()
            ));
        }));
    }

    public void sendJoinMessage(@NotNull OnlineUser player, @NotNull VeloAirChat plugin) {
        if (player.hasPermission(SILENT_JOIN_PERMISSION, false)) {
            return;
        }
        plugin.replacePlaceholders(player,
                        plugin.getDataGetter().getTextFromNode(player, "veloairchat.join_message")
                                .orElse(plugin.getSettings().getJoinAndQuitMessages().getJoin().getFormat()))
                .thenAccept(replaced -> sendJoinQuitMessage(player, MessageFormatter.format(
                        replaced, player.getAudience(), plugin.getFormattingTagResolver()
                ), plugin));
    }

    public void sendQuitMessage(@NotNull OnlineUser player, @NotNull VeloAirChat plugin) {
        if (player.hasPermission(SILENT_QUIT_PERMISSION, false)) {
            return;
        }
        plugin.replacePlaceholders(player,
                        plugin.getDataGetter().getTextFromNode(player, "veloairchat.quit_message")
                                .orElse(plugin.getSettings().getJoinAndQuitMessages().getQuit().getFormat()))
                .thenAccept(replaced -> sendJoinQuitMessage(player, MessageFormatter.format(
                        replaced, player.getAudience(), plugin.getFormattingTagResolver()
                ), plugin));
    }

    // Dispatch a join/quit message to the correct server
    private void sendJoinQuitMessage(@NotNull OnlineUser player, @NotNull Component component,
                                     @NotNull VeloAirChat plugin) {
        boolean local = List.of(Channel.BroadcastScope.LOCAL, Channel.BroadcastScope.LOCAL_PASSTHROUGH)
                .contains(plugin.getSettings().getJoinAndQuitMessages().getBroadcastScope());
        for (OnlineUser online : plugin.getOnlinePlayers()) {
            if (local && !online.getServerName().equals(player.getServerName())) {
                continue;
            }
            online.sendMessage(component);
        }
    }

    // Returns a newline-separated list of player names
    @NotNull
    public final String getGroupMemberList(@NotNull List<OnlineUser> players, @NotNull String delimiter) {
        final StringJoiner memberList = new StringJoiner(delimiter);
        for (OnlineUser player : players) {
            memberList.add(player.getName());
        }
        return memberList.toString();
    }

    // Get the corresponding subscript unicode character from a normal one
    @NotNull
    public final String superscriptNumber(int number) {
        final String[] digits = {"₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"};
        final StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(number).toCharArray()) {
            sb.append(digits[Integer.parseInt(String.valueOf(c))]);
        }
        return sb.toString();
    }

}
