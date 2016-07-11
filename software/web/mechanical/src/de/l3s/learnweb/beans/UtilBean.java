package de.l3s.learnweb.beans;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import de.l3s.learnwebBeans.LearnwebBean;
import de.l3s.learnwebBeans.LoginBean;
import de.l3s.learnwebBeans.UserBean;
import de.l3s.mt.beans.GroupBean;

@ApplicationScoped
@ManagedBean
public class UtilBean {
	public int toInt(double number) {
		System.out.println(number);
		return (int) Math.ceil(number);
	}

	public boolean isSearchPage() {
		String viewId = FacesContext.getCurrentInstance().getViewRoot()
				.getViewId();
		if (viewId.contains("search.xhtml"))
			return true;
		else
			return false;
	}

	// ------------------------

	public static ExternalContext getExternalContext() {
		FacesContext fc = FacesContext.getCurrentInstance();
		return fc.getExternalContext();
	}

	public static LearnwebBean getLearnwebBean() {
		return (LearnwebBean) getManagedBean("learnwebBean");
	}

	public static Object getManagedBean(String beanName) {
		FacesContext fc = FacesContext.getCurrentInstance();
		return fc.getApplication().getELResolver()
				.getValue(fc.getELContext(), null, beanName);
	}

	public static UserBean getUserBean() {
		return (UserBean) getManagedBean("userBean");
	}

	public static LoginBean getLoginBean() {
		return (LoginBean) getManagedBean("loginBean");
	}

	public static GroupBean getGroupBean() {
		return (GroupBean) getManagedBean("groupBean");
	}

	public static void redirect(String redirectPath) {
		ExternalContext externalContext = getExternalContext();

		try {
			externalContext.redirect(redirectPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getLocaleMessage(String msgKey, Object... args) {
		ResourceBundle bundle = ResourceBundle.getBundle(
				"de.l3s.learnweb.lang.messages", UtilBean.getUserBean()
						.getLocale());

		String msg;
		try {
			msg = bundle.getString(msgKey);
			if (args != null) {
				MessageFormat format = new MessageFormat(msg);
				msg = format.format(args);
			}
		} catch (MissingResourceException e) {
			msg = msgKey;
		}
		return msg;
	}

	// ------------------------

	public static int time() {
		return (int) (System.currentTimeMillis() / 1000);
	}
}
