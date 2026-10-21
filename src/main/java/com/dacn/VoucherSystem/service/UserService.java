package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
}
