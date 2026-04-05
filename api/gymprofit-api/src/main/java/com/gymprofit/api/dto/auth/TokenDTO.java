package com.gymprofit.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO implements Serializable {
    private String token;
    private String username;
    private List<String> roles;
}
