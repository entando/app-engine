package org.entando.entando.web.info;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;


class InfoControllerTest {

    private InfoController infoController;

    @BeforeEach
    public void setup() {
        infoController = new InfoController();
    }

    @Test
    void infoShouldReturnStatus200() {
        try (MockedStatic<InfoLoader> loader = Mockito.mockStatic(InfoLoader.class)) {
            loader.when(InfoLoader::getInfo).thenReturn(new HashMap<>());
            assertEquals(HttpStatus.OK.value(), infoController.info().getStatusCodeValue());
        }
    }

    @Test
    void infoShouldReturnStatus500() {
        try (MockedStatic<InfoLoader> loader = Mockito.mockStatic(InfoLoader.class)) {
            loader.when(InfoLoader::getInfo).thenThrow(new Exception("generic exception in loading info"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), infoController.info().getStatusCodeValue());
        }
    }

}
