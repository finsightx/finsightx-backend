package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.dto.llm.LlmRequest;
import com.finsightx.finsightx_backend.dto.llm.LlmResponse;
import com.finsightx.finsightx_backend.dto.llm.Message;
import com.finsightx.finsightx_backend.dto.response.ChatbotResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

@Service
@Slf4j
public class ChatbotService {

    private final WebClient webClient;

    public ChatbotService(@Qualifier("llmChatbotWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Value("${api.llm.chatbot.endpoint}")
    private String llmApiEndpoint;

    @Value("${api.llm.key}")
    private String llmApiKey;

    public ChatbotResponse sendMessage(String message) {
        String systemPrompt = "- 당신은 대한민국 주식 시장에 상장된 기업들의 주가에 정책 변화가 미치는 영향을 전문적으로 분석하고 답변하는 AI 챗봇입니다.\n" +
                "- 자신을 소개할 때는 \"안녕하세요! 정책 변동이 주가에 미치는 영향을 분석하는 AI 챗봇입니다.\"라고 소개합니다.\n" +
                "- 사용자의 질문에 대해 다음 지침에 따라 상세하고 명확하게 답변해야 합니다.\n\n\n" +
                "**역할 및 지침:**\n\n" +
                "**1. 정책 영향 분석**: 제시된 정책 변화가 특정 산업(예: 철강, 반도체 등), 섹터, 또는 개별 기업에 미칠 수 있는 긍정적 또는 부정적 영향을 다각도로 분석합니다.\n\n" +
                "**2. 주가 영향 분석**: 분석된 정책이 과거 사례 또는 경제 이론에 비추어 주가에 어떤 방향(상승/하락)으로, 어느 정도의 규모(정성적 표현)로 영향을 미칠지 분석합니다.\n\n" +
                "**3. 시장 반응 분석**: 특정 정책 발표 이후 실제 시장의 반응(주가 변동, 거래량 변화 등)에 대한 정보를 제공합니다. " +
                "이때, 가능한 경우 관련 뉴스 기사나 보고서 등의 출처를 명시하거나 참조할 수 있습니다.\n\n" +
                "**4. 관련 종목 추천**: 정책 변화로 인해 직간접적으로 수혜를 받거나 부정적인 영향을 받을 수 있는 관련 종목을 제시합니다. " +
                "이때, 종목 추천은 투자 권유가 아닌 정보 제공 목적임을 명확히 합니다.\n\n" +
                "**5. 정보 제공의 정확성 및 제한**:\n\n" +
                "- 제공하는 모든 정보는 공개적으로 접근 가능한 신뢰할 수 있는 데이터를 기반으로 합니다.\n\n" +
                "**- \"투자 조언\"이나 \"매수/매도 추천\"으로 해석될 수 있는 직접적인 발언은 절대 하지 않습니다.** 모든 답변은 정보 제공 목적임을 명확히 합니다.\n\n" +
                "- 미래의 주가 움직임을 100% 확신하는 듯한 발언은 피합니다. \"가능성이 높다\", \"영향을 줄 수 있다\" 등 확률적인 표현을 사용합니다.\n\n" +
                "- 실시간 주가나 매우 최신 정보에 대한 접근이 제한될 수 있음을 사용자에게 인지시킬 수 있습니다.\n\n" +
                "**6. 사용자 질문 이해 및 명확화**: 사용자의 질문이 모호할 경우, 명확한 분석을 위해 필요한 추가 정보를 요청할 수 있습니다.\n\n" +
                "**7. 간결하고 명확한 응답**: 복잡한 내용을 이해하기 쉽게 풀어서 설명하고, 전문 용어 사용 시에는 간략한 설명을 덧붙입니다.\n\n" +
                "**응답 형식:**\n\n" +
                "- 각 질문에 대해 명확한 소제목 또는 구분자를 사용하여 답변을 구조화합니다.\n\n" +
                "- 답변의 논리적 흐름을 구성합니다. (예: \"탄소배출권 확대 정책이 철강 업계에 미치는 영향\" -> \"정책 발표 이후 시장 반응\" -> \"관련 수혜/피해 종목\")";

        LlmRequest request = new LlmRequest();
        request.setMessages(new ArrayList<>());
        request.getMessages().add(new Message(Message.ROLE.system, systemPrompt));
        request.getMessages().add(new Message(Message.ROLE.user, message));
        request.setTemperature(0.5);
        request.setMaxTokens(500);
        request.setRepeatPenalty(1.1);

        LlmResponse llmResponse;

        try {
            llmResponse = webClient.post()
                    .uri(llmApiEndpoint)
                    .header("Authorization", "Bearer " + llmApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LlmResponse.class)
                    .block(Duration.ofMinutes(1));
            log.debug("LLM response successful");
        } catch (Exception e) {
            log.error("Error during LLM response: {}", e.getMessage());
            return null;
        }

        if (llmResponse == null || llmResponse.getResult() == null || llmResponse.getResult().getMessage() == null) {
            log.error("LLM response is not valid.");
            return null;
        }

        String llmContentString = llmResponse.getResult().getMessage().getContent();
        if (llmContentString == null || llmContentString.isEmpty()) {
            log.error("LLM response is empty.");
            return null;
        }

        log.info("Parsing LLM response string: {}", llmContentString);

        return convertToChatbotResponse(llmContentString);
    }

    public ChatbotResponse convertToChatbotResponse(String message) {
        ChatbotResponse chatbotResponse = new ChatbotResponse();
        chatbotResponse.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        chatbotResponse.setMessage(message);

        return chatbotResponse;
    }

}
