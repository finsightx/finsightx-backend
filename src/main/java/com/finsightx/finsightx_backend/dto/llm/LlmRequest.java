package com.finsightx.finsightx_backend.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {

    private ArrayList<Message> messages;

    private double temperature;

    private int maxTokens;

    private double repeatPenalty;

}
