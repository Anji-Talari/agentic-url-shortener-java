package com.example.agentic;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service public class WorkflowService {
 private final JdbcTemplate db;
 public WorkflowService(JdbcTemplate db){this.db=db;}
 public record StartRequest(String scenario,String requirement){}
 public record DecisionRequest(String actor,String reason,boolean approved){}
 private record Spec(String name,String deps){}
 // Two sibling tasks may run at the same graph level; join is an explicit synchronization barrier.
 private static final List<Spec> GRAPH=List.of(new Spec("understand",""),new Spec("design","understand"),new Spec("security_review","understand"),new Spec("implement","design,security_review"),new Spec("unit_tests","implement"),new Spec("docs","design"),new Spec("integration_tests","unit_tests"),new Spec("human_approval","integration_tests,docs"),new Spec("release_readiness","human_approval"));
 private void audit(String id,String event,String detail){db.update("INSERT INTO audits(workflow_id,event,detail) VALUES(?,?,?)",id,event,detail);}
 private void existence(String id){if(db.queryForObject("SELECT COUNT(*) FROM workflows WHERE id=?",Integer.class,id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workflow not found");}
 @Transactional public Map<String,Object> start(StartRequest r){if(r==null||r.scenario()==null||!Set.of("greenfield","brownfield","ambiguous").contains(r.scenario()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"scenario: greenfield, brownfield, ambiguous");if(r.requirement()==null||r.requirement().isBlank()||r.requirement().length()>4000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"requirement required, max 4000 chars");String id=UUID.randomUUID().toString();db.update("INSERT INTO workflows(id,scenario,requirement,state) VALUES(?,?,?,?)",id,r.scenario(),r.requirement(),"RUNNING");for(Spec s:GRAPH)db.update("INSERT INTO tasks(workflow_id,name,dependency,status) VALUES(?,?,?,?)",id,s.name,s.deps,"PENDING");audit(id,"CREATED","Scenario="+r.scenario());return detail(id);}
 private boolean ready(String id,Map<String,Object> task){String dep=(String)task.get("DEPENDENCY");if(dep==null||dep.isBlank())return true;for(String name:dep.split(",")){String status=db.queryForObject("SELECT status FROM tasks WHERE workflow_id=? AND name=?",String.class,id,name);if(!"DONE".equals(status))return false;}return true;}
 private String outcome(String scenario,String name,String requirement){return switch(name){
 case "understand" -> "Normalized "+scenario+" requirement: "+requirement+". Assumption: internal prototype; approval needed for release.";
 case "design" -> "Designed Java Spring Boot REST API, H2 persistence, task dependency DAG and rollback policy.";
 case "security_review" -> "Threat model: validate URL scheme, protect analytics/admin API, approval before release; production needs auth and SSRF/abuse controls.";
 case "implement" -> "Prototype source modules: UrlController, UrlService, WorkflowController, WorkflowService (reviewable in repository).";
 case "unit_tests" -> "JUnit/Spring integration tests supplied; external test execution is separate from this simulated agent task.";
 case "docs" -> "Architecture, scenario walkthroughs and trade-offs supplied under docs/.";
 case "integration_tests" -> "Simulated agent validation complete; actual mvn test and deployment checks required before production.";
 case "release_readiness" -> "Release readiness checklist prepared; no deployment performed.";
 default -> "Completed";};}
 @Transactional public Map<String,Object> advance(String id){existence(id);String state=db.queryForObject("SELECT state FROM workflows WHERE id=?",String.class,id);if(Set.of("STOPPED","DONE","REJECTED").contains(state))return detail(id);
 String scenario=db.queryForObject("SELECT scenario FROM workflows WHERE id=?",String.class,id);String requirement=db.queryForObject("SELECT requirement FROM workflows WHERE id=?",String.class,id);
 boolean changed=false;
 for(int wave=0;wave<GRAPH.size();wave++){ boolean waveChanged=false;
 for(Map<String,Object> task:db.queryForList("SELECT * FROM tasks WHERE workflow_id=? ORDER BY name",id)){
 String name=(String)task.get("NAME"),status=(String)task.get("STATUS");if(!status.equals("PENDING")||!ready(id,task))continue;
 if(name.equals("human_approval")){db.update("UPDATE tasks SET status='WAITING_APPROVAL' WHERE workflow_id=? AND name=?",id,name);db.update("UPDATE workflows SET state='WAITING_APPROVAL',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);audit(id,"APPROVAL_REQUESTED","Human checkpoint before release");changed=true;waveChanged=true;continue;}
 // No real code generation/LLM network call: deterministic, auditable prototype agents.
 db.update("UPDATE tasks SET status='DONE',attempts=attempts+1,output=? WHERE workflow_id=? AND name=?",outcome(scenario,name,requirement),id,name);audit(id,"TASK_COMPLETED",name);changed=true;waveChanged=true;
 }
 if(!waveChanged)break;
 }
 if(db.queryForObject("SELECT COUNT(*) FROM tasks WHERE workflow_id=? AND status<>'DONE'",Integer.class,id)==0){db.update("UPDATE workflows SET state='DONE',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);audit(id,"WORKFLOW_DONE","All gates passed");}
 else if(!changed && state.equals("RUNNING")){db.update("UPDATE workflows SET state='STOPPED' WHERE id=?",id);audit(id,"SAFE_STOP","No executable tasks; deadlock/missing dependency");}
 return detail(id);
 }
 @Transactional public Map<String,Object> approve(String id,DecisionRequest req){existence(id);String state=db.queryForObject("SELECT state FROM workflows WHERE id=?",String.class,id);if(!state.equals("WAITING_APPROVAL"))throw new ResponseStatusException(HttpStatus.CONFLICT,"No pending approval");if(req==null||req.actor()==null||req.actor().isBlank()||req.reason()==null||req.reason().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"actor and reason required");db.update("MERGE INTO approvals(workflow_id,task_name,decision,actor,reason) KEY(workflow_id,task_name) VALUES(?,?,?,?,?)",id,"human_approval",req.approved()?"APPROVED":"REJECTED",req.actor(),req.reason());db.update("UPDATE tasks SET status=?, output=? WHERE workflow_id=? AND name='human_approval'",req.approved()?"DONE":"REJECTED",req.reason(),id);db.update("UPDATE workflows SET state=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",req.approved()?"RUNNING":"REJECTED",id);audit(id,req.approved()?"APPROVED":"REJECTED",req.actor()+": "+req.reason());return detail(id);}
 @Transactional public Map<String,Object> replan(String id,String requirement){existence(id);if(requirement==null||requirement.isBlank()||requirement.length()>4000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid requirement");String state=db.queryForObject("SELECT state FROM workflows WHERE id=?",String.class,id);if(!Set.of("RUNNING","WAITING_APPROVAL","STOPPED").contains(state))throw new ResponseStatusException(HttpStatus.CONFLICT,"Terminal workflow cannot be replanned");db.update("UPDATE workflows SET requirement=?,version=version+1,state='RUNNING',updated_at=CURRENT_TIMESTAMP WHERE id=?",requirement,id);db.update("DELETE FROM approvals WHERE workflow_id=?",id);db.update("UPDATE tasks SET status='PENDING',attempts=0,output=NULL WHERE workflow_id=?",id);audit(id,"REPLAN","Upstream requirement changed; downstream outputs invalidated; prior audit retained");return detail(id);}
 @Transactional public Map<String,Object> retry(String id,String name){existence(id);List<Map<String,Object>> ts=db.queryForList("SELECT attempts,max_attempts,status FROM tasks WHERE workflow_id=? AND name=?",id,name);if(ts.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Task missing");Map<String,Object> t=ts.get(0);if(!"FAILED".equals(t.get("STATUS")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Only failed tasks can retry");if(((Number)t.get("ATTEMPTS")).intValue()>=((Number)t.get("MAX_ATTEMPTS")).intValue())throw new ResponseStatusException(HttpStatus.CONFLICT,"Retry budget exhausted");db.update("UPDATE tasks SET status='PENDING' WHERE workflow_id=? AND name=?",id,name);db.update("UPDATE workflows SET state='RUNNING' WHERE id=?",id);audit(id,"RETRY",name);return detail(id);}
 @Transactional public Map<String,Object> fail(String id,String name){existence(id);if(db.update("UPDATE tasks SET status='FAILED',attempts=attempts+1 WHERE workflow_id=? AND name=? AND status IN ('PENDING','DONE')",id,name)==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Task cannot be marked failed");db.update("UPDATE workflows SET state='STOPPED' WHERE id=?",id);audit(id,"FAILURE_INJECTED",name+"; safe stopped awaiting retry/rollback");return detail(id);}
 @Transactional public Map<String,Object> rollback(String id){existence(id);db.update("UPDATE workflows SET state='STOPPED',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);audit(id,"ROLLBACK","Compensating action simulated: orchestration stopped; external artifacts never deployed");return detail(id);}
 public Map<String,Object> detail(String id){existence(id);Map<String,Object> result=new LinkedHashMap<>(db.queryForMap("SELECT * FROM workflows WHERE id=?",id));result.put("tasks",db.queryForList("SELECT * FROM tasks WHERE workflow_id=? ORDER BY name",id));result.put("approvals",db.queryForList("SELECT * FROM approvals WHERE workflow_id=?",id));result.put("audit",db.queryForList("SELECT * FROM audits WHERE workflow_id=? ORDER BY id",id));return result;}
 public Map<String,Object> metrics(){return Map.of("workflows",db.queryForObject("SELECT COUNT(*) FROM workflows",Integer.class),"completed",db.queryForObject("SELECT COUNT(*) FROM workflows WHERE state='DONE'",Integer.class),"retries",db.queryForObject("SELECT COUNT(*) FROM audits WHERE event='RETRY'",Integer.class),"rollbacks",db.queryForObject("SELECT COUNT(*) FROM audits WHERE event='ROLLBACK'",Integer.class),"failures",db.queryForObject("SELECT COUNT(*) FROM audits WHERE event='FAILURE_INJECTED'",Integer.class));}
}
