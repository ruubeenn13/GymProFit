package es.pmdm.gymprofit.network.dto;

import java.util.List;

public class TokenDTO {

    private String token;
    private String username;
    private List<String> roles;

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}
