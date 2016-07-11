package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

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

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.primefaces.event.FileUploadEvent;

import de.l3s.learnweb.User;
import de.l3s.mt.model.ScoreEntry;
import de.l3s.mt.sql.UserManagerInterface;

@ManagedBean
@RequestScoped
public class ProfileBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = -2460055719611784132L;

	@Size(min = 0, max = 250)
	private String address;

	private Date dateofbirth;

	@Email
	private String email;

	@Min(value = 0)
	@Max(value = 2)
	private int gender;

	@Size(min = 0, max = 50)
	private String interest;

	@NotBlank
	@Size(min = 2, max = 25)
	private String username;

	@Size(min = 0, max = 50)
	private String phone;

	@Size(min = 0, max = 80)
	private String profession;

	@Size(min = 0, max = 250)
	private String additionalInformation;

	@Size(min = 0, max = 50)
	private String age;

	@Size(min = 0, max = 255)
	private String country;

	@Size(min = 0, max = 255)
	private String facebook;

	@NotEmpty
	private String password;

	@NotBlank
	private String confirmPassword;

	@NotBlank
	private String currentPassword;

	private boolean mailNotifications;

	public ProfileBean() {
		log("display profile");
		User user = getUser();
		username = user.getUsername();
		email = user.getEmail();
		phone = user.getPhone();
		gender = user.getGender();
		dateofbirth = user.getDateofbirth();
		additionalInformation = user.getAdditionalInformation();
		interest = user.getInterest();
		address = user.getAddress();
		profession = user.getProfession();
		age = user.getAge();
		country = user.getCountry();
		facebook = user.getFacebook();
		this.mailNotifications = user.isMailNotifications();

		/*
		 * try { logMessages = user.getLogs(); } catch (SQLException e) { //
		 * TODO Auto-generated catch block e.printStackTrace(); }
		 */
	}

	public String getUrlBase() {
		return FacesContext.getCurrentInstance().getExternalContext()
				.getRealPath("resources/avatars");
	}

	public void handleFileUpload(FileUploadEvent event) {
		System.out.println("upload");
	}

	public void saveProfile() throws SQLException {
		User user = getUser();
		user.setAdditionalinformation(additionalInformation);
		user.setAddress(address);
		user.setDateofbirth(dateofbirth);
		user.setEmail(email);
		user.setGender(gender);
		user.setInterest(interest);
		user.setPhone(phone);
		user.setProfession(profession);
		user.setUsername(username);
		user.setAge(age);
		user.setCountry(country);
		user.setFacebook(facebook);
		user.setMailNotifications(this.mailNotifications);

		getLearnweb().getUserManager().save(user);
		// update score entry in case username was changed
		ScoreEntry entry = getLearnweb().getScoreManager().getScoreEntryByUser(
				user);
		if (null != entry) {
			entry.setUsername(user.getUsername());
			getLearnweb().getScoreManager().save(entry);
		}

		log("save profile");
		addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
	}

	public void onChangePassword() {
		UserManagerInterface um = getLearnweb().getUserManager();
		try {

			getUser().setPassword(password, false);
			um.save(getUser());
			// um.setPassword(getUser().getId(), password);
			addMessage(FacesMessage.SEVERITY_INFO, "password_changed");
			log("profile - password changed");
			password = "";
			confirmPassword = "";
			currentPassword = "";
		} catch (SQLException e) {
			e.printStackTrace();
			addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
			log("profile - password change - error");
		}
	}

	public void validateUsername(FacesContext context, UIComponent component,
			Object value) throws ValidatorException, SQLException {
		if (getUser().getUsername().equals(value)) { // username not changed
			return;
		}

		if (getLearnweb().getUserManager().isUsernameAlreadyTaken(
				(String) value)) {
			throw new ValidatorException(getFacesMessage(
					FacesMessage.SEVERITY_ERROR, "username_already_taken"));
		}
	}

	/*
	 * @SuppressWarnings("deprecation") public Date getMinBirthday() { Date date
	 * = new Date(); date.setYear(date.getYear()-100); return date; }
	 */

	public Date getMaxBirthday() {
		return new Date();
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Date getDateofbirth() {
		return dateofbirth;
	}

	public void setDateofbirth(Date dateofbirth) {
		this.dateofbirth = dateofbirth;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public String getInterest() {
		return interest;
	}

	public void setInterest(String interest) {
		this.interest = interest;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getProfession() {
		return profession;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public String getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(String additionalInformation) {
		this.additionalInformation = additionalInformation;
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

	public String getFacebook() {
		return facebook;
	}

	public void setFacebook(String facebook) {
		this.facebook = facebook;
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

	public String getCurrentPassword() {
		return currentPassword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}

	public void validateCurrentPassword(FacesContext context,
			UIComponent component, Object value) throws ValidatorException,
			SQLException {
		UserManagerInterface um = getLearnweb().getUserManager();
		User user = getUser(); // the current user

		String password = (String) value;

		// returns the same user, if the password is correct
		User checkUser = um.getUser(user.getUsername(), password);

		if (!user.equals(checkUser)) {
			throw new ValidatorException(getFacesMessage(
					FacesMessage.SEVERITY_ERROR, "password_incorrect"));
		}
	}

	public void validatePassword(FacesContext context, UIComponent component,
			Object value) throws ValidatorException {
		// Find the actual JSF component for the first password field.
		UIInput passwordInput = (UIInput) context.getViewRoot().findComponent(
				"profile_passwordform:password");

		// Get its value, the entered password of the first field.
		String password = (String) passwordInput.getValue();

		if (null != password && !password.equals((String) value)) {
			throw new ValidatorException(getFacesMessage(
					FacesMessage.SEVERITY_ERROR, "passwords_do_not_match"));
		}
	}

	public String onBackToLeaderboard() {
		log("profile - back to leaderboard");
		return getTemplateDir() + "/mt.jsf?faces-redirect=true";
	}

	public boolean isMailNotifications() {
		return mailNotifications;
	}

	public void setMailNotifications(boolean mailNotifications) {
		this.mailNotifications = mailNotifications;
	}
}
