package de.persosim.driver.connector.ui.parts;

import java.util.Comparator;

public class PasswordComparatorClass implements Comparator<Object> {
	private boolean sortPwdTable = false;

	private String sortType;

	public PasswordComparatorClass(boolean sortPwdTable, String sortType) {
		this.sortPwdTable = sortPwdTable;

		this.sortType = sortType;
	}

	@Override
	public int compare(Object o1, Object o2) {
		Pin p1 = (Pin) o1;
		Pin p2 = (Pin) o2;

		int ret = 0;

		if (sortType.equals("Password")) {
			if (sortPwdTable) {
				if (p1.getPassword().length() > p2.getPassword().length())
					ret = -1;
				else
					ret = 1;
			} else {
				if (p1.getPassword().length() > p2.getPassword().length())
					ret = 1;
				else
					ret = -1;

			}

		}
		return ret;
	}
}