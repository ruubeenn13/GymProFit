package es.pmdm.gymprofit.network.dto;

import java.util.List;

public class RegisterDTO {

    private String username;
    private String password;
    private String email;
    private List<Integer> roles;

    public RegisterDTO(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public List<Integer> getRoles() {
        return roles;
    }
}
