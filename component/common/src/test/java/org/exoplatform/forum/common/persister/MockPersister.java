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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MockPersister implements Persister {

  private int count = 0;
  private List<Task> tasks = new ArrayList<Task>();
  private PersisterTask timerTask;
  private CountDownLatch doneSignal; 
  
  public MockPersister(long wakeup, TimeUnit util, int maxFixedSize) {
    this.timerTask = PersisterTask.init().persister(this).wakeup(wakeup)
                                   .timeUnit(util).maxFixedSize(maxFixedSize).build();
    this.timerTask.start();
  }
  
  @Override
  public void commit(boolean forceCommit) {
    if (forceCommit || timerTask.shoudldPersist(tasks.size())) {
      doneSignal = new CountDownLatch(tasks.size());
      for(Task t : tasks) {
        t.run();
        count--;
        doneSignal.countDown();
      }
      tasks.clear();
    }
  }
  
  public void addTask(Task task) {
    tasks.add(task);
    ++count;
    //
    commit(false);
  }
  
  public int getCommitedCount() {
    return count;
  }
  
  @Override
  public void clear() {
    this.tasks.clear();
  }
  
  public void resetCount() {
    count = 0;
  }
  
  
  public static class Task {
    public int run() {
      return 1;
    }
  }


  @Override
  public CountDownLatch doneSignal() {
    return this.doneSignal != null ? this.doneSignal : new CountDownLatch(0);
  }
  
}
