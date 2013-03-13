package org.exoplatform.faq.service.search;

import org.exoplatform.commons.api.search.data.SearchResult;


/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * March 12, 2013  
 */
public class UnifiedSearchResult extends SearchResult {

  private Double rating = 0.0;

  public UnifiedSearchResult(String url,
                             String title,
                             String excerpt,
                             String detail,
                             String imageUrl,
                             long date,
                             long relevancy,
                             Double rating) {
    super(url, title, excerpt, detail, imageUrl, date, relevancy);
    this.rating = rating;
  }

  public Double getRating() {
    return rating;
  }

  public void setRating(Double rating) {
    this.rating = rating;
  }

}
