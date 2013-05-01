/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.common.webui.cssfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BinCSSFile {
  public static final String DEFAULT_TYPE   = "default";
  public static final String DEFAULT_CSS    = "FileDefault";

  private String             mainType;

  private String             mainCSSClass;

  private Set<String>        types = new HashSet<String>();

  private List<String>       groupCSSClass  = new ArrayList<String>();

  public BinCSSFile() {
    mainCSSClass = DEFAULT_CSS;
    mainType = DEFAULT_TYPE;
  }

  public BinCSSFile(String mainType, List<String> groupCSSClass) {
    this.mainType = mainType;
    this.mainCSSClass = groupCSSClass.get(0);
    this.groupCSSClass = groupCSSClass;
    this.addToTypes(mainType);
  }

  public String getMainType() {
    return mainType;
  }

  public void setMainType(String mainType) {
    this.mainType = mainType;
  }

  public String getMainCSSClass() {
    return mainCSSClass;
  }

  public void setMainCSSClass(String mainCSSClass) {
    this.mainCSSClass = mainCSSClass;
  }

  public void addToTypes(String type) {
    this.types.add(type);
  }
  
  public boolean hasTypes(String type) {
    return this.types.contains(type);
  }

  public List<String> getGroupCSSClass() {
    return groupCSSClass;
  }

  public void setGroupCSSClass(List<String> groupCSSClass) {
    this.groupCSSClass = groupCSSClass;
  }
  
  public boolean containInGroupClass(String fileType) {
    if(fileType == null || fileType.isEmpty()) return false;
    for (String cssClass : groupCSSClass) {
      //
      if (cssClass.indexOf(fileType) >= 0) {
        return true;
      }
    }
    return false;
  }

  public boolean containExtention(String fileExtention) {
    if(fileExtention == null || fileExtention.isEmpty()) return false;
    for (String cssClass : groupCSSClass) {
      //
      if (cssClass.equalsIgnoreCase(fileExtention)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object object) {
    if(object instanceof BinCSSFile) {
      BinCSSFile binCSSFile = (BinCSSFile) object;
      return (binCSSFile.getMainType().equals(mainType) || 
          binCSSFile.getMainCSSClass().endsWith(mainCSSClass)) ? true : false;
    }
    return false;
  }

}
