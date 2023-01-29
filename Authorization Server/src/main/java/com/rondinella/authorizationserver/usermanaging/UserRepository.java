package com.rondinella.authorizationserver.usermanaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<CryptoUserDatabase, Long> {

  CryptoUserDatabase findByUsername(String username);
}
