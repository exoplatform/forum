/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.forum.utils;

import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import junit.framework.TestCase;

import org.exoplatform.commons.testing.AssertUtils;
import org.exoplatform.forum.service.UserProfile;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class UserProfileUtilsTestCase extends TestCase {
  private ExecutorService executor;
  protected void setUp() throws Exception {
    super.setUp();
    ThreadFactory threadFactory = new ThreadFactory() {
      public Thread newThread(Runnable arg0) {
        return new Thread(arg0, "UserProfile thread");
      }
    };

    executor = Executors.newFixedThreadPool(20, threadFactory);
  }

  public void testSetLastReadPostOfTopic() {

    String[] array = new String[] { "foo,foo", "bar,bar" };
    UserProfile profile = new UserProfile();
    profile.setLastReadPostOfTopic(array);
    String[] actual = profile.getLastReadPostOfTopic();
    AssertUtils.assertContains(actual, "foo,foo", "bar,bar");
    assertEquals("foo", profile.getLastPostIdReadOfTopic("foo"));
    assertEquals("", profile.getLastPostIdReadOfTopic("zed"));

  }

  public void testSetLastReadPostOfForum() {

    String[] array = new String[] { "foo,foo", "bar,bar" };
    UserProfile profile = new UserProfile();
    profile.setLastReadPostOfForum(array);
    String[] actual = profile.getLastReadPostOfForum();
    AssertUtils.assertContains(actual, "foo,foo", "bar,bar");
    assertEquals("foo", profile.getLastPostIdReadOfForum("foo"));
    assertEquals("", profile.getLastPostIdReadOfForum("zed"));

  }

  public void testGetLastTimeAccessForum() throws Exception {
    UserProfile profile = new UserProfile();
    long actual = profile.getLastTimeAccessForum("foo");
    assertEquals("Last access time of unknown forum should be zero", 0, actual);

    profile.setLastTimeAccessForum("bar", 1);
    actual = profile.getLastTimeAccessForum("bar");
    assertEquals(1, actual);
  }

  public void testGetLastTimeAccessTopic() throws Exception {
    UserProfile profile = new UserProfile();
    long actual = profile.getLastTimeAccessTopic("foo");
    assertEquals("Last access time of unknown topic should be zero", 0, actual);

    profile.setLastTimeAccessTopic("bar", 1);
    actual = profile.getLastTimeAccessTopic("bar");
    assertEquals(1, actual);
  }

  public void testGetScreenname() {

    // not set defaults to default user id
    UserProfile profile = new UserProfile();
    String actual = profile.getScreenName();
    assertEquals(UserProfile.USER_GUEST, actual);

    // null or empty defaults to user id
    profile.setUserId("foo");
    profile.setScreenName(null);
    actual = profile.getScreenName();
    assertEquals("foo", actual);
    profile.setScreenName("");
    actual = profile.getScreenName();
    assertEquals("foo", actual);

    // if set, don't use user id
    profile.setUserId("foo");
    profile.setScreenName("bar");
    actual = profile.getScreenName();
    assertEquals("bar", actual);

  }
  
  public void testConcurrentAddLastReadPost() throws Exception {
    final UserProfile userProfile = new UserProfile();
    final String s = "qwertyuiopasdfghjklzxcvbnm";
    for (int i = 0; i < 500; i++) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            String r = s.substring(0, new Random().nextInt(s.length() - 1));
            // add
            userProfile.addLastPostIdReadOfForum(r + new Random().nextLong(), r + new Random().nextInt());
            userProfile.addLastPostIdReadOfTopic(r + new Random().nextLong(), r + new Random().nextInt());
            // get
            userProfile.getLastReadPostOfForum();
            userProfile.getLastReadPostOfTopic();
            assertTrue(true);
          } catch (ConcurrentModificationException e) {
            assertFalse(true);
          }
        }
      });
    }
    //
    Thread.sleep(1000);
  }

}
