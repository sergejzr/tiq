
var no_more_results = false;
var openPreview;
var resourceCounter = 0;
var lightboxActiveResource = null;
var gridItemWidth = 190;

(function($) {
	$.fn.uncomment = function() {
		$(this).contents().each(function() {
			if ( this.nodeType == 8 ) {
				// Need to "evaluate" the HTML content,
				// otherwise simple text won't replace
				var e = $('<span>' + this.nodeValue + '</span>');
				$(this).replaceWith(e.contents());
			}
			else if ( this.hasChildNodes() ) {
				$(this).uncomment(recurse);
			} 
		});
	};
})(jQuery);

function prepareResources(resources)
{
	var padding = 9;
	var mindist = 4;
	
	resources.each(function()
	{				
		var resource = $(this);			
		
		var image = resource.find('.smallImage').first();
		var preview = resource.find('.preview').first();
		var previewImage = resource.find('.preview img').first();
		
		// open preview delayed on mouseover 
		image.mouseenter(function (event) 
		{
			image.addClass("hasFocus");			
			
		    setTimeout(function()
		    {	   	
		    	if(image.hasClass("hasFocus")) // open preview
		    	{				
		    		var offset = image.offset();		    		
		    		var width = preview.width() + 2*padding;
		    		var heightDiff = padding + (previewImage.attr('height') - image.height())/2;						
					var widthDiff = padding + (previewImage.attr('width') - image.width())/2;
					
					offset.left -= widthDiff;
					offset.top -= heightDiff;
					
					var containerWidth = $('#results').outerWidth(true); 
					var containerHeight = $(window).height();				
					var previewHeight = preview.height() + mindist + 2*padding;
					
					if(offset.left < mindist)
						offset.left = mindist;					
					else if(offset.left + width + mindist > containerWidth)
						offset.left = containerWidth - width - mindist;
					
					if(offset.top < 79)
						offset.top = 79;	
					
					else if(offset.top + previewHeight > containerHeight)
						offset.top = containerHeight - previewHeight;

					openPreview.hide();
					openPreview = preview;
				
					preview.show();	
					preview.offset(offset);      		        	
		        }
			}, 320 );
		});
		
		image.mouseleave(function () {
			image.removeClass("hasFocus");
		});
		
		preview.mouseleave(function () {
			preview.hide();
		});			

		// get content for lightbox
		var metadata = resource.find('.metadata').first();
		var embedded = metadata.children('.embedded').first();		
		var description = metadata.children('.description').first();
		var title = metadata.children('.title').first();
		var options = metadata.children('.options').first();
		
		metadata.remove();		
		
		var lightbox_open = function()
		{			
			openPreview.hide();	
			lightboxActiveResource = resource;
			
			$('#lightbox').show();
			$('#lightbox_metadata').empty();
			$('#lightbox_metadata').append(description);
			$('#lightbox_metadata').append(options);
			$('#lightbox_content .embedded').remove();
			$('#lightbox_content').append(embedded);			
			$('#lightbox_title').empty();
			$('#lightbox_title').append(title);

			embedded.uncomment();
			var image = embedded.children().first();
			
			if(image.is('img')) // load image
			{
				if(image.attr('src') != image.attr('original-src'))
			    {
					image.css("z-index",1104);
					// clone image and place it behind the small resolution image
				    var hdImage = $('<img />');
				    
				    hdImage.bind("load", function() 
				    {						    
                    	image.fadeOut(function() {
                    		image.remove();
                    	});            	
                    });				    
				    hdImage.css("z-index", 1103);				    
				    hdImage.attr('original_width', image.attr('original_width'));
				    hdImage.attr('original_height', image.attr('original_height'));

				    image.after(hdImage);
				    
				    hdImage.attr("src", image.attr('original-src'));				    				    
				}
			}
			lightbox_resize_container();
			lightbox_resize_content();
		};
		
		resource.on('openLightbox', lightbox_open);
		previewImage.mousedown(lightbox_open);
		image.mousedown(lightbox_open);
	
	});	
	
	
	if(view == 'grid')
		resources.width(gridItemWidth);
	
	/*
		resizeGridResources();
	*/	
}

function resizeGridResources()
{ console.log('resize');
	var innerWidth = $('#results').width() - 21;
	
	gridItemWidth = 100 / Math.floor($('#results').innerWidth()/190) +'%';

	$('#results .resource').width(gridItemWidth);
}

var loading = false;
function loadNextPage()
{
	if(no_more_results || loading) // nothing found || not searched
		return;
	
	loading = true;
	$('#search_loading_more_results').show();

	ajaxLoadNextPage();
}

function displayNextPage(xhr, status, args)
{	
	var results = $('#new_results > div > div');  // this can include additional html like the "Page x:" on textsearch
	var resources = results.filter('.resource');
	
	$('#search_loading_more_results').hide();
	
	if(resources.length == 0 || status != "success") 
	{
		if(status != "success")
			console.log('fehler', status);
		
		if($('#results .resource').size() > 0)
			$('#search_no_more_results').show();
		else		
			$('#search_nothing_found').show();
		
		no_more_results = true;
		return;
	}

	$('#results > .resources').append(results);		

	prepareResources(resources);	
	
	loading = false;
	
	testIfResultsFillPage();
}

function testIfResultsFillPage()
{	
	console.log($('.content').scrollTop() , $('#results').height() , $('#center_pane').height());
	
	// if results don't fill the page -> load more results
	//if($('#center_pane > div').scrollTop() > $('#results').height() - $('#center_pane').height()*2)
		
	if($('.content').scrollTop() > $('#results').height() - $('#center_pane').height()*2)
    {		
		loadNextPage();
    }	
}


function lightbox_close()
{
	openPreview.hide();	
	
	$('#lightbox').hide();
	$('#lightbox_content .embedded').remove();	

	lightboxActiveResource = null;
}

function lightbox_resize_container()
{	
	// resize lightbox container
	var height = $(window).height() - 137;
	
	if(height < 200)
		height = 200;
	
	var titleHeight = $('#lightbox_title').height() + 10;
	
	$('#lightbox_container').height(height);
	$('#lightbox_content').height(height-titleHeight);
	$('#lightbox_content .large').height(height-titleHeight);	
}

function lightbox_next()
{
	if(lightboxActiveResource == null)
		return
		
	var next = $(lightboxActiveResource).next();	
	
	if(!next.hasClass('resource')) {
		next = $('#results .resource').first();
	}
	
	next.trigger('openLightbox');
}

function lightbox_prev()
{
	if(lightboxActiveResource == null)
		return;
		
	var prev = lightboxActiveResource.prev('.resource');	
	
	// test if current resource is first resource
	if(!prev.hasClass('resource')) {
		prev = $('#results .resource').last();
	}

	prev.trigger('openLightbox');	
}

function lightbox_resize_content()
{	
	// resize and center lightbox content
	var outer = $('#lightbox_content');
	var inner = outer.find('.embedded').first().children();	
	
	if(inner.first().attr('width') == '100%' || inner.first().attr('type') == 'application/x-shockwave-flash')
	{
		inner.css({
		   position:'absolute',
		   left: 0,
		   top: 0,
		   width: outer.width(),
		   height: outer.height()
		});
		
		return;
	}

	if(typeof inner.first().attr('original_width') == 'undefined')
	{
		inner.attr('original_width', inner.width());
		inner.attr('original_height', inner.height());
	}	

	var iwidth = inner.first().attr('original_width');
	var iheight = inner.first().attr('original_height');		
	
	if(iwidth > outer.width()) {
		var ratio = outer.width() / iwidth;
		iheight = Math.ceil(iheight * ratio);
		iwidth = outer.width();
	}
	
	if(iheight > outer.height()) {
		var ratio = outer.height() / iheight;
		iwidth = Math.ceil(iwidth * ratio);
		iheight = outer.height();
	}
	
	inner.css({
	   position:'absolute',
	   left: (outer.width() - iwidth)/2,
	   top: (outer.height() - iheight)/2,
	   width: iwidth+'px',
	   height: iheight+'px',
	});
}

// call resizeend after the resize
var rtime = new Date(1, 1, 2000, 12,00,00);
var lastResize = new Date(1, 1, 2000, 12,00,00);
var timeout = false;
var delta = 250;
$(window).resize(function() 
{
 	
	rtime = new Date();
    if (timeout === false) {
        timeout = true;
        setTimeout(resizeend, delta);
    }
});

function resizeend() {
	
    if (new Date() - rtime < delta) {
        setTimeout(resizeend, delta);
    } 
    else {   	
    	if(view == 'grid')
    		resizeGridResources();
    	
    	testIfResultsFillPage();
    	lightbox_resize_container();
    	lightbox_resize_content();
    	/*
    	// neccessary for ie and safari
    	setTimeout(function() {
    		resizeend();
    	}, 400);
    	*/
    	timeout = false;
    }               
}

window.onload = testIfResultsFillPage;

$(document).ready(function() 
{	
	openPreview = $('#new_results'); // initialize openPreview with an element that can be hidden.
	prepareResources($('#results .resource'));	
	
	lightbox_resize_container();
	
	if(view == 'grid')
		resizeGridResources();
	
	// register cursor left/right and esc key
	$(document).keydown(function(event) {
		if(event.which == 37) 
			lightbox_prev();
		else if (event.which == 39)
			lightbox_next();
		else if (event.which == 27)
			lightbox_close();		
	});
	
	//$('#center_pane > div').scroll(testIfResultsFillPage);	
	
	$(".content").bind("scroll", function(e){
		testIfResultsFillPage();
	});
});
