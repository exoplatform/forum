/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.forum.service.task;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.InitParamsValue;
import org.exoplatform.forum.common.persister.Persister;
import org.exoplatform.forum.common.persister.PersisterTask;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class AbstractForumTaskManager<T extends AbstractForumTask> implements Persister {
  private static final Log LOG = ExoLogger.getLogger(AbstractForumTaskManager.class);

  /** The key to get value of time interval **/
  private static final String   INTERVAL_TASK_PERSIST_THRESHOLD_KEY = "intervalPersist";
  /** The key to get value of persister threshold **/
  private static final String   PERSISTER_THRESHOLD_KEY = "persisterThreshold";
  /** The PersisterTask **/
  private PersisterTask persisterTask;
  /** The default time milliseconds that interval to each commit tasks. **/
  private long INTERVAL_TASK_PERSIST_THRESHOLD = 5000; 
  /** The default value of persister threshold **/
  private int PERSISTER_THRESHOLD = 20;
  /** The Queue tasks **/
  private Queue<T> tasks = null;
  /** Defines the signal done when task committed*/ 
  private CountDownLatch doneSignal;
  
  public AbstractForumTaskManager(InitParams params) {
    long interval_task = InitParamsValue.getLong(params, INTERVAL_TASK_PERSIST_THRESHOLD_KEY, INTERVAL_TASK_PERSIST_THRESHOLD);
    int persisterThreshold = InitParamsValue.getInteger(params, PERSISTER_THRESHOLD_KEY, PERSISTER_THRESHOLD);

    persisterTask = PersisterTask.init()
        .persister(this)
        .wakeup(interval_task)
        .timeUnit(TimeUnit.MILLISECONDS)
        .maxFixedSize(persisterThreshold)
        .build();
    persisterTask.start();
  }

  @Override
  public void commit(boolean forceCommit) {
    persist(forceCommit);
  }

  /**
   * The method use to commit tasks
   * @param forceCommit The status to commit task is force of not.
   */
  private void persist(boolean forceCommit) {
    //
    if (tasks != null && (persisterTask.shoudldPersist(tasks.size()) || forceCommit)) {
      //
      Queue<T> processTasks = popTasks();
      try {
        RequestLifeCycle.begin(PortalContainer.getInstance());
        T task;
        doneSignal = new CountDownLatch(processTasks.size());
        while ((task = processTasks.poll()) != null) {
          //
          try {
            task.run();
            doneSignal.countDown();
          } catch (Exception e) {
            LOG.warn(String.format("The task %s running unsuccessful.", task.getClass().getName()));
            LOG.debug(e.getMessage(), e);
          }
        }
      } finally {
        processTasks = null;
        RequestLifeCycle.end();
      }
    }
  }
  
  /**
   * Gets the tasks to commit, it will ignore same task added.
   * @return
   */
  private Queue<T> popTasks() {
    Queue<T> tmp = tasks;
    tasks = null;
    Queue<T> processTasks = new LinkedBlockingQueue<T>();
    for (T forumTask : tmp) {
      if (!processTasks.contains(forumTask)) {
        processTasks.add(forumTask);
      }
    }
    //
    return processTasks;
  }

  /**
   * Add the task
   * 
   * @param task Task add to PersisterTask 
   */
  public void addTask(T task) {
    if (tasks == null) {
      tasks = new LinkedBlockingQueue<T>();
    }
    tasks.add(task);
    //
    commit(false);
  }
  
  @Override
  public CountDownLatch doneSignal() {
    return this.doneSignal != null ? this.doneSignal : new CountDownLatch(0);
  }
  
  @Override
  public void clear() {
    if (this.tasks != null) {
      this.tasks.clear();
    }
    
  }
}
