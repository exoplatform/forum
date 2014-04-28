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
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

public class LifeCycleCompletionService {
  private final String                 THREAD_NUMBER_KEY       = "thread-number";

  private final String                 ASYNC_EXECUTION_KEY     = "async-execution";
  
  private final String KEEP_ALIVE_TIME = "keepAliveTime";

  private Executor                     executor;

  private ExecutorCompletionService<?> ecs;
  
  private BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();

  private final int                   DEFAULT_THREAD_NUMBER   = 1;

  private final boolean               DEFAULT_ASYNC_EXECUTION = true;

  private int                          configThreadNumber;

  private int                          keepAliveTime = 10;

  private boolean                     configAsyncExecution;

  public LifeCycleCompletionService(InitParams params) {

    //
    ValueParam threadNumber = params.getValueParam(THREAD_NUMBER_KEY);
    ValueParam asyncExecution = params.getValueParam(ASYNC_EXECUTION_KEY);
    ValueParam aliveTime = params.getValueParam(KEEP_ALIVE_TIME);
    //
    try {
      this.configThreadNumber = Integer.valueOf(threadNumber.getValue());
    } catch (Exception e) {
      this.configThreadNumber = DEFAULT_THREAD_NUMBER;
    }

    //
    try {
      keepAliveTime = Integer.valueOf(aliveTime.getValue());
    } catch (Exception e) {
      keepAliveTime = 10;
    }

    //
    try {
      this.configAsyncExecution = Boolean.valueOf(asyncExecution.getValue());
    } catch (Exception e) {
      this.configAsyncExecution = DEFAULT_ASYNC_EXECUTION;
    }

    int threadNumber_ = configThreadNumber <= 0 ? configThreadNumber : Runtime.getRuntime().availableProcessors();

    ThreadFactory threadFactory = new ThreadFactory() {
      public Thread newThread(Runnable runable) {
        Thread t = new Thread(runable, "Forum-Thread");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
      }
    };

    //
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
}
