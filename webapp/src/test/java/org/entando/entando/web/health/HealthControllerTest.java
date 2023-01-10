package org.entando.entando.web.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.entando.entando.aps.system.services.health.HealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private HealthService healthService;

    private HealthController healthController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        healthController = new HealthController(healthService, "extended");
    }

    @Test
    void isHealthyWithWorkingSystemShouldReturnStatus200() {

        when(healthService.isHealthy()).thenReturn(true);

        assertEquals(HttpStatus.OK.value(), healthController.isHealthy().getStatusCodeValue());
    }

    @Test
    void isHealthyWithNotWorkingSystemShouldReturnStatus500() {

        when(healthService.isHealthy()).thenReturn(false);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), healthController.isHealthy().getStatusCodeValue());
    }
}
