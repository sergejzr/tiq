package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.PreDestroy;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

@ManagedBean
@SessionScoped
public class UserBean implements Serializable {

	private static final long serialVersionUID = -8577036953815676944L;

	private User user = null;
	private Locale locale;
	private HashMap<String, Object> preferences; // user preferences like search
													// mode
	private int screenWidth;
	private int screenHeight;
	private String sessionId;
	private String timeZoneString;
	private Integer timeZoneOffset;

	private static final Logger logger = Logger.getLogger(UserBean.class);

	public UserBean() {
		locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
		preferences = new HashMap<String, Object>();
	}

	public void preRenderView(ComponentSystemEvent event) {
		FacesContext fc = FacesContext.getCurrentInstance();
		if (isLoggedIn()) {
			ConfigurableNavigationHandler nav = (ConfigurableNavigationHandler) fc
					.getApplication().getNavigationHandler();
			nav.performNavigation("/mt/mt.jsf");
		}
	}

	public boolean isLoggedIn() {
		checkSession();
		return user != null;
	}

	/**
	 * Checks if this session is the most recent one. If not, user is set to
	 * null, resulting in logout.
	 */
	public void checkSession() {
		if (user == null)
			return;
		if (getSessionId() != Learnweb.getInstance().getSessionMap()
				.get(user.getId())) {
			logger.info("user " + user.getId() + ": this session ("
					+ getSessionId() + ") did not match previous session: "
					+ Learnweb.getInstance().getSessionMap().get(user.getId()));
			user = null;
		}
	}

	/**
	 * The currently logged in user
	 * 
	 * @return
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Use this function to log in a user.
	 * 
	 * @param user
	 */
	public void setUser(User user) {
		this.user = user;

		// store the user also in the session so that it is accessible in the
		// download servlet
		HttpSession session = (HttpSession) FacesContext.getCurrentInstance()
				.getExternalContext().getSession(true);
		session.setAttribute("learnweb_user", user);
	}

	@PreDestroy
	public void onDestroy() {
		if (null != user)
			user.onDestroy();
	}

	public Object getPreference(String key) {
		return preferences.get(key);
	}

	public void setPreference(String key, Object value) {
		preferences.put(key, value);
	}

	public int getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}

	private static Map<String, Object> countries;
	static {
		countries = new LinkedHashMap<String, Object>();
		countries.put("Deutsch", Locale.GERMANY);
		countries.put("English", Locale.US); // label, value

	}

	public Map<String, Object> getCountriesInMap() {
		return countries;
	}

	// value change event listener
	public void countryLocaleCodeChanged(ValueChangeEvent e) {

		String newLocaleValue = e.getNewValue().toString();
		// System.out.println("newLocaleValue "+newLocaleValue);
		// loop country map to compare the locale code
		for (Map.Entry<String, Object> entry : countries.entrySet()) {
			// System.out.println("value "+entry.getValue().toString());
			if (entry.getValue().toString().equals(newLocaleValue)) {

				FacesContext.getCurrentInstance().getViewRoot()
						.setLocale((Locale) entry.getValue());
				setLocale((Locale) entry.getValue());
			}
		}

		/*
		 * String viewId =
		 * FacesContext.getCurrentInstance().getViewRoot().getViewId(); return
		 * viewId +"?faces-redirect=true&includeViewParams=true";
		 */
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		if (isLoggedIn())
			user.setLocale(locale);
	}

	public Locale getLocale() {
		return locale;
	}

	/**
	 * 
	 * @return example "de_DE"
	 */
	public String getLocaleAsString() {
		return locale.toString();
	}

	public void setLocaleAsString(String foo) {
	}

	/**
	 * example "de"
	 * 
	 * @return
	 */
	public String getLocaleCode() {
		return locale.getLanguage();
	}

	public void setLocaleCode(String lo) {
		System.out.println("set locale" + lo);

	}

	public String getTimeZone() {
		return this.timeZoneString;
	}

	public void setTimeZone(String timeZone) {
		this.timeZoneString = timeZone;
	}

	public String getSessionId() {
		if (null == sessionId) {
			FacesContext fCtx = FacesContext.getCurrentInstance();
			HttpSession session = (HttpSession) fCtx.getExternalContext()
					.getSession(false);
			sessionId = session.getId();
			logger.debug("initialized sessionId: " + sessionId);
		}
		return sessionId;
	}

	/**
	 * Listener is called each time the client side information on timezone
	 * changes. Track timezone offset (in hours) for use by logging methods
	 */
	public void timeZoneChangedListener(AjaxBehaviorEvent e) {
		logger.debug("timezone changed: " + this.timeZoneString);
		if (null == this.timeZoneString) {
			return;
		}
		TimeZone tz = TimeZone.getTimeZone(this.timeZoneString);
		int offset = tz.getOffset(System.currentTimeMillis());
		logger.debug("tz dname: " + tz.getDisplayName() + ", offset (ms):"
				+ offset);
		setTimeZoneOffset(offset);
	}

	public Integer getTimeZoneOffset() {
		return timeZoneOffset;
	}

	public void setTimeZoneOffset(Integer timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}

}
