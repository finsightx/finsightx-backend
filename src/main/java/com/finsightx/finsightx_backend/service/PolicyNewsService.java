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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

//    TODO: Test
//    private OffsetDateTime lastProcessedNewsTime = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(30);
    private OffsetDateTime lastProcessedNewsTime = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(1);

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

        List<Stock> allStocks = stockService.findAll();
        Map<String, String> stockNameToCodeMap = allStocks.stream()
                .collect(Collectors.toMap(Stock::getStockName, Stock::getStockCode, (existing, replacement) -> existing));

        for (PolicyNewsItem newsItem : newNewsItems) {
            PolicyInfo policyInfo = llmAnalysisService.analyzePolicyNewsWithLlm(newsItem, stockNameToCodeMap);

            if (policyInfo != null) {
                log.info("LLM determined as policy change news and PolicyInfo processing complete: {}", policyInfo.getPolicyName());

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

        Set<String> allRelatedStockCodes = Stream.concat(
                        policyPositiveStockCodes.stream(),
                        policyNegativeStockCodes.stream())
                .collect(Collectors.toSet());
        Map<String, String> stockCodeToNameMap = Collections.emptyMap();
        if (!allRelatedStockCodes.isEmpty()) {
            stockCodeToNameMap = stockService.getStocksByStockCodeIn(new ArrayList<>(allRelatedStockCodes)).stream()
                    .collect(Collectors.toMap(Stock::getStockCode, Stock::getStockName));
        }

        for (User user : allUsers) {
            List<PortfolioItem> userPortfolio = user.getPortfolio();
            if (userPortfolio == null || userPortfolio.isEmpty()) {
                continue;
            }

            final Set<String> userStockCodes = userPortfolio.stream()
                    .map(PortfolioItem::getStockCode)
                    .collect(Collectors.toSet());

            Map<String, String> userStockCodeToIndustryCodeMap = stockService.getStocksByStockCodeIn(new ArrayList<>(userStockCodes)).stream()
                    .collect(Collectors.toMap(Stock::getStockCode, Stock::getIndustryCode));

            List<String> userPositiveImpactStockCodes = new ArrayList<>(userStockCodes.stream()
                    .filter(policyPositiveStockCodes::contains)
                    .toList());

            List<String> userNegativeImpactStockCodes = new ArrayList<>(userStockCodes.stream()
                    .filter(policyNegativeStockCodes::contains)
                    .toList());

            userPositiveImpactStockCodes.addAll(userStockCodes.stream()
                    .filter(stockCode -> policyPositiveIndustryCodes.contains(userStockCodeToIndustryCodeMap.get(stockCode)))
                    .toList());

            userNegativeImpactStockCodes.addAll(userStockCodes.stream()
                    .filter(stockCode -> policyNegativeIndustryCodes.contains(userStockCodeToIndustryCodeMap.get(stockCode)))
                    .toList());

            userPositiveImpactStockCodes = userPositiveImpactStockCodes.stream().distinct().collect(Collectors.toList());
            userNegativeImpactStockCodes = userNegativeImpactStockCodes.stream().distinct().collect(Collectors.toList());

            Set<String> userImpactStockCodesSet = new HashSet<>();
            userImpactStockCodesSet.addAll(userPositiveImpactStockCodes);
            userImpactStockCodesSet.addAll(userNegativeImpactStockCodes);
            List<String> userImpactStockCodes = new ArrayList<>(userImpactStockCodesSet);


            if (!userPositiveImpactStockCodes.isEmpty() || !userNegativeImpactStockCodes.isEmpty()) {
                String message = createPolicySignalMessage(
                        userPositiveImpactStockCodes,
                        userNegativeImpactStockCodes,
                        stockCodeToNameMap,
                        policyInfo.getStage()
                );

                PolicySignal createdSignal = policySignalService.createPolicySignal(
                        user.getUserId(),
                        message,
                        policyInfo.getPolicyId(),
                        userImpactStockCodes
                );
                log.info("PolicySignal created for user {}: ID {}, Policy ID {}", user.getUserId(), createdSignal.getPolicySignalId(), createdSignal.getPolicyId());

                String notificationTitle = "새로운 정책 시그널";
                String notificationBody = createNotificationBody(userImpactStockCodes, stockCodeToNameMap);

                log.info("Push notification sent to user {}.", user.getUserId());
            } else {
                 log.debug("No policy-related stocks in user {}'s holdings. Not generating a signal.", user.getUserId());
            }
        }
        log.info("Policy signal processing complete for all users.");
    }

    private String createPolicySignalMessage(List<String> positiveStocks, List<String> negativeStocks,
                                             Map<String, String> stockCodeToNameMap, String stage) {
        StringBuilder messageBuilder = new StringBuilder("귀하의 보유 종목 중 ");

        if (!positiveStocks.isEmpty()) {
            List<String> positiveStockNames = positiveStocks.stream()
                    .map(stockCodeToNameMap::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            if (positiveStockNames.size() > 3) {
                messageBuilder.append(String.format("%s, %s, %s 외 %d 종목에 긍정적 영향을",
                        positiveStockNames.get(0), positiveStockNames.get(1), positiveStockNames.get(2),
                        positiveStockNames.size() - 3));
            } else {
                messageBuilder.append(String.join(", ", positiveStockNames)).append("에 긍정적 영향을");
            }
        }

        if (!negativeStocks.isEmpty()) {
            List<String> negativeStockNames = negativeStocks.stream()
                    .map(stockCodeToNameMap::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            if (!positiveStocks.isEmpty()) {
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

        messageBuilder.append(String.format(" 줄 수 있는 정책이 %s 단계에 있습니다.", stage));

        return messageBuilder.toString();
    }

    @Transactional
    public PolicyInfoResponse analysisPolicyNewsItem(NewsItemRequest news) {
        PolicyNewsItem newsItem = new PolicyNewsItem();
        newsItem.setTitle(news.getTitle());
        newsItem.setSubTitle1(news.getSubTitle1());
        newsItem.setDataContents(news.getDataContents());

        List<Stock> allStocks = stockService.findAll();
        Map<String, String> stockNameToCodeMap = allStocks.stream()
                .collect(Collectors.toMap(Stock::getStockName, Stock::getStockCode, (existing, replacement) -> existing));

        PolicyInfo policyInfo = llmAnalysisService.analyzePolicyNewsWithLlm(newsItem, stockNameToCodeMap);

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

        List<Stock> allStocks = stockService.findAll();
        Map<String, String> stockNameToCodeMap = allStocks.stream()
                .collect(Collectors.toMap(Stock::getStockName, Stock::getStockCode, (existing, replacement) -> existing));

        for (PolicyNewsItem newsItem : newNewsItems) {
            PolicyInfo policyInfo = llmAnalysisService.analyzePolicyNewsWithLlm(newsItem, stockNameToCodeMap);

            if (policyInfo != null) {
                log.info("LLM determined as policy change news and PolicyInfo processing complete: {}", policyInfo.getPolicyName());

                policyInfo.setCreatedAt(newsItem.getApproveDate());
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

        log.info("Policy news processing for {} complete.", date);
    }

    private String createNotificationBody(List<String> userImpactStockCodes, Map<String, String> stockCodeToNameMap) {
        StringBuilder messageBuilder = new StringBuilder("귀하의 보유 종목 중 ");

        if (!userImpactStockCodes.isEmpty()) {
            List<String> positiveStockNames = userImpactStockCodes.stream()
                    .map(stockCodeToNameMap::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            if (positiveStockNames.size() > 3) {
                messageBuilder.append(String.format("%s, %s, %s 등에 영향을",
                        positiveStockNames.get(0), positiveStockNames.get(1), positiveStockNames.get(2)));
            } else {
                messageBuilder.append(String.join(", ", positiveStockNames)).append("에 영향을");
            }
        }

        messageBuilder.append(" 줄 수 있는 정책이 변화가 있습니다. 자세히 알아보세요!");

        return messageBuilder.toString();
    }

}
