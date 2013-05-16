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
package org.exoplatform.forum.service.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.forum.service.ForumSearchResult;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 16, 2013  
 */
public class DiscussionSearchResult implements Collection<ForumSearchResult> {

  private final long offset;
  private final long limit;
  private final long totalSize;
  private long skip;
  private String[] ids;
  private String[] idsSkip;
  private List<ForumSearchResult> gotList = null;

  /**
   * Constructor with offset and limit
   * @param offset
   * @param limit
   * @param totalSize total size of nodes matched filter.
   */
  public DiscussionSearchResult(long offset, long limit, long totalSize) {
    this.offset = offset;
    this.limit = limit;
    this.totalSize = totalSize;
    ids = new String[0];
    idsSkip = new String[0];
    gotList = new ArrayList<ForumSearchResult>();
    skip = 0;
  }
  
 

  @Override
  public int size() {
    return gotList.size();
  }

  @Override
  public boolean isEmpty() {
    return gotList.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    if (o instanceof ForumSearchResult) {
      ForumSearchResult a = (ForumSearchResult) o;
      return contains(a.getId());
    }
    
    //
    return false;
  }

  /**
   * Checks the id whether is existing or not in result list.
   * @param id specified id
   * @return TRUE: existing otherwise FALSE
   */
  public boolean contains(String id) {
    return ArrayUtils.indexOf(ids, id) >= 0;
  }

  /**
   * Add more element into List or not
   * @return
   */
  public boolean addMore() {
    return gotList.size() < limit && gotList.size() < totalSize;
  }
  
  public long getOffset() {
    return offset;
  }

  public long getLimit() {
    return limit;
  }

  @Override
  public Iterator<ForumSearchResult> iterator() {
    return gotList.iterator();
  }

  @Override
  public Object[] toArray() {
    return null;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return null;
  }
  
  private boolean contains(String[] skipList, Object o) {
    if (o instanceof ForumSearchResult) {
      ForumSearchResult a = (ForumSearchResult) o;
      return ArrayUtils.indexOf(skipList, a.getId()) >= 0;
    }
    
    //
    return false;
  }

  @Override
  public boolean add(ForumSearchResult e) {
    if (contains(e)) {
      return false;
    }
    
    // contains in skipList, what contains offset list
    if (contains(idsSkip, e)) {
      return false;
    }
    
    //
    if (++skip <= offset) {
      idsSkip = (String[]) ArrayUtils.add(idsSkip, e.getId());
      return false;
    }
    
    //
    ids = (String[]) ArrayUtils.add(ids, e.getId());
    gotList.add(e);
    return true;
  }
  
  public List<ForumSearchResult> result() {
    return gotList;
  }

  @Override
  public boolean remove(Object o) {
    if (o instanceof ForumSearchResult) {
      ForumSearchResult a = (ForumSearchResult) o;
      ids = (String[]) ArrayUtils.removeElement(ids, a.getId());
      return gotList.remove(o);
    }
    
    //
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends ForumSearchResult> c) {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return false;
  }

  @Override
  public void clear() {
    ids = new String[0];
    gotList = new ArrayList<ForumSearchResult>();
  }
}
