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

import junit.framework.TestCase;

public class BuiltinCSSFileTypeTest extends TestCase {

  public BuiltinCSSFileTypeTest() {
  }
  
  public void testGetCSSClassByFileNameAndFileType() {
    String actual = BuiltinCSSFileTypeUtils.getCSSClassByFileNameAndFileType("", "");
    String expected = "uiIcon16x16FileDefault uiIcon16x16nt_file";
    assertEquals(expected, actual);
    
    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileNameAndFileType("abc.txt", "text/textplain");
    expected = "uiIcon16x16FileTxt uiIcon16x16nt_file";
    assertEquals(expected, actual);
    
    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileNameAndFileType("abc.txt", "applicationx-gzip");
    expected = "uiIcon16x16FileArchiveDefault uiIcon16x16nt_file";
    assertEquals(expected, actual);

    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileNameAndFileType("abc.js", "text/textplain");
    expected = "uiIcon16x16FileJs uiIcon16x16nt_file";
    assertEquals(expected, actual);

    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileNameAndFileType("abc.js", "");
    expected = "uiIcon16x16FileJs uiIcon16x16nt_file";
    assertEquals(expected, actual);

    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileNameAndFileType("abc.js", "", "24x24");
    expected = "uiIcon24x24FileJs uiIcon24x24nt_file";
    assertEquals(expected, actual);
  }
  
  public void testGetCSSClassByFileType() {
    String actual = BuiltinCSSFileTypeUtils.getCSSClassByFileType("");
    String expected = "uiIcon16x16FileDefault uiIcon16x16nt_file";
    assertEquals(expected, actual);

    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileType("imagepsd");
    expected = "uiIcon16x16FilePsd uiIcon16x16nt_file";
    assertEquals(expected, actual);

    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileType("texthtml", "24x24");
    expected = "uiIcon24x24FileHtml uiIcon24x24nt_file";
    assertEquals(expected, actual);
    
  }

  public void testGetCSSClassByFileName() {
    String actual = BuiltinCSSFileTypeUtils.getCSSClassByFileName("");
    String expected = "uiIcon16x16FileDefault uiIcon16x16nt_file";
    assertEquals(expected, actual);
    
    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileName("test.jpg");
    expected = "uiIcon16x16FileJpg uiIcon16x16nt_file";
    assertEquals(expected, actual);

    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileName("test.flv", "24x24");
    expected = "uiIcon24x24FileFlash uiIcon24x24nt_file";
    assertEquals(expected, actual);
  }
  
  public void testGetCSSClassByFileExtension() {
    String actual = BuiltinCSSFileTypeUtils.getCSSClassByFileExtension("");
    String expected = "uiIcon16x16FileDefault uiIcon16x16nt_file";
    assertEquals(expected, actual);
    
    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileExtension("jpg");
    expected = "uiIcon16x16FileJpg uiIcon16x16nt_file";
    assertEquals(expected, actual);
    
    actual = BuiltinCSSFileTypeUtils.getCSSClassByFileExtension("mpeg", "24x24");
    expected = "uiIcon24x24FileMpeg uiIcon24x24nt_file";
    assertEquals(expected, actual);
  }
}
