/*
 * Copyright (C) 2003-${year} eXo Platform SAS.
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

package org.exoplatform.faq.service.updater;

import java.io.IOException;
import java.io.InputStream;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@Managed
@NameTemplate( { @Property(key = "product.group.id", value = "org.exoplatform.forum"), @Property(key = "location", value = "{location}") })
@ManagedDescription("Plugin that allows to migration default template of FAQ viewer.")
public class FAQPreferenceTemplateUpdaterPlugin extends UpgradeProductPlugin {
  private static Log LOG      = ExoLogger.getLogger(FAQPreferenceTemplateUpdaterPlugin.class);

  private String     location = null;

  public FAQPreferenceTemplateUpdaterPlugin(InitParams params) {
    super(params);

    ValueParam param = params.getValueParam("location");
    if (param == null) {
      LOG.warn("Value-param 'location' is missing for " + getName() + ". The plugin will not be used");
    } else {
      this.location = param.getValue();
    }
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (shouldProceedToUpgrade(newVersion, oldVersion) == true) {
      LOG.info("\n Starts migration template of FAQ viewer");
      FAQService faqService = ((FAQService)  ExoContainerContext.getCurrentContainer().getComponentInstance(FAQService.class));
      InputStream in = null;
      try {
        in = getTemplateStream();
        if(in != null) {
          byte[] data = new byte[in.available()];
          in.read(data);
          faqService.saveTemplate(new String(data, "UTF-8"));
        }
      } catch (Exception e) {
        LOG.warn("Failed to migtation template of FAQ viewer", e);
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            LOG.warn("Failed to execute migration template of FAQ viewer", e);
          }
        }
      }
      LOG.info("\n End migration template of FAQ viewer!");
    }
  }

  /**
   *  Get template input stream.
   *  
   * @return
   * @throws Exception
   */
  public InputStream getTemplateStream() throws Exception {
    if(CommonUtils.isEmpty(this.location) == false) {
      ConfigurationManager configurationManager  = ((ConfigurationManager)  ExoContainerContext.getCurrentContainer()
          .getComponentInstance(ConfigurationManager.class));
      return configurationManager.getInputStream(this.location);
    }
    return null;
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
}


