/*
 * Copyright 2024-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.plugins.jpcds.aps.system.storage;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CdsFileAttributeViewDtoTest {


    @Test
    void shouldMarshallingAndUnmarshallingUseTheCorrectFieldsName()
            throws JsonParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        CdsFileAttributeViewDto dtoObject = new CdsFileAttributeViewDto();
        dtoObject.setDirectory(false);
        dtoObject.setSize(1024L);
        dtoObject.setProtectedFolder(true);
        dtoObject.setName("myname");
        dtoObject.setPath("/mypath");

        String dtoAsString = mapper.writeValueAsString(dtoObject);


        Assertions.assertThat(dtoAsString)
                .contains("\"directory\":false")
                .contains("\"protected_folder\":true")
                .contains("\"size\":1024")
                .contains("\"path\":\"/mypath\"")
                .contains("\"name\":\"myname\"");

        String test = "{\"size\":1000,\"directory\":true,\"path\":\"/her_path\",\"name\":\"hername\",\"protected_folder\":false}";
        CdsFileAttributeViewDto exp = mapper.readValue(test, CdsFileAttributeViewDto.class);
        Assertions.assertThat(exp.getSize()).isEqualTo(1000);
        Assertions.assertThat(exp.getDirectory()).isTrue();
        Assertions.assertThat(exp.getPath()).isEqualTo("/her_path");
        Assertions.assertThat(exp.getName()).isEqualTo("hername");
        Assertions.assertThat(exp.getDate()).isNull();
        Assertions.assertThat(exp.getProtectedFolder()).isFalse();

        dtoObject.setLastModifiedTime(new HashMap<>());
        Assertions.assertThat(dtoObject.getDate()).isNull();

        Map<String,String> m = new HashMap<>();
        m.put("secs_since_epoch","1676592000");
        dtoObject.setLastModifiedTime(m);
        dtoAsString = mapper.writeValueAsString(dtoObject);
        Assertions.assertThat(dtoAsString).contains("\"last_modified_time\":{\"secs_since_epoch\":\"1676592000\"}");

        test = "{\"size\":1000,\"directory\":true,\"last_modified_time\":{\"test\":true,\"secs_since_epoch\":\"1676592000\"},\"path\":\"/her_path\",\"name\":\"hername\",\"protected_folder\":false}";
        exp = mapper.readValue(test, CdsFileAttributeViewDto.class);
        Assertions.assertThat(exp.getDate()).isNotNull();
        Assertions.assertThat(exp.getLastModifiedTime()).containsEntry("secs_since_epoch", "1676592000");

    }

    @Test
    void shouldWorkFineWithHashEqualsAndToString() {

        CdsFileAttributeViewDto dtoObject1 = new CdsFileAttributeViewDto();
        dtoObject1.setDirectory(false);
        dtoObject1.setSize(1024L);
        dtoObject1.setProtectedFolder(true);
        dtoObject1.setName("myname");
        dtoObject1.setPath("/mypath");

        CdsFileAttributeViewDto dtoObject2 = new CdsFileAttributeViewDto();
        dtoObject2.setDirectory(false);
        dtoObject2.setSize(1024L);
        dtoObject2.setProtectedFolder(true);
        dtoObject2.setName("myname");
        dtoObject2.setPath("/mypath");

        Assertions.assertThat(dtoObject2.toString()).contains("myname","/mypath","1024");
        Assertions.assertThat(dtoObject2).hasSameHashCodeAs(dtoObject1);
        Assertions.assertThat(dtoObject2.equals(dtoObject1)).isTrue();
    }

}
