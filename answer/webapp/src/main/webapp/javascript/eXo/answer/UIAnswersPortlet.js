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
  };

  UIAnswersPortlet.resizeCallback = function() {
    utils.setMaskLayer(UIAnswersPortlet.portletId);
  };
  
  UIAnswersPortlet.updateContainersHeight = function () {
    var viewQuestionContentEl = findId(UIAnswersPortlet.portletId).find('div.CategoriesContainer');
    if(!viewQuestionContentEl.exists()) viewQuestionContentEl = findId(UIAnswersPortlet.portletId).find('div.ViewQuestionContent');
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
    var oncontextmenus = findId(id).find('.disableContextMenu');
    oncontextmenus.on('contextmenu', function() {
      return false;
    });
  };

  UIAnswersPortlet.processHeightLine = function() {
    var portlet = findId(UIAnswersPortlet.portletId);
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
      var portlet = findId(UIAnswersPortlet.portletId);
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
            var portlet = findId(UIAnswersPortlet.portletId);
            var leftColumn = portlet.find('.leftColumn:first');
            var rightColumn = portlet.find('.rightColumn:first');

            var magrinL = (UIAnswersPortlet.currentMargin + deltaMove);
            var width = (UIAnswersPortlet.currentW + deltaMove);
            if (width < 50 && deltaMove < 0) {
              leftColumn.css('width', '10px').hide(300);
              rightColumn.css('margin-left', '31px');
              portlet.css('padding-left', '0px');
              portlet.find('.line:first').hide();
              var iconArrow = portlet.find('i.iconControll:first');
              iconArrow.attr('class', 'uiIconMiniArrowRight pull-left iconControll');
            } else {
              leftColumn.css('width', width + 'px').show();
              rightColumn.css('margin-left', magrinL + 'px');
            }
          }
        }
      })
    .on('mouseup', function(e) {
      UIAnswersPortlet.isDownLine = false;
    });

    var iconArrow = answerContainer.find('i.iconControll:first');
    iconArrow.on('click', function() {
      var thiz = $(this);
      var portlet = findId(UIAnswersPortlet.portletId);
      var leftColumn = portlet.find('.leftColumn:first');
      var rightColumn = portlet.find('.rightColumn:first');
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
        iconArrow.attr('class', 'uiIconMiniArrowRight pull-left iconControll');
      } else {
        portlet.css('padding-left', '20px');
        thiz.parent().find('.line').show();
        leftColumn.css({'visibility': 'hidden', 'height' : 'auto'}).show();
        var h = leftColumn.height();
        leftColumn.css('visibility', 'visible').animate({
          'width' : '220px',
          'height' : (h+'px')
        }, 300, function() { $(this).css({'overflow' : 'visible', 'height' : 'auto'});});
        rightColumn.animate({'margin-left': '250px'}, 300, function(){});
        iconArrow.attr('class', 'uiIconMiniArrowLeft pull-left iconControll');
      }
    })
  };
  
  
  UIAnswersPortlet.initVoteQuestion = function(id) {
  var parent = findId(id);
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
  } )
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
    findId(id).css('display', 'block');
    UIAnswersPortlet.hiddentMenu = false;
  };
  
  UIAnswersPortlet.hiddenTitle = function (id) {
    findId(id).css('display', 'none');
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
  
  UIAnswersPortlet.checkCustomView = function (isNotSpace, hideTitle, showTitle) {
    var cookie = eXo.core.Browser.getCookie('FAQCustomView');
    cookie = (cookie == 'none' || cookie == '' && isNotSpace == 'false') ? 'none' : '';
    $('#FAQViewCategoriesColumn').css('display', cookie);

    var title = $('#FAQTitlePanels');
    var portlet = findId(UIAnswersPortlet.portletId);
    var rightColumn = portlet.find('.rightColumn:first');
    if (cookie == 'none') {
      rightColumn.css('margin-left', '31px');
      $('#FAQCustomView').addClass('FAQCustomViewRight');
      title.attr('title', showTitle);
    } else {
      rightColumn.css('margin-left', '250px');
      title.attr('title', hideTitle);
      cookie = 'block';
    }
    eXo.core.Browser.setCookie("FAQCustomView", cookie, 1);
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
    
    eXo.core.Browser.setCookie("FAQCustomView", cookie, 1);
    if ($.isFunction(UIAnswersPortlet.initActionScroll)) UIAnswersPortlet.initActionScroll();
    if ($.isFunction(UIAnswersPortlet.initBreadCumbScroll)) UIAnswersPortlet.initBreadCumbScroll();
  };
  
  UIAnswersPortlet.changeStarForVoteQuestion = function (i, id) {
    findId(id + i).attr('class', 'OverVote');
    
    for (var j = 0; j <= i; j++) {
      findId(id + j).attr('class', 'OverVote');
    }
    
    for (var j = i + 1; j < 5; j++) {
      obj = findId(id + j).attr('class', 'RatedVote');
    }
  };
  
  UIAnswersPortlet.jumToQuestion = function (id) {
    var viewContent = findId(id.substring(id.lastIndexOf('/') + 1)).parents('.ViewQuestionContent');
    if (viewContent.exists()) {
      viewContent.scrollTop(viewContent.position().top);
    }
  };
  
  // Remove UIAnswersPortlet.OverButton function.
  
  UIAnswersPortlet.viewDivById = function (id) {
    var obj = findId(id);
    if (obj.css('display') === 'none') {
      obj.css('display', 'block');
    } else {
      obj.css('display', 'none');
      findId(id.replace('div', '')).val('');
    }
  };
  
  // Remove UIAnswersPortlet.FAQViewAllBranch function.
  // Remove UIAnswersPortlet.hidePicture function.
  
  UIAnswersPortlet.showPicture = function (src) {
    if (UIAnswersPortlet.viewImage) {
      maskLayer.showPicture(src);
    }
  };
  
  // Remove UIAnswersPortlet.getImageSize function.
  // Remove UIAnswersPortlet.showFullScreen function.
  // Remove UIAnswersPortlet.showMenu function.
  
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
    var container = $('<div></div>').addClass('UIAnswersPortlet');
    if (typeof (obj) == 'string') obj = findId(obj);
    $('#UIWorkingWorkspace').hide();
    container.append(obj.clone());
    $('body').append(container);
  };
  
  // Remove UIAnswersPortlet.closePrintAll function.
  
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
  
  // Remove UIAnswersPortlet.findDescendantsByAttribute function.
  
  UIAnswersPortlet.closePrint = function () {
    $('#UIPortalApplication').css('display', 'block');
    var children = $('body').children();
    for (var i = 0; i < children.length; i++) {
      if (children.eq(i).hasClass('UIAnswersPortlet')) children.eq(i).remove();
    }
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
    UIAnswersPortlet.showPicture(this.src);
  };
  
  UIAnswersPortlet.FAQChangeHeightToAuto = function () {
    $('#UIFAQPopupWindow').find('div.PopupContent:first').css({'height':'auto', 'maxHeight':'500px'});
  };
  
  UIAnswersPortlet.initContextMenu = function (id) {
    var cont = findId(id);
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
    var select = findId(id).find('select.selectbox:first');
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
    var obj = findId(imageId);
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
  
  // Remove UIAnswersPortlet.createLink function.
  UIAnswersPortlet.initTreeNode = function(componentId) {
    var container = findId(componentId);
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
    allNodes.attr('class', 'uiIconNode collapseIcon');
    
    thiz.attr('class', 'uiIconNode expandIcon nodeSelected');
    UIAnswersPortlet.showNode(thiz);
  };
  
  UIAnswersPortlet.showNode = function (obj) {
    if(!obj.parents('div.treeContainer').exists()) return;
    var parentNode = obj.parents('ul.nodeGroup:first').show().parents('li.node:first');
    if(parentNode.exists()) {
      var nThiz = parentNode.find('a.uiIconNode:first').attr('class', 'uiIconNode expandIcon');
      UIAnswersPortlet.showNode(nThiz);
    }
  };
  
  
  UIAnswersPortlet.submitSearch = function (id) {
    findId(id).on('keydown', UIAnswersPortlet.submitOnKey);
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
})(dragDrop, maskLayer, contextMenu, checkBoxManager, utils, gj, document, window);
