package cz.csas.smartdlk.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;

@Data
@Entity
public class EventEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NotEmpty
    private String eventName;

    @NotEmpty
    private String discriminator;

    @Lob
    private byte[] data;
}
