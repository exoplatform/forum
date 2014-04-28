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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class PersisterTask implements PersistAlgorithm {
  protected static final Log LOG = ExoLogger.getLogger(PersisterTask.class);
  /** */
  final Persister persister;
  /** */
  final long wakeupInterval;
  /** */
  final Task task;
  /** */
  final TimeUnit timeUnit;
  /** */
  final long maxFixedSize;
  /** */
  ScheduledExecutorService scheduledExecutor;
  
  public static Builder init() {
    return new Builder();
  }
  public PersisterTask(Builder builder) {
    this.wakeupInterval = builder.wakeupInterval;
    this.maxFixedSize = builder.maxFixedSize;
    this.persister = builder.persister;
    this.timeUnit = builder.timeUnit == null ? TimeUnit.MILLISECONDS : builder.timeUnit;
    this.task = new Task();
  }

  public void start() {
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutor.scheduleWithFixedDelay(task, wakeupInterval / 2, wakeupInterval, this.timeUnit);
  }
  
  public void stop() {
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdownNow();
    }
    scheduledExecutor = null;
  }
  
  public class Task implements Runnable {
    public void run() {
      try {
        persister.commit(true);
      } catch (Exception e) {
        LOG.error("Persist task encountered an unexpected error", e);
      }
    }
  }
  
  public static class Builder {
    public Persister persister;
    public long wakeupInterval;
    public TimeUnit timeUnit;
    public long maxFixedSize;
    
    public Builder() {}
    
    public Builder persister(Persister persister) {
      this.persister = persister;
      return this;
    }
    
    public Builder wakeup(long interval) {
      this.wakeupInterval = interval;
      return this;
    }
    
    public Builder timeUnit(TimeUnit timeUnit) {
      this.timeUnit = timeUnit;
      return this;
    }
    
    public Builder maxFixedSize(long maxFixedSize) {
      this.maxFixedSize = maxFixedSize;
      return this;
    }
    
    public PersisterTask build() {
      return new PersisterTask(this);
    }
  }

  @Override
  public boolean shoudldPersist(int changedSize) {
    return changedSize >= maxFixedSize;
  }
}
