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

package net.dv8tion.jda.internal.entities.channel.middleman;

import gnu.trove.map.TLongObjectMap;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.channel.mixin.middleman.StandardGuildChannelMixin;
import net.dv8tion.jda.internal.interactions.ChannelInteractionPermissions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractStandardGuildChannelImpl<T extends AbstractStandardGuildChannelImpl<T>> extends AbstractGuildChannelImpl<T>
        implements StandardGuildChannelMixin<T>
{
    protected final TLongObjectMap<PermissionOverride> overrides = MiscUtil.newLongMap();
    @Nullable private ChannelInteractionPermissions interactionPermissions;

    protected long parentCategoryId;
    protected int position;

    public AbstractStandardGuildChannelImpl(long id, GuildImpl guild)
    {
        super(id, guild);
    }

    @Override
    public long getParentCategoryIdLong()
    {
        return parentCategoryId;
    }

    @Override
    public int getPositionRaw()
    {
        return position;
    }

    @Override
    public TLongObjectMap<PermissionOverride> getPermissionOverrideMap()
    {
        return overrides;
    }

    @Nonnull
    @Override
    public ChannelInteractionPermissions getInteractionPermissions()
    {
        if (interactionPermissions == null)
            throw new IllegalStateException("Cannot get interaction permissions outside of an interaction");
        return interactionPermissions;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public T setInteractionPermissions(@Nonnull ChannelInteractionPermissions interactionPermissions)
    {
        this.interactionPermissions = interactionPermissions;
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T setParentCategory(long parentCategoryId)
    {
        this.parentCategoryId = parentCategoryId;
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T setPosition(int position)
    {
        onPositionChange();
        this.position = position;
        return (T) this;
    }

    protected final void onPositionChange()
    {
        getGuild().getChannelView().clearCachedLists();
    }
}
