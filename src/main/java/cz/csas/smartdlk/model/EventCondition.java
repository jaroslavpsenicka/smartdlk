package cz.csas.smartdlk.model;

import lombok.Data;

@Data
public class EventCondition extends Named {

    private String condition;
    private Boolean trigger;
}
