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
package org.exoplatform.forum.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;

import org.apache.commons.io.FileUtils;
import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.service.filter.model.ForumFilter;

public class ForumServiceTestCase extends BaseForumServiceTestCase {
  
  @Override
  public void setUp() throws Exception {
    super.setUp();

  }

  @Override
  public void tearDown() throws Exception {
    //
    removeAllData();
    super.tearDown();
  }
  
  public void testGetObjectNameByPathAfterDeleted() throws Exception {
    Category cate = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cate, true);
    String catId = cate.getId();
    Forum forum = createdForum();
    forumService_.saveForum(catId, forum, true);
    String forumId = forum.getId();
    
    forumService_.removeCategory(catId);
    Object objectForum = forumService_.getObjectNameByPath(catId+"/"+forumId);
    assertNull(objectForum);
  }

  public void testForumStatic() throws Exception {
    //
    resetAllUserProfile();
    
    ForumStatistic forumStatistic = forumService_.getForumStatistic();
    assertNotNull(forumStatistic);
    assertEquals(8, forumStatistic.getMembersCount());
    
    forumStatistic.setPostCount(20);
    forumStatistic.setTopicCount(10);
    forumService_.saveForumStatistic(forumStatistic);

    forumStatistic = forumService_.getForumStatistic();
    assertEquals(10, forumStatistic.getTopicCount());
    assertEquals(20, forumStatistic.getPostCount());
    // reset
    forumService_.saveForumStatistic(new ForumStatistic());
    // make one topic
    initDefaultData();
    // create 10 topics and 10 posts for each topic. On each topic contain one post is first post.
    for (int i = 0; i < 10; i++) {
      forumService_.saveTopic(categoryId, forumId, createdTopic(USER_DEMO), true, false, new MessageBuilder());
      forumService_.savePost(categoryId, forumId, topicId, createdPost(), true, new MessageBuilder());
    }
    forumStatistic = forumService_.getForumStatistic();
    // we have 11 topics and 21 posts.
    assertEquals(11, forumStatistic.getTopicCount());
    assertEquals(21, forumStatistic.getPostCount());
    
    UserProfile profile = forumService_.getUserInfo(USER_ROOT);
    
    assertEquals(1, profile.getTotalTopic());
    assertEquals(11, profile.getTotalPost());
    
    profile = forumService_.getUserInfo(USER_DEMO);
    assertEquals(10, profile.getTotalTopic());
    assertEquals(10, profile.getTotalPost());
  }

  public void testForumAdministration() throws Exception {
    ForumAdministration administration = createForumAdministration();
    forumService_.saveForumAdministration(administration);
    administration = forumService_.getForumAdministration();
    assertNotNull(administration);
    assertEquals(administration.getForumSortBy(), "forumName");
  }

  public void testGetObjectNameByPath() throws Exception {
    // set Data
    initDefaultData();

    // Test get object by path
    String topicPath = forumService_.getForumHomePath();
    topicPath = categoryId + "/" + forumId + "/" + topicId;
    assertNotNull(forumService_.getObjectNameByPath(topicPath));

    // Test get object by path in case the object has been updated
    // by saveForum
    String forumPath = categoryId + "/" + forumId;
    Forum originalForum = convertToForum(forumService_.getObjectNameByPath(forumPath));
    assertNotNull(originalForum);
    originalForum.setForumName("aaa");
    forumService_.saveForum(categoryId, originalForum, false);

    Forum updatedForum = convertToForum(forumService_.getObjectNameByPath(forumPath));
    assertNotNull(updatedForum);
    assertEquals(originalForum.getForumName(), updatedForum.getForumName());

    // by modifyForum
    originalForum.setIsLock(true);
    forumService_.modifyForum(originalForum, Utils.LOCK);
    updatedForum = convertToForum(forumService_.getObjectNameByPath(forumPath));
    assertNotNull(updatedForum);
    assertTrue(updatedForum.getIsLock());

    // by saveModerateOfForums
    List<String> list = new ArrayList<String>();
    list.add(forumPath);
    forumService_.saveModerateOfForums(list, "demo", false);
    updatedForum = convertToForum(forumService_.getObjectNameByPath(forumPath));
    assertNotNull(updatedForum);
    list.clear();
    list.addAll(Arrays.asList(updatedForum.getModerators()));
    assertTrue(list.contains("demo"));

    // by moveForum
    Category cate = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cate, true);
    Category cateNew = forumService_.getCategory(cate.getId());
    List<Forum> forums = new ArrayList<Forum>();
    forums.add(originalForum);
    forumService_.moveForum(forums, cateNew.getPath());
    originalForum = convertToForum(forumService_.getObjectNameByPath(forumPath));
    assertNull(originalForum);
    updatedForum = convertToForum(forumService_.getObjectNameByPath(cateNew.getId() + "/" + forumId));
    assertNotNull(updatedForum);

    // by removeForum
    forumService_.removeForum(cateNew.getId(), forumId);
    updatedForum = convertToForum(forumService_.getObjectNameByPath(cateNew.getId() + "/" + forumId));
    assertNull(updatedForum);
  }

  public void testGetObjectNameById() throws Exception {
    // set Data
    initDefaultData();

    // Test get object by id
    assertNotNull(forumService_.getObjectNameById(forumId, Utils.FORUM));

    // Test get object by id in case the object has been updated
    // by saveForum
    Forum originalForum = convertToForum(forumService_.getObjectNameById(forumId, Utils.FORUM));
    assertNotNull(originalForum);
    originalForum.setForumName("aaa");
    forumService_.saveForum(categoryId, originalForum, false);

    Forum updatedForum = convertToForum(forumService_.getObjectNameById(forumId, Utils.FORUM));
    assertNotNull(updatedForum);
    assertEquals(originalForum.getForumName(), updatedForum.getForumName());

    // by modifyForum
    originalForum.setIsLock(true);
    forumService_.modifyForum(originalForum, Utils.LOCK);
    updatedForum = convertToForum(forumService_.getObjectNameById(forumId, Utils.FORUM));
    assertNotNull(updatedForum);
    assertTrue(updatedForum.getIsLock());

    // by saveModerateOfForums
    List<String> list = new ArrayList<String>();
    list.add(categoryId + "/" + forumId);
    forumService_.saveModerateOfForums(list, "demo", false);
    updatedForum = convertToForum(forumService_.getObjectNameById(forumId, Utils.FORUM));
    assertNotNull(updatedForum);
    list.clear();
    list.addAll(Arrays.asList(updatedForum.getModerators()));
    assertTrue(list.contains("demo"));

    // by moveForum
    Category cate = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cate, true);
    Category cateNew = forumService_.getCategory(cate.getId());
    List<Forum> forums = new ArrayList<Forum>();
    forums.add(originalForum);
    forumService_.moveForum(forums, cateNew.getPath());
    updatedForum = convertToForum(forumService_.getObjectNameById(forumId, Utils.FORUM));
    assertNotNull(updatedForum);
    assertEquals(cateNew.getPath() + "/" + forumId, updatedForum.getPath());

    // by removeForum
    forumService_.removeForum(cateNew.getId(), forumId);
    updatedForum = convertToForum(forumService_.getObjectNameById(forumId, Utils.FORUM));
    assertNull(updatedForum);

  }

  // Private Message
  public void testPrivateMessage() throws Exception {
    ForumPrivateMessage privateMessage = new ForumPrivateMessage();
    privateMessage.setFrom("demo");
    privateMessage.setIsUnread(false);
    privateMessage.setName("privateMessage Name");
    privateMessage.setMessage("Content privateMessage");
    privateMessage.setSendTo("root");

    // savePtivateMs
    forumService_.savePrivateMessage(privateMessage);

    // get Private Message is SEND_MESSAGE
    JCRPageList pageList = forumService_.getPrivateMessage("demo", Utils.SEND_MESSAGE);
    assertNotNull(pageList);
    assertEquals(pageList.getAvailable(), 1);
    privateMessage = (ForumPrivateMessage) pageList.getPage(1).get(0);
    String privateMessageId_SEND = privateMessage.getId();

    // get Private Message is RECEIVE_MESSAGE
    pageList = forumService_.getPrivateMessage("root", Utils.RECEIVE_MESSAGE);
    assertNotNull(pageList);
    assertEquals(pageList.getAvailable(), 1);
    privateMessage = (ForumPrivateMessage) pageList.getPage(1).get(0);
    String privateMessageId_RECEIVE = privateMessage.getId();
    //
    long t = forumService_.getNewPrivateMessage("root");
    assertEquals(t, 1);

    // Remove PrivateMessage
    forumService_.removePrivateMessage(privateMessageId_SEND, "demo", Utils.SEND_MESSAGE);
    pageList = forumService_.getPrivateMessage("demo", Utils.SEND_MESSAGE);
    assertEquals(pageList.getAvailable(), 0);
    forumService_.removePrivateMessage(privateMessageId_RECEIVE, "root", Utils.RECEIVE_MESSAGE);
    pageList = forumService_.getPrivateMessage("root", Utils.RECEIVE_MESSAGE);
    assertEquals(pageList.getAvailable(), 0);
    //
  }

  // BookMark
  public void testBookMark() throws Exception {
    // set Data
    initDefaultData();

    // add bookmark
    String bookMark = Utils.CATEGORY + "//" + categoryId;
    forumService_.saveUserBookmark("root", bookMark, true);
    bookMark = Utils.FORUM + "//" + categoryId + "/" + forumId;
    forumService_.saveUserBookmark("root", bookMark, true);

    // get bookmark
    List<String> bookMarks = new ArrayList<String>();
    bookMarks.addAll(forumService_.getBookmarks("root"));
    assertEquals(bookMarks.size(), 2);
  }

  private List<Tag> getTagsByTopic(Topic topic, String userName) throws Exception {
    List<String> listTagId = new ArrayList<String>();
    String[] tagIds = topic.getTagId();
    String[] temp;
    for (int i = 0; i < tagIds.length; i++) {
      temp = tagIds[i].split(":");
      if (temp[0].equals(userName)) {
        listTagId.add(temp[1]);
      }
    }

    return forumService_.getMyTagInTopic(listTagId.toArray(new String[listTagId.size()]));
  }

  public void testTag() throws Exception {
    // set Data
    initDefaultData();
    List<Tag> tags = new ArrayList<Tag>();
    int size = 5;
    for (int i = 0; i < size; i++) {
      Tag tag = createTag("tag" + i, USER_ROOT);
      tags.add(tag);
    }

    Topic topic = forumService_.getTopic(categoryId, forumId, topicId, "");
    forumService_.addTag(tags, USER_ROOT, topic.getPath());
    
    // get tags in topic by user root
    topic = forumService_.getTopic(categoryId, forumId, topicId, "");
    assertEquals(size, topic.getTagId().length);

    List<Tag> tags_ = getTagsByTopic(topic, USER_ROOT);
    assertEquals(size, tags_.size());
    
    //test get other tags on topic
    for (int i = 10; i < 16; i++) {
      Tag tag = createTag("tag" + i, USER_ROOT);
      tags.add(tag);
    }
    forumService_.addTag(tags, USER_DEMO, topic.getPath());
    //
    List<String> list = forumService_.getTagNameInTopic(USER_ROOT+","+topicId);
    
    assertEquals(5, list.size());

    // Test get tag
    String id = Utils.TAG + tags.get(0).getName();
    assertNotNull(forumService_.getTag(id));

    // Get all tag
     assertEquals(11, forumService_.getAllTags().size());

  }

  public void testWatch() throws Exception {
    // set Data
    initDefaultData();
    // addWatch
    String topicPath = categoryId + "/" + forumId;
    List<String> values = new ArrayList<String>();
    values.add("duytucntt@gmail.com");
    forumService_.addWatch(1, topicPath, values, "root");
    // watch by user
    List<Watch> watchs = forumService_.getWatchByUser("root");
    assertEquals(watchs.get(0).getEmail(), values.get(0));
    forumService_.removeWatch(1, topicPath, "/" + values.get(0));
    watchs = forumService_.getWatchByUser("root");
    assertEquals(watchs.size(), 0);
  }

  public void testIpBan() throws Exception {
    // set Data
    initDefaultData();
    // set Ip ban
    String ip = "192.168.1.10";
    // save Ip ban
    forumService_.addBanIP(ip);
    // get Ip ban
    List<String> listBans = forumService_.getBanList();
    assertEquals("Ip have adding in listBans", listBans.get(0), ip);
    // addBanIPForum
    forumService_.addBanIPForum(ip, categoryId + "/" + forumId);
    // getForumBanList
    List<String> listIpBanInForum = forumService_.getForumBanList(categoryId + "/" + forumId);
    assertEquals("Ip add in forum", listIpBanInForum.get(0), ip);
    // removeBanIPForum
    forumService_.removeBanIPForum(ip, categoryId + "/" + forumId);
    listIpBanInForum = forumService_.getForumBanList(categoryId + "/" + forumId);
    assertEquals("Ip is removed in listIpBanInForum, size is not 0 ", listIpBanInForum.size(), 0);
    // removeIpBan
    forumService_.removeBan(ip);
    listBans = forumService_.getBanList();
    assertEquals("Ip is removed in listBans, size is not 0 ", listBans.size(), 0);
  }

  public void testCalculateDeletedGroupForSpace() throws Exception {
    removeAllData();
    // test for case in spaces:
    String groupId = "/spaces/new_space";
    String groupName = "new_space";
    String cateId = Utils.CATEGORY + "spaces";
    String forumId = Utils.FORUM_SPACE_ID_PREFIX + groupName;
    Category category = createCategory(cateId);
    category.setCategoryName("spaces");
    category.setUserPrivate(new String[] { groupId });
    forumService_.saveCategory(category, true);
    Forum forum = createdForum();
    forum.setForumName("New Space");
    forum.setId(forumId);
    forumService_.saveForum(cateId, forum, true);
    assertNotNull(String.format("The forum %s in space %s is null", forumId, groupName), forumService_.getForum(cateId, forumId));
    forumService_.calculateDeletedGroup(groupId, groupName);
    assertNull(String.format("The forum %s is not null after deleted the group %s ", forumId, groupId), forumService_.getForum(cateId, forumId));
  }

  public void testCalculateDeletedGroupForNormal() throws Exception {
    removeAllData();
    // set group in categories/forums/topics
    String groupId = "/platform/users";
    String groupName = "users";
    UserProfile profile = createdUserProfile(USER_DEMO);
    profile.setUserRole(UserProfile.USER);
    profile.setUserTitle("User");
    profile.setModerateForums(new String[] { "" });
    profile.setModerateCategory(new String[] { "" });
    forumService_.saveUserProfile(profile, false, false);
    forumService_.saveUserModerator(USER_DEMO, new ArrayList<String>(), false);
    assertEquals(UserProfile.USER, forumService_.getUserInfo(USER_DEMO).getUserRole());

    String[] groupUser = new String[] { groupId, USER_ROOT };
    Category category = createCategory(getId(Utils.CATEGORY));
    category.setUserPrivate(groupUser);
    category.setCreateTopicRole(groupUser);
    category.setModerators(groupUser);
    category.setViewer(groupUser);
    category.setPoster(groupUser);
    forumService_.saveCategory(category, true);
    Forum forum = createdForum();
    forum.setViewer(groupUser);
    forum.setCreateTopicRole(groupUser);
    forum.setPoster(groupUser);
    forum.setModerators(groupUser);
    forumService_.saveForum(category.getId(), forum, true);
    // the user demo in group "/platform/users" is moderator of forum, checking
    // it
    assertEquals(UserProfile.MODERATOR, forumService_.getUserInfo(USER_DEMO).getUserRole());

    Topic topic = createdTopic(USER_DEMO);
    topic.setCanView(groupUser);
    topic.setCanPost(groupUser);
    forumService_.saveTopic(category.getId(), forum.getId(), topic, true, false, new MessageBuilder());
    // checking data
    assertEquals(ArrayToString(groupUser), ArrayToString(forumService_.getCategory(category.getId()).getUserPrivate()));
    assertEquals(ArrayToString(groupUser), ArrayToString(forumService_.getForum(category.getId(), forum.getId()).getModerators()));
    topic = forumService_.getTopic(category.getId(), forum.getId(), topic.getId(), null);
    assertEquals(ArrayToString(groupUser), ArrayToString(topic.getCanView()));
    // deleted group in system
    forumService_.calculateDeletedGroup(groupId, groupName);
    // checking again data
    assertEquals(UserProfile.USER, forumService_.getUserInfo(USER_DEMO).getUserRole());
    assertEquals(USER_ROOT, ArrayToString(forumService_.getCategory(category.getId()).getUserPrivate()));
    assertEquals(USER_ROOT, ArrayToString(forumService_.getForum(category.getId(), forum.getId()).getModerators()));
    assertEquals(USER_ROOT, ArrayToString(forumService_.getTopic(category.getId(), forum.getId(), topic.getId(), null).getCanView()));
    
    //update forum moderator
    profile = createdUserProfile("mary");
    profile.setUserRole(UserProfile.USER);
    profile.setUserTitle("User");
    profile.setModerateForums(new String[] { "" });
    profile.setModerateCategory(new String[] { "" });
    forumService_.saveUserProfile(profile, false, false);
    
    groupUser = new String[] {"mary"};
    forum.setModerators(groupUser);
    forumService_.saveForum(category.getId(), forum, false);
    assertEquals(UserProfile.MODERATOR, forumService_.getUserInfo("mary").getUserRole());
  }

  public void testImportXML() throws Exception {
    Category cat = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cat, true);
    cat = forumService_.getCategory(cat.getId());
    String pathNode = cat.getPath();
    assertEquals("Before import data, category don't have any forum", forumService_.getForums(cat.getId(), "").size(), 0);
    try {
      File file = new File(System.getProperty("user.dir") + "/src/test/resources/conf/portal/Data.xml");
      String content = FileUtils.readFileToString(file, "UTF-8");
      byte currentXMLBytes[] = content.getBytes();
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(currentXMLBytes);
      // Import forum into category
      forumService_.importXML(pathNode, byteArrayInputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      assertEquals("Can't import forum into category", forumService_.getForums(cat.getId(), "").size(), 1);
    } catch (IOException e) {
      log.debug("Failed to test importXML", e);
    }
  }

  public void testExportXML() throws Exception {
    Category cat = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cat, true);
    cat = forumService_.getCategory(cat.getId());
    Forum forum = createdForum();
    forumService_.saveForum(cat.getId(), forum, true);
    forum = forumService_.getForum(cat.getId(), forum.getId());
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    forumService_.exportXML(cat.getId(), forum.getId(), new ArrayList<String>(), forum.getPath(), bos, false);
    assertEquals("can't export Forum into XML file", bos.size() > 0, true);
  }
  
  public void testGetScreenName() throws Exception {
    UserProfile userProfile = createdUserProfile(USER_DEMO);
    userProfile.setScreenName("Jack Miller");

    // save UserProfile
    forumService_.saveUserProfile(userProfile, true, true);

    // getUserInfo
    userProfile = forumService_.getUserInfo(USER_DEMO);
    assertEquals("Jack Miller",forumService_.getScreenName("demo"));
    
    // change screenName
    userProfile.setScreenName("John Smith");
    forumService_.saveUserSettingProfile(userProfile);
    userProfile = forumService_.getUserInfo(USER_DEMO);
    assertEquals("John Smith",forumService_.getScreenName("demo"));
  }
  
  public void testGetAllLink() throws Exception {
    // save normal category
    Category cat = createCategory(new Category().getId());
    forumService_.saveCategory(cat, true);
    // save normal forum
    Forum forum = createdForum();
    forumService_.saveForum(cat.getId(), forum, true);
    forum.setId(new Forum().getId());
    forumService_.saveForum(cat.getId(), forum, true);

    // save category in space
    String cateIdSpace = Utils.CATEGORY_SPACE_ID_PREFIX;
    cat = createCategory(cateIdSpace);
    forumService_.saveCategory(cat, true);
    // save forum in space 1
    forum.setId("forumSpaceroot_space");
    forum.setForumName("Root spcase");
    forumService_.saveForum(cat.getId(), forum, true);
    // save forum in space 2
    forum.setId("forumSpacetest_space");
    forumService_.saveForum(cat.getId(), forum, true);
    // save forum in space 3
    forum.setId("forumSpaceabc_space");
    forumService_.saveForum(cat.getId(), forum, true);

    List<ForumLinkData> forumLinks = new ArrayList<ForumLinkData>();
    StringBuilder strQueryCate = new StringBuilder();
    strQueryCate.append("[@exo:id !='").append(cateIdSpace).append("']");
    forumLinks.addAll(forumService_.getAllLink(strQueryCate.toString(), ""));
    strQueryCate = new StringBuilder();
    strQueryCate.append("[@exo:id='").append(cateIdSpace).append("']");
    String forumQr = "[(jcr:like(@" + Utils.EXO_ID + ",'%" + "test_space" + "%'))]";
    forumLinks.addAll(forumService_.getAllLink(strQueryCate.toString(), forumQr));
    // list has size is 5 (2 categories and 2 normal forums and 1 forum in category space)
    assertEquals("The size of list forumLinks not equals 5.", forumLinks.size(), 5);
  }
  
  public void testMoveForum() throws Exception {
    Category cat1 = createCategory(new Category().getId());
    forumService_.saveCategory(cat1, true);
    Forum forum = createdForum();
    forumService_.saveForum(cat1.getId(), forum, true);
    Category cat2 = createCategory(new Category().getId());
    forumService_.saveCategory(cat2, true);
    
    String forumId = forum.getId();
    cat1 = forumService_.getCategory(cat1.getId());
    assertNotNull(cat1);
    cat2 = forumService_.getCategory(cat2.getId());
    assertNotNull(cat2);
    
    List<Forum> forums = new ArrayList<Forum>();
    forums.add(forum);
    
    forumService_.moveForum(forums, cat2.getPath());
    
    //make sure forum is moved out of cat1
    forum = forumService_.getForum(cat1.getId(), forumId);
    assertNull(forum);
    
    //make sure forum is moved to cat2
    forum = forumService_.getForum(cat2.getId(), forumId);
    assertNotNull(forum);
    
    forums = new ArrayList<Forum>();
    forums.add(forum);
    forumService_.moveForum(forums, cat1.getPath());
    
    //make sure forum is moved out of cat2
    forum = forumService_.getForum(cat2.getId(), forumId);
    assertNull(forum);
    
    //make sure forum is moved to cat1
    forum = forumService_.getForum(cat1.getId(), forumId);
    assertNotNull(forum);
    
    //Move a forum from category space to a normal category
    String cateIdSpace = Utils.CATEGORY_SPACE_ID_PREFIX;
    Category catSpace = createCategory(cateIdSpace);
    forumService_.saveCategory(catSpace, true);
    // save forum in space 1
    Forum forumSpace = createdForum();
    forumSpace.setId("forumSpaceroot_space");
    forumSpace.setForumName("Root spase");
    forumService_.saveForum(catSpace.getId(), forumSpace, true);
    forums = new ArrayList<Forum>();
    forums.add(forumSpace);
    //move to cat1
    forumService_.moveForum(forums, cat1.getPath());
    
    forumSpace = forumService_.getForum(cat1.getId(), forumSpace.getId());
    assertNotNull(forumSpace);
  }
  
  public void testGetForumByFilter() throws Exception {
    initDefaultData();
    
    List<Forum> forums = forumService_.getForums(new ForumFilter(categoryId, true).isPublic(true));
    
    assertEquals(1, forums.size());
  }
  
}
