/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import org.entando.entando.plugins.jpwebform.aps.system.services.form.DtoHelper;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

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

	private void verifyFormData(FormData verify) {
		assertNotNull(verify);
		assertEquals("testo1", verify.getTesto1());
		assertEquals("testo2", verify.getTesto2());
		assertEquals("testo3", verify.getTesto3());
		assertEquals("testo4", verify.getTesto4());
		assertEquals("testo5", verify.getTesto5());

		assertEquals("valore1", verify.getValore1());
		assertEquals("valore2", verify.getValore2());
		assertEquals("valore3", verify.getValore3());
		assertEquals("valore4", verify.getValore4());
		assertEquals("valore5", verify.getValore5());

		assertEquals("etichetta1", verify.getEtichetta1());
		assertEquals("etichetta2", verify.getEtichetta2());
		assertEquals("etichetta3", verify.getEtichetta3());
		assertEquals("etichetta4", verify.getEtichetta4());
		assertEquals("etichetta5", verify.getEtichetta5());

		assertEquals("etichettaSel1", verify.getEtichettaSel1());
		assertEquals("etichettaSel2", verify.getEtichettaSel2());
		assertEquals("etichettaSel3", verify.getEtichettaSel3());
		assertEquals("etichettaSel4", verify.getEtichettaSel4());
		assertEquals("etichettaSel5", verify.getEtichettaSel5());
	}

	@Test
	public void testFormMapper() throws IOException {
		final Form form = getFormForTest();

		String json = form.toJson();
		assertNotNull(json);
		final Form verify = DtoHelper.toForm(json);
		assertNotNull(verify);

		assertEquals(verify.getId(), (Long)2677L);
		assertEquals(verify.getName(), "Oettam");
		assertEquals(verify.getSubmitted(), TODAY);
		assertEquals("address@email.it", verify.getRecipient());
		assertEquals("cc@email.it", verify.getCc());
		assertEquals("Esq. John Doe", verify.getQualifiedName());
		assertEquals("mail subject", verify.getSubject());
		verifyFormData(verify.getData());
	}

	public static FormData getFormDataForTest() {
		FormData fd = new FormData();

		fd.setTesto1("testo1");
		fd.setTesto2("testo2");
		fd.setTesto3("testo3");
		fd.setTesto4("testo4");
		fd.setTesto5("testo5");

		fd.setValore1("valore1");
		fd.setValore2("valore2");
		fd.setValore3("valore3");
		fd.setValore4("valore4");
		fd.setValore5("valore5");
		
		fd.setEtichetta1("etichetta1");
		fd.setEtichetta2("etichetta2");
		fd.setEtichetta3("etichetta3");
		fd.setEtichetta4("etichetta4");
		fd.setEtichetta5("etichetta5");

		fd.setEtichettaSel1("etichettaSel1");
		fd.setEtichettaSel2("etichettaSel2");
		fd.setEtichettaSel3("etichettaSel3");
		fd.setEtichettaSel4("etichettaSel4");
		fd.setEtichettaSel5("etichettaSel5");
		return fd;
	}

	public static Form getFormForTest() {
		Form form = new Form();

		form.setId(2677L);
		form.setName("Oettam");
		form.setSubmitted(TODAY);
		form.setData(getFormDataForTest());
		form.setRecipient("address@email.it");
		form.setQualifiedName("Esq. John Doe");
		form.setCc("cc@email.it");
		form.setSubject("mail subject");
		return form;
	}
}
