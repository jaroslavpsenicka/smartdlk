package cz.csas.smartdlk.model;

import cz.csas.smartdlk.annotation.DiscriminatorValid;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import java.util.List;

@Data
@DiscriminatorValid
public class Rule extends Named {

    @NotEmpty
    private String label;
    private String description;
    private boolean active;

    private String discriminator;

    @Valid
    private List<EventCondition> events;

    @Valid
    private List<Model> model;

}
