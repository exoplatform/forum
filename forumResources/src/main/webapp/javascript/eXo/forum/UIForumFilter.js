(function(gj) {
  var UIForumFilter = {
      parent : null,
      input : null,
      inputValue : null,
      menu : null,
      fakeInput : null,
      init : function(inputId) {
        //
        this.inputValue = gj('#'+inputId.replace('Fake', ''));
        
        this.parent = gj('#' + inputId);
        this.fakeInput = this.parent.find('span:first');
        this.input = this.parent.find('.FilterInput:first');
        this.menu = this.parent.find('.FilterMenu:first');
        var arrow = this.parent.find('.RightArrow:first');
        
        arrow.off('click').on('click', function(e) {
          //
          UIForumFilter.menu.css('height', 'auto');
          var h = UIForumFilter.menu.height();
          UIForumFilter.menu.css({'height': '0px','visibility' :'visible'})
            .animate({height: h + 'px'}, 200, function() {
              gj(this).css('height', 'auto');
              UIForumFilter.filter();
            });
        });
        
        this.input.on('keyup', UIForumFilter.filter);
      },
      filter : function(e) {
        var query = UIForumFilter.input.val();
        if(query === null || query ==='') {
          query = '_';
        }
        
        var data = UIForumFilter.getCache(query);
        if (data) {
          UIForumFilter.renderMenu(data);
        } else {
          var url = window.location.protocol + '//' + window.location.host + '/' + eXo.env.portal.rest + 
                    '/ks/forum/filterforum/' + query;
          gj.getJSON(url, function(response) {
            UIForumFilter.saveCache(query, response)
            UIForumFilter.renderMenu(response);
          });
        }
        UIForumFilter.clearCache();
      },
      renderMenu : function(data) {
        window.datas= data;
        var ul = UIForumFilter.menu.find('ul:first');
        ul.find('li.Item').remove();
        if(data != null && data.length > 0) {
          var size = data.length;
          for(var i = 0; i < size; ++i) {
            var cate = data[i];
            UIForumFilter.makeItem(cate, ul);
          }
        }
      },
      makeItem : function(cate, ul) {
        var li = gj('<li></li>');
        li.html(cate.categoryName);
        li.attr('data-catid', cate.categoryId);
        li.addClass("Item");
        ul.append(li);
        var forums = cate.forumFilters;
        li = gj('<li></li>');
        li.addClass("Item");
        var ul2 = gj('<ul></ul>');
        ul2.appendTo(li);
        for(var i = 0; i < forums.length; ++i) {
          var forum = forums[i];
          var li2 = gj('<li></li>');
          li2.html(forum.forumName);
          li2.attr('data-forid', forum.forumId);
          li2.on('click', function(e) {
            e.stopPropagation();
            var item = gj(this);
            window.console.log(UIForumFilter.input.html())
            UIForumFilter.fakeInput.html(item.html());
            UIForumFilter.inputValue.val(item.attr('data-forid'));
            UIForumFilter.menu.css({'height': '0px','visibility' :'hidden'});
            UIForumFilter.input.val('');
          });
          ul2.append(li2);
        }
        ul.append(li);
      },
      saveCache : function(key, data){
        var ojCache = gj('div#searchData');
        if(ojCache.length == 0) {
          ojCache = gj('<div id="searchData"></div>').appendTo(gj(document.body));
          ojCache.hide();
        }
        key = 'result' + ((key === ' ') ? '_20' : key);
        var datas = ojCache.data("CacheSearch");
        if (String(datas) === "undefined") datas = {};
        datas[key] = data;
        ojCache.data("CacheSearch", datas);
      },
      getCache : function(key) {
        key = 'result' + ((key === ' ') ? '_20' : key);
        var datas = gj('div#searchData').data("CacheSearch");
        return (String(datas) === "undefined") ? null : datas[key];
      },
      clearCache : function() {
        gj('div#searchData').animate({
          'cursor' : 'none'
        }, 10000, function() {
          gj(this).data("CacheSearch", {});
        });
      }
    };
  return UIForumFilter;
})(gj);