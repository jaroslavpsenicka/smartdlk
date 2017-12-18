package cz.csas.smartdlk.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;

@Data
public class Named {

    @NotEmpty
    @Pattern(regexp = "a-zA-Z0-9_-")
    private String name;
}
