package cz.csas.smartdlk.repository;

import cz.csas.smartdlk.model.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventEntityRepository extends JpaRepository<EventEntity, String> {

    List<EventEntity> findByDiscriminator(String discriminator);

}
