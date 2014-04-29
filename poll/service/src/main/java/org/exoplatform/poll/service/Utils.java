/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.poll.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Utils {
  public static final String COLON = ":".intern();

  public static Calendar getGreenwichMeanTime() {
    Calendar calendar = GregorianCalendar.getInstance();
    calendar.setLenient(false);
    int gmtoffset = calendar.get(Calendar.DST_OFFSET) + calendar.get(Calendar.ZONE_OFFSET);
    calendar.setTimeInMillis(System.currentTimeMillis() - gmtoffset);
    return calendar;
  }

  public static boolean isEmpty(String s) {
    return (s == null || s.trim().length() <= 0) ? true : false;
  }

  public static boolean isListEmpty(List<String> list) {
    if (list == null || list.size() == 0) {
      return true;
    }
    for (String string : list) {
      if (!isEmpty(string)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Calculate data of poll when user Voted. Apply for all case (Single vote and multi vote) 
   * 
   * @param poll the poll after user vote.
   * @param userId the user name voted.
   * @param voteOptions the value user voted. 
   *  - If is single vote, the value is one number. 
   *  - If is multi vote, the value has format a:b:c, with a,b,c is number user selected.
   * @throws Exception the exception
   */
  public static Poll calculateVote(Poll poll, String userId, String voteOptions) throws Exception {
    List<String> userVote = new ArrayList<String>(Arrays.asList(poll.getUserVote()));

    // remove current vote (if exist) to vote again
    String currentVote = "";
    for (String uv : userVote) {
      if (uv.startsWith(userId + COLON)) {
        currentVote = uv;
        break;
      }
    }
    userVote.remove(currentVote);
    if(voteOptions.indexOf(COLON) > 0) {
      voteOptions = StringUtils.join(new HashSet<String>(Arrays.asList(voteOptions.split(COLON))), COLON);
    }
    // add the new vote
    userVote.add(userId + COLON + voteOptions);

    // calculating...
    int[] votes = new int[poll.getOption().length];
    int total = userVote.size();
    for (String uv : userVote) {
      String[] uvArr = uv.split(COLON);
      for (int i = 1; i < uvArr.length; i++) {
        votes[Integer.parseInt(uvArr[i].trim())]++;
      }
    }

    String[] sVotes = new String[votes.length];
    for (int i = 0; i < sVotes.length; i++) {
      sVotes[i] = ((float) votes[i] / total * 100) + "";
    }

    // update the poll
    poll.setUserVote(userVote.toArray(new String[0]));
    poll.setVote(sVotes);

    return poll;
  }
  
  /**
   * Get a string from infoVote of poll
   * 
   * @param poll
   * @return
   */
  public static String getInfoVote(Poll poll) {
    //String s = "";
    StringBuilder sb = new StringBuilder();
    String[] infoVote = poll.getInfoVote();
    String[] options = poll.getOption();
    if (infoVote == null) {
      for (int j=0; j<options.length; j++) {
        sb.append(options[j]).append(":0%:0|");
      }
      sb.append("0");
    } else {
      for (int i=0; i<infoVote.length-1; i++) {
        String[] list = infoVote[i].split(":");
        if (Integer.parseInt(list[1].split("\\.")[0]) > 1) {
          sb.append(options[i]).append(":").append(list[0]).append("%:").append(list[1].split("\\.")[0]).append("|");
        } else {
          sb.append(options[i]).append(":").append(list[0]).append("%:").append(list[1].split("\\.")[0]).append("|");
        }
      }
      if (Integer.parseInt(infoVote[infoVote.length-1]) > 1) {
        sb.append(infoVote[infoVote.length-1]);
      } else {
        sb.append(infoVote[infoVote.length-1]);
      }
    }
    return sb.toString();
  }
  
  /**
   * Convert userVote of poll to s string
   * 
   * @param poll
   * @param userName
   * @return
   */
  public static String getUserVote(Poll poll, String userName) {
    StringBuilder sb = new StringBuilder();
    String[] votes = poll.getUserVote();
    String[] options = poll.getOption();
    if (votes.length == 0) return sb.toString().trim();
    for (String element : votes) {
      String[] userVote = element.split(":");
      if (userVote[0].equals(userName)) {
        for (int i=1;i<userVote.length;i++) {
          int j = Integer.parseInt(userVote[i]);
          sb.append(" ").append(options[j]);
        }
      }
    }
    return sb.toString().trim();
  }
  
  public static String getCurrentUserVote(Poll poll) {
    String[] votes = poll.getUserVote();
    if (votes.length == 0) {
      return poll.getOwner();
    }
    return votes[votes.length-1].split(":")[0];
  }
}
