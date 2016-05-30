(function(utils, $) {
  var UIPollPortlet = {
    portletId : "UIPollPortlet",
    init : function(elm) {
      if (elm && String(elm).length > 0) {
        this.portletId = String(elm);
      }
      utils.onResize(UIPollPortlet.onResizeMarkLayer);
      var jobj = $('.actionExpandCollapse:first').find('i');
      if (jobj.exists()) {
        jobj.click(UIPollPortlet.expandCollapse);
      }
    },
    onResizeMarkLayer : function() {
      utils.setMaskLayer(UIPollPortlet.portletId);
    },
    setStyleOfTable : function(contId) {
      var cont = $('#'+contId);
      if (cont.exists()) {
        $.each(cont.find('.uiGrid:first').find('th'), function(index, elm) {
          if (index == 1) {
            elm.style.width = '60px';
          }
          if (index == 2 || index == 3) {
            elm.style.width = '160px';
          }
          if (index == 4) {
            elm.style.width = '100px';
          }
        });
      }
    },
    privateField : function(id) {
      var checkbox = $('#' + id);
      UIPollPortlet.clickPrivateField(checkbox);
      checkbox.click(UIPollPortlet.clickPrivateField);
    },
    clickPrivateField : function(elm) {
      elm = (elm.type) ? this : elm;
      var thizz = $(elm);
      if (thizz.exists() && thizz.is(':checkbox')) {
        var parent = thizz.parents('.form-horizontal:first');
        var input = parent.find('#GroupPrivate');
        var groupParent = input.parents('.control-group:first')
        groupParent.css('display', (thizz.prop('checked') === false) ? 'block' : 'none');
      }
    },
    expandCollapse : function(obj) {
      var jobject = $(this)
      var forumToolbar = jobject.parents(".uiCollapExpand");
      var contentContainer = forumToolbar.find('.uiExpandContainer');
      jobject.hide();
      $('div.tooltip').remove();
      if (contentContainer.css('display') != "none") {
        contentContainer.hide(200);
        forumToolbar.find('.uiIconArrowRight').show().tooltip();
      } else {
        contentContainer.show(200);
        forumToolbar.find('.uiIconArrowDown').show().tooltip();
      }
    }
  };
  return UIPollPortlet;
})(utils, gj);
