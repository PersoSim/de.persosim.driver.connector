package de.persosim.driver.connector.pcsc;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.test.ConnectorTest;
import de.persosim.simulator.utils.HexString;

public class AbstractPcscFeatureTest extends ConnectorTest {

	AbstractPcscFeature feature;

	byte featureTag = 2;
	UnsignedInteger controlCode = new UnsignedInteger(0x123);

	@Before
	public void setUp(){
		feature = new AbstractPcscFeature(controlCode, featureTag) {
			
			@Override
			public PcscCallResult processPcscCall(PcscCallData data) {
				return null;
			}
		};
	}

	@Test
	public void testGetControlCode() {
		assertEquals(controlCode, feature.getControlCode());
	}

	@Test
	public void testGetFeatureDefinition() {
		byte[] expected = HexString.toByteArray("020400000123");
		assertArrayEquals(expected, feature.getFeatureDefinition());
	}

	/**
	 * Test case checking the immutability of the returned feature definition by
	 * trying to change it.
	 */
	@Test
	public void testGetFeatureDefinitionImmutability() {
		byte [] original = feature.getFeatureDefinition();
		byte [] toManipulate = feature.getFeatureDefinition();
		toManipulate[0] = (byte) 0xFF;
		assertArrayEquals(original, feature.getFeatureDefinition());
	}
}
