(function(utils, $) {
  var SearchIpBan = {
    searchIpBan : null,
    JUIGrid : null,
    data : null,
    jTabContent : null,
    request : null,
    
    init : function(containerId) {
      SearchIpBan.jSearchIpBan = $.fn.findId(containerId);
      if (SearchIpBan.jSearchIpBan.exists()) {
        SearchIpBan.jTabContent = SearchIpBan.jSearchIpBan.find('div.restInfo:first');
        SearchIpBan.JUIGrid = SearchIpBan.jTabContent.find('table.table-data-viewer');
        SearchIpBan.jTabContent.find('input[name=searchIpBan]').on('keyup', SearchIpBan.searchIpBan);
      }
    },
    
    searchIpBan : function() {
      // Get data from service, url: /portal/rest/ks/forum/filter/{strIP}/
      var keyword = $.trim($(this).val());
      if(keyword.length == 0) keyword = 'all';
      var restPath = SearchIpBan.jTabContent.attr("data-restPath");
      var restUrl = restPath + '/ks/forum/filter/' + keyword + '/';
      var forumId = SearchIpBan.jTabContent.attr("data-forumId");
      if (forumId != 'null') {
        restUrl = restPath + '/ks/forum/filterIpBanforum/' + forumId + '/' + keyword + '/';
      }
      SearchIpBan.url_ = restUrl;
      $.ajax({
        type : "GET",
        url : restUrl
      }).complete(function(jqXHR) {
        if (jqXHR.readyState === 4) {
          SearchIpBan.data = $.parseJSON(jqXHR.responseText);
          if (SearchIpBan.data.jsonList) {
            SearchIpBan.updateIpBanList();
          }
        }
      });
    },

    updateIpBanList : function() {
      // Remove all old items
      var tBodyNode = SearchIpBan.JUIGrid.find('tbody');
      tBodyNode.find('tr').remove();
      
      // Fill up with new list
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
        tBodyNode.append(SearchIpBan.buildIpBanItemNode(SearchIpBan.data.jsonList[i].ip, i));
      }
    },
    
    buildIpBanItemNode : function(ip, index) {
      var JTrItem = SearchIpBan.JUIGrid.find('tfoot').find('tr').clone();
      JTrItem.find('td:first').html(ip);
      //
      var aView = JTrItem.find('a.view');
      aView.attr('href', aView.attr('href').replace('IPBANNED', ip));
      //
      var aDelete = JTrItem.find('a.delete');
      aDelete.attr('href', aDelete.attr('href').replace('IPBANNED', ip));
      aDelete.attr('id', 'Confirm'+index);
      var settings = {isMulti: false, message : ''};
      aDelete.confirmation(settings);
      //
      JTrItem.find('a').tooltip();
      JTrItem.show();
      return JTrItem;
    }
  };

  return SearchIpBan;
})(utils, gj);
