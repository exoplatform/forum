/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.service.cache.model.selector;

import org.exoplatform.forum.common.cache.model.ScopeCacheKey;
import org.exoplatform.forum.common.cache.model.selector.ScopeCacheSelector;
import org.exoplatform.forum.service.cache.model.key.TopicListKey;
import org.exoplatform.services.cache.ObjectCacheInfo;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Dec 16, 2013  
 */
public class TopicListSelector extends ScopeCacheSelector<ScopeCacheKey, Object> {

  private String forumId;

  public TopicListSelector(final String forumId) {

    this.forumId = forumId;
  }

  @Override
  public boolean select(final ScopeCacheKey key, final ObjectCacheInfo<? extends Object> ocinfo) {
    if (!super.select(key, ocinfo)) {
      return false;
    }

    //clear all in TopicListCache
    if (this.forumId == null) {
      return true;
    }
    
    if (key instanceof TopicListKey) {
      return this.forumId.equals(((TopicListKey)key).getForumId());
    }
    
    return false;
  }

}
