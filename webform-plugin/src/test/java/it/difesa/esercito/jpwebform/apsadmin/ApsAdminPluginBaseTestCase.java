package it.difesa.esercito.jpwebform.apsadmin;

import com.agiletec.ConfigTestUtils;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import it.difesa.esercito.jpwebform.PluginConfigTestUtils;

public class ApsAdminPluginBaseTestCase extends ApsAdminBaseTestCase {
	
	@Override
	protected ConfigTestUtils getConfigUtils() {
		return new PluginConfigTestUtils();
	}
	
}
