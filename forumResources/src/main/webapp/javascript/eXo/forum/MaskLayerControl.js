(function(utils, gj) {
  var MaskLayerControl = {

    init : function(root) {
      root = (typeof (root) == 'string') ? findId(root) : root;
      var jnodeList = gj(root).find('.AttachImage');
      jnodeList.on('click', function() {
        MaskLayerControl.showPicture(this);
      });
      
    },

    getContainerNode : function() {
      var containerNode = gj("#UIPictutreContainer");
      if (!containerNode.exists()) {
        containerNode = gj('<div></div>');
        containerNode
            .attr('id', 'UIPictutreContainer')
            .attr('style',
                'position:absolute; top:0px; width:100%; height:100%; text-align:center')
            .attr('title', 'Click to close or press key Esc.').on('click',
                MaskLayerControl.hidePicture);
        gj("#UIPortalApplication").append(containerNode)
      }
      return containerNode;
    },

    showPicture : function(node) {
      if (typeof (node) === "string") {
        var imgSrcNode = new Image();
        imgSrcNode.src = node;
      } else {
        var attachmentContent = gj(node).parents('div:first');
        var imgSrcNode = attachmentContent.find('img:first')[0];
      }
      var src = imgSrcNode.src;
      if (String(src).length > 0) {

        var imgSize = this.getImageSize(imgSrcNode);
        var windowHeight = parseInt(document.documentElement.clientHeight);
        var windowWidth = parseInt(document.documentElement.clientWidth);
        
        var imgWidth = "auto";
        if(windowWidth < parseInt(imgSize.width)) {
          imgWidth = windowWidth + 'px';
        }
        
        var marginTop = 0;
        var imgHeight = "auto";
        if(windowHeight < parseInt(imgSize.height)) {
          imgHeight = windowHeight + 'px';
          imgWidth = "auto";
        } else {
          marginTop = parseInt((windowHeight - parseInt(imgSize.height)) / 2)
        }
        
        var imageNode = "<img src='" + imgSrcNode.src + "' style='height:"
            + imgHeight + ";width:" + imgWidth + ";margin-top:" + marginTop
            + "px;' alt='Click to close or press key Esc.'/>";
        var containerNode = MaskLayerControl.getContainerNode();
        containerNode.html(imageNode);
        var maskNode = eXo.core.UIMaskLayer.createMask('UIPortalApplication',
            containerNode[0], 30, 'CENTER');
        gj(containerNode).find('img:first').on('click', function() {
          MaskLayerControl.hidePicture();
        });
        this.scrollHandler();
        gj(document).on('keydown', MaskLayerControl.hidePictureByKey);
      }
    },

    scrollHandler : function() {
      eXo.core.UIMaskLayer.object.style.top = gj('#MaskLayer').offset().top  + "px";
      MaskLayerControl.timer = setTimeout(MaskLayerControl.scrollHandler, 1);
    },

    hidePictureByKey : function(e) {
      if(e.which && e.which === 27) {
        MaskLayerControl.hidePicture();
        gj(document).off('keydown', MaskLayerControl.hidePictureByKey);
      }
    },

    hidePicture : function() {
      
      var maskContent = eXo.core.UIMaskLayer.object;
      gj('#UIPictutreContainer').remove();
      gj('#MaskLayer').remove();
      clearTimeout(MaskLayerControl.timer);
      delete MaskLayerControl.timer;
    },

    getImageSize : function(img) {
      var imgNode = new Image();
      imgNode.src = img.src;
      return {
        "height" : imgNode.height,
        "width" : imgNode.width
      };
    }
  };
  return MaskLayerControl;
})(utils, gj);
