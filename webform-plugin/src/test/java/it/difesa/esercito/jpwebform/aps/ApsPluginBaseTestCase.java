package it.difesa.esercito.jpwebform.aps;

import com.agiletec.ConfigTestUtils;
import com.agiletec.aps.BaseTestCase;
import it.difesa.esercito.jpwebform.PluginConfigTestUtils;

public class ApsPluginBaseTestCase extends BaseTestCase {
	
	@Override
	protected ConfigTestUtils getConfigUtils() {
		return new PluginConfigTestUtils();
	}
	
}
