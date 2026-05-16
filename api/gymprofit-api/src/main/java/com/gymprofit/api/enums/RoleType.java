package com.gymprofit.api.enums;

import lombok.Getter;

@Getter
public enum RoleType {
    ADMIN(1),
    USER(2),
    GUEST(3);

    private final int value;

    RoleType(int value) {
        this.value = value;
    }
}
