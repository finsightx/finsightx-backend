package com.finsightx.finsightx_backend.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClovaMessage {

    private ROLE role;

    private String content;

    public enum ROLE {
        system, user, assistant
    }
}
