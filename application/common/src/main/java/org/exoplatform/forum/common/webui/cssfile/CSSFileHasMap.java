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

import java.util.Collection;
import java.util.HashMap;

public class CSSFileHasMap extends HashMap<String, BinCSSFile> {
  private static final long serialVersionUID = 1L;

  public BinCSSFile getByFileExtension(String fileExtension) {
    BinCSSFile cssClass = super.get(fileExtension.toLowerCase());
    if (cssClass == null) {
      Collection<BinCSSFile> binCSSFiles = super.values();
      for (BinCSSFile binCSSFile : binCSSFiles) {
        //
        if (binCSSFile.containExtention(fileExtension)) {
          return binCSSFile;
        }
      }
      return new BinCSSFile();
    }
    return cssClass;
  }

  public BinCSSFile getByFileType(String fullFileType) {
    BinCSSFile cssFile = get(fullFileType, true);
    if ((isDefaultCSSFile(cssFile))) {
      cssFile = get(fullFileType, false);
    }
    return cssFile;
  }

  private BinCSSFile get(String fullFileType, boolean isByFileType) {
    String fileType = getFileType(fullFileType);
    Collection<BinCSSFile> binCSSFiles = super.values();
    for (BinCSSFile binCSSFile : binCSSFiles) {
      //
      if ( (isByFileType == true && binCSSFile.hasTypes(fileType)) || 
           (isByFileType == false && binCSSFile.containInGroupClass(fileType)) ) {
        return binCSSFile;
      }
    }
    return new BinCSSFile();
  }

  private BinCSSFile getByGroupClass(String fullFileType) {
    return get(fullFileType, false);
  }

  private BinCSSFile getByExtensionAndFileType(String fileExtension, String fullFileType) {
    BinCSSFile cssFile = get(fullFileType, true);
    if ((isDefaultCSSFile(cssFile)) && isStringEmpty(fileExtension) == false) {
      cssFile = getByFileExtension(fileExtension);
      //
      if(isDefaultCSSFile(cssFile) ==  true) {
        cssFile = getByGroupClass(fullFileType);
      }
      //
      if(isDefaultCSSFile(cssFile) ==  false) {
        cssFile.addToTypes(fileExtension);
      }
    }
    return cssFile;
  }

  public BinCSSFile getByFileNameAndFileType(String fileName, String fullFileType) {
    String fileExtension = getFileExtension(fileName);
    return getByExtensionAndFileType(fileExtension, fullFileType);
  }

  public BinCSSFile getByFileName(String fileName) {
    fileName = getFileExtension(fileName);
    if (isStringEmpty(fileName) == false) {
      return getByFileExtension(fileName);
    } else {
      return new BinCSSFile();
    }
  }

  private String getFileExtension(String fileName) {
    if (isStringEmpty(fileName) == false && fileName.lastIndexOf(".") > 0) {
      return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    return "";
  }

  private String getFileType(String fullFileType) {
    return fullFileType.substring((fullFileType.indexOf("/") + 1)).toLowerCase();
  }

  private boolean isStringEmpty(String str) {
    return str == null || str.trim().length() == 0;
  }

  private boolean isDefaultCSSFile(BinCSSFile cssFile) {
    return (cssFile.equals(new BinCSSFile()) || cssFile.getMainCSSClass().equals("FileTxt")) ? true : false;
  }

}
