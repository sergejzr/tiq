package de.l3s.util;

import java.util.TimeZone;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.DateTimeConverter;
import javax.faces.convert.FacesConverter;

@FacesConverter("customDateTimeConverter")
/**
 * Custom DateTimeConverter class that evaluates the used pattern and timeZone at each call.
 * This is necessary for displaying a range of timestamps with possibly different timezones.
 * 
 * Source: http://stackoverflow.com/questions/7122460/jsf-convertdatetime-with-timezone-in-datatable
 * @author rokicki
 */
public class customDateTimeConverter extends DateTimeConverter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component,
			String value) {
		if (context == null) {
			throw new NullPointerException("facesContext");
		}
		if (component == null) {
			throw new NullPointerException("uiComponent");
		}
		setPattern((String) component.getAttributes().get("pattern"));
		setTimeZone(TimeZone.getTimeZone((String) component.getAttributes()
				.get("timeZone")));
		return super.getAsObject(context, component, value);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component,
			Object value) {
		if (context == null) {
			throw new NullPointerException("facesContext");
		}
		if (component == null) {
			throw new NullPointerException("uiComponent");
		}

		setPattern((String) component.getAttributes().get("pattern"));
		String timezone = (String) component.getAttributes().get("timeZone");
		setTimeZone(TimeZone.getTimeZone(timezone));
		return super.getAsString(context, component, value);
	}
}
