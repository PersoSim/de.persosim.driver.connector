package de.persosim.driver.connector.ui.parts;

import java.util.Comparator;

/**
 * The PasswordComparator can be used for sorting string lists.
 * The sorted lists are beeing parsed to long, so they can be sorted assurgent.
 * 
 * @param A String list
 * @return a assurgent sorted list
 */
public class PasswordComparator implements Comparator<String> {

	@Override
	public int compare(String password1, String password2) {
		
		return (int) Math.signum(Long.parseLong(password1) - Long.parseLong(password2));
		
		
	}
}
