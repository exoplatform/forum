/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.forum.common.lifecycle;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.picocontainer.Startable;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.InitParamsValue;

public class LifeCycleCompletionService implements Startable {
  private final String                 THREAD_NUMBER_KEY       = "thread-number";

  private final String                 ASYNC_EXECUTION_KEY     = "async-execution";

  private final String                 PRIORITY_KEY            = "thread-priority";
  
  private final String KEEP_ALIVE_TIME = "keepAliveTime";

  private Executor                     executor;

  private ExecutorCompletionService<?> ecs;
  
  private BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();

  private final int                   DEFAULT_THREAD_NUMBER   = 1;

  private final boolean               DEFAULT_ASYNC_EXECUTION = true;

  private final int                   DEFAULT_KEEP_ALIVE_TIME = 10;

  private final int                   DEFAULT_THREAD_PRIORITY = 10;

  private final boolean configAsyncExecution;


  public LifeCycleCompletionService(InitParams params) {

    //
    final int configThreadNumber = InitParamsValue.getInteger(params, THREAD_NUMBER_KEY, DEFAULT_THREAD_NUMBER);
    final int keepAliveTime = InitParamsValue.getInteger(params, KEEP_ALIVE_TIME, DEFAULT_KEEP_ALIVE_TIME);
    final int threadPriority = InitParamsValue.getInteger(params, PRIORITY_KEY, DEFAULT_THREAD_PRIORITY);
    this.configAsyncExecution = InitParamsValue.getBoolean(params, ASYNC_EXECUTION_KEY, DEFAULT_ASYNC_EXECUTION);

    int threadNumber_ = configThreadNumber <= 0 ? configThreadNumber : Runtime.getRuntime().availableProcessors();
    //
    ThreadFactory threadFactory = new ThreadFactory() {
      public Thread newThread(Runnable runable) {
        Thread t = new Thread(runable, "Forum-Thread");
        t.setPriority(threadPriority);
        return t;
      }
    };
    //
    if (configAsyncExecution) {
      executor = new ThreadPoolExecutor(threadNumber_, threadNumber_, keepAliveTime, 
                                              TimeUnit.SECONDS, workQueue, threadFactory);
      ((ThreadPoolExecutor) executor).allowCoreThreadTimeOut(true);
    } else {
      executor = new DirectExecutor();
    }

    //
    this.ecs = new ExecutorCompletionService(executor);

  }

  public void addTask(Callable callable) {
    ecs.submit(callable);
  }

  public void waitCompletionFinished() {
    try {
      if (executor instanceof ExecutorService) {
        ((ExecutorService) executor).awaitTermination(1, TimeUnit.SECONDS);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean shutdownNow() {
    boolean isDone = true;
    Future<?> f;
    while (ecs != null && (f = ecs.poll()) != null) {
      isDone &= f.cancel(true);
    }
    return isDone;
  }

  public boolean isAsync() {
    return this.configAsyncExecution;
  }

  private class DirectExecutor implements Executor {

    public void execute(final Runnable runnable) {
      if (Thread.interrupted())
        throw new RuntimeException();

      runnable.run();
    }
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
    if (executor instanceof ExecutorService) {
      ((ExecutorService) executor).shutdown();
    }
  }
}
