(function(module, utils, $, document, window) {
  var UIDateTimePicker = {
    calendarId : "UICalendarControl",
    dateField : "",
    currentDate : null, // Datetime value base of selectedDate for
    // displaying calendar below
    // if selectedDate is invalid, currentDate deals with system time;
    selectedDate : null, // Datetime value of input date&time field
    months : [ 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December' ],
    weekdays : [ 'S', 'M', 'T', 'W', 'T', 'F', 'S' ],
    tooltip : [ 'Previous Year', 'Previous Month', 'Next Month', 'Next Year' ],
    pathResource : "/forumResources/javascript/eXo/forum/lang/",
    lang : "",
    fistWeekDay : 0, // sunday: 0, monday: 1, tuesday: 2, wednesday: 3, thursday: 4, friday: 5, saturday: 6
    setUp : function(calendarId) {
      UIDateTimePicker.calendarId = calendarId;
    },
  
    getLang : function() {
      try {
        var day = UIDateTimePicker.dateField.getAttribute('fistweekday');
        if (day)
          UIDateTimePicker.fistWeekDay = day * 1 - 1; // attribute 'fistweekday' includes:
        // sunday: 1, monday: 2,..., saturday: 7
        var lang = UIDateTimePicker.dateField.getAttribute('lang');
        if (UIDateTimePicker.lang == lang)
          return;
        UIDateTimePicker.lang = lang;
        var languages = eval(ajaxAsyncGetRequest(UIDateTimePicker.pathResource + UIDateTimePicker.lang.toLowerCase() + ".js", false));
        if (!languages || (typeof (languages) != "object"))
          return;
        UIDateTimePicker.months = languages[0];
        UIDateTimePicker.weekdays = languages[1];
        UIDateTimePicker.tooltip = languages[2];
      } catch (e) {
      }
    },
  
    init : function(field, isDisplayTime, evt) {
      var event = evt || window.event;
      UIDateTimePicker.isDisplayTime = isDisplayTime;
      if (UIDateTimePicker.dateField) {
        UIDateTimePicker.dateField.parentNode.style.position = '';
      }
      UIDateTimePicker.dateField = field;
      UIDateTimePicker.getLang();
      if (!document.getElementById(UIDateTimePicker.calendarId)) {
        UIDateTimePicker.create();
      }
      // field.parentNode.style.position = 'relative' ;
      field.parentNode.insertBefore(document.getElementById(UIDateTimePicker.calendarId), field);
      UIDateTimePicker.show(event);
    },
  
    create : function() {
      var clndr = document.createElement("div");
      clndr.id = UIDateTimePicker.calendarId;
      clndr.style.position = "absolute";
      if (document.all) {
        clndr.innerHTML = "<div class='UICalendarComponent'><iframe id='" + UIDateTimePicker.calendarId
            + "IFrame' frameBorder='0' scrolling='no'></iframe><div style='position: absolute'></div></div>";
      } else {
        clndr.innerHTML = "<div class='UICalendarComponent'><div style='position: absolute; width: 100%;'></div></div>";
      }
      document.body.appendChild(clndr);
    },
  
    show : function(event) {
      utils.hideElements();
      var re = /^(\d{1,2}\/\d{1,2}\/\d{1,4})\s*(\s+\d{1,2}:\d{1,2}:\d{1,2})?$/i;
      UIDateTimePicker.selectedDate = new Date();
      if (re.test(UIDateTimePicker.dateField.value)) {
        var dateParts = UIDateTimePicker.dateField.value.split(" ");
        var arr = dateParts[0].split("/");
        UIDateTimePicker.selectedDate.setMonth(parseInt(arr[0], 10) - 1);
        UIDateTimePicker.selectedDate.setDate(parseInt(arr[1], 10));
        UIDateTimePicker.selectedDate.setFullYear(parseInt(arr[2], 10));
        if (dateParts.length > 1 && dateParts[dateParts.length - 1] != "") {
          arr = dateParts[dateParts.length - 1].split(":");
          UIDateTimePicker.selectedDate.setHours(arr[0], 10);
          UIDateTimePicker.selectedDate.setMinutes(arr[1], 10);
          UIDateTimePicker.selectedDate.setSeconds(arr[2], 10);
        }
      }
      var fieldDateTime = $(UIDateTimePicker.dateField.parentNode).find('input.DateTimeInput:first')[0];
      UIDateTimePicker.currentDate = new Date(UIDateTimePicker.selectedDate.valueOf());
      var clndr = document.getElementById(UIDateTimePicker.calendarId);
      clndr.firstChild.lastChild.innerHTML = UIDateTimePicker.renderCalendar();
      var x = 0;
      // var y = this.dateField.offsetHeight ;
      var y = fieldDateTime.offsetHeight;
      var Browser = eXo.core.Browser;
      if (Browser.isIE()) {
        x = -(Browser.findPosX(UIDateTimePicker.dateField) - Browser.findPosX(fieldDateTime) + UIDateTimePicker.dateField.offsetWidth);
      }
      
      var ct = $(clndr.firstChild).show().css('left', x + 'px').css('top', y + 2 + 'px');
     // alert($(clndr.firstChild).css('display'));  

      var jdrag = $('#BlockCaledar');
      var jcomponent = jdrag.parents('.UICalendarComponent');
      var jcalendar = jdrag.find('div.UICalendar:first');
      jdrag.on('mousedown', function(evt) {
        var event = evt || window.event;
        event.cancelBubble = true;
        jdrag.css('position', 'absolute');
        if (Browser.isIE7()) {
          jdrag.css('height', jcalendar.outerHeight(true) + 'px');
        }
        eXo.core.DragDrop.init(null, jdrag[0], component[0], event);
      });
      if (Browser.isIE6()) {
        $(clndr).find('iframe:first').css(height, jdrag.parent().outerHeight(true) + 'px');
      }
      $(clndr).find('.UICalendar:first').on('mousedown', function(evt) {
        var event = evt || window.event;
        utils.cancelEvent(event)
      });
      utils.addhideElement(clndr.firstChild);
      utils.cancelEvent(event);
    },
  
    hide : function() {
      if (UIDateTimePicker.dateField) {
        $(document.getElementById(UIDateTimePicker.calendarId).firstChild).hide();
        UIDateTimePicker.dateField = null;
      }
      utils.hideElements();
    },
  
    /* TODO: Move HTML code to a javascript template file (.jstmpl) */
    renderCalendar : function() {
      var dayOfMonth = 1;
      var validDay = 0;
      var startDayOfWeek = UIDateTimePicker.getDayOfWeek(UIDateTimePicker.currentDate.getFullYear(), UIDateTimePicker.currentDate.getMonth() + 1, dayOfMonth);
      var daysInMonth = UIDateTimePicker.getDaysInMonth(UIDateTimePicker.currentDate.getFullYear(), UIDateTimePicker.currentDate.getMonth());
      var clazz = null;
      var dayIdx = UIDateTimePicker.fistWeekDay;
      var table = '<div id="BlockCaledar" class="BlockCalendar">';
      table += '<div class="UICalendar">';
      table += '  <table class="MonthYearBox">';
      table += '    <tr>';
      table += '      <td class="MonthButton"><a class="PreviousMonth" title="' + UIDateTimePicker.tooltip[1] + '" href="javaScript: void(0);" onclick="eXo.forum.UIDateTimePicker.changeMonth(-1, event);"></a></td>';
      table += '      <td class="YearButton"><a class="PreviousYear" title="' + UIDateTimePicker.tooltip[0] + '" href="javascript: void(0);" onclick="eXo.forum.UIDateTimePicker.changeYear(-1, event);"></a></td>';
      table += '      <td><font color="#f89302">' + UIDateTimePicker.months[UIDateTimePicker.currentDate.getMonth()] + '</font> - ' + UIDateTimePicker.currentDate.getFullYear() + '</td>';
      table += '      <td class="YearButton"><a class="NextYear" title="' + UIDateTimePicker.tooltip[3] + '" href="javascript: void(0);" onclick="eXo.forum.UIDateTimePicker.changeYear(1, event);"></a></td>';
      table += '      <td class="MonthButton"><a class="NextMonth" title="' + UIDateTimePicker.tooltip[2] + '" href="javascript: void(0);" onclick="eXo.forum.UIDateTimePicker.changeMonth(1, event);"></a></td>';
      table += '    </tr>';
      table += '  </table>';
      table += '  <div style="margin-top: 6px;padding: 0px 5px;">';
      table += '    <table>';
      table += '      <tr>';
      for ( var i = 0; i < 7; i++) {
        if (dayIdx == 0) {
          table += '       <td><font color="red">' + UIDateTimePicker.weekdays[dayIdx] + '</font></td>';
        } else {
          table += '       <td><font>' + UIDateTimePicker.weekdays[dayIdx] + '</font></td>';
        }
        dayIdx = ++dayIdx % 7;
      }
      table += '      </tr>';
      table += '    </table>';
      table += '  </div>';
      table += '  <div class="CalendarGrid">';
      table += '  <table>';
      for ( var week = 0; week < 6; week++) {
        table += "<tr>";
        for ( var dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
          var currentWeekDay = (dayOfWeek + UIDateTimePicker.fistWeekDay) % 7;
          if (week == 0 && startDayOfWeek == currentWeekDay) {
            validDay = 1;
          } else if (validDay == 1 && dayOfMonth > daysInMonth) {
            validDay = 0;
          }
          if (validDay) {
            if (dayOfMonth == UIDateTimePicker.selectedDate.getDate() && UIDateTimePicker.currentDate.getFullYear() == UIDateTimePicker.selectedDate.getFullYear()
                && UIDateTimePicker.currentDate.getMonth() == UIDateTimePicker.selectedDate.getMonth()) {
              clazz = 'Current';
            } else if (dayOfWeek == 0 || dayOfWeek == 6) {
              clazz = 'Weekend';
            } else {
              clazz = 'Weekday';
            }
  
            table = table + "<td><a class='" + clazz + "' href=\"javascript:eXo.forum.UIDateTimePicker.setDate(" + UIDateTimePicker.currentDate.getFullYear() + "," + (UIDateTimePicker.currentDate.getMonth() + 1)
                + "," + dayOfMonth + ")\">" + dayOfMonth + "</a></td>";
            dayOfMonth++;
          } else {
            table = table + "<td class='empty'><div>&nbsp;</div></td>";
          }
        }
        table += "</tr>";
      }
      table += '    </table>';
      table += '  </div>';
      if (UIDateTimePicker.isDisplayTime) {
        table += '  <div class="CalendarTimeBox">';
        table += '    <div class="CalendarTimeBoxR">';
        table += '      <div class="CalendarTimeBoxM"><span><input class="InputTime" size="2" maxlength="2" value="'
            + ((UIDateTimePicker.currentDate.getHours()) > 9 ? UIDateTimePicker.currentDate.getHours() : "0" + UIDateTimePicker.currentDate.getHours())
            + '" onkeyup="eXo.forum.UIDateTimePicker.setHour(UIDateTimePicker)" >:<input size="2" class="InputTime" maxlength="2" value="'
            + ((UIDateTimePicker.currentDate.getMinutes()) > 9 ? UIDateTimePicker.currentDate.getMinutes() : "0" + UIDateTimePicker.currentDate.getMinutes())
            + '" onkeyup = "eXo.forum.UIDateTimePicker.setMinus(UIDateTimePicker)">:<input size="2" class="InputTime" maxlength="2" value="'
            + ((UIDateTimePicker.currentDate.getSeconds()) > 9 ? UIDateTimePicker.currentDate.getSeconds() : "0" + UIDateTimePicker.currentDate.getSeconds())
            + '" onkeyup = "eXo.forum.UIDateTimePicker.setSeconds(this)"></span></div>';
        table += '    </div>';
        table += '  </div>';
      }
      table += '</div>';
      table += '</div>';
      return table;
    },
  
    changeMonth : function(change, evt) {
      UIDateTimePicker.currentDate.setDate(1);
      UIDateTimePicker.currentDate.setMonth(UIDateTimePicker.currentDate.getMonth() + change);
      var clndr = document.getElementById(UIDateTimePicker.calendarId);
      clndr.firstChild.lastChild.innerHTML = UIDateTimePicker.renderCalendar();
      var event = evt || window.event;
      utils.cancelEvent(event);
    },
  
    changeYear : function(change, evt) {
      UIDateTimePicker.currentDate.setFullYear(UIDateTimePicker.currentDate.getFullYear() + change);
      UIDateTimePicker.currentDay = 0;
      var clndr = document.getElementById(UIDateTimePicker.calendarId);
      clndr.firstChild.lastChild.innerHTML = UIDateTimePicker.renderCalendar();
      var event = evt || window.event;
      utils.cancelEvent(event);
    },
  
    setDate : function(year, month, day) {
      if (UIDateTimePicker.dateField) {
        if (month < 10)
          month = "0" + month;
        if (day < 10)
          day = "0" + day;
        var dateString = month + "/" + day + "/" + year;
        if (!UIDateTimePicker.currentHours)
          UIDateTimePicker.currentHours = new Date().getHours();
        if (!UIDateTimePicker.currentMinutes)
          UIDateTimePicker.currentMinutes = new Date().getMinutes();
        if (!UIDateTimePicker.currentSeconds)
          UIDateTimePicker.currentSeconds = new Date().getSeconds();
        if (UIDateTimePicker.isDisplayTime)
          dateString += " " + UIDateTimePicker.currentHours + ":" + UIDateTimePicker.currentMinutes + ":" + UIDateTimePicker.currentSeconds;
        var objRoot = UIDateTimePicker.dateField.parentNode;
        $(objRoot).find('input.DateTimeInput:first').val(dateString);
        UIDateTimePicker.hide();
      }
      return;
    },
  
    setSeconds : function(object) {
      if (UIDateTimePicker.dateField) {
        var seconds = object.value;
        if (seconds >= 60) {
          object.value = seconds.substring(0, 1);
          return;
        }
        if (seconds.length < 2)
          seconds = "0" + seconds;
        var timeString = UIDateTimePicker.currentDate.getHours() + ":" + UIDateTimePicker.currentDate.getMinutes() + ":" + seconds;
        UIDateTimePicker.currentDate.setSeconds(seconds);
        if (!UIDateTimePicker.currentDay)
          UIDateTimePicker.currentDay = UIDateTimePicker.currentDate.getDay();
        if (!UIDateTimePicker.currentMonth)
          UIDateTimePicker.currentMonth = UIDateTimePicker.currentDate.getMonth() + 1;
        if (!UIDateTimePicker.currentYear)
          UIDateTimePicker.currentYear = UIDateTimePicker.currentDate.getFullYear();
        if (UIDateTimePicker.isDisplayTime)
          timeString = UIDateTimePicker.currentDay + "/" + UIDateTimePicker.currentMonth + "/" + UIDateTimePicker.currentYear + " " + timeString;
        UIDateTimePicker.dateField.value = timeString;
      }
      return;
    },
  
    setMinus : function(object) {
      if (UIDateTimePicker.dateField) {
        var minus = object.value;
        if (minus >= 60) {
          object.value = minus.substring(0, 1);
          return;
        }
        if (minus.length < 2)
          minus = "0" + minus;
        UIDateTimePicker.currentDate.setMinutes(minus);
        var timeString = UIDateTimePicker.currentDate.getHours() + ":" + minus + ":" + UIDateTimePicker.currentDate.getSeconds();
        if (!UIDateTimePicker.currentDay)
          UIDateTimePicker.currentDay = UIDateTimePicker.currentDate.getDay();
        if (!UIDateTimePicker.currentMonth)
          UIDateTimePicker.currentMonth = UIDateTimePicker.currentDate.getMonth() + 1;
        if (!UIDateTimePicker.currentYear)
          UIDateTimePicker.currentYear = UIDateTimePicker.currentDate.getFullYear();
        if (UIDateTimePicker.isDisplayTime)
          timeString = UIDateTimePicker.currentDay + "/" + UIDateTimePicker.currentMonth + "/" + UIDateTimePicker.currentYear + " " + timeString;
        UIDateTimePicker.dateField.value = timeString;
      }
      return;
    },
  
    setHour : function(object) {
      if (UIDateTimePicker.dateField) {
        var hour = object.value;
        if (hour >= 24) {
          object.value = hour.substring(0, 1);
          return;
        }
        if (hour.length < 2)
          hour = "0" + hour;
        UIDateTimePicker.currentDate.setHours(hour);
        var timeString = hour + ":" + UIDateTimePicker.currentDate.getMinutes() + ":" + UIDateTimePicker.currentDate.getSeconds();
        if (!UIDateTimePicker.currentDay)
          UIDateTimePicker.currentDay = UIDateTimePicker.currentDate.getDay();
        if (!UIDateTimePicker.currentMonth)
          UIDateTimePicker.currentMonth = UIDateTimePicker.currentDate.getMonth() + 1;
        if (!UIDateTimePicker.currentYear)
          UIDateTimePicker.currentYear = UIDateTimePicker.currentDate.getFullYear();
        if (UIDateTimePicker.isDisplayTime)
          timeString = UIDateTimePicker.currentDay + "/" + UIDateTimePicker.currentMonth + "/" + UIDateTimePicker.currentYear + " " + timeString;
        UIDateTimePicker.dateField.value = timeString;
      }
      return;
    },
  
    clearDate : function() {
      UIDateTimePicker.dateField.value = '';
      UIDateTimePicker.hide();
    },
  
    getDayOfWeek : function(year, month, day) {
      var date = new Date(year, month - 1, day);
      return date.getDay();
    },
  
    getDaysInMonth : function(year, month) {
      return [ 31, ((!(year % 4) && ((year % 100) || !(year % 400))) ? 29 : 28), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 ][month];
    }
  };

  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.UIDateTimePicker = UIDateTimePicker;
  module.UIDateTimePicker = window.eXo.forum.UIDateTimePicker;
  
})(module, utils, gj, document, window);
