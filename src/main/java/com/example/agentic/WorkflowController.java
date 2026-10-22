package com.example.agentic;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/workflows") public class WorkflowController {
 private final WorkflowService svc; private final AdminGuard admin;
 public WorkflowController(WorkflowService svc,AdminGuard admin){this.svc=svc;this.admin=admin;}
 @PostMapping public Map<String,Object> start(@RequestHeader("X-Admin-Key") String key,@RequestBody WorkflowService.StartRequest request){admin.check(key);return svc.start(request);}
 @PostMapping("/{id}/advance") public Map<String,Object> advance(@RequestHeader("X-Admin-Key") String key,@PathVariable String id){admin.check(key);return svc.advance(id);}
 @GetMapping("/{id}") public Map<String,Object> detail(@RequestHeader("X-Admin-Key") String key,@PathVariable String id){admin.check(key);return svc.detail(id);}
 @PostMapping("/{id}/approval") public Map<String,Object> approval(@RequestHeader("X-Admin-Key") String key,@PathVariable String id,@RequestBody WorkflowService.DecisionRequest request){admin.check(key);return svc.approve(id,request);}
 @PostMapping("/{id}/replan") public Map<String,Object> replan(@RequestHeader("X-Admin-Key") String key,@PathVariable String id,@RequestBody Map<String,String> request){admin.check(key);return svc.replan(id,request.get("requirement"));}
 @PostMapping("/{id}/tasks/{task}/fail") public Map<String,Object> fail(@RequestHeader("X-Admin-Key") String key,@PathVariable String id,@PathVariable String task){admin.check(key);return svc.fail(id,task);}
 @PostMapping("/{id}/tasks/{task}/retry") public Map<String,Object> retry(@RequestHeader("X-Admin-Key") String key,@PathVariable String id,@PathVariable String task){admin.check(key);return svc.retry(id,task);}
 @PostMapping("/{id}/rollback") public Map<String,Object> rollback(@RequestHeader("X-Admin-Key") String key,@PathVariable String id){admin.check(key);return svc.rollback(id);}
 @GetMapping("/metrics") public Map<String,Object> metrics(@RequestHeader("X-Admin-Key") String key){admin.check(key);return svc.metrics();}
}
