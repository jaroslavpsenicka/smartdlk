package cz.csas.smartdlk.model;

import lombok.Data;

@Data
public class Model extends Named {

    private ModelType type;
    private String mapping;
    private boolean optional;

}
