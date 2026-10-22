package com.example.agentic;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service public class UrlService {
 private static final String ALPHABET="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
 private final JdbcTemplate db;private final SecureRandom rng=new SecureRandom();private final String base;
 public UrlService(JdbcTemplate db,@Value("${app.base-url}") String base){this.db=db;this.base=base;}
 public record CreateUrl(String url,String alias,Instant expiresAt){}
 private void validate(String value){try{URI u=URI.create(value);String scheme=u.getScheme();if(scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https"))||u.getHost()==null||value.length()>2048||u.getUserInfo()!=null)throw new Exception();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Valid public http(s) URL required");}}
 public Map<String,Object> create(CreateUrl request){if(request==null||request.url()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"url required");validate(request.url());if(request.expiresAt()!=null && !request.expiresAt().isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Expiry must be in future");boolean custom=request.alias()!=null&&!request.alias().isBlank();if(custom && !request.alias().matches("[a-zA-Z0-9_-]{4,32}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Alias must have 4-32 permitted characters");for(int i=0;i<8;i++){String code=custom?request.alias():randomCode();try{db.update("INSERT INTO urls(code,target,expires_at) VALUES(?,?,?)",code,request.url(),request.expiresAt()==null?null:Timestamp.from(request.expiresAt()));return Map.of("code",code,"shortUrl",base+"/r/"+code,"url",request.url());}catch(DuplicateKeyException e){if(custom)throw new ResponseStatusException(HttpStatus.CONFLICT,"Alias already exists");}}throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Could not generate code");}
 private String randomCode(){StringBuilder s=new StringBuilder();for(int i=0;i<8;i++)s.append(ALPHABET.charAt(rng.nextInt(ALPHABET.length())));return s.toString();}
 @Transactional public String resolve(String code){var found=db.query("SELECT target FROM urls WHERE code=? AND active=TRUE AND (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP)",(rs,n)->rs.getString(1),code);if(found.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Short URL not found or expired");db.update("UPDATE urls SET hits=hits+1 WHERE code=?",code);db.update("INSERT INTO clicks(code) VALUES(?)",code);return found.get(0);}
 public Map<String,Object> stats(String code){var result=db.queryForList("SELECT code,target,hits,created_at,expires_at,active FROM urls WHERE code=?",code);if(result.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Code not found");return result.get(0);}
 public void disable(String code){if(db.update("UPDATE urls SET active=FALSE WHERE code=?",code)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Code not found");}
}
