(function(utils, gj) {
  var SearchTagName = {
    key : {
      ENTER : 13,
      UP : 38,
      DOWN : 40,
      SPACE : 32,
      BACK : 8
    },
    jInputSearch : null,
    data : null,
    jparent : null,
    jcontainer : null,
    request : null,
    data : {},
    lastkey : '',
    init : function() {
      var jparent = gj('#AddTagId');
      if (jparent.exists()) {
        SearchTagName.jparent = jparent;
        SearchTagName.jcontainer = jparent.findId('#searchTagName');
        SearchTagName.jcontainer.hide();
        SearchTagName.jInputSearch = jparent.findId(SearchTagName.jcontainer
            .attr('inputId'));
        SearchTagName.lastkey = '';
        if (SearchTagName.jInputSearch.exists()) {
          SearchTagName.jInputSearch.val('');
          SearchTagName.jInputSearch.on('keydown',
              SearchTagName.searchTagNameWrapper);
          SearchTagName.jInputSearch.on('click', SearchTagName.submitInput);
          var buttonSearch = gj('#ButtonSearch');
          if (buttonSearch.exists()) {
            buttonSearch.on('click', SearchTagName.submitInput);
          }
        }
      }
    },
    submitInput : function(event) {
      var str = String(SearchTagName.jInputSearch.val());
      if (SearchTagName.jcontainer.css('visibility') === 'hidden'
          && str.trim().length === 0) {
        SearchTagName.searchTagName('onclickForm');
      }
    },
    searchTagNameWrapper : function(event) {
      var key = utils.getKeynum(event);
      var KEY = SearchTagName.key;
      if (key == KEY.ENTER) {
        var str = String(SearchTagName.jInputSearch.val()).trim();
        if (SearchTagName.jcontainer.css('visibility') === 'visible') {
          SearchTagName.jInputSearch[0].focus();
          SearchTagName.jcontainer.hide();
          SearchTagName.searchTagName(' ');
        } else if (str.length > 0) {
          eval(String(SearchTagName.jcontainer.attr('linkSubmit')).replace(
              'javascript:', ''));
        }
        return;
      }
      if (key == KEY.UP || key == KEY.DOWN) {
        var items = SearchTagName.jparent.find('div.TagNameItem');
        if (items.exists()) {
          var itemSl = SearchTagName.jparent.find('div.Selected:first');
          if (itemSl.exists()) {
            var t = items.length;
            for ( var i = 0; i < t; i++) {
              if (items.eq(i)[0] === itemSl[0]) {
                itemSl.removeClass('Selected');
                if (i == 0 && key == KEY.UP) {
                  SearchTagName.setValueInput(items.eq(t - 1));
                } else if (i == (t - 1) && key == KEY.DOWN) {
                  SearchTagName.setValueInput(items.eq(0));
                } else if (key == KEY.UP) {
                  SearchTagName.setValueInput(items.eq(i - 1));
                } else if (key == KEY.DOWN) {
                  SearchTagName.setValueInput(items.eq(i + 1));
                }
              }
            }
          } else {
            SearchTagName.setValueInput(items.eq(0));
          }
        }
      } else if (key > KEY.DOWN || key == KEY.BACK || key == KEY.SPACE) {
        var str = String(SearchTagName.jInputSearch.val());
        if ((key == KEY.BACK || key == KEY.SPACE)
            && (str.trim().length == 0 || str.length == 1)) {
          SearchTagName.searchTagName('onclickForm');
        } else {
          window.setTimeout(SearchTagName.searchTagNameTimeout, 100);
        }
      }
    },
    setValueInput : function(elm) {
      elm.addClass('Selected');
      var str = String(SearchTagName.jInputSearch.val());
      str = str.substring(0, str.lastIndexOf(" "));
      var value = String(elm.html());
      value = value.substring(0, value.indexOf(" "));
      if (str.length == 0)
        str = value;
      else
        str = str + " " + value;
      SearchTagName.jInputSearch.val(str);
    },
    searchTagNameTimeout : function() {
      SearchTagName.searchTagName(SearchTagName.jInputSearch.val());
    },
    searchTagName : function(keyword) {
      // Get data from service, url: /ks/forum/filterTagNameForum/{strTagName}/
      keyword = String(keyword);
      var strs = keyword.split(" ");
      if (strs.length >= 1)
        keyword = strs[strs.length - 1];
      keyword = keyword || '';
      if (keyword === SearchTagName.lastkey) {
        return;
      }
      if (keyword.trim().length > 0) {
        var userAndTopicId = SearchTagName.jcontainer.attr("userAndTopicId");
        var restPath = SearchTagName.jcontainer.attr("restPath");
        if (userAndTopicId) {
          var url = restPath + '/ks/forum/filterTagNameForum/' + userAndTopicId
              + '/' + keyword + '/';
          SearchTagName.request = gj.getJSON(url);
          setTimeout(SearchTagName.processing, 200);
          SearchTagName.lastkey = keyword;
        }
      } else {
        SearchTagName.jcontainer.hide();
        SearchTagName.lastkey = '';
        SearchTagName.jcontainer.css('visibility', 'hidden');
      }
    },
    processing : function() {
      var txt = String(SearchTagName.request.responseText);
      if (txt != 'undefined' && txt.trim().length > 0) {
        SearchTagName.data = gj.parseJSON(txt);
        if (SearchTagName.data.jsonList) {
          SearchTagName.updateIpBanList();
        }
      }
    },
    updateIpBanList : function() {
      // Remove all old items
      SearchTagName.jcontainer.find('.TagNameItem').remove();
      // Fill up with new list
      var t = 0;
      var length_ = SearchTagName.data.jsonList.length;
      for ( var i = 0; i < length_; i++) {
        SearchTagName.jcontainer.append(SearchTagName
            .buildItemNode(SearchTagName.data.jsonList[i].ip));
        t = 1;
      }
      if (t == 1) {
        SearchTagName.jcontainer.css('visibility', 'visible');
        SearchTagName.jcontainer.show(300);
      } else {
        SearchTagName.jcontainer.hide(300);
        SearchTagName.jcontainer.css('visibility', 'hidden');
      }
    },
    buildItemNode : function(ip) {
      var itemNode = gj('<div></div>').addClass('TagNameItem').html(ip);
      itemNode.on('click', function(event) {
        var vl = ip.substring(0, ip.indexOf(' '));
        var str = String(SearchTagName.jInputSearch.val());
        str = str.substring(0, str.lastIndexOf(' '))
        if (str.length == 0)
          str = vl;
        else
          str += ' ' + vl;
        SearchTagName.jInputSearch.val(str);
        SearchTagName.jInputSearch.focus();
        SearchTagName.jcontainer.hide();
        SearchTagName.searchTagName(' ');
      });
      itemNode.on('mouseover', SearchTagName.mouseOveEvent);
      itemNode.on('focus', SearchTagName.mouseOveEvent);
      itemNode.on('mouseout', SearchTagName.mouseOutEvent);
      itemNode.on('blur', SearchTagName.mouseOutEvent);
      return itemNode;
    },
    mouseOveEvent : function() {
      if (gj(this).hasClass('Selected')) {
        gj(this).attr('class', 'TagNameItem OverItem Slect');
      } else {
        gj(this).attr('class', 'TagNameItem OverItem');
      }
    },
    mouseOutEvent : function() {
      if (gj(this).hasClass('Slect')) {
        gj(this).attr('class', 'TagNameItem Selected');
      } else {
        gj(this).attr('class', 'TagNameItem');
      }
    }
  };

  return SearchTagName;
})(utils, gj);
