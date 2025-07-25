package com.finsightx.finsightx_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsightx.finsightx_backend.domain.PolicyInfo;
import com.finsightx.finsightx_backend.dto.llm.LlmPolicyAnalysisRequest;
import com.finsightx.finsightx_backend.dto.llm.LlmPolicyAnalysisResponse;
import com.finsightx.finsightx_backend.dto.llm.Message;
import com.finsightx.finsightx_backend.dto.policyNewsApi.PolicyNewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmAnalysisService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    private static final List<String> VALID_POLICY_STAGES = Arrays.asList(
            "기획", "제안", "심의/검토", "확정/공포", "시행"
    );

    @Value("${api.llm.endpoint}")
    private String llmApiEndpoint;

    @Value("${api.llm.key}")
    private String llmApiKey;

    public PolicyInfo analyzePolicyNewsWithLlm(PolicyNewsItem newsItem, Map<String, String> stockNameToCodeMap) {
        String systemPrompt = "- 정책 뉴스를 분석하여 이 뉴스가 '주가에 영향을 미칠 만 한 정책 변화'에 해당하는지 여부를 판단하고, " +
                "정책 변화에 해당한다면 다음의 항목들을 추출하여 답변해주세요.\n" +
                "- 답변 양식에 맞게 JSON 형식으로 답변해주세요. 다른 추가적인 텍스트는 절대 포함하지 마세요.\n" +
                "<isPolicyChange 판단 기준>\n" +
                "1. 단순히 정보 전달이 아닌, 법규, 제도, 지침, 예산 배정 등에 있어 새로운 방향이나 수정이 있는지 명확하게 판단\n" +
                "2. 규모와 직접적인 경제적 파급 효과를 고려할 때 주식 시장 전반 또는 특정 상장 기업의 주가에 유의미한 영향을 미칠 만 한 정책 변화인지 판단\n\n" +
                "<업종 분류>\n" +
                "1010: 에너지\n1510: 소재\n2010: 자본재\n2020: 상업서비스와공급품\n2030: 운송\n2510: 자동차와부품\n2520: 내구소비재와의류\n" +
                "2530: 호텔,레스토랑,레저 등\n2550: 소매(유통)\n2560: 교육서비스\n3010: 식품과기본식료품소매\n3020: 식품,음료,담배\n" +
                "3030: 가정용품과개인용품\n3510: 건강관리장비와서비스\n3520: 제약과생물공학\n4010: 은행\n4020: 증권\n4030: 다각화된금융\n" +
                "4040: 보험\n4050: 부동산\n4510: 소프트웨어와서비스\n4520: 기술하드웨어와장비\n4530: 반도체와반도체장비\n4535: 전자와 전기제품\n" +
                "4540: 디스플레이\n5010: 전기통신서비스\n5020: 미디어와엔터테인먼트\n5510: 유틸리티\n\n" +
                "<분석할 항목>\n" +
                "1. isPolicyChange: 정책 변화에 해당하는지 여부 (true/false 중 하나)\n" +
                "2. policyName: 정책 이름\n" +
                "3. stage: 정책 변화 단계 ((기획 → 제안 → 심의/검토 → 확정/공포 → 시행) 다섯 단계 중 하나)\n" +
                "4. summary: 정책 요약 (해라체 평서문 ‘-(ㄴ/는)다.’/’~했다.’로 서술)\n" +
                "5. content: 정책 변화가 어떤 업종 및 종목에 긍정적/부정적 영향을 줄 수 있는지 구체적인 근거(과거의 비슷한 사례, 그로 인한 주가 변동 등)를 바탕으로 " +
                "분석한 내용, 업종 및 종목 이름 등을 포함하여 서술한 문자열 리스트 (1~3가지 정도, [\"내용1~~\", \"내용2~~\", ...] 형식)\n" +
                "6. positiveIndustries: 정책 변화가 긍정적인 영향을 줄 수 있는 대표·핵심 업종 코드(<업종 분류> 참고) 리스트 ([\"4050\", \"5010\", ...] 형식)\n" +
                "7. negativeIndustries: 정책 변화가 부정적인 영향을 줄 수 있는 대표·핵심 업종 코드(<업종 분류> 참고) 리스트 ([\"2560\", \"4020\", ...] 형식)\n" +
                "8. positiveStocks:정책 변화가 긍정적인 영향을 줄 수 있는 대표·핵심 종목명 리스트 ([\"삼성전자\", \"SK하이닉스\", ...] 형식)\n" +
                "9. negativeStocks:정책 변화가 부정적인 영향을 줄 수 있는 대표·핵심 종목명 리스트 ([\"삼성전자\", \"SK하이닉스\", ...] 형식)\n\n" +
                "<답변 양식 - isPolicyChange가 true인 경우>\n" +
                "{\n" +
                "\t\"isPolicyChange\": true, \n" +
                "\t\"policyName\": \"{policyName}\",\n" +
                "\t\"stage\": \"{stage}\",\n" +
                "\t\"summary\": \"{summary}\",\n" +
                "\t\"content\": {content},\n" +
                "\t\"positiveIndustries\": {positiveIndustries},\n" +
                "\t\"negativeIndustries\": {negativeIndustries},\n" +
                "\t\"positiveStocks\": {positiveStocks},\n" +
                "\t\"negativeStocks\": {negativeStocks}\n" +
                "}\n" +
                "\n" +
                "<답변 양식 - isPolicyChange가 false인 경우>\n" +
                "{\n" +
                "\t\"isPolicyChange\": false\n" +
                "}";

        String userPrompt = "제목: " + newsItem.getTitle() +
                "\n부제목: " + newsItem.getSubTitle1() +
                "\n내용: " + newsItem.getDataContents();

        LlmPolicyAnalysisRequest request = new LlmPolicyAnalysisRequest();
        request.setMessages(new ArrayList<>());
        request.getMessages().add(new Message(Message.ROLE.system, systemPrompt));
        request.getMessages().add(new Message(Message.ROLE.user, userPrompt));
        request.setTemperature(0.8);
        request.setMaxTokens(500);
        request.setRepeatPenalty(1.1);

        LlmPolicyAnalysisResponse llmResponse;

        try {
            llmResponse = webClient.post()
                    .uri(llmApiEndpoint)
                    .header("Authorization", "Bearer " + llmApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LlmPolicyAnalysisResponse.class)
                    .block(Duration.ofMinutes(1));
            log.debug("LLM analysis successful");
        } catch (Exception e) {
            log.error("Error during LLM analysis: {}", e.getMessage());
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
        // TODO
        System.out.println(llmContentString);

        if (llmContentString.trim().startsWith("```json")) {
            llmContentString = llmContentString.substring(llmContentString.indexOf("```json") + 7);
            if (llmContentString.endsWith("```")) {
                llmContentString = llmContentString.substring(0, llmContentString.lastIndexOf("```"));
            }
        }
        llmContentString = llmContentString.trim();

        log.info("Parsing LLM response string: {}", llmContentString);

        try {
            LlmPolicyAnalysisResponse.PolicyInfoFromLlm parsedPolicyInfo = objectMapper.readValue(llmContentString, LlmPolicyAnalysisResponse.PolicyInfoFromLlm.class);

            if (!parsedPolicyInfo.isPolicyChange()) {
                log.info("LLM determined it's general news or unsuitable for PolicyInfo processing.");
                return null;
            }

            String llmStage = parsedPolicyInfo.getStage();
            if (!VALID_POLICY_STAGES.contains(llmStage)) {
                log.warn("Stage of LLM response is not valid.: {}", llmStage);
                return null;
            }

            return convertToPolicyInfo(parsedPolicyInfo, stockNameToCodeMap);

        } catch (JsonProcessingException e) {
            log.error("LLM response JSON parsing error: {}", e.getMessage(), e);
            return null;
        }
    }

    private PolicyInfo convertToPolicyInfo(LlmPolicyAnalysisResponse.PolicyInfoFromLlm parsedInfo, Map<String, String> stockNameToCodeMap) {
        PolicyInfo policyInfo = new PolicyInfo();
        policyInfo.setPolicyName(parsedInfo.getPolicyName());
        policyInfo.setStage(parsedInfo.getStage());
        policyInfo.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        policyInfo.setSummary(parsedInfo.getSummary());
        policyInfo.setContent(parsedInfo.getContent());
        policyInfo.setPositiveIndustries(parsedInfo.getPositiveIndustries());
        policyInfo.setNegativeIndustries(parsedInfo.getNegativeIndustries());

        List<String> positiveStockCodes = parsedInfo.getPositiveStocks().stream()
                .map(name -> stockNameToCodeMap.get(name.trim()))
                .filter(java.util.Objects::nonNull)
                .toList();

        List<String> negativeStockCodes = parsedInfo.getNegativeStocks().stream()
                .map(name -> stockNameToCodeMap.get(name.trim()))
                .filter(java.util.Objects::nonNull)
                .toList();

        policyInfo.setPositiveStocks(positiveStockCodes);
        policyInfo.setNegativeStocks(negativeStockCodes);

        System.out.println("테스트 결과: " + policyInfo.getPolicyName() +
                "\n단계: " + policyInfo.getStage() +
                "\n내용: " + policyInfo.getContent() +
                "\n긍정 업종: " + policyInfo.getPositiveIndustries() +
                "\n긍정 종목: " + policyInfo.getPositiveStocks() +
                "\n부정 업종: " + policyInfo.getNegativeIndustries() +
                "\n부정 종목: " + policyInfo.getNegativeStocks());

        return policyInfo;
    }

}
