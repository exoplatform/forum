/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.forum.common.persister;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class PersisterTaskTest extends TestCase {
  

  public void testPersisterTimerTask() throws Exception {
    MockPersister persister = new MockPersister(1000, TimeUnit.MILLISECONDS, 5);
    
    assertEquals(0, persister.getCommitedCount());
    for (int i = 0; i < 4; ++i) {
      persister.addTask(new MockPersister.Task());
    }
    
    Thread.sleep(1005);
    persister.doneSignal().await();

    assertEquals(0, persister.getCommitedCount());
    persister.clear();
  }

  public void testPersisterSizeTask() throws Exception {
    MockPersister persister = new MockPersister(1000, TimeUnit.MILLISECONDS, 5);
    //Thread.sleep(510);
    //First run
    assertEquals(0, persister.getCommitedCount());
    for (int i = 0; i < 6; ++i) {
      persister.addTask(new MockPersister.Task());
    }
    // Run by fix size
    assertEquals(1, persister.getCommitedCount());
    //
    Thread.sleep(1005);
    persister.doneSignal().await();
    //
    assertEquals(0, persister.getCommitedCount());
    persister.clear();
  }

}
