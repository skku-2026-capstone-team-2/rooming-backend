package com.rooming.domain.broker.service;

import com.rooming.domain.broker.dto.BrokerOfficeCreateRequest;
import com.rooming.domain.broker.dto.BrokerOfficeData;
import com.rooming.domain.broker.dto.BrokerOfficeListData;
import com.rooming.domain.broker.entity.BrokerOffice;
import com.rooming.domain.broker.repository.BrokerOfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrokerOfficeService {

    private final BrokerOfficeRepository brokerOfficeRepository;

    @Transactional(readOnly = true)
    public BrokerOfficeListData getOffices() {
        return new BrokerOfficeListData(
                brokerOfficeRepository.findAll(Sort.by("officeName", "id")).stream()
                        .map(this::toData)
                        .toList()
        );
    }

    @Transactional
    public BrokerOfficeData createOffice(BrokerOfficeCreateRequest request) {
        return toData(brokerOfficeRepository.save(new BrokerOffice(
                request.officeName(),
                request.officePhone(),
                request.officeAddress()
        )));
    }

    private BrokerOfficeData toData(BrokerOffice office) {
        return new BrokerOfficeData(
                office.getId(),
                office.getOfficeName(),
                office.getOfficePhone(),
                office.getOfficeAddress()
        );
    }
}