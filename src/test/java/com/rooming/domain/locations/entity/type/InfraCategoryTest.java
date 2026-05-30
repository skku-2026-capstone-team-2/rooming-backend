package com.rooming.domain.locations.entity.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfraCategoryTest {

    @Test
    void unknownMiddleBizNameFallsBackToEtc() {
        assertThat(INFRA_CATEGORY.fromMiddleBizName("unknown-category"))
                .contains(INFRA_CATEGORY.ETC);
    }

    @Test
    void blankMiddleBizNameCannotBeClassified() {
        assertThat(INFRA_CATEGORY.fromMiddleBizName(" "))
                .isEmpty();
    }
}