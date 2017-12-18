package cz.csas.smartdlk.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;

@Data
@Entity
public class RuleEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NotEmpty
    @Column(unique=true)
    private String name;

    private boolean active;

    @Lob
    private byte[] rule;
}
