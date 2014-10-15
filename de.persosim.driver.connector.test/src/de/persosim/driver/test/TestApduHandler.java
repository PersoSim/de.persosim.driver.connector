package de.persosim.driver.test;

public interface TestApduHandler {

	byte[] processCommand(String apduLine);

}
