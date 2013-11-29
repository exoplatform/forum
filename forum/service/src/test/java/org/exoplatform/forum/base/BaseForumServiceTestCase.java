/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAdministration;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Tag;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.test.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/rest/exo.forum.component.service.test.configuration.xml")
  
})
public abstract class BaseForumServiceTestCase extends BaseExoTestCase {
  public static final String         USER_ROOT         = "root";

  public static final String         USER_DEMO         = "demo";

  public static final String         USER_JOHN         = "john";

  public Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();

  public ForumService                forumService_;

  public KSDataLocation              dataLocation;

  public String                      categoryId;

  public String                      forumId;

  public String                      topicId;

  @Override
  public void setUp() throws Exception {
    begin();
    //
    if (forumService_ == null) {
      forumService_ = (ForumService) getService(ForumService.class);
      dataLocation = (KSDataLocation) getService(KSDataLocation.class);
    }
  }

  @Override
  public void tearDown() throws Exception {

    removeAllData();
    //
    end();
  }
  

  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }
  
  public void initDefaultData() throws Exception {
    Category cat = createCategory(getId(Utils.CATEGORY));
    this.categoryId = cat.getId();
    forumService_.saveCategory(cat, true);
    Forum forum = createdForum();
    this.forumId = forum.getId();
    forumService_.saveForum(categoryId, forum, true);
    Topic topic = createdTopic("root");
    forumService_.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    this.topicId = topic.getId();
  }

  public void removeAllData() throws Exception {
    List<Category> cats = forumService_.getCategories();
    if (cats.size() > 0) {
      for (Category category : cats) {
        forumService_.removeCategory(category.getId());
      }
    }
  }

  public String ArrayToString(String[] strs) {
    List<String> list = Arrays.asList(strs);
    Collections.sort(list);
    return list.toString().replace("[", "").replace("]", "");
  }

  public UserProfile createdUserProfile(String userName) {
    UserProfile userProfile = new UserProfile();
    userProfile.setUserId(userName);
    userProfile.setUserRole((long) 0);
    userProfile.setUserTitle(Utils.ADMIN);
    userProfile.setEmail("duytucntt@gmail.com");
    userProfile.setJoinedDate(new Date());
    userProfile.setTimeZone(7.0);
    userProfile.setSignature("signature");
    return userProfile;
  }

  public Post createdPost() {
    Post post = new Post();
    post.setOwner(USER_ROOT);
    post.setCreatedDate(new Date());
    post.setModifiedBy(USER_ROOT);
    post.setModifiedDate(new Date());
    post.setName("SubJect");
    post.setMessage("content description");
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

  protected Tag createTag(String name, String user) {
    Tag tag = new Tag();
    tag.setName(name);
    tag.setId(Utils.TAG + name);
    tag.setUserTag(new String[] { user });
    return tag;
  }

  public ForumAdministration createForumAdministration() {
    ForumAdministration forumAdministration = new ForumAdministration();
    forumAdministration.setForumSortBy("forumName");
    forumAdministration.setForumSortByType("ascending");
    forumAdministration.setTopicSortBy("threadName");
    forumAdministration.setTopicSortByType("ascending");
    forumAdministration.setCensoredKeyword("");
    forumAdministration.setEnableHeaderSubject(false);
    forumAdministration.setHeaderSubject("");
    forumAdministration.setNotifyEmailContent("");
    return forumAdministration;
  }

  public Forum convertToForum(Object object) throws Exception {
    if (object instanceof Forum) {
      return forumService_.getForum(((Forum) object).getCategoryId(), ((Forum) object).getId());
    }
    return null;
  }

  public void setMembershipEntry(String group, String membershipType, boolean isNew) {
    MembershipEntry membershipEntry = new MembershipEntry(group, membershipType);
    if (isNew) {
      membershipEntries.clear();
    }
    membershipEntries.add(membershipEntry);
  }

  public void loginUser(String userId) {
    Identity identity = new Identity(userId, membershipEntries);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
  }

}
