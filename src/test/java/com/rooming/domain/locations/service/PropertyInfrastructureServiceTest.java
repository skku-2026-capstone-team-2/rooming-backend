package com.rooming.domain.locations.service;

import com.rooming.domain.locations.client.TmapClient;
import com.rooming.domain.locations.dto.OdsayRouteCandidate;
import com.rooming.domain.locations.dto.PropertyInfrastructureSyncResult;
import com.rooming.domain.locations.dto.TmapInfrastructureCandidate;
import com.rooming.domain.locations.dto.TmapInfrastructureSearchResult;
import com.rooming.domain.locations.entity.id.PropertyInfrastructureId;
import com.rooming.domain.locations.entity.model.InfraAccessibility;
import com.rooming.domain.locations.entity.model.Infrastructure;
import com.rooming.domain.locations.entity.type.INFRA_CATEGORY;
import com.rooming.domain.locations.entity.value.Minutes;
import com.rooming.domain.locations.entity.value.Path;
import com.rooming.domain.locations.entity.value.RoadAddress;
import com.rooming.domain.locations.entity.value.SubPath;
import com.rooming.domain.locations.repository.InfraAccessibilityRepository;
import com.rooming.domain.locations.repository.InfrastructureRepository;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyInfrastructureServiceTest {

    private final TmapClient tmapClient = mock(TmapClient.class);
    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final InfrastructureRepository infrastructureRepository = mock(InfrastructureRepository.class);
    private final InfraAccessibilityRepository infraAccessibilityRepository =
            mock(InfraAccessibilityRepository.class);
    private final PropertyInfrastructureService propertyInfrastructureService =
            new PropertyInfrastructureService(
                    tmapClient,
                    propertyRepository,
                    infrastructureRepository,
                    infraAccessibilityRepository
            );

    @Test
    void syncMissingPropertyStoresPoisAndCreatesAccessibilities() {
        Property property = property();
        Infrastructure infrastructure = infrastructure(77L);
        when(tmapClient.findInfrastructureCandidatesWithQuotaStatus(37.2910, 126.9710, 1))
                .thenReturn(searchResult(List.of(candidate()), Set.of(INFRA_CATEGORY.CONVENIENT_STORE), false));
        when(infrastructureRepository.saveAndFlush(any(Infrastructure.class))).thenReturn(infrastructure);
        when(infraAccessibilityRepository.existsById(new PropertyInfrastructureId(101L, 77L)))
                .thenReturn(false);
        when(tmapClient.findWalkingRoute(37.2910, 126.9710, 37.2912, 126.9712))
                .thenReturn(Optional.of(routeCandidate()));
        when(infraAccessibilityRepository.saveAndFlush(any(InfraAccessibility.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropertyInfrastructureSyncResult result = propertyInfrastructureService.syncMissingInfrastructure(property);

        assertThat(result.infrastructureCount()).isEqualTo(1);
        assertThat(result.createdAccessibilityCount()).isEqualTo(1);
        assertThat(result.removedAccessibilityCount()).isZero();
        assertThat(result.quotaExceeded()).isFalse();

        ArgumentCaptor<InfraAccessibility> accessibilityCaptor =
                ArgumentCaptor.forClass(InfraAccessibility.class);
        verify(infraAccessibilityRepository).saveAndFlush(accessibilityCaptor.capture());
        assertThat(accessibilityCaptor.getValue().getId().getPropertyId()).isEqualTo(101L);
        assertThat(accessibilityCaptor.getValue().getId().getInfrastructureId()).isEqualTo(77L);
    }

    @Test
    void syncMissingPropertyStillStoresPartialPoisWhenQuotaIsReached() {
        Property property = property();
        Infrastructure infrastructure = infrastructure(77L);
        when(tmapClient.findInfrastructureCandidatesWithQuotaStatus(37.2910, 126.9710, 1))
                .thenReturn(searchResult(List.of(candidate()), Set.of(INFRA_CATEGORY.CONVENIENT_STORE), true));
        when(infrastructureRepository.saveAndFlush(any(Infrastructure.class))).thenReturn(infrastructure);
        when(infraAccessibilityRepository.existsById(new PropertyInfrastructureId(101L, 77L)))
                .thenReturn(false);
        when(tmapClient.findWalkingRoute(37.2910, 126.9710, 37.2912, 126.9712))
                .thenReturn(Optional.of(routeCandidate()));
        when(infraAccessibilityRepository.saveAndFlush(any(InfraAccessibility.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropertyInfrastructureSyncResult result = propertyInfrastructureService.syncMissingInfrastructure(property);

        assertThat(result.infrastructureCount()).isEqualTo(1);
        assertThat(result.createdAccessibilityCount()).isEqualTo(1);
        assertThat(result.quotaExceeded()).isTrue();
        verify(infraAccessibilityRepository).saveAndFlush(any(InfraAccessibility.class));
    }

    @Test
    void syncMissingPropertyLimitsAccessibilitiesToTwoInfrastructuresPerCategory() {
        Property property = property();
        Infrastructure store1 = infrastructure(77L, INFRA_CATEGORY.CONVENIENT_STORE, 37.2911, 126.9711);
        Infrastructure store2 = infrastructure(78L, INFRA_CATEGORY.CONVENIENT_STORE, 37.2912, 126.9712);
        Infrastructure store3 = infrastructure(79L, INFRA_CATEGORY.CONVENIENT_STORE, 37.2913, 126.9713);
        Infrastructure pharmacy = infrastructure(91L, INFRA_CATEGORY.PHARMACY, 37.2914, 126.9714);

        when(tmapClient.findInfrastructureCandidatesWithQuotaStatus(37.2910, 126.9710, 1))
                .thenReturn(searchResult(List.of(
                        candidate("store-1", INFRA_CATEGORY.CONVENIENT_STORE, 37.2911, 126.9711),
                        candidate("store-2", INFRA_CATEGORY.CONVENIENT_STORE, 37.2912, 126.9712),
                        candidate("store-3", INFRA_CATEGORY.CONVENIENT_STORE, 37.2913, 126.9713),
                        candidate("pharmacy-1", INFRA_CATEGORY.PHARMACY, 37.2914, 126.9714)
                ), Set.of(INFRA_CATEGORY.CONVENIENT_STORE, INFRA_CATEGORY.PHARMACY), true));
        when(infrastructureRepository.saveAndFlush(any(Infrastructure.class)))
                .thenReturn(store1, store2, store3, pharmacy);
        when(infraAccessibilityRepository.existsById(any(PropertyInfrastructureId.class)))
                .thenReturn(false);
        when(tmapClient.findWalkingRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(routeCandidate()));
        when(infraAccessibilityRepository.saveAndFlush(any(InfraAccessibility.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropertyInfrastructureSyncResult result = propertyInfrastructureService.syncMissingInfrastructure(property);

        assertThat(result.infrastructureCount()).isEqualTo(3);
        assertThat(result.createdAccessibilityCount()).isEqualTo(3);

        ArgumentCaptor<InfraAccessibility> accessibilityCaptor =
                ArgumentCaptor.forClass(InfraAccessibility.class);
        verify(infraAccessibilityRepository, times(3))
                .saveAndFlush(accessibilityCaptor.capture());
        assertThat(accessibilityCaptor.getAllValues())
                .extracting(accessibility -> accessibility.getId().getInfrastructureId())
                .containsExactly(77L, 78L, 91L);
    }

    @Test
    void refreshRemovesObsoleteAccessibilityAndDeletesOrphanInfrastructure() {
        Property property = property();
        Infrastructure oldStore = infrastructure(66L, INFRA_CATEGORY.CONVENIENT_STORE, 37.2920, 126.9720);
        Infrastructure newStore = infrastructure(77L, INFRA_CATEGORY.CONVENIENT_STORE, 37.2911, 126.9711);
        InfraAccessibility oldAccessibility = new InfraAccessibility(
                property,
                oldStore,
                new Minutes(5),
                new Path(new Minutes(5), 0, List.of())
        );

        when(tmapClient.findInfrastructureCandidatesWithQuotaStatus(37.2910, 126.9710, 1))
                .thenReturn(searchResult(
                        List.of(candidate("new-store", INFRA_CATEGORY.CONVENIENT_STORE, 37.2911, 126.9711)),
                        Set.of(INFRA_CATEGORY.CONVENIENT_STORE),
                        false
                ));
        when(infrastructureRepository.saveAndFlush(any(Infrastructure.class))).thenReturn(newStore);
        when(infraAccessibilityRepository.existsById(new PropertyInfrastructureId(101L, 77L)))
                .thenReturn(false);
        when(tmapClient.findWalkingRoute(37.2910, 126.9710, 37.2911, 126.9711))
                .thenReturn(Optional.of(routeCandidate()));
        when(infraAccessibilityRepository.saveAndFlush(any(InfraAccessibility.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(infraAccessibilityRepository.findAllByPropertyPropertyId(101L))
                .thenReturn(List.of(oldAccessibility));
        when(infraAccessibilityRepository.countByIdInfrastructureId(66L)).thenReturn(0L);

        PropertyInfrastructureSyncResult result =
                propertyInfrastructureService.refreshInfrastructureSelection(property);

        assertThat(result.infrastructureCount()).isEqualTo(1);
        assertThat(result.createdAccessibilityCount()).isEqualTo(1);
        assertThat(result.removedAccessibilityCount()).isEqualTo(1);
        verify(infraAccessibilityRepository).deleteAll(List.of(oldAccessibility));
        verify(infraAccessibilityRepository).flush();
        verify(infrastructureRepository).deleteById(66L);
    }

    private Property property() {
        return Property.builder()
                .propertyId(101L)
                .latitude(37.2910)
                .longitude(126.9710)
                .build();
    }

    private TmapInfrastructureSearchResult searchResult(
            List<TmapInfrastructureCandidate> candidates,
            Set<INFRA_CATEGORY> completedCategories,
            boolean quotaExceeded
    ) {
        return new TmapInfrastructureSearchResult(candidates, completedCategories, quotaExceeded);
    }

    private TmapInfrastructureCandidate candidate() {
        return candidate("infra-77", INFRA_CATEGORY.CONVENIENT_STORE, 37.2912, 126.9712);
    }

    private TmapInfrastructureCandidate candidate(
            String externalId,
            INFRA_CATEGORY category,
            double latitude,
            double longitude
    ) {
        return new TmapInfrastructureCandidate(
                externalId,
                category.name(),
                category,
                latitude,
                longitude,
                new RoadAddress("Test Road 1")
        );
    }

    private Infrastructure infrastructure(Long id) {
        return infrastructure(id, INFRA_CATEGORY.CONVENIENT_STORE, 37.2912, 126.9712);
    }

    private Infrastructure infrastructure(
            Long id,
            INFRA_CATEGORY category,
            double latitude,
            double longitude
    ) {
        Infrastructure infrastructure = new Infrastructure(
                category.name(),
                category,
                latitude,
                longitude,
                new RoadAddress("Test Road 1")
        );
        ReflectionTestUtils.setField(infrastructure, "id", id);
        return infrastructure;
    }

    private OdsayRouteCandidate routeCandidate() {
        Minutes duration = new Minutes(3);
        return new OdsayRouteCandidate(
                duration,
                new Path(duration, 0, List.of(new SubPath(3, duration, "Property", "Infra", null)))
        );
    }
}
