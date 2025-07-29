package com.finsightx.finsightx_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsItemRequest {

    private String title;

    private String subTitle1;

    private String dataContents;

}
