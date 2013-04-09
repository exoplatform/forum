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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.search;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.exoplatform.forum.service.ForumSearch;
import org.exoplatform.forum.service.search.UnifiedSearchOrder;

/**
 * Created by The eXo Platform SAS
 * Author : Thanh Vu Cong
 *          thanh_vucong@exoplatform.com
 * Apr 9, 2013  
 */
public class UnifiedSearchOrderTest extends TestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  private List<ForumSearch> makeData() {
    List<ForumSearch> list = new ArrayList<ForumSearch>(5);
    Date current = Calendar.getInstance().getTime();
    
    //
    ForumSearch item = new ForumSearch();
    item.setName("zbC");
    item.setRelevancy(100);
    item.setCreatedDate(current);
    
    list.add(item);
    
    //
    item = new ForumSearch();
    item.setName("bAc");
    item.setRelevancy(120);
    item.setCreatedDate(current);
    list.add(item);
    
    //
    item = new ForumSearch();
    item.setName("Dyc");
    item.setRelevancy(122);
    item.setCreatedDate(current);
    list.add(item);
    
    //
    item = new ForumSearch();
    item.setName("pyc thanh");
    item.setRelevancy(1122);
    item.setCreatedDate(current);
    
    list.add(item);
    
    //
    item = new ForumSearch();
    item.setName("zxyc ");
    item.setRelevancy(251);
    item.setCreatedDate(current);
    
    list.add(item);
    
    return list;
  }
  
  public void testRelevancyASC() throws Exception {
    List<ForumSearch> list = makeData();
    List<ForumSearch> result = UnifiedSearchOrder.processOrder(list, "relevancy", "asc");
    
    //
    ForumSearch previous = null;
    for (ForumSearch e : result) {
      if (previous == null) {
        previous = e;
      } else {
        assertTrue(e.getRelevancy() >= previous.getRelevancy());
        previous = null;
      }
    }
    
  }
  
  public void testRelevancyDESC() throws Exception {
    List<ForumSearch> list = makeData();
    List<ForumSearch> result = UnifiedSearchOrder.processOrder(list, "relevancy", "desc");
    
    //
    ForumSearch previous = null;
    for (ForumSearch e : result) {
      if (previous == null) {
        previous = e;
      } else {
        assertTrue(previous.getRelevancy() >= e.getRelevancy());
        previous = null;
      }
    }
    
  }
  
  public void testTitleDESC() throws Exception {
    List<ForumSearch> list = makeData();
    List<ForumSearch> result = UnifiedSearchOrder.processOrder(list, "title", "desc");
    ForumSearch item = result.get(0);
    
    assertEquals("zxyc ", item.getName());
  }
  
  
}
