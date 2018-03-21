package org.exoplatform.forum.service.cache.model.data;

import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.Watch;

import java.util.Objects;

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
    if (o == null || getClass() != o.getClass()) return false;
    WatchData watchData = (WatchData) o;
    return isRSS == watchData.isRSS &&
            isEmail == watchData.isEmail &&
            Objects.equals(id, watchData.id) &&
            Objects.equals(userId, watchData.userId) &&
            Objects.equals(email, watchData.email) &&
            Objects.equals(nodePath, watchData.nodePath) &&
            Objects.equals(path, watchData.path) &&
            Objects.equals(typeNode, watchData.typeNode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, email, nodePath, path, typeNode, isRSS, isEmail);
  }
}
