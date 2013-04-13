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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.jcr.SessionManager;

public class TopicTestCase extends BaseForumServiceTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();

  }
  
  @Override
  public void tearDown() throws Exception {
    //
    super.tearDown();
  }
  public void testTopic() throws Exception {
    Category cat = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cat, true);
    Forum forum = createdForum();
    forumService_.saveForum(cat.getId(), forum, true);
    forum = forumService_.getForum(cat.getId(), forum.getId());
    List<String> listTopicId = new ArrayList<String>();
    // add 10 Topics
    List<Topic> list = new ArrayList<Topic>();
    Topic topic;
    for (int i = 0; i < 9; i++) {
      topic = createdTopic(USER_ROOT);
      topic.setCanView(new String[]{USER_ROOT, "*:/foo/zed"});
      list.add(topic);
      listTopicId.add(topic.getId());
      forumService_.saveTopic(cat.getId(), forum.getId(), topic, true, false, new MessageBuilder());
    }
    topic = createdTopic(USER_JOHN);
    listTopicId.add(topic.getId());
    forumService_.saveTopic(cat.getId(), forum.getId(), topic, true, false, new MessageBuilder());
    assertEquals(10, forumService_.getForum(cat.getId(), forum.getId()).getTopicCount());

    // get Topic - topic in position 8
    topic = list.get(8);
    Topic topica = forumService_.getTopic(cat.getId(), forum.getId(), topic.getId(), "");
    assertNotNull(topica);

    // get Topic by path
    topica = forumService_.getTopicByPath(cat.getId() + "/" + forum.getId() + "/" + topic.getId(), false);
    assertNotNull(topica);

    // update Topic
    topica.setIsSticky(true);
    topica.setTopicName("topic 8");
    forumService_.saveTopic(cat.getId(), forum.getId(), topica, false, false, new MessageBuilder());
    assertEquals("This topic name not is 'topic 8'", "topic 8", forumService_.getTopic(cat.getId(), forum.getId(), topic.getId(), "").getTopicName());

    // modifyTopic
    topica.setIsLock(true);
    list.clear();
    list.add(topica);
    forumService_.modifyTopic(list, 2);
    topica = forumService_.getTopic(cat.getId(), forum.getId(), topic.getId(), "");
    assertEquals("This topic is open.", topica.getIsLock(), true);
    // get PageList Topic
    JCRPageList pagelist = forumService_.getPageTopic(cat.getId(), forum.getId(), "", "");
    assertEquals("Available all topics not equals 10.", pagelist.getAvailable(), 10);
    pagelist.setPageSize(5);
    List<Topic> listTopic = pagelist.getPage(1);
    assertEquals("Available page not equals 5", listTopic.size(), 5);
    assertEquals(pagelist.getAvailablePage(), 2);

    // get Page topic has check permission.
    setMembershipEntry("/foo/zed", "member", true);
    loginUser(USER_DEMO);
    
    StringBuilder query = new StringBuilder();
    String strQuery = query.append("(").append(Utils.buildXpathHasProperty(Utils.EXO_CAN_VIEW)).append(" or ").toString();
    String buildQueryViewer = Utils.buildXpathByUserInfo(Utils.EXO_CAN_VIEW, UserHelper.getAllGroupAndMembershipOfUser(null));
    buildQueryViewer += " or @" + Utils.EXO_OWNER + "='" + USER_DEMO + "')";
    pagelist = forumService_.getPageTopic(cat.getId(), forum.getId(), strQuery + buildQueryViewer, "");
    assertEquals(10, pagelist.getAvailable());
    
    setMembershipEntry("/foo/bar", "member", true);
    loginUser(USER_JOHN);
 
    buildQueryViewer = Utils.buildXpathByUserInfo(Utils.EXO_CAN_VIEW, UserHelper.getAllGroupAndMembershipOfUser(null));
    buildQueryViewer += " or @" + Utils.EXO_OWNER + "='" + USER_JOHN + "')";
    pagelist = forumService_.getPageTopic(cat.getId(), forum.getId(), strQuery + buildQueryViewer, "");
    assertEquals(1, pagelist.getAvailable());

    // get Topic By User
    topic = createdTopic(USER_DEMO);
    forumService_.saveTopic(cat.getId(), forum.getId(), topic, true, false, new MessageBuilder());
    // We have 11 topic: 9 by root, 1 by john and 1 by demo 
    pagelist = forumService_.getPageTopicByUser(USER_ROOT, true, "");
    assertEquals(9, pagelist.getAvailable());

    // set 5 topics have last post is 2 days.
    updateLastPostDateOfTopic(listTopic, 2);
    // get topics by last post days. Example with 1 day.
    listTopic = forumService_.getAllTopicsOld(1, forum.getPath());
    assertEquals("Failed to run auto prune. List topic has size not equals 5.", listTopic.size(), 5);
    
    // run autoPrune
    PruneSetting pSetting = forumService_.getPruneSetting(forum.getPath());
    // active the pruning this forum.
    pSetting.setActive(true);
    // prune for topics have last post more than 1 day.
    pSetting.setInActiveDay(1);
    forumService_.runPrune(pSetting);
    // check topics pruned.
    // After pruned, the topics is active is 11 - 5 = 6.
    StringBuilder queryBuilder = new StringBuilder();
    // @exo:isActive = 'true'
    queryBuilder.append("@").append(ForumNodeTypes.EXO_IS_ACTIVE).append("='true'");
    int size = forumService_.getTopicList(cat.getId(), forum.getId(), queryBuilder.toString(), "", 20).getAll().size();
    assertEquals("Failed to run autoPrun topics, the size of topics active not equals 6.", size, 6);
    
    // move Topic
    // move topic from forum to forum 1
    Forum forum1 = createdForum();
    forumService_.saveForum(cat.getId(), forum1, true);
    forum1 = forumService_.getForum(cat.getId(), forum1.getId());
    List<Topic> topics = new ArrayList<Topic>();
    topics.add(topica);
    forumService_.moveTopic(topics, forum1.getPath(), "", "");
    assertNotNull("Failed to moved topic, topic is null.", forumService_.getTopic(cat.getId(), forum1.getId(), topica.getId(), ""));
    assertEquals(10, forumService_.getForum(cat.getId(), forum.getId()).getTopicCount());
    assertEquals(1, forumService_.getForum(cat.getId(), forum1.getId()).getTopicCount());

    // test remove Topic return Topic
    // remove id topic moved in list topicIds.
    if (listTopicId.contains(topica.getId()))
      listTopicId.remove(topica.getId());
    for (String topicId : listTopicId) {
      forumService_.removeTopic(cat.getId(), forum.getId(), topicId);
    }
    List<Topic> topics2 = forumService_.getTopics(cat.getId(), forum.getId());
    assertEquals("Topics in forum failed to remove. List topic has size more than 1.", topics2.size(), 1);
    assertEquals(1, forumService_.getForum(cat.getId(), forum.getId()).getTopicCount());
  }

  private void updateLastPostDateOfTopic(List<Topic> listTopic, int day) throws Exception {
    SessionManager  manager = dataLocation.getSessionManager();
    try {
      Session session = manager.openSession();
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(cal.getTimeInMillis() - day * 86400000);
      Node topicNode;
      for (Topic topic2 : listTopic) {
        topicNode = (Node) session.getItem(topic2.getPath());
        topicNode.setProperty(ForumNodeTypes.EXO_LAST_POST_DATE, cal);
        topicNode.save();
      }
    } finally {
      manager.closeSession();
    }
  }

}
