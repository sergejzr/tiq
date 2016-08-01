package de.l3s.mt.beans;

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class BeforeStartBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = 7455990698639203466L;

	public String getTimeToStartString() {
		long now = Calendar.getInstance(TimeZone.getDefault())
				.getTimeInMillis();
		long start = getLearnweb().getStart().getTimeInMillis();
		long diff = start - now;
		String result = "";

		long days = (long) Math.floor(diff / (1000 * 60 * 60 * 24));
		Double remainder = Math.floor(diff % (1000 * 60 * 60 * 24));
		long hours = (long) Math.floor(remainder / (1000 * 60 * 60));
		remainder = Math.floor(remainder % (1000 * 60 * 60));
		long minutes = (long) Math.floor(remainder / (1000 * 60));
		remainder = Math.floor(remainder % (1000 * 60));
		long seconds = (long) Math.floor(remainder / (1000));

		if (diff >= 1000 * 60) {
			result += ((days > 0) ? days + ((days != 1) ? " days, " : " day, ")
					: "")
					+ ((days > 0 || hours > 0) ? hours
							+ ((hours != 1) ? " hours" : " hour") + " and "
							: "")
					+ minutes
					+ ((minutes != 1) ? " minutes" : " minute");
		} else {
			result += seconds + ((seconds != 1) ? "seconds" : "second");
		}
		return result;
	}
}
