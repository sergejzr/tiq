package de.l3s.mt.beans;

import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class RegistrationBean extends ApplicationBean {

	private static final long serialVersionUID = 4567220515408089722L;

	@NotBlank
	@Size(min = 2, max = 25)
	private String username;

	@NotEmpty
	private String password;

	@NotBlank
	private String confirmPassword;

	@NotBlank
	@Email
	private String email;

	@Size(min = 0, max = 50)
	private String age;

	@Size(min = 0, max = 255)
	private String country;

	@Size(min = 0, max = 255)
	private String facebook;

	@Min(value = 0)
	@Max(value = 2)
	private int gender;

	private Logger logger = Logger.getLogger(RegistrationBean.class);

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public String getFacebook() {
		return facebook;
	}

	public void setFacebook(String facebook) {
		this.facebook = facebook;
	}

	public String register() throws Exception {
		Learnweb learnweb = getLearnweb();

		final User user = learnweb.getUserManager().registerUser(username,
				password, email, age, country, facebook, gender);
		// logging
		log("register");

		// log the user in
		UtilBean.getLoginBean().login(user);
		addMessage(FacesMessage.SEVERITY_INFO, "welcome_username",
				user.getUsername());
		log("login");
		return getTemplateDir() + "/new.jsf?faces-redirect=true";
	}

	public void validatePassword(FacesContext context, UIComponent component,
			Object value) throws ValidatorException {
		// Find the actual JSF component for the first password field.
		UIInput passwordInput = (UIInput) context.getViewRoot().findComponent(
				"registerform:password");

		// Get its value, the entered password of the first field.
		String password = (String) passwordInput.getValue();

		if (null != password && !password.equals((String) value)) {
			throw new ValidatorException(getFacesMessage(
					FacesMessage.SEVERITY_ERROR, "passwords_do_not_match"));
		}
	}

	public void validateUsername(FacesContext context, UIComponent component,
			Object value) throws ValidatorException, SQLException {
		if (getLearnweb().getUserManager().isUsernameAlreadyTaken(
				(String) value)) {
			throw new ValidatorException(getFacesMessage(
					FacesMessage.SEVERITY_ERROR, "username_already_taken"));
		}
	}

	public void validateEmail(FacesContext context, UIComponent component,
			Object value) throws ValidatorException, SQLException {
		if (getLearnweb().getUserManager().isMailAlreadyTaken((String) value)) {
			throw new ValidatorException(getFacesMessage(
					FacesMessage.SEVERITY_ERROR, "email_already_taken"));
		}
	}

	public void preRenderView() throws ValidatorException, SQLException {

	}
}
