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
package org.exoplatform.forum.common.webui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BuiltinCSSFileTypeProvider extends HashMap<String, String> {
  private static final long serialVersionUID = 1L;

  private static final String DEFAULT_CSS_FILE_TYPE    = "uiIcon16x16FileDefault uiIcon16x16nt_file uiIcon16x16";

  private static final String DEFAULT_TYPE             = "default";

  private static final String[] CSS_CLASSNAME_FILE_TYPES = 
      new StringBuffer("applicationoctet-stream applicationillustrator ")
  		.append("applicationother-zip videox-msvideo imagebmp applicationx-bzip2 ")
  		.append("applicationother-zip textcss fontdfont applicationmsword ")
      .append("applicationmsword applicationear-java-archive fonteot applicationeps ")
      .append("applicationexe videox-flv applicationvndgoogle-appsdocument ")
      .append("applicationoctet-stream applicationvndgoogle-appsdrawing imagegif ")
      .append("applicationvndgoogle-appsspreadsheet applicationvndgoogle-appspresentation ")
      .append("applicationx-gzip texthtml imagex-icon applicationoctet-stream ")
      .append("applicationx-indesign applicationjava-archive applicationoctet-stream ")
      .append("imagejpeg applicationx-javascript applicationjson applicationother-zip ")
      .append("audioother audiox-m4a videox-m4v applicationmsaccess videox-matroska ")
      .append("videoquicktime audiomp3 videomp4 applicationvndoasisopendocumentformula ")
      .append("applicationvndoasisopendocumentgraphics applicationvndoasisopendocumentpresentation ")
      .append("applicationvndoasisopendocumentspreadsheet applicationvndoasisopendocumenttext ")
      .append("fontotf applicationvndoasisopendocumentspreadsheet-template imagex-portable-bitmap ")
      .append("applicationoctet-stream applicationpdf imagepng applicationppt ")
      .append("applicationvndopenxmlformats-officedocumentpresentationmlpresentation applicationpostscript ")
      .append("imagepsd imagepsd applicationoctet-stream applicationrar ")
      .append("applicationraw applicationrtf applicationoctet-stream applicationoctet-stream ")
      .append("imagesvg-xml applicationvndsunxmlcalc applicationx-bzip2 applicationx-tar ")
      .append("applicationother-zip applicationother-zip applicationoctet-stream imagetiff ")
      .append("fontttf textplain applicationx-zip audiox-wav audiox-ms-wma ")
      .append("videox-ms-wmv applicationoctet-stream imagexcf applicationxls ")
      .append("textxml textxsl applicationother-zip applicationzip FileFlash").toString().split("\\s");
  
  private static final String[] FILE_TYPES               = new StringBuffer(DEFAULT_TYPE)
      .append(" AI AR Avi bmp bz2 cbz css dfont doc docx ear eot esp exe flv gdoc dbf gdraw gif gsheet gslides gz ")
      .append("html ico iff indd jar jpf jpg js json lzma m3u m4a m4v mdb mkv mov mp3 mp4 odf odg odp ods odt ")
      .append("otf ots pbm pcx pdf png ppt pptx ps psb psd pxr rar raw rtf sct sql svg sxc bz2 tar lzma xz tga ")
      .append("tif ttf txt war wav wma wmv woff xcf xls xml xsl xz zip swf").toString().toLowerCase().split("\\s");
  
  private static final String[] SpecialFileType = "msword presentation x-sh x-gzip x-javascript x-shockwave-flash java-archive".split("\\s");
  private static final String[] TypeOfSpecialFileType = "doc ppt sh zip js swf jar".split("\\s");

  public BuiltinCSSFileTypeProvider() {
    for (int i = 0; i < FILE_TYPES.length; i++) {
      addCSSFileType(FILE_TYPES[i], CSS_CLASSNAME_FILE_TYPES[i]);
    }
  }

  public void addCSSFileType(String fileType, String className) {
    super.put(fileType, new StringBuffer(DEFAULT_CSS_FILE_TYPE).append(className).toString());
  }

  private String getCSSBySpecialFileType(String fileType) {
    for (int i = 0; i < SpecialFileType.length; ++i) {
      if (fileType.indexOf(SpecialFileType[i]) >= 0) {
        return getCSSByFileType(TypeOfSpecialFileType[i]);
      }
    }

    //
    List<String> list = Arrays.asList(CSS_CLASSNAME_FILE_TYPES);
    int index = list.indexOf("application" + fileType);
    if (index > 0) {
      return this.values().toArray(new String[FILE_TYPES.length])[index];
    }

    //
    return super.get(DEFAULT_TYPE);
  }

  public String getCSSByFileType(String fileType) {
    String cssClass = super.get(fileType);
    if (cssClass == null || cssClass.length() <= 0) {
      cssClass = getCSSBySpecialFileType(fileType);
    }
    return cssClass;
  }

  public String getCSSByFullFileType(String fullFileType) {
    String fileType = fullFileType.substring((fullFileType.indexOf("/") + 1)).toLowerCase();
    return getCSSByFileType(fileType);
  }

  public String getCSSByExtensionAndFileType(String fileExtension, String fileType) {
    String cssClass = getCSSByFullFileType(fileType);
    if (cssClass.equals(super.get(DEFAULT_TYPE)) && isStringEmpty(fileExtension) == false) {
      cssClass = getCSSByFileType(fileExtension);
    }
    return cssClass;
  }

  public String getCSSByFileNameAndFileType(String fileName, String fileType) {
    String fileExtension = null;
    if (isStringEmpty(fileName) == false && fileName.lastIndexOf(".") > 0) {
      fileExtension = fileName.substring(fileName.lastIndexOf("."));
    }
    return getCSSByExtensionAndFileType(fileExtension, fileType);
  }

  private boolean isStringEmpty(String str) {
    return str == null || str.trim().length() == 0;
  }
}
