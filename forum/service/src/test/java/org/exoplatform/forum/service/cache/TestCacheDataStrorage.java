/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.forum.service.cache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumNodeTypes;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.PruneSetting;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.Watch;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.forum.service.impl.model.TopicFilter;
import org.exoplatform.forum.service.impl.model.TopicListAccess;

public class TestCacheDataStrorage extends BaseForumServiceTestCase {

  private CachedDataStorage cacheDataStorage = null;

  public TestCacheDataStrorage() throws Exception {
  }

  public void setUp() throws Exception {
    super.setUp();
    if (cacheDataStorage == null) {
      cacheDataStorage = (CachedDataStorage)getService(DataStorage.class);
    }
  }
  
  public void testPostListAccess() throws Exception {
    // set Data
    initDefaultData();

    //create 26 posts
    List<Post> posts = new ArrayList<Post>();
    for (int i = 0; i < 25; ++i) {
      Post post = createdPost();
      posts.add(post);
      cacheDataStorage.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    // getPost
    assertNotNull(cacheDataStorage.getPost(categoryId, forumId, topicId, posts.get(0).getId()));
    assertEquals(25, cacheDataStorage.getTopic(categoryId, forumId, topicId, "").getPostCount());

    // get Page 1
    List<Post> gotList = cacheDataStorage.getPosts(new PostFilter(categoryId, forumId, topicId, "", "", "", "root"), 0, 10);   
    assertEquals(10, gotList.size());// size = 10: page 1
    
    
    //Page 2
    gotList = cacheDataStorage.getPosts(new PostFilter(categoryId, forumId, topicId, "", "", "", "root"), 10, 10);   
    assertEquals(10, gotList.size());// size = 10: page 2
    
    //Page 3
    gotList = cacheDataStorage.getPosts(new PostFilter(categoryId, forumId, topicId, "", "", "", "root"), 20, 10);   
    assertEquals(6, gotList.size());// size = 6: page 2
  }

  
  public void testPostListCount() throws Exception {
    // set Data
    initDefaultData();

    //create 25 + 1 post default post when created topic
    List<Post> posts = new ArrayList<Post>();
    for (int i = 0; i < 25; ++i) {
      Post post = createdPost();
      posts.add(post);
      cacheDataStorage.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    // getPost
    assertNotNull(cacheDataStorage.getPost(categoryId, forumId, topicId, posts.get(0).getId()));
    
    //isApproved = true
    assertEquals(26, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "true", "false", "false", "root")));
    
    //isApproved = false
    assertEquals(0, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "false", "false", "false", "root")));
    
    //isHidden = true
    assertEquals(0, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "false", "true", "false", "root")));
    //isWaiting = true
    assertEquals(0, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "false", "false", "true", "root")));
    
    { //add more posts
      //add more 25 posts
      for (int i = 0; i < 25; ++i) {
        Post post = createdPost();
        posts.add(post);
        cacheDataStorage.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
      }
      
      //isApproved = true
      assertEquals(51, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "true", "false", "false", "root")));
      
      //isApproved = false
      assertEquals(0, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "false", "false", "false", "root")));
      
      //isHidden = true
      assertEquals(0, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "false", "true", "false", "root")));
      //isWaiting = true
      assertEquals(0, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "false", "false", "true", "root")));

    }

    //check whether postsCount is decremented after post removal
    {
      int oldPostListCount = cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "true", "false", "false", "root"));
      cacheDataStorage.removePost(this.categoryId,this.forumId,this.topicId,posts.get(0).getId());
      assertEquals( --oldPostListCount, cacheDataStorage.getPostsCount(new PostFilter(this.categoryId, this.forumId, topicId, "true", "false", "false", "root")));
    }
  }
  
  
  public void testTopicListAccess() throws Exception {
    // initialize default data (1 category, 1 forum and 1 topic)
    initDefaultData();

    Topic topic;
    for (int i = 0; i < 9; i++) {
      topic = createdTopic(USER_ROOT);
      topic.setTopicType("Topic_test_" + i);
      topic.setCanView(new String[] { USER_JOHN, "*:/foo/zed" });
      cacheDataStorage.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    }

    TopicFilter filter = new TopicFilter(categoryId, forumId);
    filter.isAdmin(true);
    TopicListAccess listAccess = (TopicListAccess) forumService_.getTopics(filter);

    assertEquals(10, listAccess.getSize());
    //
    listAccess.initialize(5, 1);

    assertEquals(5, listAccess.getPageSize());
    assertEquals(2, listAccess.getTotalPages());

    List<Topic> gotList = cacheDataStorage.getTopics(filter, 0, 5);
    assertEquals(5, gotList.size());

    gotList = cacheDataStorage.getTopics(filter, 5, 10);
    assertEquals(5, gotList.size());

    Topic update = gotList.get(0);

    Topic sum = cacheDataStorage.getTopicSummary(update.getPath(), false);
    assertNotNull(sum);

    update.setTopicName("New topic name");
    cacheDataStorage.saveTopic(categoryId, forumId, update, false, false, new MessageBuilder());
    // check update
    Topic got_ = cacheDataStorage.getTopic(categoryId, forumId, update.getId(), USER_ROOT);
    assertEquals(update.getTopicName(), got_.getTopicName());

    got_ = cacheDataStorage.getTopicSummary(update.getPath(), false);
    assertEquals(update.getTopicName(), got_.getTopicName());

    got_ = cacheDataStorage.getTopicByPath(update.getPath(), false);
    assertEquals(update.getTopicName(), got_.getTopicName());

    for (Topic top : listAccess.load(5, 10)) {
      if (top.getId().equals(update.getId())) {
        assertEquals(update.getTopicName(), top.getTopicName());
      }
    }

    gotList = cacheDataStorage.getTopics(filter, 5, 10);
    got_ = gotList.get(0);
    assertEquals(update.getTopicName(), got_.getTopicName());
    
    // test permission
    for (int i = 0; i < 9; i++) {
      topic = createdTopic(USER_JOHN);
      topic.setTopicType("Topic_test_" + USER_JOHN + i);
      topic.setCanView(new String[] { USER_DEMO, "ghost", "*:/foo/zed" });
      cacheDataStorage.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    }
    for (int i = 0; i < 9; i++) {
      topic = createdTopic(USER_ROOT);
      topic.setTopicType("Topic_test_" + USER_ROOT + i);
      cacheDataStorage.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    }
    filter = new TopicFilter(categoryId, forumId);
    filter.isAdmin(false);
    filter.userLogin(USER_DEMO);
    loginUser(USER_DEMO);
    // parent public
    assertEquals(19, cacheDataStorage.getTopicsCount(filter));
    // has not permission from parent
    filter.viewers(new String[]{"mary"});
    filter.userLogin("ghost");
    loginUser("ghost");
    assertEquals(9, cacheDataStorage.getTopicsCount(filter));
    // has permission from parent
    filter.userLogin("mary");
    loginUser("mary");
    assertEquals(28, cacheDataStorage.getTopicsCount(filter));
    filter.userLogin("raul");
    loginUser("raul");
    assertEquals(0, cacheDataStorage.getTopicsCount(filter));

  }
  
  public void testTopicHasPoll() throws Exception {
    // set Data
    initDefaultData();
    // get from data storage
    Topic topic = cacheDataStorage.getTopic(categoryId, forumId, topicId, null);
    String oldName = topic.getTopicName();

    // get by path
    Topic topicByPath = cacheDataStorage.getTopicByPath(topic.getPath(), false);
    assertFalse(topicByPath.getIsPoll());
    assertEquals(oldName, topicByPath.getTopicName());

    // get summary
    Topic topicSummary = cacheDataStorage.getTopicSummary(topic.getPath());
    assertFalse(topicSummary.getIsPoll());
    assertEquals(oldName, topicSummary.getTopicName());
    
    
    // save new data for topic and clear topic cached
    String newName = "Topic Rename";
    topic.setTopicName(newName);
    cacheDataStorage.saveTopic(categoryId, forumId, topic, false, false, new MessageBuilder());
    saveHasPoll(topic.getPath(), true);

    //
    topicByPath = cacheDataStorage.getTopicByPath(topic.getPath(), false);
    assertTrue(topicByPath.getIsPoll());
    assertEquals(newName, topicByPath.getTopicName());

    //
    topicSummary = cacheDataStorage.getTopicSummary(topic.getPath());
    assertTrue(topicSummary.getIsPoll());

    // Remove has poll
    saveHasPoll(topic.getPath(), false);
    assertFalse(cacheDataStorage.getTopicByPath(topic.getPath(), false).getIsPoll());
    
    cacheDataStorage.removeTopic(categoryId, forumId, topic.getId());
  }
  
  public void testClearWatchedCategory() throws Exception {
    // set Data
    initDefaultData();
    List<String> values = new ArrayList<String>();
    values.add("huongnt@exoplatform.com");

    // Add watch for category with pullPath
    Category category = cacheDataStorage.getCategory(categoryId);
    cacheDataStorage.addWatch(1, category.getPath(), values, "root");
    // Check watch's list for category
    List<Watch> watchedList = cacheDataStorage.getWatchByUser("root");
    assertEquals(watchedList.get(0).getEmail(), values.get(0));

    // Delete email in watch's list
    cacheDataStorage.removeWatch(1, category.getPath(), "/" + values.get(0));
    // Check watch's list after deleting
    watchedList = cacheDataStorage.getWatchByUser("root");
    assertEquals(watchedList.size(), 0);

    // Add watch for forum with forumPath
    Forum forum = cacheDataStorage.getForum(categoryId, forumId);
    cacheDataStorage.addWatch(1, forum.getPath(), values, "root");
    // Check watch's list for forum
    watchedList = cacheDataStorage.getWatchByUser("root");
    assertEquals(watchedList.get(0).getEmail(), values.get(0));

    // Delete email in watch's list
    cacheDataStorage.removeWatch(1, forum.getPath(), "/" + values.get(0));
    // Check watch's list after deleting
    watchedList = cacheDataStorage.getWatchByUser("root");
    assertEquals(watchedList.size(), 0);

    // Add watch for topic with fullPath
    Topic topic = cacheDataStorage.getTopic(categoryId, forumId, topicId, null);
    cacheDataStorage.addWatch(1, topic.getPath(), values, "root");
    // Check watch's list for topic
    watchedList = cacheDataStorage.getWatchByUser("root");
    assertEquals(watchedList.get(0).getEmail(), values.get(0));

    // Delete email in watch's list
    cacheDataStorage.removeWatch(1, topic.getPath(), "/" + values.get(0));
    // Check watch's list after deleting
    watchedList = cacheDataStorage.getWatchByUser("root");
    assertEquals(watchedList.size(), 0);
  }

  public void testClearWatchedCategoryWithSubPath() throws Exception {
    // set Data
    initDefaultData();
    List<String> values = new ArrayList<String>();
    values.add("john@exoplatform.com");

    // Add watch for category with subpath
    cacheDataStorage.addWatch(1, categoryId, values, "john");
    List<Watch> watchedList = cacheDataStorage.getWatchByUser("john");
    // Check category watch's list
    assertEquals(watchedList.size(), 1);

    cacheDataStorage.removeWatch(1, categoryId, "/" + values.get(0));
    watchedList = cacheDataStorage.getWatchByUser("john");
    // Check category watch's list after deleting
    assertEquals(watchedList.size(), 0);

    // Add watch for forum with subpath
    cacheDataStorage.addWatch(1, categoryId + "/" + forumId, values, "john");
    watchedList = cacheDataStorage.getWatchByUser("john");
    // Check forum watch's list
    assertEquals(watchedList.size(), 1);

    cacheDataStorage.removeWatch(1, categoryId + "/" + forumId, "/" + values.get(0));
    watchedList = cacheDataStorage.getWatchByUser("john");
    // Check forum watch's list after deleting
    assertEquals(watchedList.size(), 0);

    // Add watch for topic with subpath
    cacheDataStorage.addWatch(1, categoryId + "/" + forumId + "/" + topicId, values, "john");
    watchedList = cacheDataStorage.getWatchByUser("john");
    // Check topic watch's list
    assertEquals(watchedList.size(), 1);

    cacheDataStorage.removeWatch(1, categoryId + "/" + forumId + "/" + topicId, "/" + values.get(0));
    // Check topic watch's list after deleting
    watchedList = cacheDataStorage.getWatchByUser("john");
    assertEquals(watchedList.size(), 0);

    //
    cacheDataStorage.removeCategory(categoryId);
  }
  
  public void testRunPruneForum() throws Exception {
    // default data
    initDefaultData();
    loginUser(USER_DEMO);
    //
    Topic topic = createdTopic(USER_DEMO);
    forumService_.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    Post post = createdPost();
    forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    //
    Calendar lastPost = Calendar.getInstance();
    long twoDays = 48*60*60*1000;
    lastPost.setTimeInMillis(lastPost.getTimeInMillis() - twoDays);
    Session session = getSession();
    Node topicNode = (Node) session.getItem(topic.getPath());
    topicNode.setProperty(Utils.EXO_LAST_POST_DATE, lastPost);
    session.save();
    session.logout();
    //
    PruneSetting pruneSetting = new PruneSetting();
    pruneSetting.setId(Utils.PRUNESETTING);
    pruneSetting.setActive(true);
    pruneSetting.setForumPath(cacheDataStorage.getForum(categoryId, forumId).getPath());
    pruneSetting.setInActiveDay(1);
    // check Prune
    assertEquals(1, cacheDataStorage.checkPrune(pruneSetting));
    // check before prune
    TopicFilter filter = new TopicFilter(categoryId, forumId);
    filter.isAdmin(false).isApproved(true);
    long count = cacheDataStorage.getTopicsCount(filter);
    assertEquals(2, count);
    
    topic = cacheDataStorage.getTopic(categoryId, forumId, topic.getId(), null);
    assertTrue(topic.getIsActive());
    // run prune
    cacheDataStorage.runPrune(pruneSetting);
    // check before run pruned
    count = cacheDataStorage.getTopicsCount(filter);
    assertEquals(1, count);
    topic = cacheDataStorage.getTopic(categoryId, forumId, topic.getId(), null);
    assertFalse(topic.getIsActive());
  }
  
  private Session getSession() throws Exception {
    KSDataLocation dataLocation = getService(KSDataLocation.class);
    return dataLocation.getSessionManager().createSession();
  }
  
  private void saveHasPoll(String topicPath, boolean status) throws Exception {
    Session session = getSession();
    Node node = (Node) session.getItem(topicPath);
    node.setProperty(ForumNodeTypes.EXO_IS_POLL, status);
    session.save();
    session.logout();
  }
}