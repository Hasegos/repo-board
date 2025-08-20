package io.github.repoboard.service;

import io.github.repoboard.dto.UserDTO;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }




    @Transactional
    public User register(UserDTO userDTO){

        if(userRepository.existsByUsername(userDTO.getUsername())){
            throw new EntityExistsException("이미 사용중인 아이디입니다.");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(UserRoleType.USER);

        /* 소셜 로그인 */
        if(userDTO.getProviderId() != null && !userDTO.getProviderId().isEmpty()){
            user.setProvider(userDTO.getProvider());
            user.setProviderId(userDTO.getProviderId());
        }

        return userRepository.save(user);
    }
}