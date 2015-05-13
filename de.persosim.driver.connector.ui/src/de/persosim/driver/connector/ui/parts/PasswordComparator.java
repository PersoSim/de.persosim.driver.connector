package de.persosim.driver.connector.ui.parts;

import java.util.Comparator;


public class PasswordComparator implements Comparator<String> {
	private boolean sort;

	public PasswordComparator(boolean sort) {
		this.sort = sort;
	}

	@Override
	public int compare(String o1, String o2) throws IllegalArgumentException {
		int ret = 0;
		if (o1.matches(".*[a-zA-Z]+.*") || o2.matches(".*[a-zA-Z]+.*")) {
			try {
				throw new IllegalAccessException(
						"Invalid arguemnt in parameter");
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

		}

		if (o1.length() > o2.length()) {
			ret = 1;
		} else if (o2.length() > o1.length()) {
			ret = -1;
		} else {

			char[] array1 = o1.toCharArray();
			char[] array2 = o2.toCharArray();

			for (int y = 0; y < 10; y++) {
				if (Integer.parseInt(array1[y] + "") > Integer
						.parseInt(array2[y] + "")) {
					ret = 1;
					break;

				} else if (Integer.parseInt(array1[y] + "") < Integer
						.parseInt(array2[y] + "")) {
					ret = -1;
					break;
				}

			}
		}

		if (!sort) {
			ret *= -1;
		}
		return ret;
	}

}
