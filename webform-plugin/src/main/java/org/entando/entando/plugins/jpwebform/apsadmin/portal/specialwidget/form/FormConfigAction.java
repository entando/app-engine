/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.apsadmin.portal.specialwidget.form;

import com.agiletec.apsadmin.portal.specialwidget.SimpleWidgetConfigAction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormConfigAction extends SimpleWidgetConfigAction {

	private static final Logger _logger =  LoggerFactory.getLogger(FormConfigAction.class);
	
	protected String extractInitConfig() {
		String result = super.extractInitConfig();

		String titolo = this.getWidget().getConfig().getProperty("titolo");
		String descrizione = this.getWidget().getConfig().getProperty("descrizione");
		String idDestinatario = this.getWidget().getConfig().getProperty("idDestinatario");
		String oggetto = this.getWidget().getConfig().getProperty("oggetto");
		String invioMsgOk = this.getWidget().getConfig().getProperty("invioMsgOk");
		String invioMsgKo = this.getWidget().getConfig().getProperty("invioMsgKo");

		String opzione1 = this.getWidget().getConfig().getProperty("opzione1");
		String valoriOpzione1 = this.getWidget().getConfig().getProperty("valoriOpzione1");

		String opzione2 = this.getWidget().getConfig().getProperty("opzione2");
		String valoriOpzione2 = this.getWidget().getConfig().getProperty("valoriOpzione2");

		String opzione3 = this.getWidget().getConfig().getProperty("opzione3");
		String valoriOpzione3 = this.getWidget().getConfig().getProperty("valoriOpzione3");

		String opzione4 = this.getWidget().getConfig().getProperty("opzione4");
		String valoriOpzione4 = this.getWidget().getConfig().getProperty("valoriOpzione4");

		String opzione5 = this.getWidget().getConfig().getProperty("opzione5");
		String valoriOpzione5 = this.getWidget().getConfig().getProperty("valoriOpzione5");

		String etichetta1 = this.getWidget().getConfig().getProperty("etichetta1");
		String obbligatorio1Val = this.getWidget().getConfig().getProperty("obbligatorio1");

		String etichetta2 = this.getWidget().getConfig().getProperty("etichetta2");
		String obbligatorio2Val = this.getWidget().getConfig().getProperty("obbligatorio2");

		String etichetta3 = this.getWidget().getConfig().getProperty("etichetta3");
		String obbligatorio3Val = this.getWidget().getConfig().getProperty("obbligatorio3");

		String etichetta4 = this.getWidget().getConfig().getProperty("etichetta4");
		String obbligatorio4Val = this.getWidget().getConfig().getProperty("obbligatorio4");

		String etichetta5 = this.getWidget().getConfig().getProperty("etichetta5");
		String obbligatorio5Val = this.getWidget().getConfig().getProperty("obbligatorio5");

		if (StringUtils.isNotBlank(titolo)) {
			this.setTitolo(titolo);
		}
		if (StringUtils.isNotBlank(descrizione)) {
			this.setDescrizione(descrizione);
		}
		if (StringUtils.isNotBlank(idDestinatario)) {
			this.setIdDestinatario(idDestinatario);
		}
		if (StringUtils.isNotBlank(oggetto)) {
			this.setOggetto(oggetto);
		}
		if (StringUtils.isNotBlank(invioMsgOk)) {
			this.setInvioMsgOk(invioMsgOk);
		}
		if (StringUtils.isNotBlank(invioMsgKo)) {
			this.setInvioMsgKo(invioMsgKo);
		}

		// DROPDOWN
		if (StringUtils.isNotBlank(opzione1)) {
			this.setOpzione1(opzione1);
		}
		if (StringUtils.isNotBlank(valoriOpzione1)) {
			this.setValoriOpzione1(valoriOpzione1);
		}

		if (StringUtils.isNotBlank(opzione2)) {
			this.setOpzione2(opzione2);
		}
		if (StringUtils.isNotBlank(valoriOpzione2)) {
			this.setValoriOpzione2(valoriOpzione2);
		}

		if (StringUtils.isNotBlank(opzione3)) {
			this.setOpzione3(opzione3);
		}
		if (StringUtils.isNotBlank(valoriOpzione3)) {
			this.setValoriOpzione3(valoriOpzione3);
		}

		if (StringUtils.isNotBlank(opzione4)) {
			this.setOpzione4(opzione4);
		}
		if (StringUtils.isNotBlank(valoriOpzione4)) {
			this.setValoriOpzione4(valoriOpzione4);
		}

		if (StringUtils.isNotBlank(opzione5)) {
			this.setOpzione5(opzione5);
		}
		if (StringUtils.isNotBlank(valoriOpzione5)) {
			this.setValoriOpzione5(valoriOpzione5);
		}


		// TEXT
		if (StringUtils.isNotBlank(etichetta1)) {
			this.setEtichetta1(etichetta1);
		}
		if (StringUtils.isNotBlank(obbligatorio1Val)) {
			this.setObbligatorio1(new Boolean(obbligatorio1Val).booleanValue());
		}

		if (StringUtils.isNotBlank(etichetta2)) {
			this.setEtichetta2(etichetta2);
		}
		if (StringUtils.isNotBlank(obbligatorio2Val)) {
			this.setObbligatorio2(new Boolean(obbligatorio2Val).booleanValue());
		}

		if (StringUtils.isNotBlank(etichetta3)) {
			this.setEtichetta3(etichetta3);
		}
		if (StringUtils.isNotBlank(obbligatorio3Val)) {
			this.setObbligatorio3(new Boolean(obbligatorio3Val).booleanValue());
		}

		if (StringUtils.isNotBlank(etichetta4)) {
			this.setEtichetta4(etichetta4);
		}
		if (StringUtils.isNotBlank(obbligatorio4Val)) {
			this.setObbligatorio4(new Boolean(obbligatorio4Val).booleanValue());
		}

		if (StringUtils.isNotBlank(etichetta5)) {
			this.setEtichetta5(etichetta5);
		}
		if (StringUtils.isNotBlank(obbligatorio5Val)) {
			this.setObbligatorio5(new Boolean(obbligatorio5Val).booleanValue());
		}
		
		return result;
	}


	public String getTitolo() {
		return _titolo;
	}

	public void setTitolo(String titolo) {
		this._titolo = titolo;
	}

	public String getDescrizione() {
		return _descrizione;
	}

	public void setDescrizione(String descrizione) {
		this._descrizione = descrizione;
	}

	public String getIdDestinatario() {
		return _idDestinatario;
	}

	public void setIdDestinatario(String idDestinatario) {
		this._idDestinatario = idDestinatario;
	}

	public String getOggetto() {
		return _oggetto;
	}

	public void setOggetto(String oggetto) {
		this._oggetto = oggetto;
	}

	public String getInvioMsgOk() {
		return _invioMsgOk;
	}

	public void setInvioMsgOk(String invioMsgOk) {
		this._invioMsgOk = invioMsgOk;
	}

	public String getInvioMsgKo() {
		return _invioMsgKo;
	}

	public void setInvioMsgKo(String invioMsgKo) {
		this._invioMsgKo = invioMsgKo;
	}

	public String getOpzione1() {
		return _opzione1;
	}

	public void setOpzione1(String opzione1) {
		this._opzione1 = opzione1;
	}

	public String getValoriOpzione1() {
		return _valoriOpzione1;
	}

	public void setValoriOpzione1(String valoriOpzione1) {
		this._valoriOpzione1 = valoriOpzione1;
	}

	public String getEtichetta1() {
		return _etichetta1;
	}

	public void setEtichetta1(String etichetta1) {
		this._etichetta1 = etichetta1;
	}

	public boolean getObbligatorio1() {
		return _obbligatorio1;
	}

	public void setObbligatorio1(boolean obbligatorio1) {
		this._obbligatorio1 = obbligatorio1;
	}

	public String getOpzione2() {
		return _opzione2;
	}

	public void setOpzione2(String opzione2) {
		this._opzione2 = opzione2;
	}

	public String getOpzione3() {
		return _opzione3;
	}

	public void setOpzione3(String opzione3) {
		this._opzione3 = opzione3;
	}

	public String getOpzione4() {
		return _opzione4;
	}

	public void setOpzione4(String opzione4) {
		this._opzione4 = opzione4;
	}

	public String getOpzione5() {
		return _opzione5;
	}

	public void setOpzione5(String opzione5) {
		this._opzione5 = opzione5;
	}

	public String getValoriOpzione2() {
		return valoriOpzione2;
	}

	public void setValoriOpzione2(String valoriOpzione2) {
		this.valoriOpzione2 = valoriOpzione2;
	}

	public String getValoriOpzione3() {
		return valoriOpzione3;
	}

	public void setValoriOpzione3(String valoriOpzione3) {
		this.valoriOpzione3 = valoriOpzione3;
	}

	public String getValoriOpzione4() {
		return valoriOpzione4;
	}

	public void setValoriOpzione4(String valoriOpzione4) {
		this.valoriOpzione4 = valoriOpzione4;
	}

	public String getValoriOpzione5() {
		return valoriOpzione5;
	}

	public void setValoriOpzione5(String valoriOpzione5) {
		this.valoriOpzione5 = valoriOpzione5;
	}

	public String getEtichetta2() {
		return _etichetta2;
	}

	public void setEtichetta2(String etichetta2) {
		this._etichetta2 = etichetta2;
	}

	public String getEtichetta3() {
		return _etichetta3;
	}

	public void setEtichetta3(String etichetta3) {
		this._etichetta3 = etichetta3;
	}

	public String getEtichetta4() {
		return _etichetta4;
	}

	public void setEtichetta4(String etichetta4) {
		this._etichetta4 = etichetta4;
	}

	public String getEtichetta5() {
		return _etichetta5;
	}

	public void setEtichetta5(String etichetta5) {
		this._etichetta5 = etichetta5;
	}

	public boolean isObbligatorio2() {
		return _obbligatorio2;
	}

	public void setObbligatorio2(boolean obbligatorio2) {
		this._obbligatorio2 = obbligatorio2;
	}

	public boolean isObbligatorio3() {
		return _obbligatorio3;
	}

	public void setObbligatorio3(boolean obbligatorio3) {
		this._obbligatorio3 = obbligatorio3;
	}

	public boolean isObbligatorio4() {
		return _obbligatorio4;
	}

	public void setObbligatorio4(boolean obbligatorio4) {
		this._obbligatorio4 = obbligatorio4;
	}

	public boolean isObbligatorio5() {
		return _obbligatorio5;
	}

	public void setObbligatorio5(boolean obbligatorio5) {
		this._obbligatorio5 = obbligatorio5;
	}

	private String _titolo;
	private String _descrizione;
	private String _idDestinatario;
	private String _oggetto;
	private String _invioMsgOk;
	private String _invioMsgKo;

	private String _opzione1;
	private String _opzione2;
	private String _opzione3;
	private String _opzione4;
	private String _opzione5;
	private String _valoriOpzione1;
	private String valoriOpzione2;
	private String valoriOpzione3;
	private String valoriOpzione4;
	private String valoriOpzione5;

	private String _etichetta1;
	private String _etichetta2;
	private String _etichetta3;
	private String _etichetta4;
	private String _etichetta5;

	private boolean _obbligatorio1;
	private boolean _obbligatorio2;
	private boolean _obbligatorio3;
	private boolean _obbligatorio4;
	private boolean _obbligatorio5;
	
}
