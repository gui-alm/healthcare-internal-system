package me.a06.meditrack.repository;

import me.a06.meditrack.domain.PatientData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientDataRepository extends JpaRepository<PatientData, String> {
}
