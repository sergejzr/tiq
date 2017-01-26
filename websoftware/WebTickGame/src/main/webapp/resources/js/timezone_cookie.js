
$(document).ready(function() {
	setTimezoneCookie();
});

function setTimezoneCookie() {

	var timezone = jstz.determine().name();

	if (null == getCookie("timezoneCookie")) {
		document.cookie = "timezoneCookie=" + timezone;
	}
}

function getCookie(cookieName) {
	var cookieValue = document.cookie;
	var cookieStart = cookieValue.indexOf(" " + cookieName + "=");
	if (cookieStart == -1) {
		cookieStart = cookieValue.indexOf(cookieName + "=");
	}
	if (cookieStart == -1) {
		cookieValue = null;
	} else {
		cookieStart = cookieValue.indexOf("=", cookieStart) + 1;
		var cookieEnd = cookieValue.indexOf(";", cookieStart);
		if (cookieEnd == -1) {
			cookieEnd = cookieValue.length;
		}
		cookieValue = unescape(cookieValue.substring(cookieStart, cookieEnd));
	}
	return cookieValue;
}