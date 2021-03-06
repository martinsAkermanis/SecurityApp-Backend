package io.codelex.securityapp.authentication.admin;

import io.codelex.securityapp.api.ClientLogin;
import io.codelex.securityapp.authentication.AuthService;
import io.codelex.securityapp.repository.RepositoryClientService;
import io.codelex.securityapp.repository.RepositoryIncidentService;
import io.codelex.securityapp.repository.RepositoryUnitService;
import io.codelex.securityapp.repository.models.Client;
import io.codelex.securityapp.repository.models.Incident;
import io.codelex.securityapp.repository.models.Unit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

@CrossOrigin
@RestController
@RequestMapping("/admin-api")
class AdminController {

    private final AuthService authService;
    private RepositoryClientService clientService;
    private RepositoryIncidentService incidentService;
    private RepositoryUnitService unitService;

    public AdminController(AuthService authService, RepositoryClientService clientService, RepositoryIncidentService incidentService, RepositoryUnitService unitService) {
        this.authService = authService;
        this.clientService = clientService;
        this.incidentService = incidentService;
        this.unitService = unitService;
    }

    @GetMapping("/account")
    public String account(Principal principal) {
        return principal.getName();
    }

    @PostMapping("/sign-in")
    public ResponseEntity<Client> signIn(@Valid @RequestBody ClientLogin request) {
        if (request.getEmail().equals("admin@admin.com") && request.getPassword().equals("123456")) {
            authService.authorizeClient(request.getEmail(), request.getPassword());
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<Client> findClientById(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(clientService.findById(id), HttpStatus.OK);
        } catch (
                NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<Incident> findIncidentById(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(incidentService.findById(id), HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/units/all")
    public ResponseEntity<List<Unit>> findAllUnits() {
        return new ResponseEntity<>(unitService.findAllUnits(), HttpStatus.OK);
    }

    @GetMapping("/incidents/all")
    public ResponseEntity<List<Incident>> findAllIncidents() {
        return new ResponseEntity<>(incidentService.findAllIncident(), HttpStatus.OK);
    }

    @GetMapping("/clients/all")
    public ResponseEntity<List<Client>> findAllClients() {
        return new ResponseEntity<>(clientService.findAllClients(), HttpStatus.OK);
    }

    @PostMapping("/sign-out")
    public ResponseEntity signOut() {
            authService.clearAuthentication();
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
