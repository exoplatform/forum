/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.forum.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class TestAbstractListAccess extends TestCase {

  private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet consetetur sadipscing elitr " +
          "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat sed diam voluptua. " +
          "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren no sea takimata sanctus " +
          "est Lorem ipsum dolor sit amet.";
  private static String[] datas;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    datas = LOREM_IPSUM.split("\\s");
  }

  public void testAbstractListAccess() throws Exception {
    List<String> inputs = new ArrayList<String>();
    TestListAccess listAccess = new TestListAccess(inputs);
    assertEquals(0, listAccess.getSize());
    
    int length = datas.length;
    Random r = new Random();
    for (int i = 0; i < 35; i++) {
      inputs.add(datas[r.nextInt(length - 1)]);
    }
    //
    listAccess = new TestListAccess(inputs);
    assertEquals(35, listAccess.getSize());
    //
    listAccess.initialize(10, 1);
    assertEquals(10, listAccess.getPageSize());
    assertEquals(4, listAccess.getTotalPages());
    assertEquals(1, listAccess.getCurrentPage());
    //
    String[] outs = listAccess.load(2);
    assertEquals(2, listAccess.getCurrentPage());
    assertEquals(10, outs.length);
    assertEquals(outs[0], inputs.get(listAccess.getFrom()));
    assertEquals(outs[9], inputs.get(listAccess.getTo()));
    //
    outs = listAccess.load(4);
    assertEquals(4, listAccess.getCurrentPage());
    assertEquals(5, outs.length);
    assertEquals(outs[0], inputs.get(listAccess.getFrom()));
    assertEquals(outs[4], inputs.get(listAccess.getTo()));
  }

  private class TestListAccess extends AbstractListAccess<String> {
    private List<String> datas;

    public TestListAccess(List<String> inputs) {
      this.datas = inputs;
      this.size = inputs.size();
    }

    @Override
    public String[] load(int index, int length) throws Exception, IllegalArgumentException {
      int min = Math.min(size, index + length);
      int newLimit = min - index;
      return datas.subList(index, (newLimit + index)).toArray(new String[newLimit]);
    }

    @Override
    public int getSize() throws Exception {
      return this.size;
    }

    @Override
    public String[] load(int pageSelect) throws Exception, IllegalArgumentException {
      setCurrentPage(pageSelect);
      int offset = getOffset();
      int limit = getPageSize();
      return load(offset, limit);
    }
  }
}
