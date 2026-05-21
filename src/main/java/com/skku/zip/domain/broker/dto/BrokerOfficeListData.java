package com.skku.zip.domain.broker.dto;

import java.util.List;

public record BrokerOfficeListData(
        List<BrokerOfficeData> offices
) {
}
