(function(utils, gj) {
  var UIPollPortlet = {
    portletId : "UIPollPortlet",
    init : function(elm) {
      if (elm && String(elm).length > 0) {
        this.portletId = String(elm);
      }
      utils.onResize(UIPollPortlet.onResizeMarkLayer);
      var jobj = gj('.CollapseButton');
      if (jobj.exists()) {
        jobj.click(UIPollPortlet.expandCollapse);
      }
    },
    onResizeMarkLayer : function() {
      utils.setMaskLayer(UIPollPortlet.portletId);
    },
    privateField : function(elm) {
      var jelm = findId(elm) || gj(elm);
      UIPollPortlet.clickPrivateField(jelm);
      jelm.click(UIPollPortlet.clickPrivateField);
    },
    clickPrivateField : function(elm) {
      var elm = (elm.type) ? this : elm;
      var jelm = gj(elm);
      if (jelm.exists()) {
        var parent = jelm.parents('.OptionField');
        var childs = parent.find('div.Display');
        var input = jelm.find('input.checkbox:first');
        if (input.exists()) {
          if (String(input.attr('checked')) === "checked") {
            childs.hide();
          } else {
            childs.show();
          }
        }
      }
    },
    expandCollapse : function() {
      var jobj = gj(this);
      var forumToolbar = jobj.parents('.ForumToolbar');
      var contentContainer = forumToolbar.next('div');
      if (contentContainer.css('display') != 'none') {
        contentContainer.css('display', 'none');
        jobj.attr('class', 'IconRight ExpandButton').attr('title',
            jobj.attr('expand'));
        forumToolbar.css('borderBottom', 'solid 1px #b7b7b7');
      } else {
        contentContainer.css('display', 'block');
        jobj.attr('class', 'IconRight CollapseButton').attr('title',
            jobj.attr('collapse'));
        forumToolbar.css('borderBottom', 'none');
      }
    }
  };
  return UIPollPortlet;
})(utils, gj);
