package com.institution.gestion.repositories;

import com.institution.gestion.entities.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    List<Etudiant> findByNomContainingIgnoreCase(String nom);
    Etudiant findByMatricule(String matricule); // Ajoute cette ligne
}