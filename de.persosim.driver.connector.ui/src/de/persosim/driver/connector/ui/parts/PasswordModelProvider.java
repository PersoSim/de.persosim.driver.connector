package de.persosim.driver.connector.ui.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PinModelProvider {
	private static PinModelProvider pinModelProvider = null;

	private static Preferences prefsUi = InstanceScope.INSTANCE
			.getNode("de.persosim.driver.connector.ui");
	static Preferences nodePin = prefsUi.node("nodePwd");

	private List<String> pins;
	private boolean sort = true;

	public boolean getSort() {
		return sort;
	}

	public void setSort(boolean sort) {
		this.sort = sort;
	}

	private PinModelProvider() {
		pins = getPinsFromPrefs();
	}

	public static PinModelProvider getInstance() {
		if (pinModelProvider != null)
			return pinModelProvider;
		else
			return pinModelProvider = new PinModelProvider();
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
			for (int i = 0; i < nodePin.keys().length; i++) {
				pins.add(nodePin.get(i + "", null));
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		return pins;
	}

	public boolean checkExistence(String userInput) {

		boolean found = false;
		int numberOfPins = pins.size();

		for (int i = 0; i < numberOfPins; i++) {

			String pin = pins.get(i);

			if (userInput.equalsIgnoreCase(pin)) {
				ReaderPart.columnPin.setEditingSupport(null);

				found = true;
				break;
			}
		}

		return found;
	}

	public void save(String userPin, String type, int index) {
		String userInput = userPin;

		if (type.equals("save")) {
			if (!checkExistence(userPin)) {
				pins.add(userPin);
				nodePin.put(Integer.toString(pins.size() - 1), userPin);
			}
		} else if (type.equals("edit")) {
			nodePin.put(index + "", userInput);
			pins.remove(index);
			pins.add(index, userInput);

		}

		try {
			prefsUi.flush();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void deletePinsFromPrefs(int index) {

		try {
			nodePin.clear();
			nodePin.flush();

		} catch (BackingStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (int i = 0; i < pins.size(); i++) {
			nodePin.put(Integer.toString(i), pins.get(i));
			try {
				prefsUi.flush();
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		getPinsFromPrefs();
	}
}
