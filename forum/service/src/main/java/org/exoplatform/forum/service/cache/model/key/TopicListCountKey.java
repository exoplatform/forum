/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.forum.service.cache.model.key;

import org.exoplatform.forum.common.cache.model.ScopeCacheKey;

public class TopicListCountKey extends ScopeCacheKey {
  private static final long serialVersionUID = 1L;

  private String            key;

  private String            forumId;

  public TopicListCountKey(String key, String forumId) {
    this.key = key;
    this.forumId = forumId;
  }

  public String getForumId() {
    return forumId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof TopicListCountKey))
      return false;
    if (!super.equals(o))
      return false;

    TopicListCountKey that = (TopicListCountKey) o;

    if (key != null ? !key.equals(that.key) : that.key != null) {
      return false;
    }
    if (forumId != null ? !forumId.equals(that.getForumId()) : that.getForumId() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (key != null ? key.hashCode() : 0);
    result = 31 * result + (forumId != null ? forumId.hashCode() : 0);
    return result;
  }
}
