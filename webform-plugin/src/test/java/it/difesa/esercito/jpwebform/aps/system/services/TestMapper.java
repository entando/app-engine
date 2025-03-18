/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import java.io.IOException;
import java.time.LocalDateTime;
import junit.framework.TestCase;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.DtoHelper;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import org.junit.jupiter.api.Test;

public class TestMapper extends TestCase {

	private static final LocalDateTime TODAY = LocalDateTime.now();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testFormDataMapper() throws IOException {
		final FormData fd = getFormDataForTest();

		String json = fd.toJson();
		FormData verify = DtoHelper.toFormData(json);
		verifyFormData(verify);
	}

	private void verifyFormData(FormData data) {
		assertNotNull(data);

		assertEquals("setValore1", data.getValore1());
		assertEquals("setValore2", data.getValore2());
		assertEquals("setValore3", data.getValore3());
		assertEquals("setValore4", data.getValore4());
		assertEquals("setValore5", data.getValore5());

		assertEquals("setTesto1", data.getTesto1());
		assertEquals("setTesto2", data.getTesto2());
		assertEquals("setTesto3", data.getTesto3());
		assertEquals("setTesto4", data.getTesto4());
		assertEquals("setTesto5", data.getTesto5());

		assertEquals("setEtichettaSel1", data.getEtichettaSel1());
		assertEquals("setEtichettaSel3", data.getEtichettaSel2());

		assertNull(data.getEtichettaSel3());

		assertEquals("setEtichettaSel4", data.getEtichettaSel4());
		assertEquals("setEtichettaSel5", data.getEtichettaSel5());

		assertEquals("setEtichetta1", data.getEtichetta1());
		assertEquals("setEtichetta2", data.getEtichetta2());
		assertEquals("setEtichetta3", data.getEtichetta3());
		assertEquals("setEtichetta4", data.getEtichetta4());
		assertEquals("setEtichetta5", data.getEtichetta5());
	}

	@Test
	public void testFormMapper() throws IOException{
		final Form form = getFormForTest();

		String json = form.toJson();

		System.out.println(json);

		Form verify = DtoHelper.toForm(json);

		assertEquals(verify.getName(), "Oettam");
		assertEquals(verify.getSubmitted(), TODAY);
		assertEquals("address@email.it", verify.getDelivery().getRecipient());
		assertEquals("cc@email.it", verify.getDelivery().getCc());
		assertEquals("QualifiedName", verify.getDelivery().getQualifiedName());
		assertEquals("mail subject", verify.getDelivery().getSubject());
		verifyFormData(verify.getData());

	}

	public static FormData getFormDataForTest() {
		FormData fd = new FormData();

		fd.setValore1("setValore1");
		fd.setValore2("setValore2");
		fd.setValore3("setValore3");
		fd.setValore4("setValore4");
		fd.setValore5("setValore5");

		fd.setTesto1("setTesto1");
		fd.setTesto2("setTesto2");
		fd.setTesto3("setTesto3");
		fd.setTesto4("setTesto4");
		fd.setTesto5("setTesto5");

		fd.setEtichettaSel1("setEtichettaSel1");
		fd.setEtichettaSel2("setEtichettaSel3");
		fd.setEtichettaSel3(null);
		fd.setEtichettaSel4("setEtichettaSel4");
		fd.setEtichettaSel5("setEtichettaSel5");

		fd.setEtichetta1("setEtichetta1");
		fd.setEtichetta2("setEtichetta2");
		fd.setEtichetta3("setEtichetta3");
		fd.setEtichetta4("setEtichetta4");
		fd.setEtichetta5("setEtichetta5");

		return fd;
	}

	public static Form getFormForTest() {
		Form form = new Form();

		form.setId(2677L);
		form.setName("Oettam");
		form.setSubmitted(TODAY);
		form.setData(TestFormManager.getFormDataForTest());
		form.setDelivery(TestFormManager.getDeliveryDataForTest());
		form.setConfiguration(TestFormManager.getFormConfigForTests());
		return form;
	}


}
