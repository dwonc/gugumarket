package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUser(String userName) {
        Optional<User> siteUser = this.userRepository.findByUserName(userName);

        if(siteUser.isPresent()) {
            User user = siteUser.get();
            return user;
        }
        else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

}
