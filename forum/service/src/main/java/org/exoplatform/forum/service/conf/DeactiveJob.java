/**
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
 **/
package org.exoplatform.forum.service.conf;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.quartz.*;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class DeactiveJob implements Job {
  private static final Log LOG = ExoLogger.getLogger("job.forum.DesactiveJob");

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer oldContainer = ExoContainerContext.getCurrentContainer();
    ExoContainer container = CommonUtils.getExoContainer(context);

    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      ForumService forumService = container.getComponentInstanceOfType(ForumService.class);
      if (forumService != null) {
        JobDataMap jdatamap = context.getJobDetail().getJobDataMap();
        String inactiveDays = jdatamap.getString("inactiveDays");
        String forumName = jdatamap.getString("forumName");
        if (inactiveDays != null && inactiveDays.length() > 0) {
          int days = Integer.parseInt(inactiveDays);
          if (days > 0) {
            long oneDay = 86400000; // milliseconds of one day
            Calendar calendar = GregorianCalendar.getInstance();
            long currentDay = calendar.getTimeInMillis();
            currentDay = currentDay - (days * oneDay);
            calendar.setTimeInMillis(currentDay);
            String path = forumService.getForumHomePath();
            StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append("/jcr:root/").append(path).append("//element(*,exo:topic)[");
            stringBuffer.append("@exo:lastPostDate <= xs:dateTime('" + ISO8601.format(calendar)
                + "') and @exo:isActive = 'true']");
            NodeIterator iter = forumService.search(stringBuffer.toString());
            if (iter != null) {
              while (iter.hasNext()) {
                Node topic = iter.nextNode();
                if (forumName != null && forumName.length() > 0) {
                  if (forumName.equals(topic.getParent().getProperty("exo:name").getString())) {
                    topic.setProperty("exo:isActive", false);
                    topic.save();
                  }
                } else {
                  topic.setProperty("exo:isActive", false);
                  topic.save();
                }
                if (LOG.isDebugEnabled()) {
                  LOG.debug("\n\n The DeactiveJob have been running: The topic '" + topic.getProperty("exo:name").getString()
                      + "' deactived");
                }
              }
            }
          }
        }
      }
    } catch (NumberFormatException nfe) {
      LOG.trace("\nThe DeactiveJob can not run, a number is not format: " + nfe.getMessage() + "\n" + nfe.getCause());
    } catch (Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug(e.getMessage());
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(oldContainer);
    }
  }
}
