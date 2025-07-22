package com.finsightx.finsightx_backend.dto.policyNewsApi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyNewsItem {

    private String newsItemId;

    private String contentsStatus;

    private String modifyId;

    private Optional<OffsetDateTime> modifyDate;

    private Optional<OffsetDateTime> approveDate;

    private String approverName;

    private String embargoDate;

    private String groupingCode;

    private String title;

    private String subTitle1;

    private String subTitle2;

    private String subTitle3;

    private String contentsType;

    private String dataContents;

    private String ministerCode;

    private String originalUrl;

    private String thumbnailUrl;

    private String originalImgUrl;
}
