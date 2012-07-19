;(function($, window, document) {
  var SearchIpBan = {
    searchIpBan : null,
    JUIGrid : null,
    data : null,
    jTabContent: null,
    request: null,

    init : function(userName) {
      SearchIpBan.jSearchIpBan = $('#searchIpBan');
      if (SearchIpBan.jSearchIpBan.exists()) {
        SearchIpBan.jTabContent = SearchIpBan.jSearchIpBan.parent('.UITabContent');
        SearchIpBan.JUIGrid = SearchIpBan.jTabContent.find('table.UIGrid');
        SearchIpBan.jSearchIpBan.on('keydown', SearchIpBan.searchIpBanWrapper);
      }
    },
    
    searchIpBanWrapper : function(event) {
      window.setTimeout(SearchIpBan.searchIpBanTimeout, 50);
    },
    
    searchIpBanTimeout : function() {
      SearchIpBan.searchIpBan(SearchIpBan.jSearchIpBan.val());
    },
    
    searchIpBan : function(keyword) {
      // Get data from service, url: /portal/rest/ks/forum/filter/{strIP}/
      keyword = keyword || 'all';
      var restPath = SearchIpBan.jTabContent.attr("restPath");
      var url = restPath + '/ks/forum/filter/' + keyword + '/';
      var forumId = SearchIpBan.jTabContent.attr("forumId");
      if (forumId != 'null') {
        url = restPath + '/ks/forum/filterIpBanforum/' + forumId + '/' + keyword + '/';
      }
      SearchIpBan.url_ = url;
      SearchIpBan.request = $.getJSON(url);
      setTimeout(SearchIpBan.processing, 200);
    },
    processing : function() {
      var txt = String(SearchIpBan.request.responseText);
      if (txt != 'undefined' && txt.trim().length > 0) {
        SearchIpBan.data = eXo.core.JSON.parse(txt);
        if (SearchIpBan.data.jsonList) {
          SearchIpBan.updateIpBanList();
        }
      }
    },
    updateIpBanList : function() {
      // Remove all old items
      SearchIpBan.JUIGrid.find('tr.IpBanItem').remove();

      // Fill up with new list
      var tBodyNode = SearchIpBan.JUIGrid.find('tbody');
      var length_ = SearchIpBan.data.jsonList.length;
      var pageIter = $('#IpBanPageIterator');
      if (pageIter.exists()) {
        pageIter.hide();
        var url = String(SearchIpBan.url_);
        if (url.indexOf('all') > 0) {
          if (length_ >= 8) {
            length_ = 8;
            pageIter.show();
          }
        }
      }
      for ( var i = 0; i < length_; i++) {
        tBodyNode.append(SearchIpBan.buildIpBanItemNode(SearchIpBan.data.jsonList[i].ip));
      }
    },
    
    buildIpBanItemNode : function(ip) {
      var JipBanItem = $('<tr></tr>');
      JipBanItem.css('background', '#ffffff');
      JipBanItem.addClass('IpBanItem');
      var JfieldLabel = $('<td></td>');
      JfieldLabel.addClass('FieldLabel');
      JfieldLabel.html(ip);
      JipBanItem.append(JfieldLabel[0].cloneNode(true));
      
      JfieldLabel.attr('align', 'center');
      var link = SearchIpBan.jTabContent.attr('link');
      link = String(link).replace('OBJIP', ip);
      var link2 = String(link).replace('OpenPosts', 'UnBan');
      JfieldLabel.html('[<a href="' + link + '">Posts</a>]&nbsp;[<a style="color: red;" href="' + link2 + '">X</a>]');
      JipBanItem.append(JfieldLabel);
      return JipBanItem;
    }
  };
  
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.webservice = window.eXo.forum.webservice || {};
  window.eXo.forum.webservice.SearchIpBan = SearchIpBan;
})(gj, window, document);
