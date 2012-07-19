if (!eXo.ks)
  eXo.ks = {};

function Tour() {
  childBts = null;
  t = 0;
};

Tour.prototype.init = function() {
  this.parent_ = document.getElementById('UIKSGuidedTour');
  this.childBts = eXo.core.DOMUtil.findDescendantsByClass(this.parent_, 'li', 'Button');
};

Tour.prototype.showContent = function(obj) {
  var t = eXo.ks.Tour.t;
  var childBts = eXo.ks.Tour.childBts;
  var parent_ = eXo.ks.Tour.parent_;
  var childContents = eXo.core.DOMUtil.findDescendantsByClass(parent_, 'div', 'ContentTour');
  for ( var i = 0; i < childBts.length; i++) {
    var child = childBts[i];
    if (child === obj) {
      child.className = "Button HightLineButton";
      childContents[i].style.display = "block";
      t = i;
    } else {
      child.className = "Button NormalButton";
      childContents[i].style.display = "none";
    }
  }
};

Tour.prototype.onMouseOverButton = function(obj) {
  var t = eXo.ks.Tour.t;
  var childBts = eXo.ks.Tour.childBts;
  var parent_ = eXo.ks.Tour.parent_;
  for ( var i = 0; i < childBts.length; i++) {
    var child = childBts[i];
    if (child.className != "Button HightLineButton") {
      if (child === obj) {
        child.className = "Button OverButton";
      } else if (i != t) {
        child.className = "Button NormalButton";
      }
    }
  }
};

Tour.prototype.onMouseOutButton = function(obj) {
  var t = eXo.ks.Tour.t;
  var childBts = eXo.ks.Tour.childBts;
  var parent_ = eXo.ks.Tour.parent_;
  for ( var i = 0; i < childBts.length; i++) {
    var child = childBts[i];
    if (child.className != "Button HightLineButton" && i != t) {
      child.className = "Button NormalButton";
    }
  }
};

eXo.ks.Tour = new Tour();