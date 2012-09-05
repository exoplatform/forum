package org.exoplatform.forum.service.cache.model.data;

import java.util.List;

import org.exoplatform.forum.service.cache.model.AbstractListData;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class ListLinkData extends AbstractListData<LinkData> {

  public ListLinkData(List<LinkData> ids) {
    super(ids);
  }

}
