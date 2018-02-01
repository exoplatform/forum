(function(utils, $) {
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
    init : function(id) {
      var component = $('#' + id);
      var jparent = component.find('.addTagContainer');
      if (jparent.exists()) {
        SearchTagName.jparent = jparent;
        SearchTagName.jcontainer = jparent.find('.searchTagName:first');
        SearchTagName.jcontainer.hide();
        SearchTagName.jInputSearch = jparent.find('input#'+SearchTagName.jcontainer.attr('data-inputid'));
        SearchTagName.lastkey = '';
        if (SearchTagName.jInputSearch.exists()) {
          SearchTagName.jInputSearch.val('');
          SearchTagName.jInputSearch.on('keyup', SearchTagName.onKeyControl);
          SearchTagName.jInputSearch.on('click', SearchTagName.openAddTag);
          var tagAction = component.find('.addTagAction');
          if (tagAction.exists()) {
            tagAction.on('click', function(e)  {
              SearchTagName.openAddTag(e);
              SearchTagName.jInputSearch.focus();
            });
          }
        }
      }
    },
    openAddTag : function(event) {
      var str = String(SearchTagName.jInputSearch.val());
      if (SearchTagName.jcontainer.css('display') === 'none' && $.trim(str).length === 0) {
        SearchTagName.searchTagName('onclickForm');
      }
    },
    onKeyControl : function(event) {
      var key = utils.getKeynum(event);
      var KEY = SearchTagName.key;
      if (key == KEY.ENTER) {
        var str = $.trim(String(SearchTagName.jInputSearch.val()));
        if (SearchTagName.jcontainer.css('display') === 'none') {
          SearchTagName.jcontainer.hide();
          SearchTagName.searchTagName(' ');
        } else if (str.length > 0) {
          eval(String(SearchTagName.jcontainer.attr('data-linksubmit')).replace('javascript:', ''));
        }
        return;
      }
      if (key == KEY.UP || key == KEY.DOWN) {
        var ul = SearchTagName.jparent.find('ul:first');
        var items = ul.find('li');
        if (items.exists()) {
          var itemSl = ul.find('li.selected:first');
          if (itemSl.exists()) {
            var t = items.length;
            for ( var i = 0; i < t; i++) {
              if (items.eq(i)[0] === itemSl[0]) {
                itemSl.removeClass('selected');
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
        if ((key == KEY.BACK && $.trim(str).length == 0) || key == KEY.SPACE) {
          SearchTagName.searchTagName('onclickForm');
        } else {
          var val = $.trim(String(SearchTagName.jInputSearch.val()));
          if(val.indexOf(' ') > 0 ) {
            val = val.substring(val.indexOf(' '));
          }
          SearchTagName.searchTagName(val);
        }
      }
    },

    setValueInput : function(elm) {
      elm.addClass('selected');
      var str = String(SearchTagName.jInputSearch.val());
      str = str.substring(0, str.lastIndexOf(" "));
      var value = String(elm.text());
      value = value.substring(0, value.indexOf(" "));
      if (str.length == 0)
        str = value;
      else
        str = str + " " + value;
      SearchTagName.jInputSearch.val(str);
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
      if ($.trim(keyword).length > 0) {
        var userAndTopicId = SearchTagName.jcontainer.attr("data-userandtopicid");
        var restPath = SearchTagName.jcontainer.attr("data-restpath");
        if (userAndTopicId) {
          var restUrl = restPath + '/ks/forum/filterTagNameForum/' + userAndTopicId + '/' + keyword ;
          $.ajax({
            type: "GET",
            url: restUrl,
            complete : function (jqXHR) {
              if (jqXHR.readyState === 4) {
                SearchTagName.data = $.parseJSON(jqXHR.responseText);
                if (SearchTagName.data.jsonList) {
                  SearchTagName.updateTagList();
                }
              }
            }
          });
      }
      }else {
        SearchTagName.jcontainer.hide();
        SearchTagName.lastkey = '';
      }
    },

    updateTagList : function() {
      var ul = SearchTagName.jparent.find('ul:first');
      // Remove all old items
      ul.empty();
      // Fill up with new list
      SearchTagName.jcontainer.hide();
      var items = SearchTagName.data.jsonList;
      for(var i = 0; i < items.length; ++i){
        var li = $('<li></li>');
        li.on('mousedown mouseup', utils.cancelEvent)
        .on('mouseover', function(e) {
          utils.cancelEvent(e);
          var thiz = $(this);
          thiz.parents('ul:first').find('li').removeClass('selected');
          thiz.addClass('selected');
        })
        .on('click', function(e) {
          SearchTagName.setValueInput($(this));
          SearchTagName.jcontainer.hide();
          utils.cancelEvent(e);
        });
        var a = $('<a href="javascript:void(0);"></a>')
        a.html(items[i].ip);
        li.append(a);
        ul.append(li);
      }
      if(items.length > 0) {
        SearchTagName.jcontainer.show();
      }
    }
  };

  return SearchTagName;
})(utils, gj);
