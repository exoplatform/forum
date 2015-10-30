(function(maskLayer, contextMenu, utils, forumLeftNavigation, $, window, document) {
  var UIForumPortlet = {
    obj : null,
    event : null,
    wait : false,
    id : 'UIForumPortlet',

    init : function(id) {
      UIForumPortlet.id = id;
      var jportlet = $.fn.findId(id);
      if (jportlet.exists()) {
        jportlet.find('.oncontextmenu').on('contextmenu', utils.returnFalse);
        UIForumPortlet.initShowUserInfo();
        UIForumPortlet.disableOnClickMenu('SearchForm');
        UIForumPortlet.initTooltip(id);
        jportlet.find('.dropdown').parent().on('click', function(evt) {
          var menu = $(this).find('.dropdown-menu:first');
          if (menu.find('input:first').exists()) {
            var t = setTimeout(function() { menu.find('input:first').focus(); clearTimeout(t); }, 200)
          }
        });
      }
      utils.onResize(UIForumPortlet.resizeCallback);

      $.each(jportlet.find('ul.dropdown-menu').find('a'), function(i, item){
        $(item).on('click', function(){
          $(this).parents('.dropdown').removeClass('open');
        })
      });
    },
    
    disableOnClickMenu : function (id) {
      var jportlet = $.fn.findId(UIForumPortlet.id);
      if(id != null) {
        jportlet.find('#'+id).off('click mousedown mouseover').on('click mousedown mouseover', utils.cancelEvent);
      }
    },

    initShowUserInfo : function(id) {
      var jportlet = $.fn.findId(UIForumPortlet.id);
      if(id != null) {
        jportlet.find('#'+id).find('.uiUserInfo').off('click').on('click', utils.showUserMenu);
      } else {
        jportlet.find('.uiUserInfo').off('click').on('click', utils.showUserMenu);
      }
    },

    initTooltip : function(id) {
      utils.initTooltip(id);
    },

    resizeCallback : function() {
      utils.setMaskLayer(UIForumPortlet.id);
    },
    
    initConfirm : function(id) {
      var component = $.fn.findId(id);
      var confirms = component.find('.confirm');
      var divNumber = component.find('div#divChecked');
      
      $.each(confirms, function(idx, element) {
        var thizz = $(element);
        if(thizz.hasAttr('id') == false) {
          thizz.attr('id', id + 'Confirm' + idx);
        }
        var settings = {isMulti: false, message : ''};
        if(thizz.hasAttr('data-number')) {
          settings.isMulti = true;
          if(divNumber.exists()) {
            thizz.on('mousedown keydown', function() {
              var nb = $('#divChecked').attr("data-checked");
              $(this).attr('data-number', nb);
            });
          }
        }
        if(thizz.hasAttr('data-confirm')) {
          settings.message = thizz.attr('data-confirm');
        }
        thizz.confirmation(settings);
      });
    },

    controlLeftNavigation : function() {
      var leftNav = $('.LeftNavigationTDContainer:first');
      if(leftNav.exists()) {
        leftNav.css('height', 'auto');
        setTimeout(forumLeftNavigation.resize, 1000);
      }
    },

    selectItem : function(obj) {
      var jobj = $(obj);
      var tr = jobj.parents('tr');
      var table = tr.parents('table');
      var tbody = table.find('tbody');
      var checkbox = table.find('input:checkbox:first');
      var checkboxes = tbody.find('input:checkbox');
      var chklen = checkboxes.length;
      var j = 0;
      if (jobj[0].checked) {
        if (!tr.attr("tmpClass")) {
          tr.attr("tmpClass", tr.attr('class'));
          tr.attr('class', 'SelectedItem');
        }
        for ( var i = 0; i < chklen; i++) {
          if (checkboxes[i].checked)
            j++;
          else
            break;
        }
        if (j == chklen)
          checkbox[0].checked = (true);
      } else {
        if (tr.attr('tmpClass')) {
          tr.attr('class', tr.attr('tmpClass'));
          tr.removeAttr('tmpClass');
        }
        checkbox[0].checked = (false);
      }
      var modMenu = $('#ModerationMenu');
      if (modMenu.exists()) {
        var firstItem = modMenu.find('a:first');
        if (j >= 2) {
          if (!firstItem.attr('oldClass')) {
            firstItem.attr('oldHref', firstItem.attr('href'));
            firstItem.attr('href', 'javascript:void(0);');
            var parentIt = firstItem.parents('.MenuItem');
            firstItem.attr('oldClass', parentIt.attr('class'));
            parentIt.attr('class', 'DisableMenuItem');
          }
        } else {
          if (firstItem.attr("oldClass")) {
            firstItem.attr('href', firstItem.attr('oldHref'));
            var parentIt = firstItem.parents('.DisableMenuItem');
            parentIt.attr('class', firstItem.attr('oldClass'));
            firstItem.removeAttr('oldClass');
          }
        }
      }
    },

    checkedPost : function(elm) {
      if (elm) {
        UIForumPortlet.setChecked(elm.checked);
      }
    },

    setChecked : function(isChecked) {
      var divChecked = $('#divChecked');
      if (divChecked.exists()) {
        var check = 0;
        check = divChecked.attr("data-checked") * 1;
        if (isChecked)
          divChecked.attr("data-checked", (check + 1));
        else
          divChecked.attr("data-checked", (check - 1));
      }
    },

    OneChecked : function(formName) {
      var form = document.forms[formName];
      if (form) {
        var checkboxs = form.elements;
        for ( var i = 0; i < checkboxs.length; i++) {
          if (checkboxs[i].checked) {
            return true;
          }
        }
      }
      return false;
    },

    numberIsChecked : function(formName, checkAllName, multiAns, onlyAns, notChecked) {
      var divChecked = $('#divChecked');
      var total = 0;
      total = divChecked.attr("data-checked") * 1;
      if (total > 1) {
        var text = String(multiAns);
        return confirm(text.replace('{0}', total));
      } else if (total == 1) {
        return confirm(onlyAns);
      } else {
        alert(notChecked);
        return false;
      }
    },

    checkAll : function(obj) {
      var table = $(obj).parents('table:first');
      var tbody = table.find('tbody');
      var checkboxes = tbody.find('input:checkbox');
      var len = checkboxes.length;
      if (obj.checked) {
        for ( var i = 0; i < len; i++) {
          if (!checkboxes[i].checked) {
            UIForumPortlet.setChecked(true);
          }
          checkboxes[i].checked = (true);
          UIForumPortlet.selectItem(checkboxes.eq(i));
        }
      } else {
        for ( var i = 0; i < len; i++) {
          if (checkboxes[i].checked)
            UIForumPortlet.setChecked(false);
          checkboxes[i].checked = (false);
          UIForumPortlet.selectItem(checkboxes.eq(i));
        }
      }
    },

    processCheckBox : function(id, number) {
      var uiCategory = $.fn.findId(id);
      var checked = (number > 0) ? true : false;
      //
      var menuItems = uiCategory.find('.uiCategoryPopupMenu:first').find('a.forumAction');
      menuItems.attr('data-number', number);
      UIForumPortlet.enableDisableAction(menuItems, checked);
    },

    checkActionCategory : function(id, isNotLoad) {
      var uiCategory = $.fn.findId(id);
      var number = uiCategory.find('input[type=checkbox]:checked').length;
      if(uiCategory.find('input[name=checkAll]:checked').length === 1) {
        number = number - 1;
      }
      //
      UIForumPortlet.processCheckBox(id, number);

      //
      if(typeof isNotLoad === 'undefined'){
        var checkboxes = uiCategory.find('input[type=checkbox]').data('containerId', { container : id });
        checkboxes.on('click', function(evt) {
          var thizz = $(this);
          if (thizz.attr('name') === 'checkAll') {
            var id = thizz.data('containerId').container;
            if (thizz.is(':checked')) {
              var number = $.fn.findId(id).find('input[type=checkbox]').length - 1;
              UIForumPortlet.processCheckBox(id, number);
            } else {
              UIForumPortlet.processCheckBox(id, 0);
            }
          } else {
            UIForumPortlet.checkActionCategory(thizz.data('containerId').container, false);
          }
        });
      }
    },
    
    enableDisableAction : function(actions, checked) {
      $.each(actions, function(index, elm) {
        var thizz = $(elm);
        if (checked === false) {
          if (thizz.parents('li:first').hasClass('disabled') === false) {
            thizz.attr('data-action', thizz.attr('href'));
            thizz.attr('href', 'javascript:void(0);');
            thizz.parents('li:first').addClass('disabled');
          }
        } else {
          if (thizz.parents('li:first').hasClass('disabled') === true) {
            if(thizz.hasClass('confirm') === false) {
              thizz.attr('href', thizz.attr('data-action'));
            }
            thizz.parents('li:first').removeClass('disabled');
          }
        }
      });
    },

    visibleAction : function(id) {
      var parent = $.fn.findId(id);
      var addCategory = parent.find('#AddCategory');
      if (addCategory.exists()) {
        addCategory = addCategory.find('a:first');
        var addForum = parent.find('#AddForum').find('a:first');
        if ($("#UICategories").exists()) {
          addCategory.attr('class', "actionIcon");//disabled
          addForum.attr('class', "actionIcon");
        } else if ($("#UICategory").exists()) {
          addCategory.attr('class', "btn actionIcon disabled").attr('href', "javascript:void(0);");
          addForum.attr('class', "actionIcon");
        } else {
          addCategory.attr('class', "btn actionIcon disabled").attr('href', "javascript:void(0);");
          addForum.attr('class', "btn actionIcon disabled").attr('href', "javascript:void(0);");
        }
      }
    },

    checkActionTopic : function(obj, evt) {
      UIForumPortlet.showPopup(obj, evt);
      var parentMenu = $("#ModerationMenu");
      var menuItems = parentMenu.find('a');
      var parentContent = $("#UITopicContent");
      var checkBoxs = parentContent.find('input.checkbox');
      var clen = checkBoxs.length;
      var mlen = menuItems.length;
      var j = $('#divChecked').attr("data-checked") * 1;
      for ( var i = 1; i < clen; i++) {
        if (checkBoxs[i].checked) {
          j = 1;
          break;
        }
      }
      if (j === 0) {
        for ( var k = 0; k < mlen; k++) {
          var menuItem = menuItems.eq(k);
          if (menuItem.hasClass('ItemIcon SetUnWaiting'))
            break;
          if (!menuItem.attr("tmpClass")) {
            menuItem.attr("tmpHref", menuItem.attr('href'));
            menuItem.attr('href', "javascript:void(0);");
            var parentIt = menuItem.parents(".MenuItem");
            menuItem.attr("tmpClass", parentIt.attr('class'));
            parentIt.attr('class', "DisableMenuItem");
            parentIt.on('click', utils.cancelEvent);
          }
        }
      } else {
        for ( var n = 0; n < mlen; n++) {
          var menuItem = menuItems.eq(n);
          if (menuItem.attr("tmpClass")) {
            var parent = menuItem.find(".DisableMenuItem");
            if (parent.exists())
              parent.attr('class', menuItem.attr("tmpClass"));
            menuItem.attr('href', menuItem.attr("tmpHref"));
            menuItem.removeAttr("tmpClass");
            menuItem.removeAttr("tmpHref");
          }
        }
      }
    },

    expandCollapse : function(obj) {
      var jobject = $(obj)
      var forumToolbar = jobject.parents(".uiCollapExpand");
      var contentContainer = forumToolbar.find('.uiExpandContainer');
      jobject.hide();
      $('div.tooltip').remove();
      if (contentContainer.css('display') != "none") {
        contentContainer.hide(200);
        forumToolbar.css('border-bottom', 'none');
        forumToolbar.find('.uiIconArrowRight').show().tooltip();
      } else {
        contentContainer.show(200);
        forumToolbar.css('border-bottom', '1px solid #cfcfcf');
        forumToolbar.find('.uiIconArrowDown').show().tooltip();
      }
    },

    showTreeNode : function(obj) {
      var jobject = $(obj);
      var parentNode = jobject.parents(".nodeGroup:first");

      var nodes = parentNode.find(' > .node');
      nodes.find('.nodeGroup:first').hide();
      nodes.find('.uiIconNode:first').removeClass('expandIcon').addClass("collapseIcon");

      var selectedNode = jobject.parents(".node:first");

      selectedNode.find('.uiIconNode:first').addClass("expandIcon");
      selectedNode.find('.nodeGroup:first').show();
    },

    checkedNode : function(elm) {
      var jinput = $(elm);
      var node = jinput.parents('.node:first');
      var inputs = node.find('.nodeGroup:first').find('input[type=checkbox]');
      inputs.prop("checked", elm.checked);
    },

    checkedChildNode : function(elm) {
      if (elm.checked) {
        var parentNode = $(elm).parents('.nodeGroup:first').parents('.node:first');
        var parentCheckBox = parentNode.find('a.uiIconNode:first').find('input:first');
        parentCheckBox.prop("checked", elm.checked);
      }
    },
    
    showTree: function(node) {
      var jnode = $(node);
      var pNode = jnode.parent('.node:first');
      var groupNode = pNode.find(' > .nodeGroup');
      if(groupNode.css('display') === 'none') {
        var pNodeGroup = pNode.parent('.nodeGroup:first');
        pNodeGroup.find('.nodeGroup').hide();
        var nodes = pNodeGroup.find('.uiIconNode');
        $.each(nodes, function(i, elm) {
          var elm = $(this);
          if (elm.parent('.node').find('.nodeGroup').exists()) {
            elm.attr('class', 'uiIconNode collapseIcon');
          } else {
            elm.attr('class', 'uiIconNode uiIconEmpty');
          }
        });
        
        //
        groupNode.show();
        jnode.attr('class', 'uiIconNode expandIcon');
      } else {
        groupNode.hide();
        jnode.attr('class', 'uiIconNode collapseIcon');
      }
      
    },
    
    initTreeNode : function(formId) {
      var container = $.fn.findId(formId);
      var treeContainer = container.find('div.treeContainer:first');
      var groupFirst = treeContainer.find('.nodeGroup:first');
      var listNode = groupFirst.find('.uiIconNode');
      listNode.on('click',  function(e) {
        if($(this).parent('.node').find('.nodeGroup').exists()) {
          UIForumPortlet.showTree(this)
        }
      });
      groupFirst.find('.nodeGroup').hide();
      
      groupFirst.find('.nodeGroup:first').parent('.node:first')
                .find('.uiIconNode:first').click();
    },
    
    initVote : function(voteId, rate) {
      var vote = $.fn.findId(voteId);
      rate = parseInt(rate);
      var optsContainer = vote.find('div.optionsContainer:first');
      optsContainer.attr('data-rate', rate);
      var options = optsContainer.children('i');
      options.on('mouseover', UIForumPortlet.overVote);
      options.on('blur', UIForumPortlet.overVote);

      optsContainer.on('mouseover', utils.cancelEvent);
      optsContainer.on('blur', utils.cancelEvent);
      
      vote.on('mouseover', function() {
        UIForumPortlet.parentOverVote(this);
      });

      UIForumPortlet.parentOverVote(vote);
    },

    parentOverVote : function(elm) {
      var optsCon = $(elm).find('div.optionsContainer:first');
      var opts = optsCon.children('i');
      var rate = optsCon.attr('data-rate');
      for ( var j = 0; j < opts.length; j++) {
        if (j < rate)
          opts.eq(j).attr('class', 'uiIconRatedVote');
        else
          opts.eq(j).attr('class', 'uiIconNormalVote');
      }
    },

    overVote : function(event) {
      var optsCon = $(this).parents('div.optionsContainer:first');
      var opts = optsCon.children('i');
      var i = opts.length;
      for (--i; i >= 0; i--) {
        if (opts[i] == $(this)[0])
          break;
        opts.eq(i).attr('class', 'uiIconNormalVote');
      }
      if (opts.eq(i).attr('class') == "uiIconOverVote")
        return;
      for (; i >= 0; i--) {
        opts.eq(i).attr('class', 'uiIconOverVote');
      }
    },

    showPopup : function(elm, e) {
      var strs = [ '#goPageBottom', '#SearchForm', '.CancelEvent' ];
      for ( var t = 0; t < strs.length; t++) {
        var jelm = $(strs[t]);
        if (jelm.exists()) {
          jelm.on('click', utils.cancelEvent);
        }
      }
      eXo.webui.UIPopupSelectCategory.show(elm, e);
      utils.cancelEvent(e);
      utils.addhideElement($(elm).find('div.UIPopupCategory'));
    },

    goLastPost : function(idLastPost) {
      var isDesktop = $('#UIPageDesktop');
      if (!isDesktop.exists()) {
        if (idLastPost === "top") {
          var body = $('body')[0];
          if (body.scrollTop > 250) {
            script: scroll(0, 0);
            UIForumPortlet.scrollIntoView('KSMaskLayer');
          }
        } else {
          UIForumPortlet.scrollIntoView(idLastPost);
        }
      }
    },

    scrollIntoView : function(id) {
      var timer = setTimeout(function() {
        var obj = document.getElementById(id);
        if (obj) {
          if (eXo.core.Browser.isIE()) {
            var correctOffset = 0;
            while(obj.offsetParent) {
              correctOffset += obj.offsetTop;
              obj = obj.offsetParent;
            }
            $("html, body").scrollTop(correctOffset);
          } else {
            obj.scrollIntoView(true);
          }
        }
        clearTimeout(timer);
      }, 1000);
    },

    setEnableInput : function() {
      var parend = $("#ForumUserBan");
      if (parend.exists()) {
        var obj = parend.find("input.checkbox:first");
        if (obj.exists()) {
          $("#BanCounter").attr('disabled', 'disabled');
          $("#BanReasonSummary").attr('readonly', 'readonly');
          $("#CreatedDateBan").attr('disabled', 'disabled');
          var selectbox = parend.find("select.selectbox:first");
          if (!obj[0].checked) {
            selectbox.attr('disabled', 'disabled');
            $('#BanReason').attr('disabled', 'disabled');
          }
          $(obj).on('click', function() {
            if (!$(this)[0].checked) {
              selectbox.attr('disabled', 'disabled');
              $('#BanReason').attr('disabled', 'disabled');
            } else {
              selectbox.removeAttr('disabled');
              $('#BanReason').removeAttr('disabled');
            }
          });
        }
      }
    },

    hidePicture : function() {
      eXo.core.Browser.onScrollCallback.remove('MaskLayerControl');
      $(eXo.core.UIMaskLayer.object).remove();
      $("#MaskLayer").remove();
      $("subMaskLayer").remove();
    },

    showPicture : function(src) {
      maskLayer.showPicture(src);
    },

    getImageSize : function(imageNode) {
      var tmp = $(imageNode.cloneNode(true));
      tmp.css('visibility', 'hidden');
      $('body').append(tmp);
      var size = {
        width : tmp.outerWidth(true),
        height : tmp.outerHeight(true)
      }
      $(tmp).remove();
      return size;
    },

    showFullScreen : function(imageNode, containerNode) {
      var imageSize = UIForumPortlet.getImageSize(imageNode);
      var widthMax = $(document.documentElement).outerWidth(true);
      if ((imageSize.width + 40) > widthMax) {
        $(containerNode).css('width', widthMax + 'px');
        imageNode.css('height', 'auto');
        imageNode.width(widthMax - 40);
      }
    },

    setDisableTexarea : function() {
      var objCmdElm = $('#moderationOptions');
      var input = objCmdElm.find('input.checkbox');
      if (input.exists()) {
        if (input.attr('name') === "AutoAddEmailNotify") {
          UIForumPortlet.onClickDisableTexarea();
          input.on('click', UIForumPortlet.onClickDisableTexarea);
        }
      }
    },

    onClickDisableTexarea : function() {
      var objCmdElm = $('#moderationOptions');
      var input = objCmdElm.find('input.checkbox');
      if (objCmdElm.exists()) {
        var texares = objCmdElm.find("textarea");
        for ( var i = 0; i < texares.length; ++i) {
          var textare = texares.eq(i);
          if (textare.attr('name') === "NotifyWhenAddTopic" || textare.attr('name') === "NotifyWhenAddPost") {
            if (!input.val()) {
              texare.attr('readOnly', 'false');
            } else {
              texare.attr('readOnly', 'true');
            }
          }
        }
      }
    },

    setDisableInfo : function() {
      var strs = new Array("#CanPost", "#CanView");
      for ( var i = 0; i < strs.length; i++) {
        var elm = $(strs[i]);
        if (elm.exists()) {
          UIForumPortlet.setShowInfo(elm);
          elm.on('keyup', function() {
            UIForumPortlet.setShowInfo(this);
          });
        }
      }
    },

    setShowInfo : function(elm) {
      var info = $($(elm).attr('id') + "Info");
      if (elm.val() === '') {
        info.show();
      } else {
        info.hide();
      }
    },

    controlWorkSpace : function() {
      var slidebar = $('#ControlWorkspaceSlidebar');
      if (slidebar.exists()) {
        var slidebarButton = slidebar.find("div.SlidebarButton:first");
        if (slidebarButton.exists()) {
          slidebarButton.on('click', UIForumPortlet.onClickSlidebarButton);
        }
      }
      setTimeout(UIForumPortlet.reSizeImages, 1500);
    },
    onClickSlidebarButton : function() {
      var workspaceContainer = $('#UIWorkspaceContainer');
      if (workspaceContainer.exists()) {
        if (workspaceContainer.css('display') === 'none') {
          setTimeout(eXo.forum.UIForumPortlet.reSizeImages, 500);
        }
      }
    },
    reSizeImgViewPost : function() {
      setTimeout('eXo.forum.UIForumPortlet.setSizeImages(10, "SizeImage")', 1000);
    },
    reSizeImgViewTopic : function() {
      setTimeout('eXo.forum.UIForumPortlet.setSizeImages(225, "SizeImage")', 1000);
    },
    reSizeImages : function() {
      setTimeout('eXo.forum.UIForumPortlet.setSizeImages(225, "UITopicDetail")', 500);
    },

    reSizeImagesInMessageForm : function() {
      if (eXo.core.Browser.isIE6())
        setTimeout('eXo.forum.UIForumPortlet.setSizeImages(130, "uiViewPrivateMessage")', 800);
      else
        setTimeout('eXo.forum.UIForumPortlet.setSizeImages(10, "uiViewPrivateMessage")', 400);
    },

    setSizeImages : function(delta, classParant) {
      var parent_ = $.fn.findId(classParant);
      var imageContentContainer = parent_.find('div.ImageContentContainer:first');
      if (imageContentContainer.exists()) {
        if (!$('#UIPageDesktop').exists()) {
          var max_width = imageContentContainer.outerWidth(true) - delta;
          var max = max_width;
          if (max_width > 600)
            max = 600;
          var images_ = imageContentContainer.find("img");
          for ( var i = 0; i < images_.length; i++) {
            var image = images_.eq(i);
            if (image.hasClass("ImgAvatar") || image.hasClass("AttachImage")) {
              continue;
            }
            var img = new Image();
            img.src = image.attr('src');
            if (img.width > max) {
              image.css('height', 'auto');
              image.width(max);
            } else {
              image.css('width', 'auto');
              if (image.width() > max) {
                image.css('height', 'auto');
                image.width(max);
              }
            }
            if (img.width > 600) {
              image.on('click', UIForumPortlet.showImage);
            }
          }
        }
      }
    },

    showImage : function() {
      UIForumPortlet.showPicture(this);
    },

    resetFielForm : function(idElm) {
      var form = $.fn.findId(idElm);
      form.find("input:checkbox").attr('checked', false);
      form.find("input:text").val('');
      var slider = form.find(".uiFormSliderInput");
      if (slider.exists()) {
        $.each(slider, function(i, item) {
          eXo.forum.UISliderControl.reset($(item));
        });
      }
      form.find("textarea").val('');
    },

    RightClickBookMark : function(elmId) {
      var ancestor = $.fn.findId(elmId);
      var popupContents = ancestor.find('ul.ClickPopupContent');
      if (!popupContents.exists())
        return;
      var popupContainer = $('#RightClickContainer');
      if (!popupContainer.exists())
        return;
      var itemmenuBookMark = popupContainer.find('a.bookmark:first');
      var itemmenuWatching = popupContainer.find('a.watching:first');
      var itemmenuRSS = popupContainer.find('a.rssfeed:first');
      if (!itemmenuWatching.exists() || !itemmenuBookMark.exists())
        return;
      var cloneWatching = itemmenuWatching.clone();
      var iconWatching = $('<div></div>').append(cloneWatching.find('i')).html();
      var labelWatchings = String(cloneWatching.html()).split(";");
      for ( var i = 0; i < popupContents.length; i++) {
        var popupContent = popupContents.eq(i);
        var action = popupContent.attr('data-bookmark');
        if(action == null) continue;
        if (action.indexOf(";") < 0) {
          itemmenuBookMark.attr('href', action);
          itemmenuWatching.parent().hide();
        } else {
          var actions = action.split(";");
          itemmenuBookMark.attr('href', actions[0]);
          if (actions[1].toLowerCase().indexOf("unwatch") >= 0) {
            if (actions[1].indexOf("unwatch,") >= 0) {
              actions[1] = actions[1].replace('unwatch,', '');
            }
            itemmenuWatching.html(iconWatching + labelWatchings[1]);
          } else {
            itemmenuWatching.html(iconWatching + labelWatchings[0]);
          }
          itemmenuWatching.attr('href', actions[1]);
          if (itemmenuRSS.exists()) {
            if (actions.length == 3) {
              var link = actions[2].substring(0, actions[2].indexOf(','));
              var action = actions[2].substring(actions[2].indexOf(',') + 1);
              itemmenuRSS.attr('href', 'javascript:window.open("' + link + '"); ' + action + ';');// link,action
              itemmenuRSS.parent().show();
            } else {
              itemmenuRSS.parent().hide();
            }
          }
          itemmenuWatching.parent().show();
        }
        popupContent.removeAttr('data-bookmark');
        popupContent.html(popupContainer.html());
        popupContent.on('mouseover', function(evt) {
          evt.preventDefault();
          evt.stopPropagation();
        });
      }
    },

    ReloadImage : function() {
      if (eXo.core.Browser.isIE6()) {
        var aImage = document.getElementsByTagName("img");
        var length = aImage.length;
        for ( var i = 0; i < length; ++i) {
          aImage[i].src = aImage[i].src;
          if (aImage[i].width > 590)
            aImage[i].width = 590 + "px";
        }
      }
      utils.onResize('eXo.forum.UIForumPortlet.reSizeImages');
    },

    shareLink : function(obj) {
      var shareLinkContainer = $("#popupShareLink");
      if (shareLinkContainer.css('display') != "none")
        shareLinkContainer.hide();
      else
        shareLinkContainer.show();
    },

    closeShareLink : function(obj) {
      $(obj).parents('.UIPopupWindow').hide();
    },
    loadScroll : function() {
      UIForumPortlet.loadMoreForumActionBar('UIForumActionBar', 'More');
    },
    loadMoreForumActionBar : function(id, moreTagLabel) {
      var parent = $.fn.findId(id);
      var ul = parent.find('ul:first');
      ul.addClass('actionBarMoreItem').addClass('pull-left');
      parent.loadMoreItem({
        ulContainerClass: 'actionBarMoreItem',
        loadMoreLabel : moreTagLabel,
        processContainerWidth : UIForumPortlet.processForumActionBarrWidth
      });
    },
    processForumActionBarrWidth : function(parent) {
      var uiRightActionBar = parent.find('#uiRightActionBar');
      var widthMoreItem = parent.width() - uiRightActionBar.outerWidth() - 20;
      return widthMoreItem;
    },
/*
 * Load more tags items.
 * */
    loadMoreItem : function(id, moreTagLabel) {
      var parent = $.fn.findId(id);
      parent.loadMoreItem({
        loadMoreLabel : moreTagLabel,
        processContainerWidth : UIForumPortlet.processTagContainerWidth
      });
    },

    processTagContainerWidth : function(parent) {
      var topContainer = parent.find('.topContainer:first');
      var actionContainer = parent.find('.actionContainer:first');
      var pageIterContainer = parent.find('.pageIterContainer:first'); 
      
      var tagsContainer = parent.find('.tagsContainer:first');
      var titleTag = tagsContainer.find('.titleTag:first');
      var widthMoreItem = topContainer.width() - actionContainer.width() - pageIterContainer.width() - titleTag.width() - 20;
      return widthMoreItem;
    },

    executeLink : function(elm, evt) {
      var onclickAction = String($(elm).attr('data-link'));
      $.globalEval(onclickAction);
      evt.preventDefault();
      evt.stopPropagation();
      return false;
    },

    createLink : function(cpId, isAjax) {
      if (isAjax === 'true' || isAjax === true) {
        UIForumPortlet.addLink(cpId, 'actionOpenLink');
      }
    },

    addLink : function(cpId, clazzAction) {
      var links = $.fn.findId(cpId).find('a.' + clazzAction);
      if (links.exists() && links.hasAttr('data-link')) {
        links.on('click', function(e) {
          UIForumPortlet.executeLink(this, e);
        });
      }
    },

    setAutoScrollTable : function(idroot, idParent, idChild) {
      var rootEl = document.getElementById(idroot);
      var grid = document.getElementById(idChild);
      var tableContent = document.getElementById(idParent);
      var isIE = document.all ? true : false;
      if (isIE) {
        tableContent.style.width = "auto";
        grid.style.width = "auto";
      }
      if ((grid.offsetWidth + 10) >= (tableContent.offsetWidth)) {
        tableContent.style.paddingRight = "16px";
        tableContent.style.width = "auto";
      } else {
        tableContent.style.padding = "1px";
        tableContent.style.width = "100%";
        if (isIE) {
          rootEl.style.width = "96%";
          rootEl.style.margin = "auto";
        }
      }
      if (grid.offsetHeight > 260) {
        tableContent.style.height = "260px";
      } else {
        tableContent.style.height = "auto";
      }
    },

    initContextMenu : function(id) {
      var cont = document.getElementById(id);
      var uiContextMenu = contextMenu;
      if (!uiContextMenu.classNames)
        uiContextMenu.classNames = new Array("actionOpenLink");
      else
        uiContextMenu.classNames.push("actionOpenLink");
      uiContextMenu.setContainer(cont);
      uiContextMenu.setup();
    },

    initShowBBcodeHelp : function(id) {
      var parent = $.fn.findId(id);
      if (parent.exists()) {
        var popups = parent.find('.parentPosition');
        popups.on('mouseover', UIForumPortlet.showBBCodeHelp);
        popups.on('mouseout', utils.hideElements);
        parent.parents('.UIPopupWindow:first').css('z-index', 1000);
      }
    },

    showBBCodeHelp : function(evt) {
      utils.hideElements();
      var thiz = $(this);

      var popupContent = thiz.find('.bbcodePopupContent:first');
      var length = $.trim(popupContent.text()).length;
      var widthCt = (length / 0.8);
      if (widthCt < 230) {
        widthCt = 230;
      } else if (widthCt > 480) {
        widthCt = 480;
      }

      popupContent.css({ width : widthCt + 'px' });
      popupContent.find('.arrow:first').css({ top : '30%' });

      var top = -((popupContent.height() * 30 / 100) + 16);
      popupContent.css({ 'top' : top + 'px', 'left' : '10px' }).show();
      popupContent.on('click mousedown mouseup', utils.cancelEvent);

      utils.addhideElement(popupContent);
      utils.cancelEvent(evt);
    },

    submitOnKey : function(id) {
      var parentElm = $.fn.findId(id);
      if(parentElm.exists() === false) {
        parentElm = $.fn.findId(UIForumPortlet.id).find('.'+id);
      }
      if (parentElm.exists()) {
        parentElm.off('keydown').on('keydown', function(evt) {
          var key = utils.getKeynum(evt);
          if (key == 13) {
            var searchLinkElm = $(this).find('.actionSubmitLink');
            if (searchLinkElm.exists()) {
              var link = "";
              if(searchLinkElm.is('a')) {
                link = String(searchLinkElm.attr('href')).replace('javascript:', '');
              } else {
                link = String(searchLinkElm.attr('data-link')).replace('javascript:', '');
              }
              $.globalEval(link);
              utils.cancelEvent(evt);
              evt.preventDefault();
            }
          }
        });
      }
    },

    calculateWidthOfActionBar : function(uiRightActionBar) {
      var uiRightActionBar = $.fn.findId(uiRightActionBar);
      var textContent = uiRightActionBar.text();
      textContent = textContent.replace(/\n/g, '').replace(/\s\s|\t\t|\r\r/g, '');
      var l = (textContent.length) * 1 + 1;
      uiRightActionBar.css('width', ((l * 7) + 70) + "px");
    }
  };

  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.UIForumPortlet = UIForumPortlet;
  return UIForumPortlet;
})(forumMaskLayer, forumContextMenu, forumUtils, platformLeftNavigation, gj, window, document);
