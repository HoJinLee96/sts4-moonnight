package net.chamman.moonnight.domain.estimate.simple;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimpleEstimateRepository extends JpaRepository<SimpleEstimate, Integer>{
  List<SimpleEstimate> findByUser_UserId(int userId);
  List<SimpleEstimate> findByPhone(String phone);

}
