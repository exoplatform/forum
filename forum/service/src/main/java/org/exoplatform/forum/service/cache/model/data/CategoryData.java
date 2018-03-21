package org.exoplatform.forum.service.cache.model.data;

import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class CategoryData implements CachedData<Category> {

  private static final long serialVersionUID = 1L;

  public final static CategoryData NULL = new CategoryData(new Category());

  private final String id;
  private final String owner;
  private final String path;
  private final long categoryOrder;
  private final Date createdDate;
  private final String modifiedBy;
  private final Date modifiedDate;
  private final String name;
  private final String description;
  private final String[] moderators;
  private final String[] userPrivate;
  private final String[] createTopicRole;
  private final String[] viewer;
  private final String[] poster;
  private final long forumCount;
  private final String[] emailNotification;

  public CategoryData(Category category) {

    this.id = category.getId();
    this.owner = category.getOwner();
    this.path = category.getPath();
    this.categoryOrder = category.getCategoryOrder();
    this.createdDate = category.getCreatedDate();
    this.modifiedBy = category.getModifiedBy();
    this.modifiedDate = category.getModifiedDate();
    this.name = category.getCategoryName();
    this.description = category.getDescription();
    this.moderators = category.getModerators();
    this.userPrivate = category.getUserPrivate();
    this.createTopicRole = category.getCreateTopicRole();
    this.viewer = category.getViewer();
    this.poster = category.getPoster();
    this.forumCount = category.getForumCount();
    this.emailNotification = category.getEmailNotification();

  }

  public Category build() {

    //
    if (this == NULL) {
      return null;
    }

    //
    Category category = new Category();
    category.setId(this.id);
    category.setOwner(this.owner);
    category.setPath(this.path);
    category.setCategoryOrder(this.categoryOrder);
    category.setCreatedDate(this.createdDate);
    category.setModifiedBy(this.modifiedBy);
    category.setModifiedDate(this.modifiedDate);
    category.setCategoryName(this.name);
    category.setDescription(this.description);
    category.setModerators(this.moderators);
    category.setCreateTopicRole(this.createTopicRole);
    category.setViewer(this.viewer);
    category.setPoster(this.poster);
    category.setForumCount(this.forumCount);
    category.setEmailNotification(this.emailNotification);
    if (Utils.isEmpty(this.userPrivate)) {
      category.setUserPrivate(new String[] {});
    } else {
      category.setUserPrivate(userPrivate);
    }
    return category;

  }
  
  public String getId() {
    return this.id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CategoryData that = (CategoryData) o;
    return categoryOrder == that.categoryOrder &&
            forumCount == that.forumCount &&
            Objects.equals(id, that.id) &&
            Objects.equals(owner, that.owner) &&
            Objects.equals(path, that.path) &&
            Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(modifiedBy, that.modifiedBy) &&
            Objects.equals(modifiedDate, that.modifiedDate) &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Arrays.equals(moderators, that.moderators) &&
            Arrays.equals(userPrivate, that.userPrivate) &&
            Arrays.equals(createTopicRole, that.createTopicRole) &&
            Arrays.equals(viewer, that.viewer) &&
            Arrays.equals(poster, that.poster) &&
            Arrays.equals(emailNotification, that.emailNotification);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, owner, path, categoryOrder, createdDate, modifiedBy, modifiedDate, name,
            description, moderators, userPrivate, createTopicRole, viewer, poster, forumCount, emailNotification);
  }
}
