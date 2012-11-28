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
package org.exoplatform.poll.service;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.common.jcr.SessionManager;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumNodeTypes;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.poll.base.BaseTestCase;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Created by The eXo Platform SAS Author : Vu Duy Tu tuvd@exoplatform.com Oct
 * 8, 2012
 */
// TODO :
// * Fix tests to not have to specify the order of execution like this
// * The order of tests execution changed in Junit 4.11 (https://github.com/KentBeck/junit/blob/master/doc/ReleaseNotes4.11.md)
@FixMethodOrder(MethodSorters.JVM)
public class PollServiceTestCase extends BaseTestCase {

  protected PollService    pollService;

  protected KSDataLocation dataLocation;

  private Node             topicNode;

  @Override
  public void setUp() throws Exception {
    pollService = (PollService) getContainer().getComponentInstanceOfType(PollService.class);
    dataLocation = (KSDataLocation) getContainer().getComponentInstanceOfType(KSDataLocation.class);
    initForumdata();
    begin();
  }

  @Override
  public void tearDown() throws Exception {
    end();
  }

  public void testPollService() throws Exception {
    assertNotNull(pollService);
  }

  /**
   * create new node forum, node topic in this forum then use this topic's path
   * to create a poll later
   */
  private void initForumdata() {
    SessionManager manager = dataLocation.getSessionManager();
    try {
      // startSessionAs("root");
      Session session = manager.openSession();
      Category cat = new Category();
      Node nodeHome = session.getRootNode().getNode(dataLocation.getForumCategoriesLocation());
      Node catN = nodeHome.addNode(cat.getId(), ForumNodeTypes.EXO_FORUM_CATEGORY);
      Forum forum = new Forum();
      Node forNode = catN.addNode(forum.getId(), ForumNodeTypes.EXO_FORUM);
      Topic topic = new Topic();
      topicNode = forNode.addNode(topic.getId(), ForumNodeTypes.EXO_TOPIC);
      session.save();
      String topicPath = topicNode.getPath();
      assertNotNull(session.getItem(topicPath));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      manager.closeSession();
    }
  }

  /**
   * testSavePoll
   * 
   * @throws Exception
   */
  public void testSavePoll() throws Exception {
    // if poll of topic : parentPath = topic.getPath();
    Poll pollTopic = new Poll();
    String parentPathTopic = topicNode.getPath();
    pollTopic.setParentPath(parentPathTopic);
    pollService.savePoll(pollTopic, true, false);
    assertNotNull(pollService.getPoll(pollTopic.getId()));

    // if poll of Group : parentPath =
    // $GROUP/${PollNodeTypes.APPLICATION_DATA}/${PollNodeTypes.EXO_POLLS}
    Poll pollGroup = new Poll();
    String parentPathGroup = "platform/users/" + PollNodeTypes.APPLICATION_DATA + "/"
        + PollNodeTypes.EXO_POLLS;
    pollGroup.setParentPath(parentPathGroup);
    pollService.savePoll(pollGroup, true, false);
    assertNotNull(pollService.getPoll(pollGroup.getId()));

    // if poll of public: parentPath = $PORTAL/${PollNodeTypes.POLLS}
    Poll pollPulic = new Poll();
    String parentPathPublic = "portal" + PollNodeTypes.POLLS;
    pollPulic.setParentPath(parentPathPublic);
    pollService.savePoll(pollPulic, true, false);
    assertNotNull(pollService.getPoll(pollPulic.getId()));
  }

  /**
   * testGetPoll
   * 
   * @throws Exception
   */
  public void testGetPoll() throws Exception {
    String pollId = pollService.getPagePoll().get(1).getId();
    String path = pollService.getPagePoll().get(1).getParentPath();
    Poll p = pollService.getPoll(pollId);
    assertNotNull(p);
    assertEquals(path, p.getParentPath());
  }

  /**
   * testGetPagePoll
   * 
   * @throws Exception
   */
  public void testGetPagePoll() throws Exception {
    List<Poll> listPoll = pollService.getPagePoll();
    assertEquals(listPoll.size(), 3);
  }

  /**
   * testRemovePoll
   * 
   * @throws Exception
   */
  public void testRemovePoll() throws Exception {
    String pollId = pollService.getPagePoll().get(1).getId();
    pollService.removePoll(pollId);
    assertEquals(2, pollService.getPagePoll().size());
  }

}
