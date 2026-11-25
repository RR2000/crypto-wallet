package com.rondinella.authorizationserver.usermanaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<CryptoUserDatabase, String> {
  CryptoUserDatabase findByEmail(String username);
}
