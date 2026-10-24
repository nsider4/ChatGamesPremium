package org.nsider.chatgames.discord;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.IOException;

/**
 * Somewhere a message can be posted: a webhook, or a channel of the DiscordSRV bot. Only
 * called on the Discord thread ({@link DiscordQueue}), one request at a time, so a call may
 * block (and wait out Discord's rate limits).
 */
interface DiscordTarget {

    /** @return the posted message's id, to edit it later; null if it can't be edited */
    @Nullable
    String send(@Nonnull DiscordMessage message) throws IOException;

    void edit(@Nonnull String messageId, @Nonnull DiscordMessage message) throws IOException;

    /** @return a description for the console, never the webhook's secret token */
    @Nonnull
    String describe();
}
