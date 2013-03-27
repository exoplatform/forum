package org.exoplatform.forum.service.cache.model.key;

import org.exoplatform.forum.service.SortSettings.Direction;
import org.exoplatform.forum.service.SortSettings.SortField;
import org.exoplatform.forum.service.cache.model.ScopeCacheKey;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class ForumListKey extends ScopeCacheKey {

  private final String categoryId;
  private final String strQuery;
  private final SortField sortField;
  private final Direction orderType;

  public ForumListKey(String categoryId, String strQuery) {
    this.categoryId = categoryId;
    this.strQuery = strQuery;
    this.sortField = null;
    this.orderType = null;
  }
  
  public ForumListKey(String categoryId, String strQuery, SortField sortField, Direction orderType) {
    this.categoryId = categoryId;
    this.strQuery = strQuery;
    this.sortField = sortField;
    this.orderType = orderType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ForumListKey)) return false;
    if (!super.equals(o)) return false;

    ForumListKey that = (ForumListKey) o;

    if (categoryId != null ? !categoryId.equals(that.categoryId) : that.categoryId != null) return false;
    if (strQuery != null ? !strQuery.equals(that.strQuery) : that.strQuery != null) return false;
    if (sortField != null ? !sortField.toString().equals(that.sortField.toString()) : that.sortField != null) return false;
    if (orderType != null ? !orderType.toString().equals(that.orderType.toString()) : that.orderType != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (categoryId != null ? categoryId.hashCode() : 0);
    result = 31 * result + (strQuery != null ? strQuery.hashCode() : 0);
    result = 31 * result + (sortField != null ? sortField.toString().hashCode() : 0);
    result = 31 * result + (orderType != null ? orderType.toString().hashCode() : 0);
    return result;
  }
  
}
