package com.project.game.user.service.usecase;

import com.project.game.user.dto.UserResponse;
import com.project.game.user.entity.User;
import com.project.game.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public interface UserService {

    UserResponse findByUserId(Long userId);

}
