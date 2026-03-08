package com.institution.gestion.repositories;

import com.institution.gestion.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByEtudiantId(Long etudiantId);
    Optional<Note> findByEtudiantIdAndMatiereId(Long etudiantId, Long matiereId);
}