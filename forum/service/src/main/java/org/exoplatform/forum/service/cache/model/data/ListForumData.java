package org.exoplatform.forum.service.cache.model.data;

import java.util.List;

import org.exoplatform.forum.service.cache.model.AbstractListData;
import org.exoplatform.forum.service.cache.model.key.ForumKey;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class ListForumData extends AbstractListData<ForumKey> {

  public ListForumData(List<ForumKey> ids) {
    super(ids);
  }

}
