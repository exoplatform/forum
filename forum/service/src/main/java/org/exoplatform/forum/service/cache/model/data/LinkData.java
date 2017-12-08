package org.exoplatform.forum.service.cache.model.data;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.ForumLinkData;

public class LinkData implements CachedData<ForumLinkData> {

  private final String id;
  private final String name;
  private final String path;
  private final String type;
  private final boolean isClosed;
  private final boolean isLock;

  public LinkData(ForumLinkData link) {
    this.id = link.getId();
    this.name = link.getName();
    this.path = link.getPath();
    this.type = link.getType();
    this.isClosed = link.getIsClosed();
    this.isLock = link.getIsLock();
  }

  public ForumLinkData build() {

    ForumLinkData link = new ForumLinkData();
    link.setId(this.id);
    link.setName(this.name);
    link.setPath(this.path);
    link.setType(this.type);
    link.setIsClosed(this.isClosed);
    link.setIsLock(this.isLock);
    return link;
    
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LinkData)) return false;

    LinkData linkData = (LinkData) o;

    return StringUtils.equals(id, linkData.id) && StringUtils.equals(name, linkData.name);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
