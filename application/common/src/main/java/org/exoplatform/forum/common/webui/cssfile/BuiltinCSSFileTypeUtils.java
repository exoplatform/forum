/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation,either version 3 of the License,or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not,see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.common.webui.cssfile;

import java.util.Arrays;

public class BuiltinCSSFileTypeUtils {
  private static final String  UI_ICON                  = "uiIcon";

  public static final String   DEFAULT_CSS_ICON_FILE    = "nt_file";

  public static final String   SIZE_16x16               = "16x16";

  public static final String   SIZE_24x24               = "24x24";
  
  private static final String  CSS_CLASSNAME_FILE_TYPES = new StringBuffer()
  .append("default - FileDefault,applicationoctet-stream;")
  .append("gzip - FileArchiveDefault,applicationx-tar,tar,applicationx-gzip,gz,applicationx-bzip2,applicationother-zip;")
  .append("rar - FileArchiveRar,applicationrar;")
  .append("zip - FileArchiveZip,FileZip,applicationzip,7z;")
  .append("jar - FileJar,applicationjava-archive;")
  .append("ear - FileEar,applicationear-java-archive;")
  .append("war - FileWar,applicationx-zip;")
  .append("audio - FileAudioDefault,audiomidi,audiox-aiff,audiobasic,audiox-m4a,m4a,audiox-wav,wav,audioother;")
  .append("mp3 - FileMp3,audiomp3;")
  .append("wma - FileWma,audiox-ms-wma;")
  .append("video - FileVideo,videox-msvideo,videoquicktime,videox-sgi-movie,videox-msvideo,videompe,mpe,avi,mov;")
  .append("flash - FileFlash,applicationx-shockwave-flash,videox-flv,flv,swf;")
  .append("wmv - FileWmv,videox-ms-wmv;")
  .append("gdoc - FileGdoc,applicationvndgoogle-appsdocument;")
  .append("doc - FileWord,applicationmsword,applicationwordperfect,applicationvndopenxmlformats-officedocumentwordprocessingmldocument,FileDocx;")
  .append("odt - FileOpenOffice1,FileOdt,applicationvndoasisopendocumenttext,applicationvndoasisopendocumentspreadsheet,applicationvndoasisopendocumentdatabase;")
  .append("mpg - FileMpeg,videompeg,mpeg,videompg,videomp4,mp4;")
  .append("m4v - FileM4v,videox-m4v;")
  .append("mkv - FileMkv,videox-matroska;")
  .append("txt - FileTxt,textplain;")
  .append("lwp - FileLwp,applicationlwp;")
  .append("page - FilePage,applepages;")
  .append("mdb - FileMdb,applicationmsaccess;")
  .append("odf - FileOdf,applicationvndoasisopendocumentformula;")
  .append("exe - FileExe,applicationexe;")
  .append("pdf - FilePdf,applicationpdf;")
  .append("script - FileScript,applicationpostscript,sh;")
  .append("ftf - FileRtf,applicationrtf;")
  .append("gslides - FileGslides,applicationvndgoogle-appspresentation;")
  .append("applekey - FileAppleKey,textkey;")
  .append("office2 - FileOpenOffice2,applicationvndoasisopendocumentpresentation;")
  .append("ppt - FilePpt,applicationppt,applicationvndopenxmlformats-officedocumentpresentationmlpresentation;")
  .append("prz - FilePrz,applicationfreelance;")
  .append("gsheet - FileGsheet,applicationvndgoogle-appsspreadsheet;")
  .append("xls - FileExcel,applicationxls,applicationxlt,applicationvndopenxmlformats-officedocumentspreadsheetmlsheet,FileXls;")
  .append("office3 - FileOpenOffice3,applicationvndoasisopendocumentspreadsheet-template;")
  .append("html - FileHtml,texthtml,texthtm;")
  .append("xml - FileXml,textxml;")
  .append("js - FileJs,applicationx-javascript;")
  .append("json - FileJson,applicationjson;")
  .append("css - FileCss,textcss;")
  .append("xsl - FileXsl,textxsl;")
  .append("ttf - FileFont,fonttff,fontotf,fontttc,fonteot,fontdfont,fontttf,fontwoff;")
  .append("gdraw - FileGdraw,applicationvndgoogle-appsdrawing;")
  .append("bmp - FileBmp,imagebmp;")
  .append("icon - FileIco,imagex-icon;")
  .append("gif - FileGif,imagegif;")
  .append("odg - FileOdg,applicationvndoasisopendocumentgraphics;")
  .append("png - FilePng,imagepng;")
  .append("psd - FilePsd,imagepsd;")
  .append("raw - FileRaw,applicationraw;")
  .append("tif - FileTif,imagetiff;")
  .append("xcf - FileXcf,imagexcf;")
  .append("indd - FileIndd,applicationx-indesign;")
  .append("ai - FileAi,applicationillustrator,applicationeps;")
  .append("svg - FileSvg,imagesvg-xml;")
  .append("jpg - FileJpg,imagejpeg,jpeg").toString();

  private static CSSFileHasMap dataStorage              = new CSSFileHasMap();
  
  private static void buildData() {
    if (dataStorage.size() == 0) {
      BinCSSFile binCSSFile;
      String[] dataList = CSS_CLASSNAME_FILE_TYPES.split(";");
      for (int i = 0; i < dataList.length; i++) {
        String[] cssStype = dataList[i].split(" - ");
        binCSSFile = new BinCSSFile(cssStype[0], Arrays.asList(cssStype[1].split(",")));
        dataStorage.put(cssStype[0], binCSSFile);
      }
      //
    }
  }

  /**
   * Returns the icon CSS class name of file.
   * 
   * @param fileName - The name of file contain file extension
   * @param fullFileType - The file's type
   * @param size - The size of icon, if not use, the value default is 16x16
   * @return
   * 
   * @since 4.0.1
   */
  public static String getCSSClassByFileNameAndFileType(String fileName, String fullFileType, String... size) {
    // 
    buildData();
    
    return buildCssClass(dataStorage.getByFileNameAndFileType(fileName, fullFileType), size);
  }

  /**
   * Returns the icon CSS class name of file.
   * 
   * @param fullFileType - The file's type
   * @param size - The size of icon, if not use, the value default is 16x16
   * @return
   * 
   * @since 4.0.1
   */
  public static String getCSSClassByFileType(String fullFileType, String... size) {
    // 
    buildData();
    
    return buildCssClass(dataStorage.getByFileType(fullFileType), size);
  }

  /**
   * Returns the icon CSS class name of file.
   * 
   * @param fileName - The name of file contain file extension
   * @param size - The size of icon, if not use, the value default is 16x16
   * @return
   * 
   * @since 4.0.1
   */
  public static String getCSSClassByFileName(String fileName, String... size) {
    // 
    buildData();
    
    return buildCssClass(dataStorage.getByFileName(fileName), size);
  }

  /**
   * Returns the icon CSS class name of file.
   * 
   * @param fileExtension - The file extension(example: mpg, jpg, flv, txt, ....)
   * @param size - The size of icon, if not use, the value default is 16x16
   * @return
   * 
   * @since 4.0.1
   */
  public static String getCSSClassByFileExtension(String fileExtension, String... size) {
    // 
    buildData();
    
    return buildCssClass(dataStorage.getByFileExtension(fileExtension), size);
  }
  
  private static String buildCssClass(BinCSSFile binCSSFile, String... size) {
    if (size == null || size.length == 0) {
      size = new String[] { "16x16" };
    }
    if (size.length > 1) {
      size = new String[] { size[0] };
    }
    return String.format("%s%s%s %s%s%s", UI_ICON, size[0], binCSSFile.getMainCSSClass(), UI_ICON, size[0], DEFAULT_CSS_ICON_FILE);
  }
  
}
