package sask.practise.elya.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sask.practise.elya.models.Position;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long>, JpaSpecificationExecutor<Position> {
    Optional<Position> findByName(String position);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.position.id = :positionId")
    long countEmployeesByPositionId(@Param("positionId") Long positionId);
}
