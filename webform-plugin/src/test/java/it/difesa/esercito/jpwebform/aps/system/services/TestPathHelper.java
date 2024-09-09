package it.difesa.esercito.jpwebform.aps.system.services;


import it.difesa.esercito.plugins.jpwebform.aps.system.services.sige.PathHelper;
import junit.framework.TestCase;

public class TestPathHelper extends TestCase {

    public void testMergePath() {
        String baseUrl = "http://sito.com/path/";

        String resulting = PathHelper.mergePath(baseUrl, "/api/user");
        assertEquals("http://sito.com/path/api/user", resulting);

        baseUrl = "https://sito.com/path";

        resulting = PathHelper.mergePath(baseUrl, "api/user");
        assertEquals("https://sito.com/path/api/user", resulting);

        baseUrl = "https://sito.com/path";

        resulting = PathHelper.mergePath(baseUrl, "//api//user");
        assertEquals("https://sito.com/path/api/user", resulting);

    }

}
