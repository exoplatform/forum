/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.service.cache.model.selector;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.forum.common.cache.model.ScopeCacheKey;
import org.exoplatform.forum.common.cache.model.key.SimpleCacheKey;
import org.exoplatform.forum.common.cache.model.selector.ScopeCacheSelector;
import org.exoplatform.forum.service.cache.model.key.TopicListCountKey;
import org.exoplatform.services.cache.ObjectCacheInfo;

public class MiscDataSelector extends ScopeCacheSelector<ScopeCacheKey, Object> {

  private final String  type;


  public MiscDataSelector(String type) {
    this.type = type;
  }

  @Override
  public boolean select(ScopeCacheKey key, ObjectCacheInfo<? extends Object> ocinfo) {

    if (!super.select(key, ocinfo) || key instanceof SimpleCacheKey == false) {
      return false;
    }

    return StringUtils.equals(this.type, ((SimpleCacheKey) key).getType());
  }
}