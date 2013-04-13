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

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.poll.base.BaseTestCase;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

// TODO :
// * Fix tests to not have to specify the order of execution like this
// * The order of tests execution changed in Junit 4.11 (https://github.com/KentBeck/junit/blob/master/doc/ReleaseNotes4.11.md)
@FixMethodOrder(MethodSorters.JVM)
public class PollServiceTestCase extends BaseTestCase {
  
  private List<Poll> tearDownPollList;

  public void setUp() throws Exception {
    super.setUp();
    tearDownPollList = new ArrayList<Poll>();
  }

  public void tearDown() throws Exception {
    for (Poll poll : tearDownPollList) {
      pollService.removePoll(poll.getId());
    }
    super.tearDown();
  }

  public void testPollService() throws Exception {
    assertNotNull(getPollService());
  }

  /**
   * testSavePoll
   * 
   * @throws Exception
   */
  public void testSavePoll() throws Exception {
    // if poll of topic : parentPath = topic.getPath();
    Poll pollTopic = new Poll();
    pollTopic.setParentPath(topicPath);
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
    
    /*    Clear all poll    */
    tearDownPollList.add(pollTopic);
    tearDownPollList.add(pollGroup);
    tearDownPollList.add(pollPulic);
  }

  /**
   * testGetPoll
   * 
   * @throws Exception
   */
  public void testGetPoll() throws Exception {
    
    /*    Create new polls    */
    Poll pollTopic = new Poll();
    pollTopic.setParentPath(topicPath);
    pollService.savePoll(pollTopic, true, false);
    Poll pollGroup = new Poll();
    String parentPathGroup = "platform/users/" + PollNodeTypes.APPLICATION_DATA + "/"
        + PollNodeTypes.EXO_POLLS;
    pollGroup.setParentPath(parentPathGroup);
    pollService.savePoll(pollGroup, true, false);
    Poll pollPulic = new Poll();
    String parentPathPublic = "portal" + PollNodeTypes.POLLS;
    pollPulic.setParentPath(parentPathPublic);
    pollService.savePoll(pollPulic, true, false);
    
    /*    test getPoll    */
    Poll pTopic = pollService.getPoll(pollTopic.getId());
    assertNotNull(pTopic);
    assertEquals(pTopic.getId(),pollTopic.getId());
    
    /*    Clear all poll    */
    tearDownPollList.add(pollTopic);
    tearDownPollList.add(pollGroup);
    tearDownPollList.add(pollPulic);
  }

  /**
   * testGetPagePoll
   * 
   * @throws Exception
   */
  public void testGetPagePoll() throws Exception {
    /*    Create new polls    */
    Poll pollTopic = new Poll();
    pollTopic.setParentPath(topicPath);
    pollService.savePoll(pollTopic, true, false);
    Poll pollGroup = new Poll();
    String parentPathGroup = "platform/users/" + PollNodeTypes.APPLICATION_DATA + "/"
        + PollNodeTypes.EXO_POLLS;
    pollGroup.setParentPath(parentPathGroup);
    pollService.savePoll(pollGroup, true, false);
    Poll pollPulic = new Poll();
    String parentPathPublic = "portal" + PollNodeTypes.POLLS;
    pollPulic.setParentPath(parentPathPublic);
    pollService.savePoll(pollPulic, true, false);
    
    /*    test getPagePoll    */
    List<Poll> listPoll = pollService.getPagePoll();
    assertEquals(listPoll.size(), 3);
    
    /*    Clear all poll    */
    tearDownPollList.add(pollTopic);
    tearDownPollList.add(pollGroup);
    tearDownPollList.add(pollPulic);
  }

  /**
   * testRemovePoll
   * 
   * @throws Exception
   */
  public void testRemovePoll() throws Exception {
    /*    Create new polls    */
    Poll pollTopic = new Poll();
    pollTopic.setParentPath(topicPath);
    pollService.savePoll(pollTopic, true, false);
    Poll pollGroup = new Poll();
    String parentPathGroup = "platform/users/" + PollNodeTypes.APPLICATION_DATA + "/"
        + PollNodeTypes.EXO_POLLS;
    pollGroup.setParentPath(parentPathGroup);
    pollService.savePoll(pollGroup, true, false);
    Poll pollPulic = new Poll();
    String parentPathPublic = "portal" + PollNodeTypes.POLLS;
    pollPulic.setParentPath(parentPathPublic);
    pollService.savePoll(pollPulic, true, false);
    
    /*    test removePoll   */
    pollService.removePoll(pollGroup.getId());
    assertEquals(2, pollService.getPagePoll().size());
    assertNull(pollService.getPoll(pollGroup.getId()));
    
    /*    Clear all poll    */
    tearDownPollList.add(pollTopic);
    tearDownPollList.add(pollPulic);
  }

}
