package org.exoplatform.forum.service.cache.model.data;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.Watch;

public class WatchData implements CachedData<Watch> {

  private final String id;
  private final String userId;
  private final String email;
  private final String nodePath;
  private final String path;
  private final String typeNode;
  private final boolean isRSS;
  private final boolean isEmail;

  public WatchData(Watch watch) {
    this.id = watch.getId();
    this.userId = watch.getUserId();
    this.email = watch.getEmail();
    this.nodePath = watch.getNodePath();
    this.path = watch.getPath();
    this.typeNode = watch.getTypeNode();
    this.isRSS = watch.isAddWatchByRS();
    this.isEmail = watch.isAddWatchByEmail();
  }

  public Watch build() {
    Watch watch = new Watch();
    watch.setId(this.id);
    watch.setUserId(this.userId);
    watch.setEmail(this.email);
    watch.setNodePath(this.nodePath);
    watch.setPath(this.path);
    watch.setTypeNode(this.typeNode);
    watch.setIsAddWatchByRSS(this.isRSS);
    watch.setIsAddWatchByEmail(this.isEmail);
    return watch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof WatchData)) return false;

    WatchData watchData = (WatchData) o;

    return StringUtils.equals(id, watchData.id) && StringUtils.equals(userId, watchData.userId);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (userId != null ? userId.hashCode() : 0);
    return result;
  }
}
