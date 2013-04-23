package org.exoplatform.forum.service.cache.model.data;

import java.util.List;

import org.exoplatform.forum.common.cache.model.AbstractListData;
import org.exoplatform.forum.service.cache.model.key.ForumKey;

public class ListForumData extends AbstractListData<ForumKey> {

  public ListForumData(List<ForumKey> ids) {
    super(ids);
  }

}
