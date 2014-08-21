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

import org.exoplatform.commons.testing.BaseExoContainerTestSuite;
import org.exoplatform.commons.testing.ConfigTestCase;
import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.search.DiscussionSearchConnectorTestCase;
import org.exoplatform.forum.service.cache.TestCacheDataStrorage;
import org.exoplatform.forum.service.impl.JCRDataStorageTestCase;
import org.exoplatform.forum.service.impl.TestForumEventListener;
import org.exoplatform.forum.service.upgrade.ForumServiceUpdaterTestCase;
import org.exoplatform.forum.service.ws.test.ForumWebserviceTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
  ForumServiceTestCase.class, 
  UserProfileTestCase.class,
  CategoryForumTestCase.class,
  TopicTestCase.class,
  PostTestCase.class,
  SearchTestCase.class,
  JCRDataStorageTestCase.class,
  TestCacheDataStrorage.class,
  ForumServiceUpdaterTestCase.class,
  ForumWebserviceTestCase.class,
  ForumListenerTestCase.class,
  TestForumEventListener.class,
  DiscussionSearchConnectorTestCase.class
})
@ConfigTestCase(BaseForumServiceTestCase.class)
public class BaseForumTestSuite extends BaseExoContainerTestSuite {

  @BeforeClass
  public static void setUp() throws Exception {
    initConfiguration(BaseForumTestSuite.class);
    beforeSetup();
  }

  @AfterClass
  public static void tearDown() {
    afterTearDown();
  }
}
