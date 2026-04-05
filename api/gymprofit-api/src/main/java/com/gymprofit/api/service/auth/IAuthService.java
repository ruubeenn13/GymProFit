package com.gymprofit.api.service.auth;

import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.TokenDTO;

public interface IAuthService {

    TokenDTO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);
}
