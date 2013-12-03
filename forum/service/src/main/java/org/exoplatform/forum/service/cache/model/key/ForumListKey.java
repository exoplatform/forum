package org.exoplatform.forum.service.cache.model.key;

import org.exoplatform.forum.common.cache.model.ScopeCacheKey;
import org.exoplatform.forum.service.SortSettings.Direction;
import org.exoplatform.forum.service.SortSettings.SortField;
import org.exoplatform.forum.service.filter.model.ForumFilter;

public class ForumListKey extends ScopeCacheKey {
  private static final long serialVersionUID = 1L;
  private final ForumFilter filter;
  private final SortField sortField;
  private final Direction orderType;
  
  public ForumListKey(String categoryId, String strQuery) {
    filter = new ForumFilter(categoryId, strQuery, false);
    this.sortField = null;
    this.orderType = null;
  }
  
  public ForumListKey(String categoryId, String strQuery, SortField sortField, Direction orderType) {
    filter = new ForumFilter(categoryId, strQuery, false);
    this.sortField = sortField;
    this.orderType = orderType;
  }
  
  public ForumListKey(ForumFilter filter, SortField sortField, Direction orderType) {
    this.filter = filter;
    this.sortField = sortField;
    this.orderType = orderType;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ForumListKey)) return false;
    if (!super.equals(o)) return false;

    ForumListKey that = (ForumListKey) o;
    
    if(filter.equals(that.filter) == false) return false;

    if (sortField != null ? !sortField.toString().equals(that.sortField.toString()) : that.sortField != null) return false;
    if (orderType != null ? !orderType.toString().equals(that.orderType.toString()) : that.orderType != null) return false;

    return true;
  }

  private int hashCode(int current, Object o) {
    if (o != null) {
      return 31 * current + o.toString().hashCode();
    }
    return current;
  }
  
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = hashCode(result, orderType);
    result = hashCode(result, sortField);
    return 31 * result + filter.hashCode();
  }
  
}
