package com.nextgen.mparivahan.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {
    private final JdbcTemplate db;
    public ApiController(JdbcTemplate db) { this.db = db; }

    public record LoginRequest(@NotBlank @Pattern(regexp="\\d{10}") String mobile,
                               @NotBlank @Pattern(regexp="\\d{4}") String mpin) {}
    public record ComplaintRequest(@NotBlank @Pattern(regexp="\\d{10}") String mobile, @NotBlank String category,
                                   @NotBlank String subcategory, @NotBlank String vehicle,
                                   @NotBlank String remarks) {}
    public record TransactionRequest(@NotBlank @Pattern(regexp="\\d{10}") String mobile, @NotBlank String type, @NotBlank String value) {}
    public record RegisterRequest(@NotBlank String name, @NotBlank @Pattern(regexp="\\d{10}") String mobile, String email) {}
    public record GenerateMpinRequest(@NotBlank @Pattern(regexp="\\d{10}") String mobile) {}
    public record ProfileUpdateRequest(@NotBlank String name, String email) {}

    @GetMapping("/health") public Map<String,Object> health() {
        return Map.of("status", "UP", "service", "NextGen mParivahan API", "time", Instant.now().toString());
    }

    @PostMapping("/auth/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest req) {
        List<Map<String,Object>> rows = db.queryForList("SELECT mobile, full_name, email FROM app_users WHERE mobile=? AND mpin=?", req.mobile(), req.mpin());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid mobile number or MPIN");
        Map<String,Object> result = new LinkedHashMap<>(); result.put("success", true); result.put("message", "Login successful"); result.put("user", rows.get(0)); return result;
    }


    @PostMapping("/auth/register") public Map<String,Object> register(@Valid @RequestBody RegisterRequest req) {
        String pin=String.format(Locale.ROOT,"%04d",new java.security.SecureRandom().nextInt(10000));
        db.update("MERGE INTO app_users (mobile,mpin,full_name,email) KEY(mobile) VALUES (?,?,?,?)",req.mobile(),pin,req.name(),req.email()==null?"":req.email());
        return Map.of("success",true,"message","Demo account created","mobile",req.mobile(),"mpin",pin);
    }

    @PostMapping("/auth/generate-mpin") public Map<String,Object> generateMpin(@Valid @RequestBody GenerateMpinRequest req) {
        Integer count=db.queryForObject("SELECT COUNT(*) FROM app_users WHERE mobile=?",Integer.class,req.mobile());
        if(count==null||count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No API account exists for this mobile number");
        String pin=String.format(Locale.ROOT,"%04d",new java.security.SecureRandom().nextInt(10000));
        db.update("UPDATE app_users SET mpin=? WHERE mobile=?",pin,req.mobile());
        return Map.of("success",true,"mobile",req.mobile(),"mpin",pin);
    }

    @GetMapping("/licences/{number}") public Map<String,Object> licence(@PathVariable String number) {
        List<Map<String,Object>> rows=db.queryForList("SELECT licence_number AS licence, owner_name AS owner, state_name AS state, status FROM licence_records WHERE UPPER(licence_number)=?",number.toUpperCase(Locale.ROOT));
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No demo driving licence found");
        return rows.get(0);
    }

    @GetMapping("/people") public List<Map<String,Object>> people() {
        return db.queryForList("SELECT owner_name AS owner, registration FROM vehicle_records ORDER BY state_name,owner_name");
    }

    @GetMapping("/nearby") public List<Map<String,Object>> nearby() {
        return List.of(Map.of("id","pollution","name","Pollution Checking Centre","description","Demo nearby service"),Map.of("id","rto","name","Regional Transport Office","description","Demo nearby service"),Map.of("id","fuel","name","Fuel Pump","description","Demo nearby service"),Map.of("id","hospital","name","Hospital","description","Demo nearby service"));
    }

    @GetMapping("/emergency") public List<Map<String,Object>> emergency() {
        return List.of(Map.of("name","Ambulance","number","102"),Map.of("name","Police","number","112"),Map.of("name","Fire","number","101"));
    }

    @PutMapping("/profile/{mobile}") public Map<String,Object> updateProfile(@PathVariable String mobile,@Valid @RequestBody ProfileUpdateRequest req) {
        int updated=db.update("UPDATE app_users SET full_name=?,email=? WHERE mobile=?",req.name(),req.email()==null?"":req.email(),mobile);
        if(updated==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found");
        return Map.of("success",true,"message","Profile updated");
    }

    @GetMapping("/services") public List<Map<String,Object>> services() {
        return List.of(Map.of("id","vehicle-search","name","Vehicle Search"), Map.of("id","dl-search","name","Driving Licence Search"),
                Map.of("id","complaints","name","Citizen Complaints"), Map.of("id","transactions","name","Transaction History"));
    }

    @GetMapping("/vehicles/{registration}") public Map<String,Object> vehicle(@PathVariable String registration) {
        List<Map<String,Object>> rows = db.queryForList("SELECT registration, owner_name AS owner, state_name AS state, city, vehicle, fuel, status FROM vehicle_records WHERE UPPER(registration)=?", registration.toUpperCase(Locale.ROOT));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No demo vehicle found for this registration");
        return rows.get(0);
    }

    @PostMapping("/complaints") public Map<String,Object> complaint(@Valid @RequestBody ComplaintRequest req) {
        db.update("INSERT INTO complaints(mobile,category,subcategory,vehicle,remarks,created_at) VALUES(?,?,?,?,?,?)",
                req.mobile(), req.category(), req.subcategory(), req.vehicle().toUpperCase(Locale.ROOT), req.remarks(), Instant.now().toString());
        return Map.of("success", true, "message", "Demo complaint saved", "note", "Not submitted to any government authority");
    }

    @GetMapping("/complaints/{mobile}") public List<Map<String,Object>> complaints(@PathVariable String mobile) {
        return db.queryForList("SELECT id,category,subcategory,vehicle,remarks,created_at FROM complaints WHERE mobile=? ORDER BY id DESC", mobile);
    }

    @PostMapping("/transactions") public Map<String,Object> transaction(@Valid @RequestBody TransactionRequest req) {
        db.update("INSERT INTO transactions(mobile,type,transaction_value,created_at) VALUES(?,?,?,?)", req.mobile(), req.type(), req.value(), Instant.now().toString());
        return Map.of("success", true, "message", "Demo transaction recorded");
    }

    @GetMapping("/transactions/{mobile}") public List<Map<String,Object>> transactions(@PathVariable String mobile) {
        return db.queryForList("SELECT id,type,transaction_value AS value,created_at FROM transactions WHERE mobile=? ORDER BY id DESC", mobile);
    }

    @GetMapping("/profile/{mobile}") public Map<String,Object> profile(@PathVariable String mobile) {
        List<Map<String,Object>> rows = db.queryForList("SELECT mobile,full_name AS name,email FROM app_users WHERE mobile=?", mobile);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        return rows.get(0);
    }
}
