package com.lcc.schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Slf4j
public class HotTagTasks {

    @Scheduled(fixedRate =5000)
    public void hotTagSchedule() {

        log.info("hotTagSchedule start {}", new Date());
    }
}
