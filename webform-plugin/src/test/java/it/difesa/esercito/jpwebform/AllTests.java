package it.difesa.esercito.jpwebform;

import it.difesa.esercito.jpwebform.aps.TestApsSample;
import it.difesa.esercito.jpwebform.aps.system.services.TestFormManager;
import it.difesa.esercito.jpwebform.aps.system.services.TestMailManager;
import it.difesa.esercito.jpwebform.aps.system.services.TestMapper;
import it.difesa.esercito.jpwebform.aps.system.services.TestPathHelper;
import it.difesa.esercito.jpwebform.aps.system.services.TestSigeManager;
import it.difesa.esercito.jpwebform.apsadmin.TestApsAdminSample;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 // FOR TESTING WITHIN IDE
 SME_SMTP_HOST=sandbox.smtp.mailtrap.io;SME_SMTP_DEBUG=true;SME_SMTP_PORT=587;SME_SMTP_USERNAME=24deba168f6610;SME_SMTP_PASSWORD=2a5513ac5ad9f8;SME_SMTP_AUTH=true;SME_SMTP_STARTTLS=true;SME_SMTP_STARTTLS_REQUIRED=true;SME_SSL_ENABLE=false;SME_SMTP_RECIPIENTS=key1=cantelli@localhost.it,key2=oriani@localhost.biz,from=from@localhost.uk;SME_SIGE_PROXY_ENDPOINT=https://aggate.apps.clustersme.smeentando.com/aggate-sige-proxy-70028b8f/aggate-sige-proxy-ms/;SME_SIGE_CLIENT_AUTH_URL=https://aggate.apps.clustersme.smeentando.com;SME_SIGE_CLIENT_ID=ceigs_confidential;SME_SIGE_CLIENT_SECRET=vesMCB45x22OUBHqggwqefblIH3OckBu;SME_SIGE_REALM=entando

 // FOR TESTING FROM COMMAND LINE
 export SME_SMTP_HOST="sandbox.smtp.mailtrap.io" &&
 export SME_SMTP_DEBUG="true" &&
 export SME_SMTP_PORT="587" &&
 export SME_SMTP_USERNAME="24deba168f6610" &&
 export SME_SMTP_PASSWORD="2a5513ac5ad9f8"  &&
 export SME_SMTP_AUTH="true" &&
 export SME_SMTP_STARTTLS="true" &&
 export SME_SMTP_STARTTLS_REQUIRED="true" &&
 export SME_SSL_ENABLE="false" &&
 export SME_SMTP_RECIPIENTS="key1=cantelli@localhost.net,key2=oriani@localhost.uk,from=from@localhost.net" &&
 export SME_SIGE_PROXY_ENDPOINT="https://aggate.apps.clustersme.smeentando.com/aggate-sige-proxy-70028b8f/aggate-sige-proxy-ms/" &&
 export SME_SIGE_CLIENT_AUTH_URL="https://aggate.apps.clustersme.smeentando.com" &&
 export SME_SIGE_CLIENT_ID="ceigs_confidential" &&
 export SME_SIGE_CLIENT_SECRET="vesMCB45x22OUBHqggwqefblIH3OckBu" &&
 export SME_SIGE_REALM="entando"
 */

public class AllTests {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Change me with a suitable description");

//		suite.addTestSuite(TestMapper.class);
//		suite.addTestSuite(TestFormManager.class);
		suite.addTestSuite(TestMailManager.class);
//		suite.addTestSuite(TestPathHelper.class);
//		suite.addTestSuite(TestSigeManager.class);

//		suite.addTestSuite(TestApsSample.class);
//		suite.addTestSuite(TestApsAdminSample.class);
		
		return suite;
	}
	
}
