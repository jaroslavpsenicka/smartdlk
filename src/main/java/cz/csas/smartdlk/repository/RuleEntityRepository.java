package cz.csas.smartdlk.repository;

import cz.csas.smartdlk.model.Rule;
import cz.csas.smartdlk.model.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RuleEntityRepository extends JpaRepository<RuleEntity, String> {

    Optional<RuleEntity> findByName(String name);

}
