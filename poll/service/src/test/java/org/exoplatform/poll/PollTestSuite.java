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
package org.exoplatform.poll;

import org.exoplatform.commons.testing.BaseExoContainerTestSuite;
import org.exoplatform.commons.testing.ConfigTestCase;
import org.exoplatform.poll.base.BaseTestCase;
import org.exoplatform.poll.service.PollServiceTestCase;
import org.exoplatform.poll.webservice.PollWebserviceTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Nov 15, 2012  
 */
@RunWith(Suite.class)
@SuiteClasses({ 
  PollServiceTestCase.class,
  PollWebserviceTest.class
})
@ConfigTestCase(BaseTestCase.class)
public class PollTestSuite extends BaseExoContainerTestSuite {

  @BeforeClass
  public static void setUp() throws Exception {
    initConfiguration(PollTestSuite.class);
    beforeSetup();
  }

  @AfterClass
  public static void tearDown() {
    afterTearDown();
  }
}