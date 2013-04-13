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
package org.exoplatform.forum.service.search;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.forum.service.ForumSearch;

public class UnifiedSearchOrder {
  
  /**
   * Processes order by condition for unified searching.
   * 
   * @param listSearchResult
   * @param sort 'title', 'relevancy', or 'date'
   * @param order 'desc' or 'asc'
   * @return ordered list
   * @since 4.0.0
   */
  public static List<ForumSearch> processOrder(List<ForumSearch> listSearchResult, String sort, String order) {
    
    RelavancyCompatator comparator = new RelavancyCompatator(sort, order);
    Collections.sort(listSearchResult, comparator);
    
    return listSearchResult;
  }
  
  static class RelavancyCompatator implements Comparator<ForumSearch> {
    
    private String sort;
    private String order;
    
    public RelavancyCompatator(String sort, String order) {
      this.sort = sort;
      this.order = order;
    }

    @Override
    public int compare(ForumSearch o1, ForumSearch o2) {
      if("relevancy".equalsIgnoreCase(sort) && "ASC".equalsIgnoreCase(order)) {
        //ascending order
        return o1.getRelevancy().compareTo(o2.getRelevancy());
      } else if("relevancy".equalsIgnoreCase(sort) && "DESC".equalsIgnoreCase(order)) {
        //descending order
        return o2.getRelevancy().compareTo(o1.getRelevancy());
      } else if("title".equalsIgnoreCase(sort) && "ASC".equalsIgnoreCase(order)) {
        //ascending order
        return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
      } else if("title".equalsIgnoreCase(sort) && "DESC".equalsIgnoreCase(order)) {
        //descending order
        return o2.getName().toUpperCase().compareTo(o1.getName().toUpperCase());
      } else if("date".equalsIgnoreCase(sort) && "ASC".equalsIgnoreCase(order)) {
        //ascending order
        return o1.getCreatedDate().compareTo(o2.getCreatedDate());
      } else if("date".equalsIgnoreCase(sort) && "DESC".equalsIgnoreCase(order)) {
        //descending order
        return o2.getCreatedDate().compareTo(o1.getCreatedDate());
      }
      return 0;
    }
    
  }
}
