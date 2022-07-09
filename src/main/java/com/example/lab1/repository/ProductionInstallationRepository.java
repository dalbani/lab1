package com.example.lab1.repository;

import com.example.lab1.model.ProductionInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(
        path = "production-installations"
)
public interface ProductionInstallationRepository extends JpaRepository<ProductionInstallation, Long> {

    List<ProductionInstallation> findAllByName(String name);

    List<ProductionInstallation> findAllByOutputPowerBetween(
            @Param("powerGreaterThan") Double lowerLimit,
            @Param("powerLowerThan") Double upperLimit);

}
