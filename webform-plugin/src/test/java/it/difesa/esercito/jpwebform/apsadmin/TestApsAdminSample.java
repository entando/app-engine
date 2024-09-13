package it.difesa.esercito.jpwebform.apsadmin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestApsAdminSample extends ApsAdminBaseTestCase {


	@Test
	public void test() {
		assertTrue(true);
	}

	@BeforeEach
	public void init() {
    	try {
    		// init services
		} catch (Exception e) {
			throw e;
		}
    }
	
}
