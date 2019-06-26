isc.ClassFactory.defineClass("BizMap","Canvas");isc.BizMap.addClassMethods({v:0,initialise:function(){eval(isc.BizMap.id+".build()")}});isc.BizMap.addMethods({init:function(a){this._refreshTime=10;this._refreshRequired=true;this._refreshing=false;this.width="100%";this.height="100%";this.styleName="googleMapDivParent",this.ID="bizMap"+isc.BizMap.v++;this.redrawOnResize=false;this.Super("init",arguments);this._objects={}},getInnerHTML:function(){return'<div id="'+this.ID+'_map" style="margin:0;padding:0;height:100%">Loading Map...</div>'},draw:function(){if(window.google&&window.google.maps){if(!this.isDrawn()){this.build();return this.Super("draw",arguments)}}else{isc.BizMap.id=this.ID;SKYVE.Util.loadJS("wicket/wicket.js?v="+isc.BizUtil.version,function(){SKYVE.Util.loadJS("wicket/wicket-gmap3.js?v="+isc.BizUtil.version,function(){if(isc.BizUtil.googleMapsV3ApiKey){SKYVE.Util.loadJS("https://maps.googleapis.com/maps/api/js?v=3&libraries=drawing&callback=isc.BizMap.initialise&key="+isc.BizUtil.googleMapsV3ApiKey)}else{SKYVE.Util.loadJS("https://maps.googleapis.com/maps/api/js?v=3&libraries=drawing&callback=isc.BizMap.initialise")}})});return this.Super("draw",arguments)}},setDataSource:function(b){if(window.google&&window.google.maps&&this._map){if(this._view){this._modelName=b;this._moduleName=null;this._queryName=null;this._geometryBinding=null;var c=this._view._grids[b];if(c){}else{c={};this._view._grids[b]=c}c[this.getID()]=this}else{var a=b.indexOf("_");this._moduleName=b.substring(0,a);this._queryName=b.substring(a+1);a=this._queryName.indexOf("_");this._geometryBinding=this._queryName.substring(a+1);this._queryName=this._queryName.substring(0,a);this._modelName=null}this._refresh(true,false)}else{this.delayCall("setDataSource",arguments,100)}},build:function(){if(this.isDrawn()){var a={zoom:4,center:new google.maps.LatLng(-26,133.5),mapTypeId:google.maps.MapTypeId.ROADMAP};if(this._map){a.zoom=this._map.getZoom();a.center=this._map.getCenter();a.mapTypeId=this._map.getMapTypeId()}this._infoWindow=new google.maps.InfoWindow({content:""});this._map=new google.maps.Map(document.getElementById(this.ID+"_map"),a);this._refresh(true,false);this.delayCall("_addForm",null,1000)}else{this.delayCall("build",null,100)}},_addForm:function(){},rerender:function(){this._refresh(false,false)},resume:function(){this._zoomed=false},_refresh:function(c,f){if(!this._refreshRequired){return}if(this._zoomed){return}if(this._refreshing){return}if(!this.isDrawn()){return}if(!this.isVisible()){return}var e=new Wkt.Wkt();var b=isc.BizUtil.URL_PREFIX+"map?";if(this._view){if(this._modelName){var a=this._view.gather(false);b+="_c="+a._c+"&_m="+this._modelName}else{return}}else{if(this._queryName){b+="_mod="+this._moduleName+"&_q="+this._queryName+"&_geo="+this._geometryBinding}else{return}}this._refreshing=true;var d=this;isc.RPCManager.sendRequest({showPrompt:true,evalResult:true,actionURL:b,httpMethod:"GET",callback:function(p,G,v){d._refreshing=false;var q=G.items;if(f){for(var o in d._objects){if(!q.containsProperty("bizId",o)){var C=d._objects[o];for(var y=0,u=C.overlays.length;y<u;y++){C.overlays[y].setMap(null);C.overlays[y]=null}delete C.overlays;delete d._objects[o]}}}else{for(var o in d._objects){var C=d._objects[o];for(var y=0,u=C.overlays.length;y<u;y++){C.overlays[y].setMap(null);C.overlays[y]=null}delete C.overlays;delete d._objects[o]}}for(var y=0,u=q.length;y<u;y++){var B=q[y];var H=d._objects[B.bizId];if(H){var w=(H.overlays.length==B.features.length);if(w){for(var x=0,t=H.overlays.length;x<t;x++){if(H.overlays[x].geometry!==B.features[x].geometry){w=false;break}}}if(!w){for(var x=0,t=H.overlays.length;x<t;x++){H.overlays[x].setMap(null);H.overlays[x]=null}delete H.overlays;delete d._objects[o];H=null}}if(H){}else{H={overlays:[]};for(var x=0,t=B.features.length;x<t;x++){var k=B.features[x];try{e.read(k.geometry)}catch(A){if(A.name==="WKTError"){alert(k.geometry+" is invalid WKT.");continue}}var h={editable:k.editable};if(k.strokeColour){h.strokeColor=k.strokeColour}if(k.fillColour){h.fillColor=k.fillColour}if(k.fillOpacity){h.fillOpacity=k.fillOpacity}if(k.iconDynamicImageName){h.icon={url:"image?_n="+k.iconDynamicImageName};if(k.iconAnchorX&&k.iconAnchorY){h.icon.anchor=new google.maps.Point(k.iconAnchorX,k.iconAnchorY);h.icon.origin=new google.maps.Point(0,0)}}var z=e.toObject(h);H.overlays.push(z);z.setMap(d._map);if(k.zoomable){z.bizId=B.bizId;z.geometry=k.geometry;z.fromTimestamp=B.fromTimestamp;z.toTimestamp=B.toTimestamp;z.photoId=B.photoId;z.mod=B.moduleName;z.doc=B.documentName;z.infoMarkup=B.infoMarkup;google.maps.event.addListener(z,"click",function(j){var I=this.infoMarkup;I+='<p/><input type="button" value="Zoom" onclick="'+d.ID+".zoom(";if(this.getPosition){var l=this.getPosition();I+=l.lat()+","+l.lng()+","+l.lat()+","+l.lng()+",'";I+=this.mod+"','"+this.doc+"','"+this.bizId+"')\"/>";d._infoWindow.open(d._map,this);d._infoWindow.setContent(I)}else{if(this.getPath){var i=new google.maps.LatLngBounds();var M=this.getPath();for(var K=0,m=M.getLength();K<m;K++){i.extend(M.getAt(K))}var J=i.getNorthEast();var L=i.getSouthWest();I+=J.lat()+","+L.lng()+","+L.lat()+","+J.lng()+",'";I+=this.mod+"','"+this.doc+"','"+this.bizId+"')\"/>";d._infoWindow.setPosition(j.latLng);d._infoWindow.open(d._map);d._infoWindow.setContent(I)}}})}}d._objects[B.bizId]=H}}if(c){var n=new google.maps.LatLngBounds();var g=false;for(var s in d._objects){g=true;var H=d._objects[s];var F=H.overlays;for(var y=0,u=F.length;y<u;y++){var z=F[y];if(z.getPath){var r=z.getPath();for(var x=0,t=r.getLength();x<t;x++){n.extend(r.getAt(x))}}else{if(z.getPosition){n.extend(z.getPosition())}}}}if(g){if(n.getNorthEast().equals(n.getSouthWest())){var E=new google.maps.LatLng(n.getNorthEast().lat()+0.01,n.getNorthEast().lng()+0.01);var D=new google.maps.LatLng(n.getNorthEast().lat()-0.01,n.getNorthEast().lng()-0.01);n.extend(E);n.extend(D)}d._map.fitBounds(n)}}}})},zoom:function(l,d,n,f,q,c,g){this._zoomed=true;var t=Math.pow(2,this._map.getZoom());var r=new google.maps.LatLng(this._map.getBounds().getNorthEast().lat(),this._map.getBounds().getSouthWest().lng());var p=this._map.getProjection().fromLatLngToPoint(r);var a=new google.maps.LatLng(l,d);var e=this._map.getProjection().fromLatLngToPoint(a);var h=new google.maps.LatLng(n,f);var b=this._map.getProjection().fromLatLngToPoint(h);var i=this.getPageRect();var k=Math.floor((e.x-p.x)*t)+i[0];var j=Math.floor((e.y-p.y)*t)+i[1];var o=Math.floor((b.x-p.x)*t)+i[0]-k;var m=Math.floor((b.y-p.y)*t)+i[1]-j;var s=this;isc.BizUtil.getEditView(q,c,function(u){isc.WindowStack.popup([k,j,o,m],"Edit",true,[u]);u.editInstance(g,null,null);s._infoWindow.close()})}});