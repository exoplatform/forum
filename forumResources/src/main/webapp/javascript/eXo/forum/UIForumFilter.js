(function(gj) {
  var UIForumFilter = {
      parent : null,
      input : null,
      inputValue : null,
      menu : null,
      fakeInput : null,
      onChange : null,
      init : function(inputId) {
        //
        this.parent = gj('#' + inputId);

        this.inputValue = gj('#' + inputId.replace('uiForumFilter', ''));
        
        this.fakeInput = this.parent.find('span:first');
        
        this.input = this.parent.find('input.filterInput:first');
        
        this.menu = this.parent.find('.filterMenu:first');
        
        var jOnchange = this.parent.find('div.forumFilterData');
        this.onChange = jOnchange.attr('data-onchange');
        
        var arrow = this.parent.find('.rightArrow:first');
        
        arrow.off('click').on('click', function(e) {
          e.stopPropagation();
          
          //
          UIForumFilter.filter(e);
          
          //
          UIForumFilter.menu.css('height', 'auto');
          var h = UIForumFilter.menu.height();
          UIForumFilter.menu.css({'height': '0px','visibility' :'visible'})
            .animate({height: h + 'px'}, 200, function() {
              gj(this).css('height', 'auto');
              UIForumFilter.input.focus();
            });
        });
        
        this.input.on('keydown click focus', function(e) {
          e.stopPropagation();
        });
        
        this.input.on('keyup', UIForumFilter.filter);

        var uiForm = this.parent.parents('.UIForm:first');
        function parentClick() {
          var pr = gj(this);
          pr.find('.filterMenu').animate({'height': '0px'}, 400, function() {
            gj(this).css({'visibility' :'hidden'});
          });
        }
        uiForm.off('click', parentClick).on('click', parentClick);
      },
      filter : function(e) {
        e.stopPropagation();
        var query = UIForumFilter.input.val();
        if(query === null || query ==='') {
          query = '_';
        }
        
        var data = UIForumFilter.getCache(query);
        if (data) {
          UIForumFilter.renderMenu(data);
        } else {
          var url = window.location.protocol + '//' + window.location.host + '/' + eXo.env.portal.rest + 
                    '/ks/forum/filterforum?name=' + query + '&maxSize=0';
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
        ul.find('li.item').remove();
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
        li.addClass("item category");
        li.on('click', function(e) {
          e.stopPropagation();
        });
        ul.append(li);
        var forums = cate.forumFilters;
        li = gj('<li></li>');
        li.addClass("item forum");
        var ul2 = gj('<ul></ul>');
        ul2.appendTo(li);
        for(var i = 0; i < forums.length; ++i) {
          var forum = forums[i];
          var li2 = gj('<li></li>');
          li2.html(forum.forumName);
          li2.attr('data-forid', forum.forumId);
          li2.attr('data-catid', cate.categoryId);
          li2.on('click', function(e) {
            e.stopPropagation();
            var item = gj(this);
            UIForumFilter.fakeInput.html(item.html());
            var value = item.attr('data-catid') + ';' + 
                        item.attr('data-forid') + ';' + 
                        gj.trim(item.html());
            
            UIForumFilter.inputValue.val(value);

            UIForumFilter.input.val('');
            
            UIForumFilter.menu.animate({'height': '0px'}, 400, function() {
              gj(this).css({'visibility' :'hidden'});
              if(UIForumFilter.onChange != null && UIForumFilter.onChange.length > 0){
                gj('<div onclick="'+UIForumFilter.onChange+'"></div>').trigger('click');
              }
            });
            
          });
          ul2.append(li2);
        }
        ul.append(li);
      },
      saveCache : function(key, data){
        var ojCache = gj('div#searchDataForumFilter');
        if(ojCache.length == 0) {
          ojCache = gj('<div id="searchDataForumFilter"></div>').appendTo(gj(document.body));
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
        var datas = gj('div#searchDataForumFilter').data("CacheSearch");
        return (String(datas) === "undefined") ? null : datas[key];
      },
      clearCache : function() {
        gj('div#searchDataForumFilter').stop().animate({
          'cursor' : 'none'
        }, 10000, function() {
          gj(this).data("CacheSearch", {});
        });
      }
    };
  return UIForumFilter;
})(gj);
