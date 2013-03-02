(function(maskLayer, contextMenu, utils, $, window, document) {
  var UIForumPortlet = {
    obj : null,
    event : null,
    wait : false,
    id : 'UIForumPortlet',

    init : function(id) {
      UIForumPortlet.id = id;
      var jportlet = findId(id);
      if (jportlet.exists()) {
        jportlet.find('.oncontextmenu').on('contextmenu', utils.returnFalse);
        jportlet.find('.UserMenuInfo').on('click', utils.showUserMenu);
        initTooltip();
      }
      utils.onResize(UIForumPortlet.resizeCallback);

      $.each($('ul.dropdown-menu').find('a'), function(i, item){
        $(item).on('click', function(){
          $(this).parents('.dropdown').removeClass('open');
        })
      });
    },
    
    initTooltip : function(id) {
      var jportlet = findId(UIForumPortlet.id);
      if(id != null) {
        jportlet.find('#'+id).find('[rel=tooltip]').tooltip();
      } else {
        jportlet.find('[rel=tooltip]').tooltip();
      }
    },

    resizeCallback : function() {
      utils.setMaskLayer(UIForumPortlet.id);
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

    numberIsCheckedForum : function(formName, checkAllName, multiAns, onlyAns, notChecked) {
      var total = 0;
      var form = document.forms[formName];
      if (form) {
        var checkboxs = form.elements;
        for ( var i = 0; i < checkboxs.length; i++) {
          if (checkboxs[i].type == "checkbox" && checkboxs[i].checked && checkboxs[i].name != "checkAll") {
            total = total + 1;
          }
        }
      }
      if (total > 1) {
        var text = String(multiAns).replace("?", "").replace('{0}', total) + " ?";
        return confirm(text);
      } else if (total == 1) {
        return confirm(String(onlyAns).replace("?", "") + " ?");
      } else {
        alert(notChecked);
        return false;
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
        window.console.log(check + ' ' + isChecked);
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
      var table = $(obj).parents('table');
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

    checkAction : function(obj, evt) {
      UIForumPortlet.showPopup(obj, evt);
      var uiCategory = $('UICategory');
      var checkboxes = uiCategory.find('input.checkbox');
      var uiRightClickPopupMenu = $(obj).find('.UIRightClickPopupMenu:first');
      var clen = checkboxes.length;
      var menuItems = uiRightClickPopupMenu.find('a');
      var mlen = menuItems.length;
      var checked = false;
      for ( var i = 1; i < clen; i++) {
        if (checkboxes[i].checked && checkboxes.eq(i).attr('name').indexOf("forum") == 0) {
          checked = true;
          break;
        }
      }
      var j = 0;
      for ( var i = 0; i < mlen; i++) {
        if (String(menuItems.eq(i).attr('class')).indexOf("AddForumIcon") > 0) {
          j = i + 1;
          break;
        }
      }
      for ( var n = j; n < mlen; n++) {
        var menuItem = menuItems.eq(n);
        if (!checked) {
          if (!menuItem.attr("tmpHref")) {
            menuItem.attr("tmpHref", menuItem.attr('href'));
            menuItem.attr('href', 'javascript:void(0);');
            menuItem.attr('tmpClass', $(menuItem[0].parentNode).attr('class'));
            $(menuItem[0].parentNode).attr('class', 'DisableMenuItem');
          }
        } else {
          if (menuItem.attr("tmpHref")) {
            menuItem.attr('href', menuItem.attr('tmpHref'));
            $(menuItem[0].parentNode).attr('class', menuItem.attr('tmpClass'));
            menuItem.removeAttr("tmpHref");
            menuItem.removeAttr("tmpClass");
          }
        }
      }
    },

    visibleAction : function(id) {
      var parent = findId(id);
      var addCategory = parent.find('div.AddCategory:first');
      if (addCategory.exists()) {
        var addForum = parent.find('div.AddForum:first');
        var isIE = document.all ? true : false;
        if ($("#UICategories").exists()) {
          addCategory.attr('class', "Icon AddCategory");
          addForum.attr('class', "Icon AddForum");
        } else if ($("#UICategory").exists()) {
          addCategory.attr('class', "Icon AddCategory DisableAction");
          addForum.attr('class', "Icon AddForum");
          if (isIE)
            addCategory.find(':first-child').attr('href', "javascript:void(0);");
          else
            addCategory.children().eq(1).attr('href', "javascript:void(0);");
        } else {
          addCategory.attr('class', "Icon AddCategory DisableAction");
          addForum.attr('class', "Icon AddForum DisableAction");
          if (isIE) {
            addCategory.find(':first-child').attr('href', "javascript:void(0);");
            addForum.find(':first-child').attr('href', "javascript:void(0);");
          } else {
            addCategory.children().eq(1).attr('href', "javascript:void(0);");
            addForum.children().eq(1).attr('href', "javascript:void(0);");
          }
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
      var forumToolbar = jobject.parents(".uiBox");
      var contentContainer = forumToolbar.find('.uiContentBox');
      if (contentContainer.css('display') != "none") {
        contentContainer.hide();
        jobject.attr( 'class', 'uiIconArrowRight pull-right');
        jobject.attr("title", jobject.attr("expand"));
      } else {
        contentContainer.show(100);
        jobject.attr( 'class', 'uiIconArrowDown pull-right');
        jobject.attr("title", jobject.attr("collapse"));
      }
    },

    showTreeNode : function(obj, isShow) {
      if (isShow === "false")
        return;
      var jobject = $(obj);
      var parentNode = jobject.parents(".ParentNode");
      var nodes = parentNode.find('div.Node');
      var selectedNode = jobject.parents(".Node");
      var nodeSize = nodes.length;
      var childrenContainer = null;
      for ( var i = 0; i < nodeSize; i++) {
        var node = nodes.eq(i);
        childrenContainer = node.find("div.ChildNodeContainer:first");
        if (node[0] === selectedNode[0]) {
          childrenContainer.show();
          node.addClass("Node SmallGrayPlus").removeClass('SmallGrayMinus');
        } else {
          childrenContainer.hide();
          if (node.hasClass("Node SmallGrayPlus false"))
            continue;
          node.addClass("Node SmallGrayMinus");
        }
      }
    },

    checkedNode : function(elm) {
      var jelm = $(elm);
      var jinput = jelm.find('input:first');

      var parentNode = jinput.parents('.Node');
      var containerChild = parentNode.find('div.ChildNodeContainer:first');
      if (containerChild.exists()) {
        var checkboxes = containerChild.find('input');
        for ( var i = 0; i < checkboxes.length; ++i) {
          if (jinput[0].checked)
            checkboxes[i].checked = (true);
          else
            checkboxes[i].checked = (false);
        }
      }
    },

    checkedChildNode : function(elm) {
      var input = $(elm).find('input:first');
      if (input.exists()) {
        if (input[0].checked) {
          var parentCheckBoxNode = elm.parent().parent().parent();
          var parentCheckBox = parentCheckBoxNode.find('div.ParentCheckBox:first');
          parentCheckBox.find('input:first')[0].checked = (true);
        }
      }
    },

    initVote : function(voteId, rate) {
      var vote = findId(voteId);
      vote.attr('rate', rate);
      rate = parseInt(rate);
      var optsContainer = vote.find('div.OptionsContainer:first');
      var options = optsContainer.children('div');
      options.on('mouseover', UIForumPortlet.overVote);
      options.on('blur', UIForumPortlet.overVote);
      for ( var i = 0; i < options.length; i++) {
        if (i < rate)
          options.eq(i).attr('class', 'RatedVote');
      }
      vote.on('mouseover', UIForumPortlet.parentOverVote);
      vote.on('blur', UIForumPortlet.parentOverVote);
      optsContainer.on('mouseover', utils.cancelEvent);
      optsContainer.on('blur', utils.cancelEvent);
    },

    parentOverVote : function(event) {
      var optsCon = $(this).find('div.OptionsContainer:first');
      var opts = optsCon.children('div');
      var rate = $(this).attr('rate');
      for ( var j = 0; j < opts.length; j++) {
        if (j < rate)
          opts.eq(j).addClass('RatedVote').removeClass('NormalVote');
        else
          opts.eq(j).addClass('NormalVote').removeClass('RatedVote');
      }
    },

    overVote : function(event) {
      var optsCon = $(this).parents('div.OptionsContainer:first');
      var opts = optsCon.children('div');
      var i = opts.length;
      for (--i; i >= 0; i--) {
        if (opts[i] == $(this)[0])
          break;
        opts.eq(i).attr('class', 'NormalVote');
      }
      if (opts.eq(i).attr('class') == "OverVote")
        return;
      for (; i >= 0; i--) {
        opts.eq(i).attr('class', 'OverVote');
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
            var viewPage = $('#KSMaskLayer');
            if (viewPage.exists())
              viewPage[0].scrollIntoView(true);
          }
        } else {
          var obj = document.getElementById(idLastPost);
          if (obj)
            obj.scrollIntoView(true);
        }
      }
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
        setTimeout('eXo.forum.UIForumPortlet.setSizeImages(130, "UIViewPrivateMessageForm")', 800);
      else
        setTimeout('eXo.forum.UIForumPortlet.setSizeImages(10, "UIViewPrivateMessageForm")', 400);
    },

    setSizeImages : function(delta, classParant) {
      var parent_ = findId(classParant);
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
      UIForumPortlet.showPicture($(this).attr('src'));
    },

    resetFielForm : function(idElm) {
      var elm = findId(idElm);
      elm.find("input:checkbox").val('false');
      elm.find("input:text").val('');
      if (elm.find("input.UISliderInput").exists()) {
        eXo.webui.UISliderControl.reset(elm.find("input.UISliderInput"));
      }
      elm.find("textarea").val('');
    },

    RightClickBookMark : function(elmId) {
      var ancestor = findId(elmId);
      var popupContents = ancestor.find('ul.ClickPopupContent');
      if (!popupContents.exists())
        return;
      var popupContainer = $('#RightClickContainer');
      if (!popupContainer.exists())
        return;
      var itemmenuBookMark = popupContainer.find('a.AddLinkToBookIcon:first');
      var itemmenuWatching = popupContainer.find('a.AddWatchingIcon:first');
      var itemmenuRSS = popupContainer.find('a.ForumRSSFeed:first');
      if (!itemmenuWatching.exists() || !itemmenuBookMark.exists())
        return;
      var labelWatchings = String(itemmenuWatching.html()).split(";");
      for ( var i = 0; i < popupContents.length; i++) {
        var popupContent = popupContents.eq(i);
        var action = popupContent.attr('title');
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
            itemmenuWatching.html(labelWatchings[1]);
          } else {
            itemmenuWatching.html(labelWatchings[0]);
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
        popupContent.removeAttr('title');
        popupContent.html(popupContainer.html());
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

    loadScroll : function(e) {
      var uiNav = eXo.forum.UIForumPortlet;
      var container = $("#UIForumActionBar");
      if (container.exists()) {
        uiNav.scrollMgr = new navigation.ScrollManager("UIForumActionBar");
        uiNav.scrollMgr.initFunction = uiNav.initScroll;
        uiNav.scrollMgr.mainContainer = container.find('td.ControlButtonContainer:first')[0];
        uiNav.scrollMgr.arrowsContainer = container.find('li.ScrollButtons:first')[0];
        uiNav.scrollMgr.loadElements("ControlButton", true);

        var button = $(uiNav.scrollMgr.arrowsContainer).find('div');
        if (button.length >= 2) {
          uiNav.scrollMgr.initArrowButton(button[0], "left", "ScrollLeftButton", "HighlightScrollLeftButton", "DisableScrollLeftButton");
          uiNav.scrollMgr.initArrowButton(button[1], "right", "ScrollRightButton", "HighlightScrollRightButton", "DisableScrollRightButton");
        }

        uiNav.scrollManagerLoaded = true;
        uiNav.initScroll();
        var lastButton = $('#OpenBookMarkSp');
        if (lastButton.exists() && lastButton.css('display') == 'none') {
          $('#OpenBookMark').hide();
        }
      }
    },

    initScroll : function() {
      var uiNav = UIForumPortlet;
      if (!uiNav.scrollManagerLoaded)
        uiNav.loadScroll();
      var elements = uiNav.scrollMgr.elements;
      uiNav.scrollMgr.init();
      if (eXo.core.Browser.isIE6())
        uiNav.scrollMgr.arrowsContainer.setAttribute("space", 35);
      uiNav.scrollMgr.checkAvailableSpace();
      uiNav.scrollMgr.renderElements();
    },

    loadTagScroll : function() {
      var uiNav = eXo.forum.UIForumPortlet;
      var container = $("#TagContainer");
      if (container) {
        uiNav.tagScrollMgr = new navigation.ScrollManager("TagContainer");
        uiNav.tagScrollMgr.initFunction = uiNav.initTagScroll;
        uiNav.tagScrollMgr.mainContainer = container[0];
        uiNav.tagScrollMgr.arrowsContainer = container.find('li.ScrollButtons')[0];

        uiNav.tagScrollMgr.cleanElements();
        uiNav.tagScrollMgr.elements.clear();
        var items = $(uiNav.tagScrollMgr.mainContainer).find('li.' + "MenuItem");
        for ( var i = 0; i < items.length; i++) {
          uiNav.tagScrollMgr.elements.push(items[i]);
        }

        var button = $(uiNav.tagScrollMgr.arrowsContainer).find('div');
        if (button.length >= 2) {
          uiNav.tagScrollMgr.initArrowButton(button[0], "left", "ScrollLeftButton", "HighlightScrollLeftButton", "DisableScrollLeftButton");
          uiNav.tagScrollMgr.initArrowButton(button[1], "right", "ScrollRightButton", "HighlightScrollRightButton", "DisableScrollRightButton");
        }

        uiNav.scrollManagerLoaded = true;
        uiNav.initTagScroll();

      }
    },

    initTagScroll : function() {
      var uiNav = UIForumPortlet;
      var elements = uiNav.tagScrollMgr.elements;
      var jarrowsContainer = $(uiNav.tagScrollMgr.arrowsContainer);
      var menu = jarrowsContainer.find('ul.UIRightPopupMenuContainer:first')[0];
      var tmp = null;
      uiNav.setTagContainerWidth(uiNav.tagScrollMgr.mainContainer);
      uiNav.tagScrollMgr.init();
      uiNav.tagScrollMgr.checkAvailableSpace();

      removeChildren(menu);

      jarrowsContainer.on('mouseover', over);
      jarrowsContainer.on('focus', over);
      jarrowsContainer.on('mouseout', out);
      jarrowsContainer.on('blur', out);
      for ( var i = 0; i < elements.length; i++) {
        if (elements[i].isVisible) {
          $(elements[i]).show();
        } else {
          tmp = $(elements[i].cloneNode(true));
          tmp.removeClass('FloatLeft').addClass('TagItem').show();
          $(menu).append(tmp);
          $(elements[i]).hide();
          jarrowsContainer.show();
        }
      }

      setPosition(menu);
      function removeChildren(cont) {
        $(cont).find('div.MenuTagContainer:first').remove();
      }

      function setPosition(menu) {
        var uiPopupCategory = $(menu).parents('.UIPopupCategory');
        uiPopupCategory.show();
        uiPopupCategory.attr('style', 'top:24px; left:-400px;');
        uiPopupCategory.hide();
      }

      function over() {
        $(this).addClass('ScrollButtonsOver');
      }

      function out() {
        $(this).removeClass('ScrollButtonsOver');
      }
    },

    setTagContainerWidth : function(container) {
      var nodes = $(container.parentNode).find('div');
      var width = 0;
      var i = nodes.length;
      while (i--) {
        if ((nodes[i].className == container.className) || !nodes[i].className)
          continue;
        if (nodes[i].className == "UIForumPageIterator") {
          var right = $(nodes[i]).find('div.RightPageIteratorBlock:first')[0];
          var left = $(nodes[i]).find('div.LeftPageIteratorBlock:first')[0];
          width += getWidth(left, "div") + getWidth(right, "a");
          continue;
        }
        width += UIForumPortlet.tagScrollMgr.getElementSpace(nodes[i]);
      }
      width = UIForumPortlet.tagScrollMgr.getElementSpace(container.parentNode) - width - 15;
      container.style.width = width + "px";
      // Private method to get real width of the element by html tag name
      function getWidth(obj, tag) {
        if (!obj)
          return 0;
        var children = $(obj).find(tag);
        var w = 0;
        var i = children.length;
        while (i--) {
          w += children[i].offsetWidth;
        }
        return w;
      }
    },

    executeLink : function(evt) {
      var onclickAction = String($(this).attr("rel"));
      eval(onclickAction);
      utils.cancelEvent(evt);
      return false;
    },

    createLink : function(cpId, isAjax) {
      if (!isAjax || isAjax === 'false') {
        var isM = document.getElementById("SetMode");
        if (isM && isM.innerHTML === 'true') {
          UIForumPortlet.addLink(cpId, "ActionIsMod");
        }
        return;
      }
      UIForumPortlet.addLink(cpId, "ActionLink");
    },

    addLink : function(cpId, clazzAction) {
      var links = findId(cpId).find('a.' + clazzAction);
      if (links.exists()) {
        links.on('click', UIForumPortlet.executeLink);
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
        uiContextMenu.classNames = new Array("ActionLink");
      else
        uiContextMenu.classNames.push("ActionLink");
      uiContextMenu.setContainer(cont);
      uiContextMenu.setup();
    },

    showBBCodeHelp : function(id, isIn) {
      var parentElm = document.getElementById(id);
      var popupHelp = document.getElementById(id + "ID");
      if (parentElm) {
        if (isIn == "true") {
          popupHelp.style.display = "block";
          var contentHelp = $(popupHelp).find('div.ContentHelp:first')[0];
          contentHelp.style.height = "auto";
          var l = String(contentHelp.innerHTML).length;
          if (l < 100) {
            contentHelp.style.width = (l * 4) + "px";
            contentHelp.style.height = "45px";
          } else {
            contentHelp.style.width = "400px";
            if (l > 150) {
              contentHelp.style.height = "auto";
            } else {
              contentHelp.style.height = "45px";
            }
          }
          var parPopup = document.getElementById("UIForumPopupWindow");
          var parPopup2 = document.getElementById("UIForumChildPopupWindow");
          var left = 0;
          var worksPace = document.getElementById('UIWorkingWorkspace');
          var worksPaceW = 1 * 1;
          if (worksPace) {
            worksPaceW = (worksPace.offsetWidth) * 1;
          } else {
            worksPaceW = (document.getElementById('UIPortalApplication').offsetWidth) * 1;
          }
          left = (parPopup.offsetLeft) * 1 + (parPopup2.offsetLeft) * 1 + parentElm.offsetLeft + parentElm.parentNode.offsetLeft;
          if (left + popupHelp.offsetWidth > worksPaceW) {
            popupHelp.style.left = "-" + (contentHelp.offsetWidth + 18) + "px";
            popupHelp.className = "RightBBCodeHelpPopup";
          } else {
            popupHelp.className = "LeftBBCodeHelpPopup";
            popupHelp.style.left = "-2px";
          }
        } else {
          popupHelp.style.display = "none";
        }
      }
    },

    submitSearch : function(id) {
      var parentElm = document.getElementById(id);
      if (parentElm) {
        parentElm.onkeydown = UIForumPortlet.submitOnKey;
      }
    },

    submitOnKey : function(event) {
      var key = utils.getKeynum(event);
      if (key == 13) {
        var searchLinkElm = $(this).find('a.SearchLink');
        if (searchLinkElm.exists()) {
          var link = String(searchLinkElm.attr('href')).replace("javascript:", "");
          eval(link);
          utils.cancelEvent(event);
          return false;
        }
      }
    },

    calculateWidthOfActionBar : function(uiRightActionBar) {
      var uiRightActionBar = findId(uiRightActionBar);
      var textContent = uiRightActionBar.text();
      textContent = textContent.replace(/\n/g, '').replace(/\s\s|\t\t|\r\r/g, '');
      var l = (textContent.length) * 1 + 1;
      uiRightActionBar.css('width', ((l * 6.5) + 65) + "px");
    }
  };

  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.UIForumPortlet = UIForumPortlet;
  return UIForumPortlet;
})(maskLayer, contextMenu, utils, gj, window, document);