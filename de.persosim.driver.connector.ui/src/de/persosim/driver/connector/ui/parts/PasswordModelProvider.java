package de.persosim.driver.connector.ui.parts;

import static org.globaltester.logging.BasicLogger.log;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.globaltester.logging.tags.LogLevel;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;


/**
 * The PasswordModelProvider is responsible for dealing with the passwords in
 * the list. This class manages the passwords in the list and in the
 * preferences. This includes loading the list from the preferences and deleting
 * the files from preferences
 */

public class PasswordModelProvider {
	private static PasswordModelProvider pinModelProvider = null;

	private static Preferences prefsUi = InstanceScope.INSTANCE
			.getNode("de.persosim.driver.connector.ui");

	private static final String AUTO_LOGIN = "AUTO_LOGIN";
	private static Preferences nodePassword = prefsUi.node("nodePwd");

	private List<String> pins;
	private boolean sort = true;
	private String autoLogin = null;

	public boolean getSort() {
		return sort;
	}

	public String getAutoLogin() {
		return autoLogin;
	}

	public void saveAutoLogin(String pin) {
		this.autoLogin = pin;
		prefsUi.put(AUTO_LOGIN, pin);
		prefsUiFlush();
	}

	public void removeAutoLogin() {
		autoLogin = null;
		prefsUi.remove(AUTO_LOGIN);
		prefsUiFlush();
	}

	public void setSort(boolean sort) {
		this.sort = sort;
	}

	private PasswordModelProvider() {
		pins = getPinsFromPrefs();
		autoLogin = prefsUi.get(AUTO_LOGIN, null);
	}

	public static PasswordModelProvider getInstance() {
		if (pinModelProvider != null)
			return pinModelProvider;
		else
			return pinModelProvider = new PasswordModelProvider();
	}

	public void setPins(List<String> pins) {
		this.pins = pins;
	}

	public List<String> getPins() {
		return pins;
	}

	public List<String> getPinsFromPrefs() {
		pins = new ArrayList<String>();

		try {
			for (int i = 0; i < nodePassword.keys().length; i++) {
				pins.add(nodePassword.get(i + "", null));
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		
		return pins;
	}
	
	public void setPinsToPrefs(List<String> pins) {
		deletePinsFromPrefs();
		for (int i = 0; i < pins.size(); i++) {
			nodePassword.put(i+"", pins.get(i));
		}
	


	}

	public boolean contains(String userInput) {

		boolean found = false;
		int numberOfPins = pins.size();

		for (int i = 0; i < numberOfPins; i++) {

			String pin = pins.get(i);

			if (userInput.equalsIgnoreCase(pin)) {
				ReaderPart.columnPassword.setEditingSupport(null);

				found = true;
				break;
			}
		}

		return found;
	}

	public void save(String userPin, String type, int index) {
		String userInput = userPin;

		if (type.equals("save")) {
			if (!contains(userPin)) {
				pins.add(userPin);
				nodePassword.put(Integer.toString(pins.size() - 1), userPin);
			}
		} else if (type.equals("edit")) {
			nodePassword.put(index + "", userInput);
			pins.remove(index);
			pins.add(index, userInput);

		}

		prefsUiFlush();
	}

	public void deletePinsFromPrefs() {

		try {
			nodePassword.clear();
			nodePassword.flush();

		} catch (BackingStoreException e1) {
			log(getClass(), "could not save the password in the preferences file", LogLevel.ERROR);
			e1.printStackTrace();
		}
		for (int i = 0; i < pins.size(); i++) {
			nodePassword.put(Integer.toString(i), pins.get(i));
			try {
				prefsUi.flush();
			} catch (BackingStoreException e) {
				log(getClass(), "could not save the password in the preferences file", LogLevel.ERROR);
				e.printStackTrace();
			}

		}
		getPinsFromPrefs();
	}

	private void prefsUiFlush() {
		try {
			prefsUi.flush();
		} catch (BackingStoreException e) {
			log(getClass(), "could not save the password in the preferences file", LogLevel.ERROR);
			e.printStackTrace();
		}
	}
}
