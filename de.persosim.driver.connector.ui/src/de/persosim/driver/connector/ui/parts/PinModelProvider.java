package de.persosim.driver.connector.ui.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public enum PinModelProvider {

	INSTANCE;
	private Preferences prefsUi = InstanceScope.INSTANCE
			.getNode("de.persosim.driver.connector.ui");
	Preferences nodePin = prefsUi.node("nodePwd");

	public List<Pin> pins;
	public int pinNumber = 0;

	private PinModelProvider() {
		pins = new ArrayList<Pin>();

		pinNumber = 0;
		while (checkExistenceOfKey(pinNumber)) {

			String str = nodePin.get(Integer.toString(pinNumber), "");

			Pin pin = new Pin(str);
			pins.add(pin);
			pinNumber++;
		}

	}

	public List<Pin> getPins() {
		return pins;
	}

	public boolean checkExistenceOfKey(int key) {
		try {
			String[] keylist = nodePin.keys();
			for (int i = 0; i < keylist.length; i++) {
				if (keylist[i].equals(key + "")) {
					return true;
				}
			}
			return false;
		} catch (BackingStoreException e) {

		}
		return false;
	}

	public boolean checkExistence(String userInput) {

		boolean found = false;
		int numberOfPins = PinModelProvider.INSTANCE.pins.size();

		for (int i = 0; i < numberOfPins; i++) {
			Pin pin = PinModelProvider.INSTANCE.pins.get(i);
			;
			if (userInput.equalsIgnoreCase(pin.getPassword())) {
				ReaderPart.columnPin.setEditingSupport(null);

				found = true;
				break;
			}
		}

		return found;
	}

	public void save(Pin userPin, String type, String index) {
		String userInput = userPin.getPassword();

		if (type.equals("save")) {
			pins.add(userPin);
			nodePin.put(Integer.toString(pins.size() - 1),
					userPin.getPassword());
		} else if (type.equals("edit")) {
			nodePin.put(index, userInput);

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
			nodePin.put(Integer.toString(i), pins.get(i).getPassword());
			try {
				prefsUi.flush();
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		

	};
}
