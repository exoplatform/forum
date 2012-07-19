;(function($, window, document) {

  function MaskLayerControl() {
  }
  
  MaskLayerControl.prototype.init = function(root){
    root = (typeof(root) == 'string') ? findId(root) : root ;
    var jnodeList = $(root).find('span.ViewDownloadIcon');
    for (var i=0; i<nodeList.length; i++) {
      jnodeList.eq(i).find('a:first').on('click', function() {
        eXo.forum.MaskLayerControl.showPicture(this) ;
      });
    }
  } ;
  
  MaskLayerControl.prototype.showPicture = function(node) {
  	if(typeof(node) == "string"){
  		var imgSrcNode = new Image();
  		imgSrcNode.src = node;
  	}else{
  	  var attachmentContent = $(node).parents('.AttachmentContent') ;
  	  var imgSrcNode = attachmentContent.find('img.AttachmentFile:first')[0] ;		
  	}
  	if(!document.getElementById("UIPictutreContainer")){		
  	  var containerNode = document.createElement('div') ;
  		containerNode.id = "UIPictutreContainer";
  	  with (containerNode.style) {
  			position = "absolute";
  			top = "0px";
  	    width = '100%' ;
  	    height = '100%' ;
  	    textAlign = 'center' ;
  	  }
  	  containerNode.setAttribute('title', 'Click to close') ;
  	  containerNode.onclick = this.hidePicture ;
  		document.getElementById("UIPortalApplication").appendChild(containerNode)
  	}else containerNode = document.getElementById("UIPictutreContainer");
  	var imgSize = this.getImageSize(imgSrcNode);
  	var windowHeight = document.documentElement.clientHeight;
  	var windowWidth = document.documentElement.clientWidth;
  	var marginTop = (windowHeight < parseInt(imgSize.height))?0:parseInt((windowHeight - parseInt(imgSize.height))/2);
  	var imgHeight = (windowHeight < parseInt(imgSize.height))?windowHeight + "px":"auto";
  	var imgWidth = (windowWidth < parseInt(imgSize.width))?windowWidth + "px":"auto";
  	var imageNode = "<img src='" + imgSrcNode.src +"' style='height:" + imgHeight + ";width:"+ imgWidth +";margin-top:" + marginTop + "px;' alt='Click to close'/>";
    containerNode.innerHTML = imageNode;
    var maskNode = eXo.core.UIMaskLayer.createMask('UIPortalApplication', containerNode, 30, 'CENTER') ;
  	this.scrollHandler();	
  } ;
  
  MaskLayerControl.prototype.scrollHandler = function() {	
    eXo.core.UIMaskLayer.object.style.top = document.getElementById("MaskLayer").offsetTop + "px" ;
  	eXo.forum.MaskLayerControl.timer = setTimeout(eXo.forum.MaskLayerControl.scrollHandler,1);
  } ;
  
  MaskLayerControl.prototype.hidePicture = function() {
    eXo.core.Browser.onScrollCallback.remove('MaskLayerControl') ;
    var maskContent = eXo.core.UIMaskLayer.object ;
    var maskNode = document.getElementById("MaskLayer") || document.getElementById("subMaskLayer") ;
    if (maskContent) maskContent.parentNode.removeChild(maskContent) ;
    if (maskNode) maskNode.parentNode.removeChild(maskNode) ;
  	clearTimeout(eXo.forum.MaskLayerControl.timer);
  	delete eXo.forum.MaskLayerControl.timer;
  } ;
  
  MaskLayerControl.prototype.getImageSize = function(img) {
  	var imgNode = new Image();
  	imgNode.src = img.src;
  	return {"height":imgNode.height,"width":imgNode.width};
  };
  
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.MaskLayerControl = new MaskLayerControl();

})(gj, window, document);