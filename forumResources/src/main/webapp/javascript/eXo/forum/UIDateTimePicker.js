(function(utils, $, document, window) {
  var UIDateTimePicker = {
    calendarId : "UICalendarControl",
    container : null,
    input : null,
    currentDate : null, // Datetime value base of selectedDate for
    selectedDate : null, // Datetime value of input date&time field
    months : [ 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December' ],
    weekdays : [ 'S', 'M', 'T', 'W', 'T', 'F', 'S' ],
    tooltip : [ 'Previous Year', 'Previous Month', 'Next Month', 'Next Year' ],
    pathResource : "/forumResources/javascript/eXo/forum/lang/",
    lang : "",
    fistWeekDay : 0, // sunday: 0, monday: 1, tuesday: 2, wednesday: 3, thursday: 4, friday: 5, saturday: 6
  
    getLang : function() {
      try {
        var day = UIDateTimePicker.dataInfo.attr('data-fistweekday');
        if (day) {
          UIDateTimePicker.fistWeekDay = day * 1 - 1;
        }

        var lang = UIDateTimePicker.dataInfo.attr('data-lang');
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
  
    init : function(fieldId) {
      var container = $('#'+fieldId);
      UIDateTimePicker.container = container;
      UIDateTimePicker.input = container.find('input.dateTimeInput');
      UIDateTimePicker.input.attr('placeholder', 'mm/dd/yyyy');
      UIDateTimePicker.dataInfo = container.find('div.dataInfo');
      UIDateTimePicker.getLang();
      
      UIDateTimePicker.input.on('click', function(evt) {
        UIDateTimePicker.input = $(this);
        UIDateTimePicker.container = $('#DateTime'+ UIDateTimePicker.input.attr('id'));
        var popup = UIDateTimePicker.container.find('div.uiCalendarComponent:first');
        if (popup.length <= 0) {
          UIDateTimePicker.container.append($('<div class="uiCalendarComponent uiBox"></div>'));
          popup = UIDateTimePicker.container.find('div.uiCalendarComponent:first');
          popup.css({'position': 'absolute', 'z-index' : 10});
        }
        UIDateTimePicker.show(popup, evt);
      });
    },
  
    show : function(popup, event) {
      utils.hideElements();
      var re = /^(\d{1,2}\/\d{1,2}\/\d{1,4})\s*(\s+\d{1,2}:\d{1,2}:\d{1,2})?$/i;
      UIDateTimePicker.selectedDate = new Date();
      if (re.test(UIDateTimePicker.input.val()) === true) {
        var dateParts = UIDateTimePicker.input.val().split(" ");
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
      UIDateTimePicker.currentDate = new Date(UIDateTimePicker.selectedDate.valueOf());
      popup.html(UIDateTimePicker.renderCalendar());
      popup.show();
      utils.addhideElement(popup[0]);
      utils.cancelEvent(event);
    },
  
    hide : function() {
      var popup = UIDateTimePicker.container.find('div.uiCalendarComponent');
      if (popup.length > 0) {
        popup.hide();
      }
      utils.hideElements();
    },

    renderCalendar : function() {
      var dayOfMonth = 1;
      var validDay = 0;
      var startDayOfWeek = UIDateTimePicker.getDayOfWeek(UIDateTimePicker.currentDate.getFullYear(), UIDateTimePicker.currentDate.getMonth() + 1, dayOfMonth);
      var maxDayInMonth = UIDateTimePicker.getDaysInMonth(UIDateTimePicker.currentDate.getFullYear(), UIDateTimePicker.currentDate.getMonth());
      var clazz = null;
      var dayIdx = UIDateTimePicker.fistWeekDay;
      var table = '';
      table += '<div class="uiCalendar" id="BlockCaledar" onmousedown="event.cancelBubble = true">';
      table += '  <h5 class="title clearfix">';
      table += '    <a data-placement="right" rel="tooltip" onclick="eXo.forum.UIDateTimePicker.changeMonth(-1, event);" class="actionIcon pull-left" data-original-title="'+ UIDateTimePicker.tooltip[1]+ '"><i class="uiIconMiniArrowLeft uiIconLightGray"></i></a>';
      table += '    <span>'+ UIDateTimePicker.months[UIDateTimePicker.currentDate.getMonth()] +', '+ UIDateTimePicker.currentDate.getFullYear() + '</span>';
      table += '    <a data-placement="right" rel="tooltip" onclick="eXo.forum.UIDateTimePicker.changeMonth(1, event);" class="actionIcon pull-right" data-original-title="'+ UIDateTimePicker.tooltip[2]+ '"><i class="uiIconMiniArrowRight uiIconLightGray"></i></a>';
      table += '  </h5>';
      
      table += '  <table class="weekList">';
      table += '    <tr>';  
      for ( var i = 0; i < 7; i++) {
        if (dayIdx == 0) {
          table += '     <td><font color="red">' + UIDateTimePicker.weekdays[dayIdx] + '</font></td>';
        } else {
          table += '     <td>' + UIDateTimePicker.weekdays[dayIdx] + '</td>';
        }
        dayIdx = ++dayIdx % 7;
      }
      table += '    </tr>';
      table += '  </table>';
      table += '  <hr/>';

      var _pyear, _pmonth, _pday, _nyear, _nmonth, _nday, _weekend;
      var _today = new Date();
      var tableRow='';
      if(startDayOfWeek==0) startDayOfWeek = 7;
      _pyear = (UIDateTimePicker.currentDate.getMonth() == 0) ? UIDateTimePicker.currentDate.getFullYear() - 1 : UIDateTimePicker.currentDate.getFullYear();
      _pmonth = (UIDateTimePicker.currentDate.getMonth() == 0) ? 11 : UIDateTimePicker.currentDate.getMonth() - 1;
      _pday = UIDateTimePicker.getDaysInMonth(_pyear, _pmonth) - (startDayOfWeek % 7) + 1;
      
      _nmonth = (UIDateTimePicker.currentDate.getMonth() == 11) ? 0 : UIDateTimePicker.currentDate.getMonth() + 1;
      _nyear = (UIDateTimePicker.currentDate.getMonth() == 11) ? UIDateTimePicker.currentDate.getFullYear() + 1 : UIDateTimePicker.currentDate.getFullYear();
      _nday = 1;
    
      table += '  <table class="weekDays">';

      for ( var week = 0; week < 6; week++) {
        table += "    <tr classCurrentWeek" + week + ">";
        validDay = 1;
        for ( var dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
          var currentWeekDay = (dayOfWeek + UIDateTimePicker.fistWeekDay) % 7;
          if (dayOfMonth > maxDayInMonth) {
            validDay = 0;
          }

          if (validDay) {
            if (dayOfMonth == UIDateTimePicker.selectedDate.getDate() && 
                UIDateTimePicker.currentDate.getFullYear() == UIDateTimePicker.selectedDate.getFullYear() &&
                UIDateTimePicker.currentDate.getMonth() == UIDateTimePicker.selectedDate.getMonth()) {
              clazz = 'highLight today';
              table = table.replace("classCurrentWeek" + week, 'class="currentWeek"');
            } else if (dayOfWeek == 0 || dayOfWeek == 6) {
              clazz = 'Weekend';
            } else {
              clazz = 'Weekday';
            }
  
            table += '    <td><a href="#SelectDate" class="' + clazz + '" onclick="eXo.forum.UIDateTimePicker.setDate(' + UIDateTimePicker.currentDate.getFullYear() + ',' + (UIDateTimePicker.currentDate.getMonth() + 1)
                + ',' + dayOfMonth + ')">' + dayOfMonth + '</a></td>';
            dayOfMonth++;
            _weekend = week;
          } else if(validDay == 0 && week == 0) {
            
            table += '      <td><a href="#SelectDate" class="otherMonth" onclick="eXo.forum.UIDateTimePicker.setDate('
                      + _pyear + ','
                      + (_pmonth + 1) + ',' + _pday + ')">'
                      + _pday + '</a></td>';
            _pday++;
          } else if(validDay == 0 && week != 0 && _weekend == week ){
            table += '      <td><a href="#SelectDate" class="otherMonth" onclick="eXo.forum.UIDateTimePicker.setDate('
                    + _nyear + ','
                    + (_nmonth + 1) + ',' + _nday + ')">'
                    + _nday + '</a></td>';
            _nday++;
          }
        }
        table = table.replace('classCurrentWeek' + week, '') + '    </tr>';
      }
      table += '  </table>';
      
      
      table += '</div>';
      return table;
    },
  
    changeMonth : function(change, evt) {
      UIDateTimePicker.currentDate.setDate(1);
      UIDateTimePicker.currentDate.setMonth(UIDateTimePicker.currentDate.getMonth() + change);
      var popup = UIDateTimePicker.container.find('div.uiCalendarComponent');
      popup.html(UIDateTimePicker.renderCalendar())
      var event = evt || window.event;
      utils.cancelEvent(event);
    },
  
    changeYear : function(change, evt) {
      UIDateTimePicker.currentDate.setFullYear(UIDateTimePicker.currentDate.getFullYear() + change);
      UIDateTimePicker.currentDay = 0;
      var popup = UIDateTimePicker.container.find('div.uiCalendarComponent');
      popup.html(UIDateTimePicker.renderCalendar())
      var event = evt || window.event;
      utils.cancelEvent(event);
    },
  
    setDate : function(year, month, day) {
      if (UIDateTimePicker.input.length > 0) {
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
        
        UIDateTimePicker.input.val(dateString);
        UIDateTimePicker.hide();
      }
      return;
    },
  
    setSeconds : function(object) {
      if (UIDateTimePicker.input.length > 0) {
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
        
        UIDateTimePicker.input.val(timeString);
      }
      return;
    },
  
    setMinus : function(object) {
      if (UIDateTimePicker.input.length > 0) {
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
        
        UIDateTimePicker.input.val(timeString);
      }
      return;
    },
  
    setHour : function(object) {
      if (UIDateTimePicker.input.length > 0) {
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
        
        UIDateTimePicker.input.val(timeString);
      }
      return;
    },
  
    clearDate : function() {
      UIDateTimePicker.input.val('');
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
  return UIDateTimePicker;
  
})(utils, gj, document, window);
