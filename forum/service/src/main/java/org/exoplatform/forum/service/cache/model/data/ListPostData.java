package org.exoplatform.forum.service.cache.model.data;

import java.util.List;

import org.exoplatform.forum.common.cache.model.AbstractListData;
import org.exoplatform.forum.service.cache.model.key.PostKey;

public class ListPostData extends AbstractListData<PostKey> {

  private static final long serialVersionUID = 1L;

  public ListPostData(List<PostKey> ids) {
    super(ids);
  }

}