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

package net.dv8tion.jda.api.entities.messages;

import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.Range;

import java.util.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MessageSearchRequest extends RestAction<MessageSearchResponse> {
    @Nonnull
    @CheckReturnValue
    MessageSearchRequest limit(@Nullable @Range(from = 1, to = 25) Integer limit);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest offset(@Nullable @Range(from = 1, to = 9975) Integer offset);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest minId(@Nullable Long minId);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest maxId(@Nullable Long maxId);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest slop(@Nullable @Range(from = 0, to = 100) Integer slop);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest content(@Nullable String content);

    // TODO store in Set internally
    @Nonnull
    @CheckReturnValue
    MessageSearchRequest channels(@Nonnull Collection<? extends GuildMessageChannel> channels);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest channels(@Nonnull GuildMessageChannel... channels) {
        return channels(Arrays.asList(channels));
    }

    // TODO store in EnumSet internally
    @Nonnull
    @CheckReturnValue
    MessageSearchRequest authorTypes(@Nonnull Collection<AuthorType> authorTypes);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest authorTypes(@Nonnull AuthorType... authorTypes) {
        return authorTypes(Arrays.asList(authorTypes));
    }

    // TODO store in long set
    @Nonnull
    @CheckReturnValue
    MessageSearchRequest authors(@Nonnull Collection<? extends UserSnowflake> authors);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest authors(@Nonnull UserSnowflake... authors) {
        return authors(Arrays.asList(authors));
    }

    // TODO store in long set
    @Nonnull
    @CheckReturnValue
    MessageSearchRequest mentions(@Nonnull Collection<? extends UserSnowflake> mentions);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest mentions(@Nonnull UserSnowflake... mentions) {
        return mentions(Arrays.asList(mentions));
    }

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest mentionsEveryone(boolean mentionsEveryone);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest pinned(boolean pinned);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest hasTypes(@Nonnull Collection<HasType> hasTypes);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest hasTypes(@Nonnull HasType... hasTypes) {
        return hasTypes(Arrays.asList(hasTypes));
    }

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest embedTypes(@Nonnull Collection<EmbedType> embedTypes);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest embedTypes(@Nonnull EmbedType... embedTypes) {
        return embedTypes(Arrays.asList(embedTypes));
    }

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest embedProvider(@Nonnull Collection<String> embedProviders);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest embedProvider(@Nonnull String... embedProviders) {
        return embedProvider(Arrays.asList(embedProviders));
    }

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest linkHostnames(@Nonnull Collection<String> linkHostnames);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest linkHostnames(@Nonnull String... linkHostnames) {
        return linkHostnames(Arrays.asList(linkHostnames));
    }

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest attachmentFilenames(@Nonnull Collection<String> attachmentFilenames);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest attachmentFilenames(@Nonnull String... attachmentFilenames) {
        return attachmentFilenames(Arrays.asList(attachmentFilenames));
    }

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest attachmentExtensions(@Nonnull Collection<String> attachmentExtensions);

    @Nonnull
    @CheckReturnValue
    default MessageSearchRequest attachmentExtensions(@Nonnull String... attachmentExtensions) {
        return attachmentExtensions(Arrays.asList(attachmentExtensions));
    }

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest sortBy(@Nullable SortType sortType);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest sortOrder(@Nullable SortOrder sortOrder);

    @Nonnull
    @CheckReturnValue
    MessageSearchRequest includeNsfw(boolean includeNsfw);

    enum AuthorType {
        USER("user"),
        BOT("bot"),
        WEBHOOK("webhook");

        private final String value;

        AuthorType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum HasType {
        IMAGE("image"),
        SOUND("sound"),
        VIDEO("video"),
        FILE("file"),
        STICKER("sticker"),
        EMBED("embed"),
        LINK("link"),
        POLL("poll"),
        SNAPSHOT("snapshot"),
        ;

        private final String value;

        HasType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum EmbedType {
        IMAGE("image"),
        VIDEO("video"),
        SOUND("sound"),
        ARTICLE("article"),
        ;

        private final String value;

        EmbedType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum SortType {
        TIMESTAMP("timestamp"),
        RELEVANCE("relevance"),
        ;

        private final String value;

        SortType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum SortOrder {
        DESC("desc"),
        ASC("asc");

        private final String value;

        SortOrder(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
