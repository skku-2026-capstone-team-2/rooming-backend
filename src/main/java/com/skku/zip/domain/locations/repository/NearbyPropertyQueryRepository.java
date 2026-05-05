package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.Userplace;
import com.skku.zip.domain.property.entity.Property;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NearbyPropertyQueryRepository {

    private static final double DEFAULT_RADIUS_KM = 5.0;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Property> findWithinFiveKilometers(Userplace userplace) {
        return findWithinKilometers(userplace.getLatitude(), userplace.getLongitude(), DEFAULT_RADIUS_KM);
    }

    public List<Property> findWithinKilometers(double latitude, double longitude, double radiusKm) {
        String sql = """
                SELECT *
                FROM property p
                WHERE p.latitude IS NOT NULL
                  AND p.longitude IS NOT NULL
                  AND (
                    6371.0 * acos(
                      LEAST(1.0, GREATEST(-1.0,
                        cos(radians(:latitude)) * cos(radians(p.latitude))
                        * cos(radians(p.longitude) - radians(:longitude))
                        + sin(radians(:latitude)) * sin(radians(p.latitude))
                      ))
                    )
                  ) <= :radiusKm
                ORDER BY (
                  6371.0 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(:latitude)) * cos(radians(p.latitude))
                      * cos(radians(p.longitude) - radians(:longitude))
                      + sin(radians(:latitude)) * sin(radians(p.latitude))
                    ))
                  )
                )
                """;

        return entityManager.createNativeQuery(sql, Property.class)
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("radiusKm", radiusKm)
                .getResultList();
    }
}
