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
package org.exoplatform.forum.ext.activity;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.InitParamsValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.idm.PicketLinkIDMServiceImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.hibernate.TransactionException;
import org.picocontainer.Startable;

public class ForumTaskManager implements Startable {
  private static final Log    LOG                = ExoLogger.getExoLogger(ForumTaskManager.class);
  private static final String PERIOD_TIME_KEY  = "periodTime";
  private static final String MAX_PERSIST_SIZE = "maxPersistSize";
  private static final String PRIORITY_KEY = "thread-priority";
  private static final String ASYNC_EXECUTION_KEY = "async-execution";

  private ScheduledExecutorService scheduler ;
  private Queue<Task<ForumActivityContext>> tasks = null;
  private static long   INTERVAL         = 5000l;
  private static int    MAX_SIZE_PERSIST = 25;
  private static int THREAD_PRIORITY = 1;
  private boolean isDone = true;
  private boolean forceStop = false;
  private boolean isAsync = true;

  public ForumTaskManager(InitParams params) {
    INTERVAL = InitParamsValue.getLong(params, PERIOD_TIME_KEY, INTERVAL);
    MAX_SIZE_PERSIST = InitParamsValue.getInteger(params, MAX_PERSIST_SIZE, MAX_SIZE_PERSIST);
    THREAD_PRIORITY = InitParamsValue.getInteger(params, PRIORITY_KEY, THREAD_PRIORITY);
    THREAD_PRIORITY = InitParamsValue.getInteger(params, PRIORITY_KEY, THREAD_PRIORITY);
    isAsync = InitParamsValue.getBoolean(params, ASYNC_EXECUTION_KEY, true);
  }

  @Override
  public void start() {
    //
    if(isAsync) {
      makeInterval();
    }
  }

  @Override
  public void stop() {
    isDone = false;
    forceStop = true;
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
    scheduler = null;
  }

  private void makeInterval() {
    //
    ThreadFactory threadFactory = new ThreadFactory() {
      public Thread newThread(Runnable runable) {
        Thread t = new Thread(runable, "Forum-task-manager-thread");
        t.setPriority(THREAD_PRIORITY);
        return t;
      }
    };
    scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
    //
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        if (isCommit(true)) {
          commit();
        }
      }
    }, 30000, INTERVAL, TimeUnit.MILLISECONDS);
  }
  
  public Boolean commit() {
    try {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      persist();
    } finally {
      PicketLinkIDMServiceImpl idmServiceImpl = CommonsUtils.getService(PicketLinkIDMServiceImpl.class);
      if (idmServiceImpl != null) {
        try {
          if (idmServiceImpl.getIdentitySession().getTransaction().isActive()) {
            idmServiceImpl.getIdentitySession().getTransaction().commit();
          }
        } catch (TransactionException e) {
          LOG.debug("The PoolingConnection is null ", e);
        } catch (Exception e) {
          LOG.debug("End request life cycle unsuccessfully ", e);
        }
      }
      RequestLifeCycle.end();
    }
    return true;
  }

  public void addTask(Task<ForumActivityContext> task) {
    if(tasks == null){
      tasks = new LinkedBlockingQueue<Task<ForumActivityContext>>();
    }
    tasks.add(task);
    if(!isAsync) {
      persist();
    }
    //
    if (isCommit(false)) {
      scheduler.submit(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return commit();
        }
      });
    }
  }

  private boolean isCommit(boolean forceCommit) {
    if (tasks == null || forceStop) {
      return false;
    }
    if (isDone && (forceCommit || tasks.size() >= MAX_SIZE_PERSIST)) {
      return true;
    }
    return false;
  }

  private Queue<Task<ForumActivityContext>> popTasks() {
    Queue<Task<ForumActivityContext>> tmp = tasks;
    tasks = null;
    Queue<Task<ForumActivityContext>> processTasks = new LinkedBlockingQueue<Task<ForumActivityContext>>();
    for (Task<ForumActivityContext> forumTask : tmp) {
      if (!processTasks.contains(forumTask)) {
        processTasks.add(forumTask);
      }
    }
    //
    return processTasks;
  }
  
  private void persist() {
    //
    isDone = false;
    try {
      //
      Queue<Task<ForumActivityContext>> tasks = popTasks();

      Task<ForumActivityContext> task;
      while (!forceStop && (task = tasks.poll()) != null) {
        ConversationState lastState = ConversationState.getCurrent();
        try {
          startProcess(task);
          processTask(task);
        } finally {
          endProcess(task, lastState);
        }
      }
    } catch (Exception e) {
      LOG.warn("Running task of forum activity unsuccessful.", e);
      LOG.debug(e.getMessage(), e);
    } finally {
      isDone = true;
    }
  }

  private void startProcess(Task<ForumActivityContext> task) {
    try{
      ConversationState.setCurrent(task.getState());
    }catch(Exception e){
      LOG.warn("Failed to set state context for forum activity task", e);
    }
  }

  private void processTask(Task<ForumActivityContext> task) {
    ActivityTask<ForumActivityContext> activityTask = task.getTask();
    //
    ExoSocialActivity got = ActivityExecutor.execute(activityTask, task.getContext());
    //
    if (activityTask instanceof PostActivityTask) {
      //
      PostActivityTask task_ = PostActivityTask.ADD_POST;
      if (got != null && activityTask.equals(task_)) {
        //
        ForumActivityUtils.takeCommentBack(task.getContext().getPost(), got);
      }
    } else if (activityTask instanceof TopicActivityTask) {
      //
      TopicActivityTask task_ = TopicActivityTask.ADD_TOPIC;
      if (got != null && activityTask.equals(task_)) {
        ForumActivityUtils.takeActivityBack(task.getContext().getTopic(), got);
      }
    }
  }

  private void endProcess(Task<ForumActivityContext> task, ConversationState lastState) {
    try{
      ConversationState.setCurrent(lastState);
    }catch(Exception e){
      LOG.warn("Failed to reset state context for forum activity task executing", e);
    }
  }

  public static class Task<T> {
    private ForumActivityContext ctx;
    private ActivityTask<T>      task;
    private final ConversationState state;

    public Task(ForumActivityContext ctx, ActivityTask<T> task) {
      this.ctx = ctx;
      this.task = task;
      this.state = ConversationState.getCurrent();
    }

    public ForumActivityContext getContext() {
      return ctx;
    }

    public ActivityTask<T> getTask() {
      return task;
    }
    
    public ConversationState getState(){
      return this.state;
    }
  }
}
