package org.exoplatform.forum.webui;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

@ComponentConfig(template = "app:/templates/forum/webui/UIForumDescription.gtmpl")
public class UICategoryDescription extends UIContainer {
  private ForumService forumService;

  private String       categoryId;

  private Category     category  = null;

  private boolean      hasUpdate = false;

  public UICategoryDescription() throws Exception {
    forumService = (ForumService) ExoContainerContext.getCurrentContainer()
                                                     .getComponentInstanceOfType(ForumService.class);
  }

  public void setCategory(Category category) {
    this.category = category;
    this.hasUpdate = false;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
    this.hasUpdate = true;
  }

  public String getName() {
    return (getCategory() != null) ? category.getCategoryName() : null;
  }

  public String getDescription() {
    return (getCategory() != null) ? category.getDescription() : null;
  }

  private Category getCategory() {
    if (this.category == null || hasUpdate) {
      this.category = forumService.getCategory(categoryId);
    }
    return this.category;
  }

}
