package stats.client;

import dto.GetStatsDto;
import dto.SaveHitDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsFeignClient {

    @PostMapping(value = "/hit", consumes = MediaType.APPLICATION_JSON_VALUE)
    void saveStats(@RequestBody @Valid SaveHitDto requestBody);

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    List<GetStatsDto> getStats(@RequestParam("start") String start,
                               @RequestParam("end") String end,
                               @RequestParam(value = "uris", required = false) List<String> uris,
                               @RequestParam(value = "unique", required = false, defaultValue = "false") Boolean unique);
}