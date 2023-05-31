package org.entando.entando.plugins.jacms.web.resource.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import org.entando.entando.aps.system.services.IComponentDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class AssetDto implements IComponentDto {
    private String id;

    private String correlationCode;

    private String type;

    private String name;

    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss'Z'")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss'Z'")
    private Date updatedAt;

    private String group;

    private List<String> categories;

    private String owner;

    private String folderPath;

    private String fileName;

    @Override
    public String getCode() {
        return this.getId();
    }
    
}
