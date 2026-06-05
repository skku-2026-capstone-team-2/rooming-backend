package com.rooming.domain.locations.service;

import com.rooming.domain.locations.entity.model.TmapPoiSyncState;
import com.rooming.domain.locations.repository.TmapPoiSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TmapPoiSyncQuotaService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final TmapPoiSyncStateRepository syncStateRepository;

    public boolean isQuotaExhaustedToday() {
        return syncState().isQuotaExhaustedOn(LocalDate.now(KOREA_ZONE));
    }

    public void markQuotaExhaustedNow() {
        TmapPoiSyncState state = syncState();
        state.markQuotaExhausted(LocalDate.now(KOREA_ZONE), Instant.now());
        syncStateRepository.save(state);
    }

    private TmapPoiSyncState syncState() {
        return syncStateRepository.findById(TmapPoiSyncState.GLOBAL_ID)
                .orElseGet(() -> new TmapPoiSyncState(TmapPoiSyncState.GLOBAL_ID));
    }
}
