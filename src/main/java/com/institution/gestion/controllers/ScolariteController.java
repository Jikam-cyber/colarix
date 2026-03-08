package com.institution.gestion.controllers;

import com.institution.gestion.entities.*;
import com.institution.gestion.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
public class ScolariteController {

    @Autowired private EtudiantRepository etudiantRepository;
    @Autowired private NoteRepository noteRepository;
    @Autowired private MatiereRepository matiereRepository;

    @GetMapping("/")
    public String accueil() { return "accueil"; }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // --- LOGIN ---
    @GetMapping("/login-prof")
    public String loginProfPage(Model model) { model.addAttribute("role", "PROF"); return "login"; }

    @GetMapping("/login-admin")
    public String loginAdminPage(Model model) { model.addAttribute("role", "ADMIN"); return "login"; }

    @PostMapping("/verifier-login")
    public String verifierLogin(@RequestParam String username, @RequestParam String password, 
                                @RequestParam String role, HttpSession session, RedirectAttributes ra) {
        if ("ADMIN".equals(role) && "admin".equals(username) && "rectorat2026".equals(password)) {
            session.setAttribute("userRole", "ADMIN");
            return "redirect:/espace-admin";
        } else if ("PROF".equals(role) && "prof".equals(username) && "scolarix123".equals(password)) {
            session.setAttribute("userRole", "PROF");
            return "redirect:/espace-prof";
        }
        ra.addFlashAttribute("error", "Identifiants invalides !");
        return "redirect:/login-" + role.toLowerCase();
    }

    // --- ESPACE PROF ---
    @GetMapping("/espace-prof")
    public String espaceProf(HttpSession session, Model model) {
        if (!"PROF".equals(session.getAttribute("userRole"))) return "redirect:/login-prof";
        model.addAttribute("etudiants", etudiantRepository.findAll());
        return "index";
    }

    @GetMapping("/ajouter-note/{id}")
    public String formNote(@PathVariable Long id, HttpSession session, Model model) {
        if (!"PROF".equals(session.getAttribute("userRole"))) return "redirect:/login-prof";
        model.addAttribute("etudiant", etudiantRepository.findById(id).orElseThrow());
        model.addAttribute("matieres", matiereRepository.findAll());
        return "ajouter-note";
    }

    @PostMapping("/enregistrer-note")
    public String enregistrerNote(@RequestParam Long etudiantId, @RequestParam Long matiereId, 
                                  @RequestParam Double valeur, RedirectAttributes ra) {
        Optional<Note> noteExistante = noteRepository.findByEtudiantIdAndMatiereId(etudiantId, matiereId);
        if (noteExistante.isPresent()) {
            ra.addFlashAttribute("error", "Une note existe déjà pour cette matière !");
            return "redirect:/ajouter-note/" + etudiantId;
        }
        Note n = new Note();
        n.setEtudiant(etudiantRepository.findById(etudiantId).get());
        n.setMatiere(matiereRepository.findById(matiereId).get());
        n.setValeur(valeur);
        noteRepository.save(n);
        return "redirect:/bulletin/" + etudiantId;
    }

    // --- ESPACE ADMIN ---
    @GetMapping("/espace-admin")
    public String espaceAdmin(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/login-admin";
        model.addAttribute("totalEtudiants", etudiantRepository.count());
        model.addAttribute("totalMatieres", matiereRepository.count());
        List<Note> notes = noteRepository.findAll();
        double moy = notes.stream().mapToDouble(Note::getValeur).average().orElse(0.0);
        model.addAttribute("moyenneEcole", String.format("%.2f", moy));
        model.addAttribute("listeEtudiants", etudiantRepository.findAll());
        model.addAttribute("listeMatieres", matiereRepository.findAll());
        return "dashboard-admin";
    }

    @PostMapping("/admin/ajouter-etudiant")
    public String addEtudiant(@RequestParam String nom, @RequestParam String prenom, @RequestParam String matricule) {
        Etudiant e = new Etudiant(); e.setNom(nom); e.setPrenom(prenom); e.setMatricule(matricule);
        etudiantRepository.save(e);
        return "redirect:/espace-admin";
    }

    @PostMapping("/admin/ajouter-matiere")
    public String addMatiere(@RequestParam String nom) {
        Matiere m = new Matiere(); m.setNom(nom);
        matiereRepository.save(m);
        return "redirect:/espace-admin";
    }

    @GetMapping("/admin/supprimer-etudiant/{id}")
    public String deleteEtudiant(@PathVariable Long id) {
        etudiantRepository.deleteById(id);
        return "redirect:/espace-admin";
    }

    // --- ETUDIANT & BULLETIN ---
    @GetMapping("/login-etudiant")
    public String loginEtudiant() { return "login-etudiant"; }

    @PostMapping("/rechercher-bulletin")
    public String rechercherBulletin(@RequestParam String matricule, RedirectAttributes ra) {
        Optional<Etudiant> e = etudiantRepository.findAll().stream()
                .filter(s -> s.getMatricule().equalsIgnoreCase(matricule)).findFirst();
        if (e.isPresent()) return "redirect:/bulletin/" + e.get().getId();
        ra.addFlashAttribute("error", "Matricule introuvable.");
        return "redirect:/login-etudiant";
    }

    @GetMapping("/bulletin/{id}")
    public String voirBulletin(@PathVariable Long id, Model model) {
        Etudiant e = etudiantRepository.findById(id).orElseThrow();
        List<Note> notes = noteRepository.findByEtudiantId(id);
        double moy = notes.stream().mapToDouble(Note::getValeur).average().orElse(0.0);
        String ment, coul;
        if (moy >= 16) { ment = "Très Bien"; coul = "text-success"; }
        else if (moy >= 14) { ment = "Bien"; coul = "text-info"; }
        else if (moy >= 12) { ment = "Assez Bien"; coul = "text-primary"; }
        else if (moy >= 10) { ment = "Passable"; coul = "text-dark"; }
        else { ment = "Ajourné"; coul = "text-danger"; }
        model.addAttribute("etudiant", e); model.addAttribute("notes", notes);
        model.addAttribute("moyenne", String.format("%.2f", moy));
        model.addAttribute("mention", ment); model.addAttribute("couleurMention", coul);
        return "bulletin";
    }
}