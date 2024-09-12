package it.difesa.esercito.jpwebform.aps;

import com.agiletec.ConfigTestUtils;
import com.agiletec.aps.BaseTestCase;
import it.difesa.esercito.jpwebform.PluginConfigTestUtils;

public abstract class ApsPluginBaseTestCase extends BaseTestCase{ // TestCase

	@Override
	protected ConfigTestUtils getConfigUtils() {
		return new PluginConfigTestUtils();
	}

	protected abstract void setUp() throws Exception;
}
