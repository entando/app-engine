package org.entando.entando.web.assertionhelper;

import org.entando.entando.aps.system.services.component.ComponentUsageEntity;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;

public class PageAssertionHelper {


    public static void assertUsagePageDetails(ResultActions resultActions, List<ComponentUsageEntity> compUsageEntList) throws Exception {

        resultActions.andExpect(status().isOk());

        IntStream.range(0, compUsageEntList.size())
                .forEach(i -> assertUsagePageDetails(resultActions, compUsageEntList.get(i), "$.payload[" + i + "]"));
    }


    public static void assertUsagePageDetails(ResultActions resultActions, ComponentUsageEntity componentUsageEntity, String prefix) {

        try {
            resultActions
                    .andExpect(jsonPath(prefix + ".type", is(componentUsageEntity.getType())))
                    .andExpect(jsonPath(prefix + ".code", is(componentUsageEntity.getCode())))
                    .andExpect(jsonPath(prefix + ".status", is(componentUsageEntity.getStatus())));
        } catch (Exception e) {
            Assertions.fail();
        }
    }
}
