package ai.openfabric.api.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dockerjava.api.model.Container;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@Entity()
@Table(name="worker")
public class Worker extends Datable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "of-uuid")
    @GenericGenerator(name = "of-uuid", strategy = "ai.openfabric.api.model.IDGenerator")
    @Getter
    @Setter
    @Column(name = "id")
    public String id;

    @Getter
    @Setter
    @Column(name = "name")
    public String name;

    @Getter
    @Setter
    @Column(name = "container_id")
    private String containerId;

    @Getter
    @Setter
    @Transient
    @JsonProperty("container_details")
    private Container container;

}
