package org.exoplatform.forum.service.cache.model.data;

import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.Tag;

public class TagData implements CachedData<Tag> {

  private final String id;
  private final String name;
  private final String[] userTag;
  private final long useCount;

  public TagData(Tag tag) {
    this.id = tag.getId();
    this.name = tag.getName();
    this.userTag = tag.getUserTag();
    this.useCount = tag.getUseCount();
  }

  public Tag build() {

    Tag tag = new Tag();
    tag.setId(this.id);
    tag.setName(this.name);
    tag.setUserTag(this.userTag);
    tag.setUseCount(this.useCount);

    return tag;
  }
}
