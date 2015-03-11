package de.persosim.driver.connector.ui.parts;

public class Pin {

	private String password;

	public Pin(String password) {
		super();
		this.password = password;

	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return password;
	}

}
