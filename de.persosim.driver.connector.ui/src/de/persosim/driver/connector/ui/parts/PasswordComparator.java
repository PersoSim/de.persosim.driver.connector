package de.persosim.driver.connector.ui.parts;

import java.util.Comparator;

/**
 * The PasswordComparator can be used for sorting string lists.
 * The sorted lists are beeing parsed to int, so they can be sorted degressive.
 * 
 * @param A String list
 * @return a degressive sorted list
 */
public class PasswordComparator implements Comparator<String> {

	@Override
	public int compare(String password1, String password2) {
		
		return Integer.parseUnsignedInt(password2) - Integer.parseUnsignedInt(password1);
		
		
	}
}
