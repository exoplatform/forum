;(function($, window, document) {

  function UIAnswersPortlet() {
    this.viewImage = true;
    this.scrollManagerLoaded = false;
    this.hiddentMenu = true;
    this.scrollMgr = [];
    this.portletId = 'UIAnswersPortlet';
  };
  
  UIAnswersPortlet.prototype.init = function (portletId) {
    this.portletId = new String(portletId);
    this.updateContainersHeight();
    this.controlWorkSpace();
    this.disableContextMenu(this.portletId);
    eXo.core.Browser.addOnResizeCallback(portletId, this.resizeCallback)
    eXo.core.Browser.init();
  };

  UIAnswersPortlet.prototype.resizeCallback = function() {
    eXo.forum.ForumUtils.setMaskLayer(this.portletId);
  };
  
  UIAnswersPortlet.prototype.updateContainersHeight = function () {
    var viewQuestionContentEl = findId(this.portletId + ' div.CategoriesContainer div.ViewQuestionContent');
    viewQuestionContentEl.css('height', viewQuestionContentEl.height() - 67);
  };
  
  UIAnswersPortlet.prototype.controlWorkSpace = function () {
    $('#ControlWorkspaceSlidebar div.SlidebarButton').on('click', this.onClickSlidebarButton);
    setTimeout(this.reSizeImages, 1500);
  };
  
  UIAnswersPortlet.prototype.disableContextMenu = function (id) {
    var oncontextmenus = findId(id + ' .disableContextMenu');
    for (var i = 0; i < oncontextmenus.length; i++) {
      oncontextmenus.eq(i).on('contextmenu', function() {
        return false;
      });
    }
  };
  
  UIAnswersPortlet.prototype.selectCateInfor = function (number) {
    var obj = null;
    for (var i = 0; i < 3; i++) {
      obj = $('#uicategoriesCateInfors' + i);
      if (i == number) obj.css('fontWeight', 'bold');
      else obj.css('fontWeight', 'normal');
    }
  };
  
  UIAnswersPortlet.prototype.setCheckEvent = function (isCheck) {
    this.hiddentMenu = isCheck;
  };
  
  UIAnswersPortlet.prototype.viewTitle = function (id) {
    findId(id).css('display', 'block');
    this.hiddentMenu = false;
  };
  
  UIAnswersPortlet.prototype.hiddenTitle = function (id) {
    findId(id).css('display', 'none');
  };
  
  UIAnswersPortlet.prototype.hiddenMenu = function () {
    if (this.hiddentMenu) {
      this.hiddenTitle('FAQCategroManager');
      this.hiddentMenu = false;
    }
    setTimeout('eXo.answer.UIAnswersPortlet.checkAction()', 1000);
  };
  
  UIAnswersPortlet.prototype.checkAction = function () {
    if (this.hiddentMenu) {
      setTimeout('eXo.answer.UIAnswersPortlet.hiddenMenu()', 1500);
    }
  };
  
  UIAnswersPortlet.prototype.checkCustomView = function (isNotSpace, hideTitle, showTitle) {
    var cookie = eXo.core.Browser.getCookie('FAQCustomView');
    cookie = (cookie == 'none' || cookie == '' && isNotSpace == 'false') ? 'none' : '';
    $('#FAQViewCategoriesColumn').css('display', cookie);
    
    var title = $('#FAQTitlePanels');
    if (cookie == 'none') {
      $('#FAQCustomView').addClass('FAQCustomViewRight');
      title.attr('title', showTitle);
    } else {
      title.attr('title', hideTitle);
      cookie = 'block';
    }
    eXo.core.Browser.setCookie("FAQCustomView", cookie, 1);
  };
  
  UIAnswersPortlet.prototype.changeCustomView = function (change, hideTitle, showTitle) {
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
    if ($.isFunction(this.initActionScroll)) this.initActionScroll();
    if ($.isFunction(this.initBreadCumbScroll)) this.initBreadCumbScroll();
  };
  
  UIAnswersPortlet.prototype.changeStarForVoteQuestion = function (i, id) {
    findId(id + i).attr('class', 'OverVote');
    
    for (var j = 0; j <= i; j++) {
      findId(id + j).attr('class', 'OverVote');
    }
    
    for (var j = i + 1; j < 5; j++) {
      obj = findId(id + j).attr('class', 'RatedVote');
    }
  };
  
  UIAnswersPortlet.prototype.jumToQuestion = function (id) {
    var viewContent = findId(id.substring(id.lastIndexOf('/') + 1)).parents('.ViewQuestionContent');
    if (viewContent.exists()) {
      viewContent.scrollTop(viewContent.position().top);
    }
  };
  
  // Remove UIAnswersPortlet.prototype.OverButton function.
  
  UIAnswersPortlet.prototype.viewDivById = function (id) {
    var obj = findId(id);
    if (obj.css('display') === 'none') {
      obj.css('display', 'block');
    } else {
      obj.css('display', 'none');
      findId(id.replace('div', '')).val('');
    }
  };
  
  // Remove UIAnswersPortlet.prototype.FAQViewAllBranch function.
  // Remove UIAnswersPortlet.prototype.hidePicture function.
  
  UIAnswersPortlet.prototype.showPicture = function (src) {
    if (this.viewImage) {
      eXo.forum.MaskLayerControl.showPicture(src);
    }
  };
  
  // Remove UIAnswersPortlet.prototype.getImageSize function.
  // Remove UIAnswersPortlet.prototype.showFullScreen function.
  // Remove UIAnswersPortlet.prototype.showMenu function.
  
  UIAnswersPortlet.prototype.printPreview = function (obj) {
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
    dummyPortlet = this.removeLink(dummyPortlet);
    dummyPortlet.css('width', '98.5%');
    this.removeLink(dummyPortlet).insertBefore(uiPortalApplication);
    uiPortalApplication.hide();
    $(window).scrollTop(0).scrollLeft(0);
  
    cancelAction.on('click', eXo.answer.UIAnswersPortlet.closePrint);
    printAction.on('click', window.print);
  
    this.viewImage = false;
  };
  
  UIAnswersPortlet.prototype.printAll = function (obj) {
    var container = $('<div></div>').addClass('UIAnswersPortlet');
    if (typeof (obj) == 'string') obj = findId(obj);
    $('#UIWorkingWorkspace').hide();
    container.append(obj.clone());
    $('body').append(container);
  };
  
  // Remove UIAnswersPortlet.prototype.closePrintAll function.
  
  UIAnswersPortlet.prototype.removeLink = function (rootNode) {
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
  
  // Remove UIAnswersPortlet.prototype.findDescendantsByAttribute function.
  
  UIAnswersPortlet.prototype.closePrint = function () {
    $('#UIPortalApplication').css('display', 'block');
    var children = $('body').children();
    for (var i = 0; i < children.length; i++) {
      if (children.eq(i).hasClass('UIAnswersPortlet')) children.eq(i).remove();
    }
    $(window).scrollTop(0).scrollLeft(0);
    this.viewImage = true;
  };

  UIAnswersPortlet.prototype.loadActionScroll = function () {
    var uiNav = eXo.answer.UIAnswersPortlet;
    var container = $("#UIQuestions");
    if (container.exists()) {
      var callback = uiNav.initActionScroll;
      uiNav.loadScroll("UIQuestions", container, callback);
    }
  };

  UIAnswersPortlet.prototype.loadBreadcumbScroll = function () {
    var uiNav = eXo.answer.UIAnswersPortlet;
    var container = $("#UIBreadcumbs");
    if (container.exists()) {
      var callback = uiNav.initBreadcumbScroll;
      uiNav.loadScroll("UIBreadcumbs", container, callback);
    }
  };

  UIAnswersPortlet.prototype.initBreadcumbScroll = function () {
    if ($('#UIPortalApplication').css('display') == 'none') return;
    var uiNav = eXo.answer.UIAnswersPortlet;
    if(uiNav.scrollMgr['UIBreadcumbs'] != undefined) {
      uiNav.scrollMgr['UIBreadcumbs'].init();
      uiNav.scrollMgr['UIBreadcumbs'].checkAvailableSpace();
      if (uiNav.scrollMgr['UIBreadcumbs'].arrowsContainer) {
        uiNav.scrollMgr['UIBreadcumbs'].renderElements();
      }
    }
  };

  UIAnswersPortlet.prototype.loadScroll = function (scrollname, container, callback) {
    var uiNav = eXo.answer.UIAnswersPortlet;
    var controlButtonContainer = container.find('td.ControlButtonContainer:first');
    if (container.exists() && controlButtonContainer.exists()) {
      uiNav.scrollMgr[scrollname] = new ScrollManager(scrollname);
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

  UIAnswersPortlet.prototype.scrollCallback = function () {};

  UIAnswersPortlet.prototype.initActionScroll = function () {
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
  
  UIAnswersPortlet.prototype.onClickSlidebarButton = function () {
    if ($('#UIWorkspaceContainer').css('display') == none) setTimeout(this.reSizeImages, 500);
  };
  
  UIAnswersPortlet.prototype.reSizeImagesView = function () {
    setTimeout('eXo.answer.UIAnswersPortlet.setSizeImages(10, "SetWidthImageContent")', 1000);
  };
  
  UIAnswersPortlet.prototype.reSizeImages = function () {
    eXo.answer.UIAnswersPortlet.setSizeImages(10, 'SetWidthContent');
  };
  
  UIAnswersPortlet.prototype.setSizeImages = function (delta, classParant) {
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
            images_[i].onclick = eXo.answer.UIAnswersPortlet.showImage;
          }
        }
      }
    }
  };
  
  UIAnswersPortlet.prototype.showImage = function () {
    this.showPicture(this.src);
  };
  
  UIAnswersPortlet.prototype.FAQChangeHeightToAuto = function () {
    $('#UIFAQPopupWindow').find('div.PopupContent:first').css({'height':'auto', 'maxHeight':'500px'});
  };
  
  UIAnswersPortlet.prototype.initContextMenu = function (id) {
    var cont = findId(id);
    if (cont.exists()) {
      this.disableContextMenu(id);
      var uiContextMenu = eXo.forum.UIContextMenu;
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
  
  UIAnswersPortlet.prototype.setSelectboxOnchange = function (id) {
    if (!$.browser.mozilla) return;
    var select = findId(id).find('select.selectbox:first');
    if (select.exists()) select.attr('onchange',
                                      select.attr('onchange').replace('javascript:', 'javascript:eXo.answer.UIAnswersPortlet.setDisableSelectbox(this);'));
  };
  
  UIAnswersPortlet.prototype.setDisableSelectbox = function (selectbox) {
    selectbox.disabled = true;
  };
  
  UIAnswersPortlet.prototype.voteAnswerUpDown = function (imageId, isVote) {
    var obj = findId(imageId);
    if (isVote) obj.css({'filter':' alpha(opacity: 100)', 'MozOpacity':1});
    else obj.css({'filter':' alpha(opacity: 70)', 'MozOpacity':0.7});
  };
  
  UIAnswersPortlet.prototype.openDiscussLink = function (link) {
    link = link.replace(/&amp;/g, "&");
    window.open(link);
  };
  
  UIAnswersPortlet.prototype.executeLink = function (evt) {
    var onclickAction = String(this.getAttribute('actions'));
    eval(onclickAction);
    eXo.forum.ForumUtils.cancelEvent(evt);
    return false;
  };
  
  // Remove UIAnswersPortlet.prototype.createLink function.
  
  UIAnswersPortlet.prototype.showTreeNode = function (obj, isShow) {
    if (isShow === "false") return;
    var parentNode = $(obj).parents('.ParentNode');
    var nodes = parentNode.find('div.Node');
    var selectedNode = obj.parents('.Node');
    var nodeSize = nodes.length;
    var childrenContainer = null;
    for (var i = 0; i < nodeSize; i++) {
      childrenContainer = $(nodes.eq(i)).find('div.ChildNodeContainer:first');
      if (nodes.eq(i) === selectedNode) {
        childrenContainer.css('display', 'block');
        $(nodes.eq(i)).attr('class', 'Node SmallGrayPlus');
      } else {
        childrenContainer.css('display', 'none');
        if ($(nodes.eq(i)).attr('class') === "Node SmallGrayPlus false") continue;
        $(nodes.eq(i)).attr('class', 'Node SmallGrayMinus');
      }
    }
  };
  
  UIAnswersPortlet.prototype.submitSearch = function (id) {
    findId(id).on('keydown', this.submitOnKey);
  };
  
  UIAnswersPortlet.prototype.submitOnKey = function (event) {
    var key = eXo.forum.ForumUtils.getKeynum(event);
    if (key == 13) {
      $(this).find('div.ActionSearch:first').click();
      eXo.core.EventManager.cancelEvent(event);
      return false;
    }
  };
  
  ScrollManager.prototype.answerLoadItems = function (elementClass, clean) {
    if (clean) this.cleanElements();
    this.elements.clear();
    this.elements.pushAll($(this.mainContainer).find('.' + elementClass).toArray().reverse());
  };
  
  // Expose
  window.eXo = eXo || {};
  window.eXo.answer = eXo.answer || {} ;
  window.eXo.answer.UIAnswersPortlet = new UIAnswersPortlet();

})(gj, window, document);
