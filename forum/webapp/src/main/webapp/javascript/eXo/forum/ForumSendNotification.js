(function(Cometd, $, document, window) {
  
  var ForumSendNotification = {
      i18n : {
          notification : "Notification",
          message : "message",
          post : "post",
          privatePost : "You have received a new private post",
          privateMessage : "You have received a new private message",
          from : "From",
          briefContent : "Brief content",
          goDirectly : "Go directly to the {0}",
          clickHere : "Click here.",
          title : "Title",
      },
      portletId : "UIForumPortlet",
      currentUser : "",
      postLink :"/portal/intranet/forum/topic/topicID"
  };

  ForumSendNotification.initParam = function(portletId, postLink, i18n) {
    ForumSendNotification.i18n = $.extend(true, {}, ForumSendNotification.i18n, i18n);
    ForumSendNotification.portletId = portletId || ForumSendNotification.portletId;
    ForumSendNotification.postLink = postLink || ForumSendNotification.postLink;
    ForumSendNotification.currentUser = $.trim(eXo.core.Browser.getCookie('forumCurrentUserId') || '');
  };

  ForumSendNotification.initCometd = function(eXoUser, eXoToken, contextName) {
    if (String(eXoToken) != '') {
      if (Cometd.isConnected() === false) {
        if (ForumSendNotification.currentUser !== eXoUser || ForumSendNotification.currentUser === '') {
          ForumSendNotification.currentUser = eXoUser;
          document.cookie = 'forumCurrentUserId=' + escape(eXoUser) + ';path=/portal';
          Cometd._connecting = false;
          Cometd.currentTransport = null;
          Cometd.clientId = null;
        }
        Cometd.url = '/' + contextName + '/cometd';
        Cometd.exoId = eXoUser;
        Cometd.exoToken = eXoToken;
        Cometd.addOnConnectionReadyCallback(ForumSendNotification.subcribeCometdSendNotification);
        Cometd.init(Cometd.url);
      } else {
        ForumSendNotification.subcribeCometdSendNotification();
      }
    }
  };

  ForumSendNotification.subcribeCometdSendNotification = function() {
    Cometd.subscribe('/eXo/Application/Forum/NotificationMessage', function(eventObj) {
      ForumSendNotification.createMessage(JSON.parse(eventObj.data))
    });
  };
  
  ForumSendNotification.getPlainText = function(str) {
    return $.trim($('<span></span>').html(str).text().replace(/</gi, '&lt;').replace(/>/gi, '&gt;'));
  };
  
  ForumSendNotification.buildLink = function(type, alink) {
    var link = ForumSendNotification.i18n.goDirectly.replace('{0}', type);
    link = link + ': <a style="color:#204AA0" href="' + alink + '">' + ForumSendNotification.i18n.clickHere + '</a>';
    return link;
  };

  ForumSendNotification.createMessage = function(message) {
    var component = ForumSendNotification;
    var i18n = component.i18n;
    var jPortlet = $.fn.findId(component.portletId);
    var msgBox = jPortlet.find('.uiNotification:first').clone();
    if(msgBox.exists()) {
      var name = msgBox.find('.name:first');
      name.html(name.html().replace('Message', i18n.notification));
      msgBox.find('.closePopup:first').on('click', component.closeBox);
      //
      if(message.type === 'PrivatePost') {
        var link = component.postLink.replace('topicID', String(message.id));
        msgBox.find('.nameMessage:first').html('<strong>' + i18n.privatePost + '</strong>');
        msgBox.find('.link:first').html(component.buildLink(i18n.post, link));
      } else {
        msgBox.find('.nameMessage:first').html('<strong>' + i18n.privateMessage + '</strong>');
        var alink = $('#privateMessageLink');
        if (alink.exists()) {
          msgBox.find('.link:first').html(component.buildLink(i18n.message, alink.attr('href')));
        }
      }
      //
      msgBox.find('.from:first').html('<strong>' + i18n.from + ':</strong> ' + message.from);
      msgBox.find('.title:first').html('<strong>' + i18n.title + ':</strong> ' + component.getPlainText(message.name).replace(/Reply:/g, ''));
      //
      var cont = component.getPlainText(message.message);
      msgBox.find('.content:first').html('<strong>' + i18n.briefContent + ':</strong> ' + cont);
      msgBox.find('.link:first').find('a').on('mouseup', component.closeBox);
      //
      var info = component.getInfo();
      msgBox.css({'width': '300px', 'top': info.top + 'px', 'right' : ((info.left > 5) ? info.left : 10) + 'px', 'z-index' : 1000});
      var container = $('<div class="uiForumPortlet forumBoxNotification"></div>')
      container.css({'width': '1px', 'height' : '1px', 'z-index' : 10})
      container.attr('data-time', new Date().getTime());
      container.append(msgBox);
      $(document.body).append(container);
      msgBox.show('linear');
      //
      setTimeout(component.closeTimeBox, 15001);
      var reloadLink = $('#Reload');
      eval(String(reloadLink.attr('href')).replace('javascript:', '')); 
    }
  };

  ForumSendNotification.getInfo = function() {
    var info = {top:0, left:0};
    var jPortlet = $.fn.findId(ForumSendNotification.portletId);
    var actionBar = jPortlet.find('.uiForumActionBar:first');
    var left = $(window).width() - (jPortlet.offset().left + jPortlet.outerWidth() - 20);
    var top = actionBar.offset().top + actionBar.height() + 14;
    var containers = $(document.body).find('.forumBoxNotification');
    if(containers.exists()) {
      $.each(containers, function(index, item) {
        top += $(item).find('.uiNotification:first').outerHeight();
      });
    }
    info.left = left;
    info.top = top;
    return info;
  };

  ForumSendNotification.closeBox = function(e) {
    if(e && e.type) {
      e.stopPropagation();
    }
    ForumSendNotification.hideBox($(this).parents('.forumBoxNotification:first'));
  };

  ForumSendNotification.hideBox = function(container) {
    if(container.exists()) {
      container.find('.uiNotification').css('overflow', 'hidden')
      .animate({height: '0px'}, 300, function() {container.remove();});
    }
  };
  
  ForumSendNotification.closeTimeBox = function() {
    var containers = $(document.body).find('.forumBoxNotification');
    if(containers.exists()) {
      var time = new Date().getTime();
      $.each(containers, function(index, item) {
         var dataTime = $(item).data('time')*1 + 15000;
         if(dataTime <= time) {
           ForumSendNotification.hideBox($(item));
         }
      });
    }
  };
  return ForumSendNotification;

})(cometd, gj, document, window);
