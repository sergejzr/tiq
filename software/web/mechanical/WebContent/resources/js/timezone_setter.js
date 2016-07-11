$(document).ready(function() {
	var oldTimezone = $("#timezone_holder").val();
	// determine current timezone value
	var timezone = jstz.determine().name();
	
    if (oldTimezone != timezone) {
    	// if the value has changed, trigger listener
    	$("#timezone_holder").val(timezone);
    	$("#timezone_holder").change();
    	//alert("changed timezone. new timezone:" + timezone + ", old value was:" + oldTimezone);
    }
});