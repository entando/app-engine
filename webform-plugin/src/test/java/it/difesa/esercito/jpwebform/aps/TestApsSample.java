package it.difesa.esercito.jpwebform.aps;

import static org.junit.Assert.assertTrue;

public class TestApsSample extends ApsPluginBaseTestCase {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.init();
	}
	
	public void test() {
		assertTrue(true);
	}
	
	private void init() throws Exception {
    	try {
    		// init services
		} catch (Exception e) {
			throw e;
		}
    }
	
}
