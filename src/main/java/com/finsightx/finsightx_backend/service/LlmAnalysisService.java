package com.finsightx.finsightx_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsightx.finsightx_backend.domain.PolicyInfo;
import com.finsightx.finsightx_backend.dto.llm.LlmRequest;
import com.finsightx.finsightx_backend.dto.llm.LlmResponse;
import com.finsightx.finsightx_backend.dto.llm.Message;
import com.finsightx.finsightx_backend.dto.policyNewsApi.PolicyNewsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class LlmAnalysisService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    public LlmAnalysisService(@Qualifier("llmAnalysisWebClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    private static final List<String> VALID_POLICY_STAGES = Arrays.asList(
            "기획", "제안", "심의/검토", "확정/공포", "시행"
    );

    @Value("${api.llm.analysis.endpoint}")
    private String llmApiEndpoint;

    @Value("${api.llm.key}")
    private String llmApiKey;

    public PolicyInfo analyzePolicyNewsWithLlm(PolicyNewsItem newsItem, Map<String, String> stockNameToCodeMap) {
        String systemPrompt = "- 당신은 정책 뉴스를 분석하여 주식 시장에 미치는 영향을 판단하고 예측하는 전문 AI 애널리스트입니다.\n\n" +
                "- 사용자로부터 정책 뉴스 기사를 입력받으면, 다음 지침에 따라 분석을 수행하고 지정된 JSON 형식으로 결과를 출력합니다.\n\n" +
                "### **분석 목표 및 기본 원칙:**\n\n" +
                "1. **정확한 정책 변화 판단**: 입력된 뉴스가 단순 정보 전달이 아닌, **법규, 제도, 지침, 예산 배정 등**에 있어 " +
                "새로운 방향 제시 또는 수정이 포함된 **정책 변화**인지 명확하게 판단합니다.\n" +
                "2. **주가 영향 분석**: 판단된 정책 변화가 **규모와 직접적인 경제적 파급 효과**를 고려할 때, " +
                "**주식 시장 전반 또는 특정 상장 기업의 주가에 유의미한 영향을 미칠 만한 변화**인지 심층적으로 분석합니다.\n" +
                "3. **객관적이고 근거 기반 분석**: 정책 변화의 잠재적 영향을 분석할 때는 **과거의 유사 사례, 관련 산업의 특성, " +
                "거시 경제 지표 등 구체적인 근거**를 바탕으로 객관적인 시각을 유지합니다. 추측이나 주관적인 의견은 배제합니다.\n" +
                "4. **한국 주식 시장 특성 반영**: 한국의 업종 분류 및 기업 정보에 대한 이해를 바탕으로 분석을 수행합니다.\n\n" +
                "### **업종 분류 참고:**\n\n" +
                "다음 업종 코드를 활용하여 정책의 영향을 받는 업종을 정확하게 분류합니다.\n\n" +
                "- 1010: 에너지\n- 1510: 소재\n- 2010: 자본재\n- 2020: 상업서비스와공급품\n- 2030: 운송\n- 2510: 자동차와부품\n" +
                "- 2520: 내구소비재와의류\n- 2530: 호텔,레스토랑,레저 등\n- 2550: 소매(유통)\n- 2560: 교육서비스\n- 3010: 식품과기본식료품소매\n" +
                "- 3020: 식품,음료,담배\n- 3030: 가정용품과개인용품\n- 3510: 건강관리장비와서비스\n- 3520: 제약과생물공학\n- 4010: 은행\n" +
                "- 4020: 증권\n- 4030: 다각화된금융\n- 4040: 보험\n- 4050: 부동산\n- 4510: 소프트웨어와서비스\n- 4520: 기술하드웨어와장비\n" +
                "- 4530: 반도체와반도체장비\n- 4535: 전자와 전기제품\n- 4540: 디스플레이\n- 5010: 전기통신서비스\n- 5020: 미디어와엔터테인먼트\n- 5510: 유틸리티\n\n" +
                "### **분석할 항목 및 출력 형식:**\n\n" +
                "**1. `isPolicyChange` (필수)**\n\n" +
                "- **판단 기준**:\n" +
                "    1. 단순한 정보 전달이 아닌, **법규, 제도, 지침, 예산 배정 등**에 있어 **새로운 방향이나 수정**이 있는지 명확하게 판단합니다.\n" +
                "    2. **규모와 직접적인 경제적 파급 효과**를 고려할 때, 주식 시장 전반 또는 특정 상장 기업의 주가에 **유의미한 영향을 미칠 만한 정책 변화**인지 판단합니다.\n" +
                "- **값**: `true` 또는 `false`\n\n" +
                "**2. `policyName` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**: 정책의 핵심 내용을 담은 간결한 이름 (예: \"반도체 산업 육성 특별법 제정\", \"탄소중립 목표 상향 조정\")\n\n" +
                "**3. `stage` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**: 정책 변화의 현재 단계 (다음 중 하나: `기획`, `제안`, `심의/검토`, `확정/공포`, `시행`)\n\n" +
                "**4. `summary` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**: 정책의 주요 내용을 간결하게 요약합니다. **해라체 평서문**(`(ㄴ/는)다.`, `~했다.`)으로 서술합니다.\n\n" +
                "**5. `content` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**:\n" +
                "    - 정책 변화가 **어떤 업종 및 종목에 긍정적/부정적 영향을 줄 수 있는지**를 분석합니다.\n" +
                "    - **구체적인 근거**를 포함합니다. (예: 과거의 비슷한 사례, 그로 인한 주가 변동, 관련 시장 규모 변화 예측 등)\n" +
                "    - **해요체**로 서술하며, 내용은 1~3가지 정도로 압축하여 제공합니다.\n" +
                "    - `positiveIndustries`, `negativeIndustries`, `positiveStocks`, `negativeStocks`에 해당하는 업종 및 종목 이름이 `content` 내에 포함되어 설명되면 좋습니다.\n" +
                "- **형식**: `[\"내용1\", \"내용2\", ...]` (리스트 형태의 문자열)\n\n" +
                "**6. `positiveIndustries` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**: 정책 변화가 긍정적인 영향을 줄 수 있는 **대표·핵심 업종 코드** 리스트. `<업종 분류>`를 참고하여 정확한 코드를 사용합니다. 업종 이름은 포함하지 않습니다.\n" +
                "- **형식**: `[\"0000\", \"1123\", ...]` (리스트 형태의 문자열)\n\n" +
                "**7. `negativeIndustries` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**: 정책 변화가 부정적인 영향을 줄 수 있는 **대표·핵심 업종 코드** 리스트. `<업종 분류>`를 참고하여 정확한 코드를 사용합니다. 업종 이름은 포함하지 않습니다.\n" +
                "- **형식**: `[\"0000\", \"1123\", ...]` (리스트 형태의 문자열)\n\n" +
                "**8. `positiveStocks` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**: 정책 변화가 긍정적인 영향을 줄 수 있는 **대표·핵심 종목 이름** 리스트. (실제 상장된 기업 이름)\n" +
                "- **형식**: `[\"삼성전자\", \"SK하이닉스\", ...]` (리스트 형태의 문자열)\n\n" +
                "**9. `negativeStocks` (isPolicyChange가 true일 경우 필수)**\n\n" +
                "- **값**: 정책 변화가 부정적인 영향을 줄 수 있는 **대표·핵심 종목 이름** 리스트. (실제 상장된 기업 이름)\n" +
                "- **형식**: `[\"삼성전자\", \"SK하이닉스\", ...]` (리스트 형태의 문자열)\n\n" +
                "### **JSON 출력 형식**\n\n" +
                "**- `isPolicyChange`가 `true`인 경우:**\n\n" +
                "`{\n" +
                "\t\"isPolicyChange\": true,\n" +
                "\t\"policyName\": \"{정책 이름}\",\n" +
                "\t\"stage\": \"{정책 변화 단계}\",\n" +
                "\t\"summary\": \"{정책 요약}\",\n" +
                "\t\"content\": [\"{정책 변화가 주가에 미치는 영향 분석 내용 1}\", \"{정책 변화가 주가에 미치는 영향 분석 내용 2}\"],\n" +
                "\t\"positiveIndustries\": [\"{긍정 영향 업종 코드 1}\", \"{긍정 영향 업종 코드 2}\"],\n" +
                "\t\"negativeIndustries\": [\"{부정 영향 업종 코드 1}\", \"{부정 영향 업종 코드 2}\"],\n" +
                "\t\"positiveStocks\": [\"{긍정 영향 종목명 1}\", \"{긍정 영향 종목명 2}\"],\n" +
                "\t\"negativeStocks\": [\"{부정 영향 종목명 1}\", \"{부정 영향 종목명 2}\"]\n" +
                "}`\n\n" +
                "**- `isPolicyChange`가 `false`인 경우:**\n\n" +
                "`{\n" +
                "\t\"isPolicyChange\": false\n" +
                "}`\n\n" +
                "### **출력 지침**\n\n" +
                "- 모든 결과는 **JSON 형식**으로 출력하며, 추가적인 설명이나 서론/결론 없이 JSON 객체만 반환합니다.\n" +
                "- JSON 필드명은 정확히 위에서 지정된 이름을 사용합니다.\n" +
                "- 값이 없는 필드는 `null`이 아닌, 빈 리스트 (`[]`) 또는 빈 문자열 (`\"\"`)로 처리합니다. " +
                "단, `isPolicyChange`가 `false`일 때는 `isPolicyChange: false`만 포함합니다.";

        String userPrompt = "제목: " + newsItem.getTitle() +
                "\n부제목: " + newsItem.getSubTitle1() +
                "\n내용: " + newsItem.getDataContents();

        LlmRequest request = new LlmRequest();
        request.setMessages(new ArrayList<>());
        request.getMessages().add(new Message(Message.ROLE.system, systemPrompt));
        request.getMessages().add(new Message(Message.ROLE.user, userPrompt));
        request.setTemperature(0.8);
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
            LlmResponse.PolicyInfoFromLlm parsedPolicyInfo = objectMapper.readValue(llmContentString, LlmResponse.PolicyInfoFromLlm.class);

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

    private PolicyInfo convertToPolicyInfo(LlmResponse.PolicyInfoFromLlm parsedInfo, Map<String, String> stockNameToCodeMap) {
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
