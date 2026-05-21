package com.skku.zip.domain.user.service;

import com.skku.zip.domain.broker.repository.BrokerRepository;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.user.entity.User;
import com.skku.zip.domain.seeker.repository.SeekerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final SeekerRepository seekerRepository;
    private final BrokerRepository brokerRepository;

    public User findByAccountTypeAndId(AccountType accountType, Long id) {
        return switch (accountType) {
            case SEEKER -> seekerRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Seeker not found."));
            case BROKER -> brokerRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Broker not found."));
        };
    }
}
