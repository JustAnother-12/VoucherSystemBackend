package com.dacn.VoucherSystem.service.impl;


import com.dacn.VoucherSystem.dto.response.UserResponse;
import com.dacn.VoucherSystem.entity.User;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.UserMapper;
import com.dacn.VoucherSystem.repository.UserRepository;
import com.dacn.VoucherSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        if(users.isEmpty())
            throw new ResourceNotFoundException("No users found in the system");

        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }
}
