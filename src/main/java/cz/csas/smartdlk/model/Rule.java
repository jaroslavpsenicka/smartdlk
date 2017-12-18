package cz.csas.smartdlk.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import java.util.List;

@Data
public class Rule extends Named {

    @NotEmpty
    private String label;
    private String description;
    private boolean active;

    @Valid
    private List<EventCondition> events;

    @Valid
    private List<Model> model;

}
