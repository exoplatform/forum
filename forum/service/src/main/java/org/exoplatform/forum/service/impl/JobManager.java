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
package org.exoplatform.forum.service.impl;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.forum.service.conf.DeactiveJob;
import org.exoplatform.forum.service.conf.DelayWritesJob;
import org.exoplatform.forum.service.conf.LoginJob;
import org.exoplatform.forum.service.conf.RecountActiveUserJob;
import org.exoplatform.forum.service.conf.SendMailJob;
import org.exoplatform.forum.service.conf.UpdateDataJob;
import org.exoplatform.forum.service.conf.UpdateUserProfileJob;
import org.exoplatform.forum.service.user.AutoPruneJob;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

@Managed
@NameTemplate( { @Property(key = "service", value = "forum"), @Property(key = "view", value = "jobs"), @Property(key = "name", value = "{Name}") })
@ManagedDescription("Forum management jobs")
public class JobManager {
  JobDetail jobDetail;
  
  public static final List<String> forumJobs = Arrays.asList(DeactiveJob.class.getName(), DelayWritesJob.class.getName(),
                                                               LoginJob.class.getName(), RecountActiveUserJob.class.getName(),
                                                               SendMailJob.class.getName(), UpdateDataJob.class.getName(), 
                                                               UpdateUserProfileJob.class.getName(), AutoPruneJob.class.getName());

  public JobManager(JobDetail jobDetail) {
    this.jobDetail = jobDetail;
  }

  @Managed
  @ManagedName("Name")
  public String getName() {
    return jobDetail.getKey().getName();
  }

  @Managed
  @ManagedName("DataMap")
  public JobDataMap getDataMap() {
    return jobDetail.getJobDataMap();
  }

  @Managed
  @ManagedName("JobClassName")
  public String getJobClassName() {
    return jobDetail.getJobClass().getName();
  }
}
