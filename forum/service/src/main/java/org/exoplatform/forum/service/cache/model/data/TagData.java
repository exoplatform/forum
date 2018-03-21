package org.exoplatform.forum.service.cache.model.data;

import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.Tag;

import java.util.Arrays;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TagData tagData = (TagData) o;
    return useCount == tagData.useCount &&
            Objects.equals(id, tagData.id) &&
            Objects.equals(name, tagData.name) &&
            Arrays.equals(userTag, tagData.userTag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, userTag, useCount);
  }
}
