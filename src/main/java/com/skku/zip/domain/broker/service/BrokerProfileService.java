package com.skku.zip.domain.broker.service;

import com.skku.zip.common.exception.BadRequestException;
import com.skku.zip.common.exception.NotFoundException;
import com.skku.zip.domain.broker.dto.BrokerAdditionalInfoRequest;
import com.skku.zip.domain.broker.dto.BrokerProfileData;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.entity.BrokerOffice;
import com.skku.zip.domain.broker.repository.BrokerOfficeRepository;
import com.skku.zip.domain.broker.repository.BrokerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BrokerProfileService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final BrokerRepository brokerRepository;
    private final BrokerOfficeRepository brokerOfficeRepository;

    @Transactional(readOnly = true)
    public BrokerProfileData toProfile(Broker broker) {
        return toData(findBroker(broker));
    }

    @Transactional
    public BrokerProfileData updateAdditionalInfo(
            Broker broker,
            BrokerAdditionalInfoRequest request,
            MultipartFile verificationDocument
    ) {
        Broker managedBroker = findBroker(broker);
        byte[] documentBytes = verificationDocumentBytes(verificationDocument);
        managedBroker.updateAdditionalInfo(
                findOffice(request.officeId()),
                request.registrationNo(),
                request.phoneNumber(),
                verificationDocument.getOriginalFilename(),
                verificationDocument.getContentType(),
                documentBytes
        );
        return toData(managedBroker);
    }

    private BrokerProfileData toData(Broker broker) {
        return new BrokerProfileData(
                broker.getId(),
                broker.getEmail(),
                broker.getName(),
                broker.getAccountType().name(),
                broker.getOfficeId(),
                broker.getOfficeName(),
                broker.getRegistrationNo(),
                broker.getOfficePhone(),
                broker.getOfficeAddress(),
                broker.getPhoneNumber(),
                broker.hasVerificationDocument(),
                broker.getVerificationDocumentFileName(),
                broker.isVerified(),
                broker.isProfileComplete()
        );
    }

    private Broker findBroker(Broker broker) {
        if (broker == null || broker.getId() == null) {
            throw new NotFoundException("Broker not found.");
        }
        return brokerRepository.findById(broker.getId())
                .orElseThrow(() -> new NotFoundException("Broker not found."));
    }

    private BrokerOffice findOffice(Long officeId) {
        if (officeId == null) {
            return null;
        }
        return brokerOfficeRepository.findById(officeId)
                .orElseThrow(() -> new NotFoundException("Broker office not found."));
    }

    private byte[] verificationDocumentBytes(MultipartFile verificationDocument) {
        if (verificationDocument == null || verificationDocument.isEmpty()) {
            throw new BadRequestException("Verification document is required.");
        }
        if (!isSupportedDocumentType(verificationDocument.getContentType())) {
            throw new BadRequestException("Verification document must be an image or PDF.");
        }

        try {
            return verificationDocument.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Verification document could not be read.");
        }
    }

    private boolean isSupportedDocumentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalizedContentType = contentType.trim().toLowerCase(Locale.ROOT);
        return normalizedContentType.startsWith("image/") || PDF_CONTENT_TYPE.equals(normalizedContentType);
    }
}
