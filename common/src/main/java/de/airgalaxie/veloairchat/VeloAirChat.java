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

package de.airgalaxie.veloairchat;

import de.airgalaxie.veloairchat.config.ConfigProvider;
import de.airgalaxie.veloairchat.channel.Channel;
import de.airgalaxie.veloairchat.event.EventProvider;
import de.airgalaxie.veloairchat.filter.FilterProvider;
import de.airgalaxie.veloairchat.getter.DataGetter;
import de.airgalaxie.veloairchat.placeholders.PlaceholderReplacer;
import de.airgalaxie.veloairchat.user.OnlineUser;
import de.airgalaxie.veloairchat.util.AudiencesProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public interface VeloAirChat extends AudiencesProvider, ConfigProvider, FilterProvider, EventProvider {

    int SPIGOT_RESOURCE_ID = 94496;

    @NotNull
    List<PlaceholderReplacer> getPlaceholderReplacers();

    @NotNull
    default TagResolver getFormattingTagResolver() {
        return TagResolver.empty();
    }

    default CompletableFuture<String> replacePlaceholders(@NotNull OnlineUser player, @NotNull String message) {
        CompletableFuture<String> future = CompletableFuture.completedFuture(message);
        for (PlaceholderReplacer replacer : getPlaceholderReplacers()) {
            future = future.thenComposeAsync(toFormat -> replacer.formatPlaceholders(toFormat, player));
        }
        return future;
    }

    @NotNull
    DataGetter getDataGetter();
    @NotNull
    String getVersion();

    @NotNull
    String getPluginDescription();

    @NotNull
    String getPlatform();

    Optional<OnlineUser> getPlayer(@NotNull UUID uuid);

    Optional<OnlineUser> findPlayer(@NotNull String username);

    @NotNull
    Collection<OnlineUser> getOnlinePlayers();

    @NotNull
    Collection<OnlineUser> getOnlinePlayersOnServer(@NotNull OnlineUser player);

    boolean isPluginPresent(@NotNull String dependency);

    default void signChatMessage(@NotNull OnlineUser sender, @NotNull Channel channel, @NotNull String message) {
    }
/*
    @NotNull
    default UpdateChecker getUpdateChecker() {
        return UpdateChecker.builder()
                .currentVersion(getVersion())
                .endpoint(UpdateChecker.Endpoint.SPIGOT)
                .resource(Integer.toString(SPIGOT_RESOURCE_ID))
                .build();
    }

    default void checkForUpdates() {
        if (getSettings().isCheckForUpdates()) {
            getUpdateChecker().check().thenAccept(checked -> {
                if (!checked.isUpToDate()) {
                    log(Level.WARNING, "A new version of VeloAirChat is available: v"
                            + checked.getLatestVersion() + " (running v" + getVersion() + ")");
                }
            });
        }
    }

*/
    void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... throwable);
}
