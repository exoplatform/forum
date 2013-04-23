package org.exoplatform.forum.service.cache.model.data;

import java.util.List;

import org.exoplatform.forum.common.cache.model.AbstractListData;
import org.exoplatform.forum.service.cache.model.key.CategoryKey;

public class ListCategoryData extends AbstractListData<CategoryKey> {

  public ListCategoryData(List<CategoryKey> ids) {
    super(ids);
  }

}
