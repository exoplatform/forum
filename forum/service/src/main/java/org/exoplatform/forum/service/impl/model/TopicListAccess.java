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
package org.exoplatform.forum.service.impl.model;

import java.util.List;

import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.Topic;

public class TopicListAccess extends AbstractListAccess<Topic> {
  
  private TopicFilter filter;
  private DataStorage  storage;
  private Type type;
  
  public enum Type {
    TOPICS, BY_DATE, BY_USER
  }

  public TopicListAccess(Type type, DataStorage  storage, TopicFilter filter) {
    this.filter = filter;
    this.storage = storage;
    this.type = type;
  }

  @Override
  public Topic[] load(int index, int length) throws Exception, IllegalArgumentException {
    List<Topic> got = null;

    switch (type) {
      case TOPICS:
        got = storage.getTopics(filter, index, length);
        break;
      case BY_DATE:
        got = storage.getTopicsByDate(filter.date(), filter.forumPath(), length, index);
        break;
      default:
        break;
    }

    //
    reCalculate(index, length);
    if (got == null) {
      return new Topic[] {};
    }
    return got.toArray(new Topic[got.size()]);
  }

  @Override
  public int getSize() throws Exception {

    switch (type) {
      case TOPICS:
        size = storage.getTopicsCount(filter);
        break;
      case BY_DATE:
        size = (int) storage.getTotalTopicOld(filter.date(), filter.forumPath());
        break;
      default:
        break;
    }
    return size;
  }

  @Override
  public Topic[] load(int pageSelect) throws Exception, IllegalArgumentException {
    int offset = getOffset(pageSelect);
    int limit = getPageSize();
    return load(offset, limit);
  }

}
