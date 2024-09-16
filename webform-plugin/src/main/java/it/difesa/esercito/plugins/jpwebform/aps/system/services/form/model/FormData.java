package it.difesa.esercito.plugins.jpwebform.aps.system.services.form.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormData {

    public String valore1;
    public String valore2;
    public String valore3;
    public String valore4;
    public String valore5;

    public String testo1;
    public String testo2;
    public String testo3;
    public String testo4;
    public String testo5;

    public String etichettaSel1;
    public String etichettaSel2;
    public String etichettaSel3;
    public String etichettaSel4;
    public String etichettaSel5;

    public String etichetta1;
    public String etichetta2;
    public String etichetta3;
    public String etichetta4;
    public String etichetta5;

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(this);
    }

    public String getValore1() {
        return valore1;
    }

    public void setValore1(String valore1) {
        this.valore1 = valore1;
    }

    public String getValore2() {
        return valore2;
    }

    public void setValore2(String valore2) {
        this.valore2 = valore2;
    }

    public String getValore3() {
        return valore3;
    }

    public void setValore3(String valore3) {
        this.valore3 = valore3;
    }

    public String getValore4() {
        return valore4;
    }

    public void setValore4(String valore4) {
        this.valore4 = valore4;
    }

    public String getValore5() {
        return valore5;
    }

    public void setValore5(String valore5) {
        this.valore5 = valore5;
    }

    public String getTesto1() {
        return testo1;
    }

    public void setTesto1(String testo1) {
        this.testo1 = testo1;
    }

    public String getTesto2() {
        return testo2;
    }

    public void setTesto2(String testo2) {
        this.testo2 = testo2;
    }

    public String getTesto3() {
        return testo3;
    }

    public void setTesto3(String testo3) {
        this.testo3 = testo3;
    }

    public String getTesto4() {
        return testo4;
    }

    public void setTesto4(String _testo4) {
        this.testo4 = _testo4;
    }

    public String getTesto5() {
        return testo5;
    }

    public void setTesto5(String _testo5) {
        this.testo5 = _testo5;
    }

    public String getEtichettaSel1() {
        return etichettaSel1;
    }

    public void setEtichettaSel1(String etichettaSel1) {
        this.etichettaSel1 = etichettaSel1;
    }

    public String getEtichettaSel2() {
        return etichettaSel2;
    }

    public void setEtichettaSel2(String etichettaSel2) {
        this.etichettaSel2 = etichettaSel2;
    }

    public String getEtichettaSel3() {
        return etichettaSel3;
    }

    public void setEtichettaSel3(String etichettaSel3) {
        this.etichettaSel3 = etichettaSel3;
    }

    public String getEtichettaSel4() {
        return etichettaSel4;
    }

    public void setEtichettaSel4(String etichettaSel4) {
        this.etichettaSel4 = etichettaSel4;
    }

    public String getEtichettaSel5() {
        return etichettaSel5;
    }

    public void setEtichettaSel5(String etichettaSel5) {
        this.etichettaSel5 = etichettaSel5;
    }

    public String getEtichetta1() {
        return etichetta1;
    }

    public void setEtichetta1(String etichetta1) {
        this.etichetta1 = etichetta1;
    }

    public String getEtichetta2() {
        return etichetta2;
    }

    public void setEtichetta2(String etichetta2) {
        this.etichetta2 = etichetta2;
    }

    public String getEtichetta3() {
        return etichetta3;
    }

    public void setEtichetta3(String etichetta3) {
        this.etichetta3 = etichetta3;
    }

    public String getEtichetta4() {
        return etichetta4;
    }

    public void setEtichetta4(String etichetta4) {
        this.etichetta4 = etichetta4;
    }

    public String getEtichetta5() {
        return etichetta5;
    }

    public void setEtichetta5(String etichetta5) {
        this.etichetta5 = etichetta5;
    }
}
