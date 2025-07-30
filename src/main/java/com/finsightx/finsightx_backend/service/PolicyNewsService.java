package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.*;
import com.finsightx.finsightx_backend.dto.policyNewsApi.PolicyNewsApiResponse;
import com.finsightx.finsightx_backend.dto.policyNewsApi.PolicyNewsItem;
import com.finsightx.finsightx_backend.dto.request.NewsItemRequest;
import com.finsightx.finsightx_backend.dto.response.PolicyInfoResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyNewsService {

    private final RestTemplate restTemplate;

    @Value("${api.policy-news.service-key}")
    private String serviceKey;

    @Value("${api.policy-news.endpoint}")
    private String apiEndpoint;

    private final DateTimeFormatter apiDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter apiDateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    private final LlmAnalysisService llmAnalysisService;
    private final PolicyInfoService policyInfoService;
    private final PolicySignalService policySignalService;
    private final UserService userService;
    private final StockService stockService;
    private final DailyReportService dailyReportService;

    private OffsetDateTime lastProcessedNewsTime = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(30);

    private Map<String, Stock> allStocksMap;
    private Map<String, String> stockCodeToIndustryCodeMap;
    private Map<String, String> stockCodeToNameMap;
    private Map<String, String> industryCodeToNameMap;

    @jakarta.annotation.PostConstruct
    public void initStockMaps() {
        log.info("Initializing all stock data maps...");
        List<Stock> allStocks = stockService.findAll();
        allStocksMap = allStocks.stream()
                .collect(Collectors.toMap(Stock::getStockCode, Function.identity()));
        stockCodeToIndustryCodeMap = allStocks.stream()
                .collect(Collectors.toMap(Stock::getStockCode, Stock::getIndustryCode));
        stockCodeToNameMap = allStocks.stream()
                .collect(Collectors.toMap(Stock::getStockCode, Stock::getStockName));
        industryCodeToNameMap = allStocks.stream()
                .collect(Collectors.toMap(Stock::getIndustryCode, Stock::getIndustryName, (existing, replacement) -> existing));
        log.info("Stock data maps initialized with {} stocks.", allStocks.size());
    }

    private PolicyNewsApiResponse fetchPolicyNewsFromApi(LocalDate startDate, LocalDate endDate) {
        String formattedStartDate = startDate.format(apiDateFormat);
        String formattedEndDate = endDate.format(apiDateFormat);

        String urlString = apiEndpoint + "?serviceKey=" + serviceKey + "&startDate=" + formattedStartDate + "&endDate=" + formattedEndDate;
        final URI url = URI.create(urlString);

        String xmlResponse = null;
        try {
            xmlResponse = restTemplate.getForObject(url, String.class);
            return parseXmlResponse(xmlResponse);
        } catch (Exception e) {
            log.error("Policy news API call or XML parsing error. URL: {}, Response: {}", url, xmlResponse, e);
            PolicyNewsApiResponse errorResponse = new PolicyNewsApiResponse();
            errorResponse.setResultCode("99");
            errorResponse.setResultMsg("API call or XML parsing error: " + e.getMessage());
            errorResponse.setNewsItems(Collections.emptyList());
            return errorResponse;
        }
    }

    private PolicyNewsApiResponse parseXmlResponse(String xmlResponse) {
        PolicyNewsApiResponse policyNewsApiResponse = new PolicyNewsApiResponse();
        List<PolicyNewsItem> newsItems = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));
            doc.getDocumentElement().normalize();

            NodeList headerList = doc.getElementsByTagName("header");
            if (headerList.getLength() > 0) {
                Element headerElement = (Element) headerList.item(0);
                String resultCode = getTagValue("resultCode", headerElement);
                String resultMsg = getTagValue("resultMsg", headerElement);
                policyNewsApiResponse.setResultCode(resultCode);
                policyNewsApiResponse.setResultMsg(resultMsg);

                if (!"0".equals(resultCode) && !"NORMAL_SERVICE".equals(resultMsg)) {
                    log.warn("Policy news API Error: {} - {}", resultCode, resultMsg);
                    return policyNewsApiResponse;
                }
            } else {
                log.warn("Missing header tag in API response.");
                policyNewsApiResponse.setResultCode("98");
                policyNewsApiResponse.setResultMsg("Missing header tag in API response.");
                return policyNewsApiResponse;
            }

            NodeList newsItemList = doc.getElementsByTagName("NewsItem");
            for (int i = 0; i < newsItemList.getLength(); i++) {
                Element newsItemElement = (Element) newsItemList.item(i);
                PolicyNewsItem item = new PolicyNewsItem();
                item.setNewsItemId(getTagValue("NewsItemId", newsItemElement));
                item.setContentsStatus(getTagValue("ContentsStatus", newsItemElement));
                item.setModifyId(getTagValue("ModifyId", newsItemElement));
                item.setApproveDate(parseDateString(getTagValue("ApproveDate", newsItemElement)));
                item.setApproverName(getTagValue("ApproverName", newsItemElement));
                item.setEmbargoDate(getTagValue("EmbargoDate", newsItemElement));
                item.setGroupingCode(getTagValue("GroupingCode", newsItemElement));
                item.setTitle(getTagValue("Title", newsItemElement));
                item.setSubTitle1(getTagValue("SubTitle1", newsItemElement));
                item.setSubTitle2(getTagValue("SubTitle2", newsItemElement));
                item.setSubTitle3(getTagValue("SubTitle3", newsItemElement));
                item.setContentsType(getTagValue("ContentsType", newsItemElement));
                item.setDataContents(getTagValue("DataContents", newsItemElement));
                item.setMinisterCode(getTagValue("MinisterCode", newsItemElement));
                item.setOriginalUrl(getTagValue("OriginalUrl", newsItemElement));
                item.setThumbnailUrl(getTagValue("ThumbnailUrl", newsItemElement));
                item.setOriginalImgUrl(getTagValue("OriginalimgUrl", newsItemElement));
                newsItems.add(item);
            }
        } catch (Exception e) {
            log.error("XML parsing error. Response: {}", xmlResponse, e);
            policyNewsApiResponse.setResultCode("99");
            policyNewsApiResponse.setResultMsg("XML parsing error: " + e.getMessage());
            policyNewsApiResponse.setNewsItems(Collections.emptyList());
        }
        policyNewsApiResponse.setNewsItems(newsItems);
        return policyNewsApiResponse;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nl = element.getElementsByTagName(tag);
        if (nl != null && nl.getLength() > 0) {
            NodeList child = nl.item(0).getChildNodes();
            if (child != null && child.getLength() > 0) {
                return child.item(0).getNodeValue();
            }
        }
        return null;
    }

    private OffsetDateTime parseDateString(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateString, apiDateTimeFormatter);
            return localDateTime.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        } catch (Exception e) {
            log.warn("Date string parsing error: '{}'", dateString, e);
            return null;
        }
    }

    private String parseOriginalUrl(String originalUrl) {
        String cleanedUrl = originalUrl;

        int startIndex = cleanedUrl.indexOf("[CDATA[");
        int endIndex = cleanedUrl.indexOf("]]>");

        if (startIndex != -1 && endIndex != -1) {
            cleanedUrl = cleanedUrl.substring(startIndex + "[CDATA[".length(), endIndex);
            cleanedUrl = cleanedUrl.trim();
        }

        int paramIndex = cleanedUrl.indexOf("&call_from=");
        if (paramIndex != -1) {
            cleanedUrl = cleanedUrl.substring(0, paramIndex);
        }

        return cleanedUrl;
    }

    @Transactional
    public void processPolicyNews() {
        log.info("Start processing policy news. Last processing time: {}", lastProcessedNewsTime);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate startDate = lastProcessedNewsTime.toLocalDate();
        LocalDate endDate = now.toLocalDate();

        PolicyNewsApiResponse apiResponse = fetchPolicyNewsFromApi(startDate, endDate);

        if (!"0".equals(apiResponse.getResultCode())) {
            log.error("Policy news API call or XML parsing error: {} - {}", apiResponse.getResultCode(), apiResponse.getResultMsg());
            return;
        }
        if (apiResponse.getNewsItems() == null || apiResponse.getNewsItems().isEmpty()) {
            log.info("Missing NewsItem in API");
            lastProcessedNewsTime = now;
            return;
        }

        log.info("Collected {} NewsItems from API.", apiResponse.getNewsItems().size());

        List<PolicyNewsItem> newNewsItems = apiResponse.getNewsItems().stream()
                .filter(newsItem -> newsItem.getApproveDate() != null
                        && newsItem.getApproveDate().isAfter(lastProcessedNewsTime)
                        && newsItem.getGroupingCode().contains("policy"))
                .toList();

        if (newNewsItems.isEmpty()) {
            log.info("No new approved policy news or news has already been processed.");
            lastProcessedNewsTime = now;
            return;
        }

        log.info("Start processing {} newly approved news items.", newNewsItems.size());

        Map<String, String> currentStockNameToCodeMap = allStocksMap.values().stream()
                .collect(Collectors.toMap(Stock::getStockName, Stock::getStockCode, (existing, replacement) -> existing));


        for (PolicyNewsItem newsItem : newNewsItems) {
            PolicyInfo policyInfo = llmAnalysisService.analyzePolicyNewsWithLlm(newsItem, currentStockNameToCodeMap);

            if (policyInfo != null) {
                log.info("LLM determined as policy change news and PolicyInfo processing complete: {}", policyInfo.getPolicyName());

                policyInfo.setOriginalUrl(parseOriginalUrl(newsItem.getOriginalUrl()));
                try {
                    policyInfo = policyInfoService.savePolicyInfo(policyInfo);
                    log.info("PolicyInfo saved: ID {}", policyInfo.getPolicyId());
                } catch (Exception e) {
                    log.error("Failed to save PolicyInfo: {}", e.getMessage());
                }

                processPolicySignalsForUsers(policyInfo);
            } else {
                log.info("LLM determined it's general news or unsuitable for PolicyInfo processing. News Title: {}", newsItem.getTitle());
            }
        }

        lastProcessedNewsTime = now;
        log.info("Policy news processing complete. Updating last processed time for next scheduling: {}", lastProcessedNewsTime);
    }

    private void processPolicySignalsForUsers(PolicyInfo policyInfo) {
        List<User> allUsers = userService.findAll();
        log.info("Start processing policy signal for all {} users.", allUsers.size());

        final Set<String> policyPositiveIndustryCodes = Optional.ofNullable(policyInfo.getPositiveIndustries()).orElse(Collections.emptyList()).stream().collect(Collectors.toSet());
        final Set<String> policyNegativeIndustryCodes = Optional.ofNullable(policyInfo.getNegativeIndustries()).orElse(Collections.emptyList()).stream().collect(Collectors.toSet());
        final Set<String> policyPositiveStockCodes = Optional.ofNullable(policyInfo.getPositiveStocks()).orElse(Collections.emptyList()).stream().collect(Collectors.toSet());
        final Set<String> policyNegativeStockCodes = Optional.ofNullable(policyInfo.getNegativeStocks()).orElse(Collections.emptyList()).stream().collect(Collectors.toSet());

//        Set<String> allRelatedStockCodes = Stream.concat(
//                        policyPositiveStockCodes.stream(),
//                        policyNegativeStockCodes.stream())
//                .collect(Collectors.toSet());
//        Map<String, String> stockCodeToNameMap = Collections.emptyMap();
//        if (!allRelatedStockCodes.isEmpty()) {
//            stockCodeToNameMap = stockService.getStocksByStockCodeIn(new ArrayList<>(allRelatedStockCodes)).stream()
//                    .collect(Collectors.toMap(Stock::getStockCode, Stock::getStockName));
//        }

        for (User user : allUsers) {
            List<PortfolioItem> userPortfolio = user.getPortfolio();
            if (userPortfolio == null || userPortfolio.isEmpty()) {
                continue;
            }

            // 사용자 보유 종목 코드들
            final Set<String> userPortfolioStockCodes = userPortfolio.stream()
                    .map(PortfolioItem::getStockCode)
                    .collect(Collectors.toSet());


            // PolicySignal 메시지 및 저장될 종목 이름을 모으는 Set (중복 제거, 순서 유지)
            Set<String> userImpactStockNamesForSignal = new LinkedHashSet<>();

            Set<String> positiveImpactStockCodes = userPortfolioStockCodes.stream()
                    .filter(policyPositiveStockCodes::contains)
                    .collect(Collectors.toSet());
            Set<String> negativeImpactStockCodes = userPortfolioStockCodes.stream()
                    .filter(policyNegativeStockCodes::contains)
                    .collect(Collectors.toSet());

            for (String userStockCode : userPortfolioStockCodes) {
                String industryCode = stockCodeToIndustryCodeMap.get(userStockCode);
                if (industryCode != null) {
                    if (policyPositiveIndustryCodes.contains(industryCode)) {
                        positiveImpactStockCodes.add(userStockCode);
                    }
                    if (policyNegativeIndustryCodes.contains(industryCode)) {
                        negativeImpactStockCodes.add(userStockCode);
                    }
                }
            }
            // 모든 영향 종목의 이름을 finalUserImpactStockNames 에 추가
            // (PolicySignal에 저장될 최종 종목 이름 리스트)
            Stream.concat(positiveImpactStockCodes.stream(), negativeImpactStockCodes.stream())
                    .distinct() // 직접 영향과 산업 영향에서 중복 제거
                    .map(stockCodeToNameMap::get)
                    .filter(Objects::nonNull)
                    .forEach(userImpactStockNamesForSignal::add);

            List<String> finalUserImpactStockNames = new ArrayList<>(userImpactStockNamesForSignal);


//            // 1. 정책에 직접 언급된 종목 중 사용자 보유 종목
//            for (String userStockCode : userPortfolioStockCodes) {
//                if (policyPositiveStockCodes.contains(userStockCode) || policyNegativeStockCodes.contains(userStockCode)) {
//                    Optional.ofNullable(allStocksMap.get(userStockCode))
//                            .map(Stock::getStockName)
//                            .ifPresent(userImpactStockNamesForSignal::add);
//                }
//
//                String industryCode = stockCodeToIndustryCodeMap.get(userStockCode); // 미리 만들어둔 맵 활용
//                if (industryCode != null) {
//                    if (policyPositiveIndustryCodes.contains(industryCode) || policyNegativeIndustryCodes.contains(industryCode)) {
//                        Optional.ofNullable(allStocksMap.get(userStockCode))
//                                .map(Stock::getStockName)
//                                .ifPresent(userImpactStockNamesForSignal::add);
//                    }
//                }
//            }

            // 최종적으로 PolicySignal에 저장할 종목 이름 리스트
//            List<String> finalUserImpactStockNames = new ArrayList<>(userImpactStockNamesForSignal);


            if (!finalUserImpactStockNames.isEmpty()) {
                // 메시지 생성 시에는 여전히 종목 코드 리스트를 기반으로 createPolicySignalMessage 호출
                // (이 메서드는 내부에서 종목 코드를 이름으로 변환)
                String message = createPolicySignalMessage(
                        new ArrayList<>(positiveImpactStockCodes),
                        new ArrayList<>(negativeImpactStockCodes),
                        policyInfo.getStage() // 이제 stockCodeToNameMap은 전역 맵을 사용
                );

                PolicySignal createdSignal = policySignalService.createPolicySignal(
                        user.getUserId(),
                        message,
                        policyInfo.getPolicyId(),
                        finalUserImpactStockNames, // 여기에 산업군 관련 종목 이름까지 포함된 리스트 전달
                        policyInfo.getCreatedAt()
                );
                log.info("PolicySignal created for user {}: ID {}, Policy ID {}", user.getUserId(), createdSignal.getPolicySignalId(), createdSignal.getPolicyId());

                String notificationTitle = "새로운 정책 시그널";
                String notificationBody = createNotificationBody(finalUserImpactStockNames); // 이제 이름 리스트만 받음

                log.info("Push notification sent to user {}.", user.getUserId());
            } else {
                log.debug("No policy-related stocks in user {}'s holdings. Not generating a signal.", user.getUserId());
            }
        }
        log.info("Policy signal processing complete for all users.");
    }


    private String createPolicySignalMessage(List<String> positiveStockCodes, List<String> negativeStockCodes, String stage) {
        StringBuilder messageBuilder = new StringBuilder("귀하의 보유 종목 중 ");

        // 종목 코드를 이름으로 변환하는 헬퍼 함수
        Function<List<String>, List<String>> getStockNamesFromCodes = codes ->
                codes.stream()
                        .map(stockCodeToNameMap::get) // 전역 stockCodeToNameMap 사용
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        List<String> positiveStockNames = getStockNamesFromCodes.apply(positiveStockCodes);
        List<String> negativeStockNames = getStockNamesFromCodes.apply(negativeStockCodes);
        System.out.println("positiveStockNames: " + positiveStockNames);
        System.out.println("negativeStockNames: " + negativeStockNames);


        if (!positiveStockNames.isEmpty()) {
            if (positiveStockNames.size() > 3) {
                messageBuilder.append(String.format("%s, %s, %s 외 %d 종목에 긍정적 영향을",
                        positiveStockNames.get(0), positiveStockNames.get(1), positiveStockNames.get(2),
                        positiveStockNames.size() - 3));
            } else {
                messageBuilder.append(String.join(", ", positiveStockNames)).append("에 긍정적 영향을");
            }
        }

        if (!negativeStockNames.isEmpty()) {
            if (!positiveStockNames.isEmpty()) {
                messageBuilder.append(", ");
            }

            if (negativeStockNames.size() > 3) {
                messageBuilder.append(String.format("%s, %s, %s 외 %d 종목에 부정적 영향을",
                        negativeStockNames.get(0), negativeStockNames.get(1), negativeStockNames.get(2),
                        negativeStockNames.size() - 3));
            } else {
                messageBuilder.append(String.join(", ", negativeStockNames)).append("에 부정적 영향을");
            }
        }

        if (positiveStockNames.isEmpty() && negativeStockNames.isEmpty()) {
            log.warn("createPolicySignalMessage called with no positive or negative impact stocks, returning empty message.");
            return "";
        }


        messageBuilder.append(String.format(" 줄 수 있는 정책이 %s 단계에 있습니다.", stage));

        return messageBuilder.toString();
    }

    @Transactional
    public PolicyInfoResponse analysisPolicyNewsItem(NewsItemRequest news) {
        PolicyNewsItem newsItem = new PolicyNewsItem();
        newsItem.setTitle(news.getTitle());
        newsItem.setSubTitle1(news.getSubTitle1());
        newsItem.setDataContents(news.getDataContents());

        Map<String, String> currentStockNameToCodeMap = allStocksMap.values().stream()
                .collect(Collectors.toMap(Stock::getStockName, Stock::getStockCode, (existing, replacement) -> existing));

        PolicyInfo policyInfo = llmAnalysisService.analyzePolicyNewsWithLlm(newsItem, currentStockNameToCodeMap);

        if (policyInfo != null) {
            log.info("LLM determined as policy change news and PolicyInfo processing complete: {}", policyInfo.getPolicyName());

            return policyInfoService.toPolicyInfoResponse(policyInfo);

        } else {
            log.info("LLM determined it's general news or unsuitable for PolicyInfo processing. News Title: {}", newsItem.getTitle());
            return null;
        }

    }

    @Transactional
    public void processPolicyNewsByDate(String dateString) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString, apiDateFormat);
            System.out.println("Converted LocalDate: " + date);

        } catch (DateTimeParseException e) {
            System.err.println("Date Format error: " + e.getMessage());
            return;
        }

        LocalDate startDate = date;
        LocalDate endDate = date;

        PolicyNewsApiResponse apiResponse = fetchPolicyNewsFromApi(startDate, endDate);

        if (!"0".equals(apiResponse.getResultCode())) {
            log.error("Policy news API call or XML parsing error: {} - {}", apiResponse.getResultCode(), apiResponse.getResultMsg());
            return;
        }
        if (apiResponse.getNewsItems() == null || apiResponse.getNewsItems().isEmpty()) {
            log.info("Missing NewsItem in API");
            return;
        }

        log.info("Collected {} NewsItems from API.", apiResponse.getNewsItems().size());

        List<PolicyNewsItem> newNewsItems = apiResponse.getNewsItems().stream()
                .filter(newsItem -> newsItem.getGroupingCode().contains("policy"))
                .toList();

        if (newNewsItems.isEmpty()) {
            log.info("No new approved policy news or news has already been processed.");
            return;
        }

        log.info("Start processing {} newly approved news items.", newNewsItems.size());

        Map<String, String> currentStockNameToCodeMap = allStocksMap.values().stream()
                .collect(Collectors.toMap(Stock::getStockName, Stock::getStockCode, (existing, replacement) -> existing));

        for (PolicyNewsItem newsItem : newNewsItems) {
            PolicyInfo policyInfo = llmAnalysisService.analyzePolicyNewsWithLlm(newsItem, currentStockNameToCodeMap);

            if (policyInfo != null) {
                log.info("LLM determined as policy change news and PolicyInfo processing complete: {}", policyInfo.getPolicyName());

                policyInfo.setCreatedAt(newsItem.getApproveDate());
                policyInfo.setOriginalUrl(parseOriginalUrl(newsItem.getOriginalUrl()));
                try {
                    policyInfo = policyInfoService.savePolicyInfo(policyInfo);
                    log.info("PolicyInfo saved: ID {}", policyInfo.getPolicyId());
                } catch (Exception e) {
                    log.error("Failed to save PolicyInfo: {}", e.getMessage());
                }

                processPolicySignalsForUsers(policyInfo);
            } else {
                log.info("LLM determined it's general news or unsuitable for PolicyInfo processing. News Title: {}", newsItem.getTitle());
            }

            try {
                TimeUnit.SECONDS.sleep(6);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while sleeping", e);
            }
        }

        try {
            dailyReportService.createDailyReport(date);
        } catch (Exception e) {
            log.error("Failed to create daily report for {}.", date, e);
        }

        log.info("Policy news processing for {} complete.", date);
    }

    private String createNotificationBody(List<String> userImpactStockNames) {
        StringBuilder messageBuilder = new StringBuilder("귀하의 보유 종목 중 ");

        if (!userImpactStockNames.isEmpty()) {
            List<String> displayNames = userImpactStockNames.stream().distinct().collect(Collectors.toList()); // 혹시 모를 중복 제거
            if (displayNames.size() > 3) {
                messageBuilder.append(String.format("%s, %s, %s 등에 영향을",
                        displayNames.get(0), displayNames.get(1), displayNames.get(2)));
            } else {
                messageBuilder.append(String.join(", ", displayNames)).append("에 영향을");
            }
        }

        messageBuilder.append(" 줄 수 있는 정책 변화가 있습니다. 자세히 알아보세요!");

        return messageBuilder.toString();
    }

}
