package com.skku.zip.domain.seeker.entity;

import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RegisteredTargetPlaceTest {

    @Test
    void addTargetPlaceStoresMemoOnSeekerAssociation() {
        Seeker seeker = Seeker.builder()
                .name("seeker")
                .email("seeker@example.com")
                .loginId("login-id")
                .build();
        ReflectionTestUtils.setField(seeker, "id", 1L);

        TargetPlace targetPlace = new TargetPlace(
                PLACE_CATEGORY.WORK_PLACE,
                "Office",
                new RoadAddress("Seoul road 1"),
                37.1,
                127.1
        );
        ReflectionTestUtils.setField(targetPlace, "id", 10L);

        RegisteredTargetPlace first = seeker.addTargetPlace(targetPlace, " first memo ");
        RegisteredTargetPlace second = seeker.addTargetPlace(targetPlace, "second memo");

        assertThat(seeker.getTargetPlaces()).hasSize(1);
        assertThat(second).isSameAs(first);
        assertThat(second.getMemo()).isEqualTo("second memo");
        assertThat(second.getTargetPlace()).isSameAs(targetPlace);
    }
}
