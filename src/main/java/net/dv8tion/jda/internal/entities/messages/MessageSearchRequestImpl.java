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

package net.dv8tion.jda.internal.entities.messages;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.messages.MessageSearchRequest;
import net.dv8tion.jda.api.entities.messages.MessageSearchResponse;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MessageSearchRequestImpl extends RestActionImpl<MessageSearchResponse> implements MessageSearchRequest {
    private final Guild guild;

    private Integer limit, offset;
    private Long minId, maxId;
    private Integer slop;
    private String content;
    private Set<String> channels = Collections.emptySet();
    private Set<AuthorType> authorTypes = Collections.emptySet();
    private Set<String> authors = Collections.emptySet();
    private Set<String> mentions = Collections.emptySet();
    private Boolean mentionsEveryone;
    private Boolean pinned;
    private Set<HasType> hasTypes = Collections.emptySet();
    private Set<EmbedType> embedTypes = Collections.emptySet();
    private Set<String> embedProviders = Collections.emptySet();
    private Set<String> linkHostnames = Collections.emptySet();
    private Set<String> attachmentFilenames = Collections.emptySet();
    private Set<String> attachmentExtensions = Collections.emptySet();
    private SortType sortBy = null;
    private SortOrder sortOrder = null;
    private Boolean includeNsfw;

    public MessageSearchRequestImpl(Guild guild) {
        super(guild.getJDA(), Route.Guilds.SEARCH_MESSAGES.compile(guild.getId()));
        this.guild = guild;
    }

    @Nonnull
    @Override
    public MessageSearchRequest limit(@Nullable Integer limit) {
        if (limit != null) {
            Checks.positive(limit, "Limit");
            Checks.check(limit <= 25, "Limit must be lower than or equal to 25");
        }
        this.limit = limit;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest offset(@Nullable Integer offset) {
        if (offset != null) {
            Checks.positive(offset, "Offset");
            Checks.check(offset <= 9975, "Offset must be lower than or equal to 9975");
        }
        this.offset = offset;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest minId(@Nullable Long minId) {
        if (minId != null) {
            Checks.notNegative(minId, "Min ID");
        }
        this.minId = minId;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest maxId(@Nullable Long maxId) {
        if (maxId != null) {
            Checks.notNegative(maxId, "Max ID");
        }
        this.maxId = maxId;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest slop(@Nullable Integer slop) {
        if (slop != null) {
            Checks.notNegative(slop, "Slop");
            Checks.check(slop <= 100, "Slop must be lower than or equal to 100");
        }
        this.slop = slop;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest content(@Nullable String content) {
        if (content != null) {
            Checks.inRange(content, 0, 1024, "Content");
        }
        this.content = content;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest channels(@Nonnull Collection<? extends GuildMessageChannel> channels) {
        Checks.noneNull(channels, "Channels");
        for (GuildMessageChannel channel : channels) {
            Checks.check(
                    channel.getGuild().equals(guild),
                    "Channel %s is from a different guild (expected %s, was %s)",
                    channel,
                    guild,
                    channel.getGuild());
            Checks.check(
                    guild.getSelfMember().hasAccess(channel), "Channel %s from %s is inaccessible", channel, guild);
        }
        this.channels = channels.stream().map(GuildMessageChannel::getId).collect(Collectors.toSet());
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest authorTypes(@Nonnull Collection<AuthorType> authorTypes) {
        Checks.noneNull(authorTypes, "Author types");
        this.authorTypes = Helpers.copyEnumSet(AuthorType.class, authorTypes);
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest authors(@Nonnull Collection<? extends UserSnowflake> authors) {
        Checks.noneNull(authors, "Authors");
        this.authors = authors.stream().map(UserSnowflake::getId).collect(Collectors.toSet());
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest mentions(@Nonnull Collection<? extends UserSnowflake> mentions) {
        Checks.noneNull(mentions, "Mentions");
        this.mentions = mentions.stream().map(UserSnowflake::getId).collect(Collectors.toSet());
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest mentionsEveryone(boolean mentionsEveryone) {
        this.mentionsEveryone = mentionsEveryone;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest pinned(boolean pinned) {
        this.pinned = pinned;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest hasTypes(@Nonnull Collection<HasType> hasTypes) {
        this.hasTypes = Helpers.copyEnumSet(HasType.class, hasTypes);
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest embedTypes(@Nonnull Collection<EmbedType> embedTypes) {
        this.embedTypes = Helpers.copyEnumSet(EmbedType.class, embedTypes);
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest embedProvider(@Nonnull Collection<String> embedProviders) {
        this.embedProviders = new HashSet<>(embedProviders);
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest linkHostnames(@Nonnull Collection<String> linkHostnames) {
        this.linkHostnames = new HashSet<>(linkHostnames);
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest attachmentFilenames(@Nonnull Collection<String> attachmentFilenames) {
        this.attachmentFilenames = new HashSet<>(attachmentFilenames);
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest attachmentExtensions(@Nonnull Collection<String> attachmentExtensions) {
        this.attachmentExtensions = new HashSet<>(attachmentExtensions);
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest sortBy(@Nullable MessageSearchRequest.SortType sortType) {
        this.sortBy = sortType;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest sortOrder(@Nullable MessageSearchRequest.SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest includeNsfw(boolean includeNsfw) {
        this.includeNsfw = includeNsfw;
        return this;
    }

    @Nonnull
    @Override
    public MessageSearchRequest setCheck(BooleanSupplier checks) {
        return (MessageSearchRequest) super.setCheck(checks);
    }

    @Nonnull
    @Override
    public MessageSearchRequest timeout(long timeout, @Nonnull TimeUnit unit) {
        return (MessageSearchRequest) super.timeout(timeout, unit);
    }

    @Nonnull
    @Override
    public MessageSearchRequest deadline(long timestamp) {
        return (MessageSearchRequest) super.deadline(timestamp);
    }

    @Override
    protected Route.CompiledRoute finalizeRoute() {
        Route.CompiledRoute route = super.finalizeRoute();
        if (limit != null) {
            route = route.withQueryParams("limit", Integer.toString(limit));
        }
        if (offset != null) {
            route = route.withQueryParams("offset", Integer.toString(offset));
        }
        if (minId != null) {
            route = route.withQueryParams("min_id", Long.toString(minId));
        }
        if (maxId != null) {
            route = route.withQueryParams("max_id", Long.toString(maxId));
        }
        if (slop != null) {
            route = route.withQueryParams("slop", Integer.toString(slop));
        }
        if (content != null) {
            route = route.withQueryParams("content", content);
        }
        if (!channels.isEmpty()) {
            route = route.withQueryParams("channel_id", String.join(",", channels));
        }
        if (!authorTypes.isEmpty()) {
            route = route.withQueryParams(
                    "author_type",
                    authorTypes.stream().map(AuthorType::getValue).collect(Collectors.joining(",")));
        }
        if (!authors.isEmpty()) {
            route = route.withQueryParams("author_id", String.join(",", authors));
        }
        if (!mentions.isEmpty()) {
            route = route.withQueryParams("mentions", String.join(",", mentions));
        }
        if (mentionsEveryone != null) {
            route = route.withQueryParams("mention_everyone", Boolean.toString(mentionsEveryone));
        }
        if (pinned != null) {
            route = route.withQueryParams("pinned", Boolean.toString(pinned));
        }
        if (!hasTypes.isEmpty()) {
            route = route.withQueryParams(
                    "has", hasTypes.stream().map(HasType::getValue).collect(Collectors.joining(",")));
        }
        if (!embedTypes.isEmpty()) {
            route = route.withQueryParams(
                    "embed_type", embedTypes.stream().map(EmbedType::getValue).collect(Collectors.joining(",")));
        }
        if (!embedProviders.isEmpty()) {
            route = route.withQueryParams("embed_provider", String.join(",", embedProviders));
        }
        if (!linkHostnames.isEmpty()) {
            route = route.withQueryParams("link_hostname", String.join(",", linkHostnames));
        }
        if (!attachmentFilenames.isEmpty()) {
            route = route.withQueryParams("attachment_filename", String.join(",", attachmentFilenames));
        }
        if (!attachmentExtensions.isEmpty()) {
            route = route.withQueryParams("attachment_extension", String.join(",", attachmentExtensions));
        }
        if (sortBy != null) {
            route = route.withQueryParams("sort_by", sortBy.getValue());
        }
        if (sortOrder != null) {
            route = route.withQueryParams("sort_order", sortOrder.getValue());
        }
        if (includeNsfw != null) {
            route = route.withQueryParams("include_nsfw", Boolean.toString(includeNsfw));
        }

        return route;
    }

    @Override
    protected void handleSuccess(Response response, Request<MessageSearchResponse> request) {
        MessageSearchResponse searchResponse;
        DataObject object = response.getObject();
        if (response.code == 202) {
            searchResponse = new MessageSearchResponseImpl.FailureImpl(
                    object.getInt("documents_indexed"), object.getInt("retry_after"));
        } else {
            searchResponse = new MessageSearchResponseImpl.BodyImpl(
                    object.getString("analytics_id"),
                    // get [[unknown]]
                    object
                            .getArray("messages")
                            // Transform into Stream<[unknown]>
                            .stream(DataArray::getArray)
                            // Stream<[unknown]> -> Stream<object>
                            .flatMap(array -> array.stream(DataArray::getObject))
                            // Stream<object> -> Stream<Message>
                            .map(d -> api.getEntityBuilder().createMessageWithLookup(d, guild, false))
                            .collect(Helpers.toUnmodifiableList()),
                    object.getBoolean("doing_deep_historical_index"),
                    object.getInt("total_results"));
        }

        request.onSuccess(searchResponse);
    }
}
