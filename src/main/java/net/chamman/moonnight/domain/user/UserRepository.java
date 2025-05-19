package net.chamman.moonnight.domain.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;

@Component
@Repository
public interface UserRepository extends JpaRepository<User, Integer>{
  Optional<User> findByUserProviderAndEmail(UserProvider userProvider, String email);
  Optional<User> findByUserProviderAndPhone(UserProvider userProvider, String phone);
  Optional<User> findByUserProviderAndEmailAndPhone(UserProvider userProvider, String email, String phone);
  Optional<UserStatus> findUserStatusByUserProviderAndEmail(UserProvider userProvider,String email);
  boolean existsByUserProviderAndEmail(UserProvider userProvider, String email);
  boolean existsByUserProviderAndPhone(UserProvider userProvider, String phone);

//  @Modifying(clearAutomatically = true)
//  @Query("UPDATE User u SET u.status = 'STAY' WHERE u.userProvider = :userProvider AND u.email = :email")
//  int updateStatusSetStayByEmailAndUserProvider(@Param("userProvider") UserProvider userProvider, @Param("email") String email);

}