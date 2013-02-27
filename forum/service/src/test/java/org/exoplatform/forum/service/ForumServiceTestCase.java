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

/**
 * Created by The eXo Platform SAS Author : Vu Duy Tu tuvd@exoplatform.com Oct
 * 5, 2012
 */
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

  public void testForumStatic() throws Exception {
    ForumStatistic forumStatistic = new ForumStatistic();
    forumService_.saveForumStatistic(forumStatistic);
    assertNotNull(forumService_.getForumStatistic());
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

  public void testTopicType() throws Exception {
    // set Data
    initDefaultData();
    TopicType topicType = createTopicType("Musics");
    forumService_.saveTopicType(topicType);
    forumService_.saveTopicType(createTopicType("Dance"));
    forumService_.saveTopicType(createTopicType("Sing"));
    topicType = forumService_.getTopicType(topicType.getId());
    assertNotSame("Can not save and get Topic type.", topicType.getId(), TopicType.DEFAULT_ID);
    // Check get All
    List<TopicType> listTopicType = forumService_.getTopicTypes();
    assertEquals("Can not get all topic type. Size of topicTypes list is not 3.", listTopicType.size(), 3);
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

  public void testTag() throws Exception {
    // set Data
    initDefaultData();
    Tag tag = createTag("Tag1");
    Tag tag2 = createTag("Tag2");
    Tag tag3 = createTag("Tag3");

    // add tag
    List<Tag> tags = new ArrayList<Tag>();
    tags.add(tag);
    tags.add(tag2);
    tags.add(tag3);
    Topic topic = forumService_.getTopic(categoryId, forumId, topicId, "");
    forumService_.addTag(tags, USER_ROOT, topic.getPath());
    // get Tags name in topic by root.
    // List<String> list =
    // forumService_.getTagNameInTopic(USER_ROOT+","+topicId);

    // Test get tag
    String id = Utils.TAG + tag.getName();
    tag = forumService_.getTag(id);
    assertNotNull(tag);

    // Get all tag
    // assertEquals("All tags size is not 3", 3,
    // forumService_.getAllTags().size());

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
    assertEquals(ArrayToString(groupUser), ArrayToString(forumService_.getTopic(category.getId(), forum.getId(), topic.getId(), null).getCanView()));
    // deleted group in system
    forumService_.calculateDeletedGroup(groupId, groupName);
    // checking again data
//    assertEquals(UserProfile.USER, forumService_.getUserInfo(USER_DEMO).getUserRole());
    assertEquals(USER_ROOT, ArrayToString(forumService_.getCategory(category.getId()).getUserPrivate()));
    assertEquals(USER_ROOT, ArrayToString(forumService_.getForum(category.getId(), forum.getId()).getModerators()));
    assertEquals(USER_ROOT, ArrayToString(forumService_.getTopic(category.getId(), forum.getId(), topic.getId(), null).getCanView()));
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
}
