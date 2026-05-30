package com.rooming.domain.broker.dto;

import java.util.List;

public record BrokerOfficeListData(
        List<BrokerOfficeData> offices
) {
}