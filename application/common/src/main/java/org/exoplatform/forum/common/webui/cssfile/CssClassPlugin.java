/***************************************************************************
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
 ***************************************************************************/
package org.exoplatform.forum.common.webui.cssfile;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

public class CssClassPlugin extends BaseComponentPlugin {

  private List<CssClassIconFile> cssClassIconFile = new ArrayList<CssClassIconFile>();

  public CssClassPlugin(InitParams params) {
    cssClassIconFile = params.getObjectParamValues(CssClassIconFile.class);
  }

  public List<CssClassIconFile> getCssClassIconFile() {
    return cssClassIconFile;
  }

  public List<String> getCssClasss() {
    List<String> result = new ArrayList<String>();
    List<CssClassIconFile> data = getCssClassIconFile();
    for (CssClassIconFile cssClassIconFile : data) {
      result.add(cssClassIconFile.getType());
    }
    return result;
  }

}
