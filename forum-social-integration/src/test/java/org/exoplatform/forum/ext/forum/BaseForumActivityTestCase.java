/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.forum;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.search.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.test.portal-configuration.xml")
})

public abstract class BaseForumActivityTestCase extends BaseExoTestCase {

  protected ForumService      forumService;
  
  protected KSDataLocation    dataLocation;

  protected IdentityManager   identityManager;

  protected ActivityManager   activityManager;

  public String categoryId = "forumCategoryabctest";

  public String forumId    = "forumtheidforumoftest";

  public String topicId    = "topicthetopicoftest";
  
  @Override
  public void setUp() throws Exception {
    begin();
    dataLocation = getService(KSDataLocation.class);
    forumService = getService(ForumService.class);
    identityManager = getService(IdentityManager.class);
    activityManager = getService(ActivityManager.class);
    //
    initDefaultData();
  }

  @Override
  public void tearDown() throws Exception {
    removeAllData();
    end();
  }

  public void removeAllData() throws Exception {
    List<Category> cats = forumService.getCategories();
    if (cats.size() > 0) {
      for (Category category : cats) {
        forumService.removeCategory(category.getId());
      }
    }
  }
  
  public ForumService getForumService() {
    return forumService;
  }

  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  
  public void initDefaultData() throws Exception {
    Category cat = createCategory(getId(Utils.CATEGORY));
    categoryId = cat.getId();
    forumService.saveCategory(cat, true);
    Forum forum = createdForum();
    forumId = forum.getId();
    forumService.saveForum(categoryId, forum, true);
    Topic topic = createdTopic("root");
    forumService.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    topicId = topic.getId();
  }
  
  public Post createdPost(String name, String message) {
    Post post = new Post();
    post.setOwner("root");
    post.setCreatedDate(new Date());
    post.setModifiedBy("root");
    post.setModifiedDate(new Date());
    post.setName(name);
    post.setMessage(message);
    post.setRemoteAddr("192.168.1.11");
    post.setIcon("classNameIcon");
    post.setIsApproved(true);
    post.setIsActiveByTopic(true);
    post.setIsHidden(false);
    post.setIsWaiting(false);
    return post;
  }

  public Topic createdTopic(String owner) {
    Topic topicNew = new Topic();

    topicNew.setOwner(owner);
    topicNew.setTopicName("TestTopic");
    topicNew.setCreatedDate(new Date());
    topicNew.setModifiedBy("root");
    topicNew.setModifiedDate(new Date());
    topicNew.setLastPostBy("root");
    topicNew.setLastPostDate(new Date());
    topicNew.setDescription("Topic description");
    topicNew.setPostCount(0);
    topicNew.setViewCount(0);
    topicNew.setIsNotifyWhenAddPost("");
    topicNew.setIsModeratePost(false);
    topicNew.setIsClosed(false);
    topicNew.setIsLock(false);
    topicNew.setIsWaiting(false);
    topicNew.setIsActive(true);
    topicNew.setIcon("classNameIcon");
    topicNew.setIsApproved(true);
    topicNew.setCanView(new String[] {});
    topicNew.setCanPost(new String[] {});
    return topicNew;
  }

  public Forum createdForum() {
    Forum forum = new Forum();
    forum.setOwner("root");
    forum.setForumName("TestForum");
    forum.setForumOrder(1);
    forum.setCreatedDate(new Date());
    forum.setModifiedBy("root");
    forum.setModifiedDate(new Date());
    forum.setLastTopicPath("");
    forum.setDescription("description");
    forum.setPostCount(0);
    forum.setTopicCount(0);

    forum.setNotifyWhenAddTopic(new String[] {});
    forum.setNotifyWhenAddPost(new String[] {});
    forum.setIsModeratePost(false);
    forum.setIsModerateTopic(false);
    forum.setIsClosed(false);
    forum.setIsLock(false);

    forum.setViewer(new String[] {});
    forum.setCreateTopicRole(new String[] {""});
    forum.setModerators(new String[] {});
    return forum;
  }

  public Category createCategory(String id) {
    Category cat = new Category(id);
    cat.setOwner("root");
    cat.setCategoryName("testCategory");
    cat.setCategoryOrder(1);
    cat.setCreatedDate(new Date());
    cat.setDescription("desciption");
    cat.setModifiedBy("root");
    cat.setModifiedDate(new Date());
    return cat;
  }

  public String getId(String type) {
    try {
      return type + IdGenerator.generate();
    } catch (Exception e) {
      return type + String.valueOf(new Random().nextLong());
    }
  }
}
