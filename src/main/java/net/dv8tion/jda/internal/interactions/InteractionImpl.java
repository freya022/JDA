/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.internal.interactions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.IntegrationOwners;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.MemberImpl;
import net.dv8tion.jda.internal.entities.UserImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.PrivateChannelImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

public class InteractionImpl implements Interaction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Interaction.class);

    protected final long id;
    protected final long channelId;
    protected final int type;
    protected final String token;
    protected final Guild guild;
    protected final Member member;
    protected final User user;
    protected final Channel channel;
    protected final DiscordLocale userLocale;
    protected final InteractionContextType context;
    protected final IntegrationOwners integrationOwners;
    protected final Set<Permission> userPermissions, appPermissions;
    protected final JDAImpl api;

    //This is used to give a proper error when an interaction is ack'd twice
    // By default, discord only responds with "unknown interaction" which is horrible UX so we add a check manually here
    private boolean isAck;

    public InteractionImpl(JDAImpl jda, DataObject data)
    {
        this.api = jda;
        this.id = data.getUnsignedLong("id");
        this.token = data.getString("token");
        this.type = data.getInt("type");
        this.guild = jda.getGuildById(data.getUnsignedLong("guild_id", 0L));
        this.channelId = data.getUnsignedLong("channel_id", 0L);
        this.userLocale = DiscordLocale.from(data.getString("locale", "en-US"));
        if (data.hasKey("context"))
            this.context = InteractionContextType.fromKey(data.getString("context"));
        else
        {
            //TODO someone claimed they received no context, it is documented as being nullable,
            // but I've not seen context being null.
            LOGGER.warn("No context provided in interaction");
            this.context = null;
        }
        //TODO The bot and user permissions could be added in the temporary GuildChannel
        // Meaning that you can still use (Self)Member#hasPermission(GuildChannel, Permission...) transparently
        // The drawback is that the user might see the permission overrides and think they have them (document it)
        // The code calculating the effective/explicit permissions uses the guild, which doesn't exist
        // The user might also mistakenly use (Self)Member#hasPermission(Permission...) which will give unexpected results,
        // should it throw if the guild is unknown?
        // PermissionUtil.getEffectivePermission/getExplicitPermission(Member) would throw if the guild is unknown
        // While the overload using the channel would ignore unknown guilds as it can use the channel overrides
        //TODO should the default value really be 0?
        this.userPermissions = Collections.unmodifiableSet(Permission.getPermissions(data.getObject("channel").getLong("permissions", 0L)));
        this.appPermissions = Collections.unmodifiableSet(Permission.getPermissions(data.getLong("app_permissions")));
        this.integrationOwners = new IntegrationOwnersImpl(data.getObject("authorizing_integration_owners"));

        DataObject channelJson = data.getObject("channel");
        if (guild != null)
        {
            member = jda.getEntityBuilder().createMember((GuildImpl) guild, data.getObject("member"));
            jda.getEntityBuilder().updateMemberCache((MemberImpl) member);
            user = member.getUser();

            GuildChannel channel = guild.getGuildChannelById(channelJson.getUnsignedLong("id"));
            if (channel == null && ChannelType.fromId(channelJson.getInt("type")).isThread())
                channel = api.getEntityBuilder().createThreadChannel((GuildImpl) guild, channelJson, guild.getIdLong(), false);
            if (channel == null)
                throw new IllegalStateException("Failed to create channel instance for interaction! Channel Type: " + channelJson.getInt("type"));
            this.channel = channel;
        }
        else
        {
            member = null;
            long channelId = channelJson.getUnsignedLong("id");
            ChannelType type = ChannelType.fromId(channelJson.getInt("type"));
            if (type != ChannelType.PRIVATE)
                throw new IllegalArgumentException("Received interaction in unexpected channel type! Type " + type + " is not supported yet!");
            PrivateChannel channel = jda.getPrivateChannelById(channelId);
            if (channel == null)
            {
                channel = jda.getEntityBuilder().createPrivateChannel(
                    DataObject.empty()
                        .put("id", channelId)
                        .put("recipient", data.getObject("user"))
                );
            }
            this.channel = channel;

            User user = channel.getUser();
            if (user == null)
            {
                user = jda.getEntityBuilder().createUser(data.getObject("user"));
                ((PrivateChannelImpl) channel).setUser(user);
                ((UserImpl) user).setPrivateChannel(channel);
            }
            this.user = user;
        }
    }

    // Used to allow interaction hook to send messages after acknowledgements
    // This is implemented only in DeferrableInteractionImpl where a hook is present!
    public synchronized void releaseHook(boolean success) {}

    // Ensures that one cannot acknowledge an interaction twice
    public synchronized boolean ack()
    {
        boolean wasAck = isAck;
        this.isAck = true;
        return wasAck;
    }

    @Override
    public synchronized boolean isAcknowledged()
    {
        return isAck;
    }

    @Override
    public long getIdLong()
    {
        return id;
    }

    @Override
    public int getTypeRaw()
    {
        return type;
    }

    @Nonnull
    @Override
    public String getToken()
    {
        return token;
    }

    @Nullable
    @Override
    public Guild getGuild()
    {
        return guild;
    }

    @Nullable
    @Override
    public Channel getChannel()
    {
        return channel;
    }

    @Override
    public long getChannelIdLong()
    {
        return channelId;
    }

    @Nonnull
    public DiscordLocale getUserLocale()
    {
        return userLocale;
    }

    @Nullable
    @Override
    public InteractionContextType getContext()
    {
        return context;
    }

    @Nonnull
    @Override
    public Set<Permission> getUserPermissions()
    {
        return userPermissions;
    }

    @Nonnull
    @Override
    public Set<Permission> getApplicationPermissions()
    {
        return appPermissions;
    }

    @Nonnull
    @Override
    public IntegrationOwners getIntegrationOwners()
    {
        return integrationOwners;
    }

    @Nonnull
    @Override
    public User getUser()
    {
        return user;
    }

    @Nullable
    @Override
    public Member getMember()
    {
        return member;
    }

    @Nonnull
    @Override
    public JDA getJDA()
    {
        return api;
    }
}
