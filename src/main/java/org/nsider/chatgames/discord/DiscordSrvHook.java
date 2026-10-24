package org.nsider.chatgames.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.MessageAction;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Everything that touches DiscordSRV. Its classes are only loaded once this class is, which
 * {@link DiscordManager} does only when DiscordSRV is enabled, so servers without it never see them.
 */
final class DiscordSrvHook {

    private DiscordSrvHook() {}

    /** @return a target for a DiscordSRV channel name (from its config) or a channel ID */
    static DiscordTarget target(@Nonnull String channelName, @Nonnull String channel) {
        return new Target(channelName, channel.trim());
    }

    /**
     * @return the Discord user ID linked to this player, or null (not linked, not looked up yet, or
     *         DiscordSRV isn't ready). Only DiscordSRV's cache is read: this runs on the server thread.
     */
    @Nullable
    static String linkedId(@Nonnull UUID player) {
        try {
            AccountLinkManager links = DiscordSRV.getPlugin().getAccountLinkManager();
            return links == null ? null : links.getDiscordIdFromCache(player);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static final class Target implements DiscordTarget {
        private final String channelName;
        private final String channel;

        Target(String channelName, String channel) {
            this.channelName = channelName;
            this.channel = channel;
        }

        @Nullable
        @Override
        public String send(@Nonnull DiscordMessage message) throws IOException {
            try {
                Message posted = mentions(channel().sendMessage(toJda(message)), message).complete();
                return posted.getId();
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage() != null ? e.getMessage() : e.toString(), e);
            }
        }

        @Override
        public void edit(@Nonnull String messageId, @Nonnull DiscordMessage message) throws IOException {
            try {
                mentions(channel().editMessageById(messageId, toJda(message)), message).complete();
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage() != null ? e.getMessage() : e.toString(), e);
            }
        }

        @Nonnull
        @Override
        public String describe() {
            return "DiscordSRV channel '" + channel + "' (channel '" + channelName + "')";
        }

        private TextChannel channel() throws IOException {
            if (!DiscordSRV.isReady) throw new IOException("DiscordSRV isn't connected to Discord yet");
            DiscordSRV discordSrv = DiscordSRV.getPlugin();
            TextChannel found = null;
            if (channel.matches("\\d{5,25}")) {
                JDA jda = discordSrv.getJda();
                found = jda == null ? null : jda.getTextChannelById(channel);
            }
            if (found == null) found = discordSrv.getDestinationTextChannelForGameChannelName(channel);
            if (found == null) throw new IOException("DiscordSRV has no channel called '" + channel
                    + "' (use a name from the Channels section of DiscordSRV's config.yml, or a channel ID)");
            return found;
        }

        private static MessageAction mentions(MessageAction action, DiscordMessage message) {
            // Nothing pings except the roles and users the message allows
            action = action.allowedMentions(EnumSet.noneOf(Message.MentionType.class));
            if (!message.getRoles().isEmpty()) action = action.mentionRoles(message.getRoles().toArray(new String[0]));
            if (!message.getUsers().isEmpty()) action = action.mentionUsers(message.getUsers().toArray(new String[0]));
            return action;
        }

        private static Message toJda(DiscordMessage message) {
            MessageBuilder builder = new MessageBuilder();
            if (message.getContent() != null) builder.setContent(message.getContent());
            DiscordMessage.Embed embed = message.getEmbed();
            if (embed != null && !embed.isEmpty()) {
                EmbedBuilder jda = new EmbedBuilder();
                if (embed.getTitle() != null) jda.setTitle(embed.getTitle(), embed.getUrl());
                if (embed.getDescription() != null) jda.setDescription(embed.getDescription());
                if (embed.getColor() != null) jda.setColor(embed.getColor());
                if (embed.getAuthorName() != null) jda.setAuthor(embed.getAuthorName(), embed.getAuthorUrl(), embed.getAuthorIcon());
                if (embed.getThumbnail() != null) jda.setThumbnail(embed.getThumbnail());
                if (embed.getImage() != null) jda.setImage(embed.getImage());
                if (embed.getFooterText() != null) jda.setFooter(embed.getFooterText(), embed.getFooterIcon());
                if (embed.getTimestamp() != null) jda.setTimestamp(embed.getTimestamp());
                for (DiscordMessage.Field field : embed.getFields()) jda.addField(field.getName(), field.getValue(), field.isInline());
                builder.setEmbeds(jda.build());
            }
            return builder.build();
        }
    }
}
