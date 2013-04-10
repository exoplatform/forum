(function(dragDrop, maskLayer, contextMenu, checkBoxManager, utils, $, document, window) {

  var UIAnswersPortlet = {
    viewImage : true,
    scrollManagerLoaded : false,
    hiddentMenu : true,
    scrollMgr : [],
    elements : [],
    portletId : 'UIAnswersPortlet'
  };
  
  UIAnswersPortlet.init = function (portletId) {
    UIAnswersPortlet.portletId = String(portletId);
    UIAnswersPortlet.updateContainersHeight();
    UIAnswersPortlet.controlWorkSpace();
    UIAnswersPortlet.disableContextMenu(UIAnswersPortlet.portletId);
    utils.onResize(UIAnswersPortlet.resizeCallback);
    UIAnswersPortlet.initTooltip();
  };
  
  UIAnswersPortlet.initTooltip = function(id) {
    var jportlet = $.fn.findId(UIAnswersPortlet.portletId);
    if(id != null) {
      jportlet.find('#'+id).find('[rel=tooltip]').tooltip();
    } else {
      jportlet.find('[rel=tooltip]').tooltip();
    }
  };

  UIAnswersPortlet.resizeCallback = function() {
    utils.setMaskLayer(UIAnswersPortlet.portletId);
  };
  
  UIAnswersPortlet.updateContainersHeight = function () {
    var viewQuestionContentEl = $.fn.findId(UIAnswersPortlet.portletId).find('div.CategoriesContainer');
    if(!viewQuestionContentEl.exists()) viewQuestionContentEl = $.fn.findId(UIAnswersPortlet.portletId).find('div.ViewQuestionContent');
  };
  
  UIAnswersPortlet.controlWorkSpace = function () {
    $('#ControlWorkspaceSlidebar div.SlidebarButton').on('click', UIAnswersPortlet.onClickSlidebarButton);
    setTimeout(UIAnswersPortlet.reSizeImages, 1500);
  };
  
  UIAnswersPortlet.rightClickQuestionMenu = function () {
    var oncontextmenus = $('.questionRightClickMenu');
    oncontextmenus.off('contextmenu').on('contextmenu', function(evt) {
       var thiz = $(this);
       var menu = $('#'+thiz.attr('data-contextid'));
       if(menu.exists) {
         menu.parent().css('position', 'absolute');
         utils.hideElements();
         contextMenu.setPosition(thiz, menu, evt);
         utils.addhideElement(menu);
         utils.cancelEvent(evt);
         evt.preventDefault();
       }
    });
  };

  UIAnswersPortlet.disableContextMenu = function (id) {
    var oncontextmenus = $.fn.findId(id).find('.disableContextMenu');
    oncontextmenus.on('contextmenu', function() {
      return false;
    });
  };

  UIAnswersPortlet.processHeightLine = function() {
    var portlet = $.fn.findId(UIAnswersPortlet.portletId);
    var pageBody = $('#UIPageBody');
    var trContainer = pageBody.parents('tr.TRContainer');
    var leftTDContainer = trContainer.find('td.LeftNavigationTDContainer');
    var leftHeight = leftTDContainer.outerHeight();
    var delta = leftHeight - pageBody.outerHeight();
    var answerContainer = portlet.find('div.uiAnserContainer:first');;
      var line = answerContainer.find('#resizeLineBar').find('div.line');
    var height = answerContainer.outerHeight();
    
    if (delta > 0) {
      height += delta;
    }
    line.css('height', (height) + 'px');
  };
  
  UIAnswersPortlet.resizeLineBar = function(idPr) {
    UIAnswersPortlet.currentPosW = 0;
    UIAnswersPortlet.currentW = 0;
    UIAnswersPortlet.isDownLine = false;

    var answerContainer = $('#' + idPr); 
    var parent = answerContainer.find('#resizeLineBar');
    var line = parent.find('div.line');
    line.on('mousedown', function(e) {
      UIAnswersPortlet.currentPosW = e.clientX;
      var portlet = $.fn.findId(UIAnswersPortlet.portletId);
      var leftColumn = portlet.find('.leftColumn:first');
      var rightColumn = portlet.find('.rightColumn:first');
      UIAnswersPortlet.currentW = leftColumn.width();
      UIAnswersPortlet.currentMargin = parseInt(rightColumn.css('margin-left'));
      UIAnswersPortlet.isDownLine = true;
    });
    $(document).on('mouseover', function(e) {
        if (UIAnswersPortlet.isDownLine) {
          var next = e.clientX;
          var deltaMove = next - UIAnswersPortlet.currentPosW;
          if (deltaMove != 0) {
            var portlet = $.fn.findId(UIAnswersPortlet.portletId);
            var leftColumn = portlet.find('.leftColumn:first');
            var rightColumn = portlet.find('.rightColumn:first');

            var magrinL = (UIAnswersPortlet.currentMargin + deltaMove);
            var width = (UIAnswersPortlet.currentW + deltaMove);
            if (width < 50 && deltaMove < 0) {
              leftColumn.css('width', '10px').hide(300);
              rightColumn.css('margin-left', '31px');
              portlet.css('padding-left', '0px');
              portlet.find('.line:first').hide();

              var iconArrow = portlet.find('a.iconControll:first').find('i:first');
              iconArrow.attr('class', 'uiIconMiniArrowRight pull-left');
              iconArrow.attr('data-original-title', iconArrow.data('tooltipInfo').show).tooltip();

              portlet.find('#resizeLineBar').addClass('resizeLt');
              UIAnswersPortlet.setCookie("FAQCustomView", 'none', 1);
            } else {
              leftColumn.css('width', width + 'px').show();
              rightColumn.css('margin-left', magrinL + 'px');
              portlet.find('#resizeLineBar').removeClass('resizeLt');
            }
          }
        }
      })
    .on('mouseup', function(e) {
      UIAnswersPortlet.isDownLine = false;
    });

    var aIconArrow = answerContainer.find('.iconControll:first');
    aIconArrow.on('click', function() {
      var thiz = $(this);
      var portlet = $.fn.findId(UIAnswersPortlet.portletId);
      var leftColumn = portlet.find('.leftColumn:first');
      var rightColumn = portlet.find('.rightColumn:first');
      var iconArrow = thiz.find('i:first');

      if (leftColumn.css('display') === 'block') {
        leftColumn.css({'overflow' : 'hidden'}).animate({
          'width' : '10px',
          'height' : (leftColumn.height() + 'px')
        }, 300, function() { 
          $(this).hide(); 
          portlet.css('padding-left', '0px');
          thiz.parent().find('.line').hide();
        });
        rightColumn.animate({'margin-left': '31px'}, 300, function(){});

        iconArrow.attr('class', 'uiIconMiniArrowRight pull-left');
        iconArrow.attr('data-original-title', iconArrow.data('tooltipInfo').show).tooltip();

        portlet.find('#resizeLineBar').addClass('resizeLt');
        UIAnswersPortlet.setCookie("FAQCustomView", 'none', 1);
      } else {
        portlet.css('padding-left', '20px');
        thiz.parent().find('.line').show();

        leftColumn.css({'visibility': 'hidden', 'height' : 'auto'}).show();
        var h = leftColumn.height();
        leftColumn.css('visibility', 'visible').animate({
          'width' : '220px',
          'height' : (h+'px')
        }, 300, function() { $(this).css({'overflow' : 'visible', 'height' : 'auto'});});
        rightColumn.animate({'margin-left': '255px'}, 300, function(){});

        iconArrow.attr('class', 'uiIconMiniArrowLeft pull-left');
        iconArrow.attr('data-original-title', iconArrow.data('tooltipInfo').hide).tooltip();

        portlet.find('#resizeLineBar').removeClass('resizeLt');
        UIAnswersPortlet.setCookie("FAQCustomView", 'block', 1);
      }
    })
  };
  
  
  UIAnswersPortlet.initVoteQuestion = function(id) {
    var parent = $.fn.findId(id);
    var voted = parent.find('div.inforVoted:first');
    var voting = parent.find('div.voting:first');
    voted.on('mouseover', function() {
      var thiz = $(this);
      thiz.hide();
      thiz.parent().find('div.voting:first').show();
    });
    voting.on('mouseout', function() {
      var thiz = $(this);
      thiz.hide();
      thiz.parent().find('div.inforVoted:first').show();
    });
  
    var stars = voting.find('> i');
    stars.on('mouseover', function(e) {
      var thiz = $(this);
      var index = thiz.attr('data-index');
      var stars = thiz.parent().find(' > i');
      stars.attr('class', 'unvoted');
      for(var i = 0; i < index; ++i) {
        stars.eq(i).attr('class', 'voted');
      }   
    })
    .on('mouseout', function() {
      $(this).parent().find(' > i').attr('class', 'unvoted');
    });
  };
  
  UIAnswersPortlet.selectCateInfor = function (number) {
    var obj = null;
    for (var i = 0; i < 3; i++) {
      obj = $('#uicategoriesCateInfors' + i);
      if (i == number) obj.css('fontWeight', 'bold');
      else obj.css('fontWeight', 'normal');
    }
  };
  
  UIAnswersPortlet.setCheckEvent = function (isCheck) {
    UIAnswersPortlet.hiddentMenu = isCheck;
  };
  
  UIAnswersPortlet.viewTitle = function (id) {
    $.fn.findId(id).css('display', 'block');
    UIAnswersPortlet.hiddentMenu = false;
  };
  
  UIAnswersPortlet.hiddenTitle = function (id) {
    $.fn.findId(id).css('display', 'none');
  };
  
  UIAnswersPortlet.hiddenMenu = function () {
    if (UIAnswersPortlet.hiddentMenu) {
      UIAnswersPortlet.hiddenTitle('FAQCategroManager');
      UIAnswersPortlet.hiddentMenu = false;
    }
    setTimeout('eXo.answer.UIAnswersPortlet.checkAction()', 1000);
  };
  
  UIAnswersPortlet.checkAction = function () {
    if (UIAnswersPortlet.hiddentMenu) {
      setTimeout('eXo.answer.UIAnswersPortlet.hiddenMenu()', 1500);
    }
  };
  
  UIAnswersPortlet.setCookie = function(name, value, expiredays) {
      var exdate = new Date();
      exdate.setDate(exdate.getDate() + 10);
      expiredays = ';expires=' + exdate.toGMTString();
      var path = ';path=/portal';
      document.cookie = name + "=" + escape(value) + expiredays + path;
  };
    
  UIAnswersPortlet.checkCustomView = function (isNotSpace, hideTitle, showTitle) {
    var cookie = eXo.core.Browser.getCookie('FAQCustomView');
    cookie = (cookie == 'none' || cookie == '' && isNotSpace === 'false') ? 'none' : '';

    var portlet = $.fn.findId(UIAnswersPortlet.portletId);
    var leftColumn = portlet.find('.leftColumn:first');
    var rightColumn = portlet.find('.rightColumn:first');
    var iconArrow = portlet.find('a.iconControll:first').find('i:first');
    var line = portlet.find('.line:first');
    var resizeLineBar = portlet.find('#resizeLineBar');

    iconArrow.data('tooltipInfo', {hide : hideTitle, show : showTitle});
    
    if (cookie == 'none') {
      line.hide();
      portlet.css('padding-left', '0px');
      leftColumn.hide();
      rightColumn.css('margin-left', '31px');
      resizeLineBar.addClass('resizeLt');
      iconArrow.attr('class', 'uiIconMiniArrowRight pull-left');
      iconArrow.attr('title', showTitle).tooltip();
    } else {
      line.show();
      portlet.css('padding-left', '20px');
      leftColumn.show();
      rightColumn.css('margin-left', '255px');
      resizeLineBar.removeClass('resizeLt');
      iconArrow.find('i:first').attr('class', 'uiIconMiniArrowLeft pull-left');
      iconArrow.attr('data-original-title', hideTitle).tooltip();
      cookie = 'block';
    }
    UIAnswersPortlet.setCookie("FAQCustomView", cookie, 1);
  };
  
  UIAnswersPortlet.changeCustomView = function (change, hideTitle, showTitle) {
    var columnCategories = $('#FAQViewCategoriesColumn');
    var buttomView = $('#FAQCustomView');
    var title = $('#FAQTitlePanels');
    var cookie = '';
    
    if (columnCategories.css('display') != 'none') {
      columnCategories.css('display', 'none');
      buttomView.addClass('FAQCustomViewRight');
      title.attr('title', showTitle);
      cookie = 'none';
    } else {
      columnCategories.css('display', '');
      buttomView.removeClass('FAQCustomViewRight');
      title.attr('title', hideTitle);
      cookie = 'block';
    }
    
    UIAnswersPortlet.setCookie("FAQCustomView", cookie, 1);
    if ($.isFunction(UIAnswersPortlet.initActionScroll)) UIAnswersPortlet.initActionScroll();
    if ($.isFunction(UIAnswersPortlet.initBreadCumbScroll)) UIAnswersPortlet.initBreadCumbScroll();
  };
  
  UIAnswersPortlet.changeStarForVoteQuestion = function (i, id) {
    $.fn.findId(id + i).attr('class', 'OverVote');
    
    for (var j = 0; j <= i; j++) {
      $.fn.findId(id + j).attr('class', 'OverVote');
    }
    
    for (var j = i + 1; j < 5; j++) {
      obj = $.fn.findId(id + j).attr('class', 'RatedVote');
    }
  };
  
  UIAnswersPortlet.jumToQuestion = function (id) {
    var viewContent = $.fn.findId(id.substring(id.lastIndexOf('/') + 1)).parents('.ViewQuestionContent');
    if (viewContent.exists()) {
      viewContent.scrollTop(viewContent.position().top);
    }
  };
  
  UIAnswersPortlet.viewDivById = function (id) {
    var obj = $.fn.findId(id);
    if (obj.css('display') === 'none') {
      obj.css('display', 'block');
    } else {
      obj.css('display', 'none');
      $.fn.findId(id.replace('div', '')).val('');
    }
  };
  
  UIAnswersPortlet.showPicture = function (src) {
    if (UIAnswersPortlet.viewImage) {
      maskLayer.showPicture(src);
    }
  };
  
  UIAnswersPortlet.printPreview = function (obj) {
    var uiPortalApplication = $("#UIPortalApplication");
    var answerContainer = $(obj).parents('.AnswersContainer');
    var printArea = answerContainer.find('div.QuestionSelect:first');
    printArea = printArea.clone();
    
    var dummyPortlet = $('<div></div>').addClass('UIAnswersPortlet UIPrintPreview');
    var FAQContainer = $('<div></div>').addClass('AnswersContainer');
    var FAQContent   = $('<div></div>').addClass('FAQContent');
    var printActions = $('<div></div>').addClass('UIAction')
                                       .css('display', 'block');
    var printActionInApp = answerContainer.find('div.PrintAction:first');
    var cancelAction = $('<a></a>').addClass('ActionButton LightBlueStyle')
                                   .attr('href', 'javascript:void(0);')
                                   .html(printActionInApp.attr('title'));
    var printAction = $('<a></a>').addClass('ActionButton LightBlueStyle')
                                  .html(printActionInApp.html());
  
    printActions.append(printAction);
    printActions.append(cancelAction);
  
    if (!$.browser.msie) {
      var cssContent = $('<div></div>').html('<style type="text/css">.DisablePrint{display:none;}</style>')
                                       .css('display', 'block');
      FAQContent.append(cssContent);
    }
    FAQContent.append(printArea);
    FAQContainer.append(FAQContent);
    FAQContainer.append(printActions);
    dummyPortlet.append(FAQContainer);
    if ($.browser.msie) {
      dummyPortlet.find('.DisablePrint').hide();
    }
    dummyPortlet = UIAnswersPortlet.removeLink(dummyPortlet);
    dummyPortlet.css('width', '98.5%');
    UIAnswersPortlet.removeLink(dummyPortlet).insertBefore(uiPortalApplication);
    uiPortalApplication.hide();
    $(window).scrollTop(0).scrollLeft(0);
  
    cancelAction.on('click', UIAnswersPortlet.closePrint);
    printAction.on('click', window.print);
  
    UIAnswersPortlet.viewImage = false;
  };
  
  UIAnswersPortlet.printAll = function (obj) {
    var container = $('<div></div>').addClass('PrintAllAnswersPortlet');
    if (typeof (obj) == 'string') obj = $.fn.findId(obj);
    $('#UIWorkingWorkspace').hide();
    obj.parents('#UIAnswersPopupWindow').hide();
    container.append(obj.clone());
    $('body').append(container);
  };
  
  UIAnswersPortlet.removeLink = function (rootNode) {
    rootNode.find('a').attr('href', 'javascript:void(0);');
    rootNode.find('a[onclick]').removeAttr('onclick');

    rootNode.find('div[onmousedown]')
            .removeAttr('onmousedown')
            .removeAttr('onkeydown');

    rootNode.find('div[onmouseover]')
            .removeAttr('onmouseover')
            .removeAttr('onmouseout');

    contextAnchors = rootNode.find('div[onclick]');
    var i = contextAnchors.length;
    while (i--) {
      if (contextAnchors.eq(i).hasClass('ActionButton')) continue;
      if (contextAnchors.eq(i).attr('onclick') != undefined) contextAnchors.eq(i).attr('onclick', 'javascript:void(0);');
    }
    return rootNode;
  };
  
  UIAnswersPortlet.closePrint = function () {
    $('#UIWorkingWorkspace').show();
    $(document.body).find('div.PrintAllAnswersPortlet').remove();
    $(window).scrollTop(0).scrollLeft(0);
    UIAnswersPortlet.viewImage = true;
  };

  UIAnswersPortlet.loadActionScroll = function () {
    var container = $("#UIQuestions");
    if (container.exists()) {
      UIAnswersPortlet.loadScroll("UIQuestions", container, UIAnswersPortlet.initActionScroll);
    }
  };

  UIAnswersPortlet.loadBreadcumbScroll = function () {
    var container = $("#UIBreadcumbs");
    if (container.exists()) {
      UIAnswersPortlet.loadScroll("UIBreadcumbs", container, UIAnswersPortlet.initBreadcumbScroll);
    }
  };

  UIAnswersPortlet.initBreadcumbScroll = function () {
    if ($('#UIPortalApplication').css('display') == 'none') return;
    if(UIAnswersPortlet.scrollMgr['UIBreadcumbs'] != undefined) {
      UIAnswersPortlet.scrollMgr['UIBreadcumbs'].init();
      UIAnswersPortlet.scrollMgr['UIBreadcumbs'].checkAvailableSpace();
      if (UIAnswersPortlet.scrollMgr['UIBreadcumbs'].arrowsContainer) {
        UIAnswersPortlet.scrollMgr['UIBreadcumbs'].renderElements();
      }
    }
  };

  UIAnswersPortlet.loadScroll = function (scrollname, container, callback) {
    var uiNav = UIAnswersPortlet;
    var controlButtonContainer = container.find('td.ControlButtonContainer:first');
    if (container.exists() && controlButtonContainer.exists()) {
      uiNav.scrollMgr[scrollname] = new navigation.ScrollManager(scrollname);
      uiNav.scrollMgr[scrollname].initFunction = callback;
      uiNav.scrollMgr[scrollname].mainContainer = controlButtonContainer.eq(0);
      uiNav.scrollMgr[scrollname].answerLoadItems('ControlButton');
      if (uiNav.scrollMgr[scrollname].elements.length <= 0) return;
      uiNav.scrollMgr[scrollname].arrowsContainer = controlButtonContainer.find('div.ScrollButtons:first').eq(0);
      var button = $(uiNav.scrollMgr[scrollname].arrowsContainer).find('div:first');

      if (button.length >= 2) {
        uiNav.scrollMgr[scrollname].initArrowButton(button.eq(0), "left", "ScrollLeftButton", "HighlightScrollLeftButton", "DisableScrollLeftButton");
        uiNav.scrollMgr[scrollname].initArrowButton(button.eq(1), "right", "ScrollRightButton", "HighlightScrollRightButton", "DisableScrollRightButton");
      }

      uiNav.scrollMgr[scrollname].callback = uiNav.scrollCallback;
      uiNav.scrollManagerLoaded = true;
      callback();
    }
  };

  UIAnswersPortlet.scrollCallback = function () {};

  UIAnswersPortlet.initActionScroll = function () {
    if ($('#UIPortalApplication').css('display') == 'none') return;
    var uiNav = eXo.answer.UIAnswersPortlet;
    if(uiNav.scrollMgr['UIQuestions'] != undefined) {
      uiNav.scrollMgr['UIQuestions'].init();
      uiNav.scrollMgr['UIQuestions'].checkAvailableSpace();
      uiNav.scrollMgr['UIQuestions'].renderElements();
    } else {
      uiNav.loadActionScroll();
    }
  };
  
  UIAnswersPortlet.onClickSlidebarButton = function () {
    if ($('#UIWorkspaceContainer').css('display') == none) setTimeout(UIAnswersPortlet.reSizeImages, 500);
  };
  
  UIAnswersPortlet.reSizeImagesView = function () {
    setTimeout('eXo.answer.UIAnswersPortlet.setSizeImages(10, "SetWidthImageContent")', 1000);
  };
  
  UIAnswersPortlet.reSizeImages = function () {
    UIAnswersPortlet.setSizeImages(10, 'SetWidthContent');
  };
  
  UIAnswersPortlet.setSizeImages = function (delta, classParant) {
    var widthContent = document.getElementById(classParant);
    if (widthContent) {
      var isDesktop = document.getElementById('UIPageDesktop');
      if (!isDesktop) {
        var max_width = widthContent.offsetWidth - delta;
        var max = max_width;
        if (max_width > 600) max = 600;
        var images_ = widthContent.getElementsByTagName("img");
        for (var i = 0; i < images_.length; i++) {
          var className = String(images_[i].className);
          if (className.indexOf("FAQAvatar") >= 0 || className.indexOf("AttachmentFile") >= 0) {
            continue;
          }
          var img = new Image();
          img.src = images_[i].src;
          if (img.width > max) {
            images_[i].style.width = max + "px";
            images_[i].style.height = "auto";
          } else {
            images_[i].style.width = "auto";
            if (images_[i].width > max) {
              images_[i].style.width = max + "px";
              images_[i].style.height = "auto";
            }
          }
          if (img.width > 600) {
            images_[i].onclick = UIAnswersPortlet.showImage;
          }
        }
      }
    }
  };
  
  UIAnswersPortlet.showImage = function () {
    UIAnswersPortlet.showPicture(this);
  };

  UIAnswersPortlet.FAQChangeHeightToAuto = function(id) {
    var parent = $.fn.findId(id);
    var uiWindow = parent.parents('.UIPopupWindow:first');
    var winH = $(window).height();
    var scrollTop = $(window).scrollTop();
    if (parent.find('.uiResponseForm').exists() || parent.find('.uiQuestionForm').exists()) {
      uiWindow.animate({
        top : scrollTop + 'px'
      }, 200, function() {
        var resizeAble = parent.parents('.resizable:first');
        if (resizeAble.height() < winH) {
          var mH = (winH - 150);
          resizeAble.css({
            height : mH + 'px',
            maxHeight : mH + 'px'
          });
        }
      });
    } else {
      var mH = (winH - 160);
      var resizeAble = parent.parents('.resizable:first');
      resizeAble.css({
        height : 'auto',
        maxHeight : mH + 'px'
      });
      var top = (winH - uiWindow.height()) / 2;
      uiWindow.animate({
        top : (scrollTop + top) + 'px'
      }, 200, function() { });
    }
  };
  
  UIAnswersPortlet.initContextMenu = function (id) {
    var cont = $.fn.findId(id);
    if (cont.exists()) {
      UIAnswersPortlet.disableContextMenu(id);
      var uiContextMenu = contextMenu;
      if (!uiContextMenu.classNames) {
        uiContextMenu.classNames = new Array("oncontextmenu", "QuestionContextMenu");
      } else {
        uiContextMenu.classNames.push("oncontextmenu");
        uiContextMenu.classNames.push("QuestionContextMenu");
      }
      uiContextMenu.setContainer(cont.eq(0));
      uiContextMenu.setup();
    }
  };
  
  UIAnswersPortlet.setSelectboxOnchange = function (id) {
    if (!$.browser.mozilla) return;
    var select = $.fn.findId(id).find('select.selectbox:first');
    if (select.exists()) {
      select.attr('onchange', 
          select.attr('onchange')
                .replace('javascript:', 'javascript:eXo.answer.UIAnswersPortlet.setDisableSelectbox(this);'));
    }
  };
  
  UIAnswersPortlet.setDisableSelectbox = function (selectbox) {
    selectbox.disabled = true;
  };
  
  UIAnswersPortlet.voteAnswerUpDown = function (imageId, isVote) {
    var obj = $.fn.findId(imageId);
    if (isVote) obj.css({'filter':' alpha(opacity: 100)', 'MozOpacity':1});
    else obj.css({'filter':' alpha(opacity: 70)', 'MozOpacity':0.7});
  };
  
  UIAnswersPortlet.openDiscussLink = function (link) {
    link = link.replace(/&amp;/g, "&");
    window.open(link);
  };
  
  UIAnswersPortlet.executeLink = function (evt) {
    var onclickAction = String(this.getAttribute('actions'));
    eval(onclickAction);
    utils.cancelEvent(evt);
    return false;
  };
  
  UIAnswersPortlet.initTreeNode = function(componentId) {
    var container = $.fn.findId(componentId);
    var treeContainer = container.find('div.treeContainer:first');
    treeContainer.find('.nodeGroup').hide();
    treeContainer.find('.nodeGroup:first').show();
  };
  
  UIAnswersPortlet.showTreeNode = function (obj) {
    var thiz = $(obj);
    var treeContainer = thiz.parents('div.treeContainer:first');
    treeContainer.find('.nodeGroup').hide();
    treeContainer.find('.nodeGroup:first').show();
    
    var parentNode = thiz.parents('li.node:first');
    var childrenNodeGroup = parentNode.find('ul.nodeGroup:first').show();
    
    var allNodes = treeContainer.find('a.uiIconNode');
    $.each(allNodes, function(id, elm) {
       var thizz = $(elm);
       if(thizz.hasClass('uiIconEmpty')) {
         thizz.removeClass('nodeSelected');
       } else {
         thizz.attr('class', 'uiIconNode collapseIcon');
       }
    });
    
    if(thiz.hasClass('uiIconEmpty') == false) {
      thiz.attr('class', 'uiIconNode expandIcon nodeSelected');
    } else {
      thiz.addClass('nodeSelected');
    }
    
    UIAnswersPortlet.showNode(thiz);
  };
  
  UIAnswersPortlet.showNode = function (obj) {
    if(!obj.parents('div.treeContainer').exists()) return;
    var parentNode = obj.parents('ul.nodeGroup:first').show().parents('li.node:first');
    if(parentNode.exists()) {
      var nThiz = parentNode.find('a.uiIconNode:first');
      if(nThiz.hasClass('uiIconEmpty') == false) {
        nThiz.attr('class', 'uiIconNode expandIcon');
      }
      UIAnswersPortlet.showNode(nThiz);
    }
  };

  UIAnswersPortlet.checkedNode = function(obj, evt) {
    var thizz = $(obj);
    if(obj.checked === true) {
      var nodes = thizz.parents('.nodeGroup:first').parents('.node');
      var inputs = nodes.find('input[type=checkbox]:first');
      inputs.prop("checked", obj.checked);
    }

    var nodeGroup = thizz.parents('.node:first').find('.nodeGroup:first');
    if (nodeGroup.length > 0) {
      var inputChilds = nodeGroup.find('.node').find('input[type=checkbox]:first');
      inputChilds.prop("checked", obj.checked);
    }
    utils.cancelEvent(evt);
  };
  
  UIAnswersPortlet.submitSearch = function (id) {
    $.fn.findId(id).on('keydown', UIAnswersPortlet.submitOnKey);
  };
  
  UIAnswersPortlet.submitOnKey = function (event) {
    var key = utils.getKeynum(event);
    if (key == 13) {
      $(this).find('div.actionAnswerSearch:first').click();
      eXo.core.EventManager.cancelEvent(event);
      return false;
    }
  };
  
  navigation.ScrollManager.prototype.answerLoadItems = function (elementClass, clean) {
    if (clean) UIAnswersPortlet.cleanElements();
    UIAnswersPortlet.elements.clear();
    UIAnswersPortlet.elements.pushAll($(UIAnswersPortlet.mainContainer).find('.' + elementClass).toArray().reverse());
  };
  
  // Expose
  window.eXo = eXo || {};
  window.eXo.answer = eXo.answer || {} ;
  window.eXo.answer.UIAnswersPortlet = UIAnswersPortlet;

  return UIAnswersPortlet;
})(answerDragDrop, forumMaskLayer, forumContextMenu, forumCheckBoxManager, forumUtils, gj, document, window);
