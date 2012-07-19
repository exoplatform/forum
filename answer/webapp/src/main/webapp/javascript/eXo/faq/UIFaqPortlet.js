if(!eXo.faq){
	eXo.faq = {} ;
}

function UIFaqPortlet() {
};


UIFaqPortlet.prototype.executeLink = function (evt) {
  var onclickAction = String(this.getAttribute("actions"));
  eval(onclickAction);
  eXo.ks.EventManager.cancelEvent(evt);
  return false;
};


UIFaqPortlet.prototype.createLink = function (cpId, isAjax) {
  if (!isAjax || isAjax === 'false') return;
  var comp = document.getElementById(cpId);
  var uiCategoryTitle = eXo.core.DOMUtil.findDescendantsByClass(comp, "a", "ActionLink");
  var i = uiCategoryTitle.length;
  if (!i || (i <= 0)) return;
  while (i--) {
    uiCategoryTitle[i].onclick = this.executeLink;
  }
};

UIFaqPortlet.prototype.checkedNode = function (elm) {
  var input = elm.getElementsByTagName("input")[0];
  var DOMUtil = eXo.core.DOMUtil;
  var parentNode = DOMUtil.findAncestorByClass(input, "FAQDomNode");
  var ancestorNode = DOMUtil.findAncestorByClass(parentNode, "FAQDomNode");
  if (ancestorNode) {
    firstInput = DOMUtil.findFirstDescendantByClass(ancestorNode, "input", "checkbox");
    if (input.checked && firstInput.checked === false) {
      var msg = document.getElementById('viewerSettingMsg');
      if (msg) {
        alert(msg.innerHTML);
      } else {
        alert('You need to check on parent or ancestor of this category first!');
      }
      input.checked = false;
      input.disabled = true;
    }
  }

  var containerChild = DOMUtil.findFirstDescendantByClass(parentNode, "div", "FAQChildNodeContainer");
  if (containerChild) {
    var checkboxes = containerChild.getElementsByTagName("input");
    for (var i = 0; i < checkboxes.length; ++i) {
      checkboxes[i].checked = input.checked;
      if (!input.checked) checkboxes[i].disabled = true;
      else checkboxes[i].disabled = false;
    }
  }
};

UIFaqPortlet.prototype.treeView = function (id) {
  var obj = document.getElementById(id);
  if (obj) {
    if (obj.style.display == '' || obj.style.display === "none") {
      obj.style.display = "block";
    } else {
      obj.style.display = "none";
    }
  }
};

eXo.faq.UIFaqPortlet = new UIFaqPortlet();