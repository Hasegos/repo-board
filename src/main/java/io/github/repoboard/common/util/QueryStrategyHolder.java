package io.github.repoboard.common.util;

import io.github.repoboard.dto.QueryStrategyDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class QueryStrategyHolder {

    private final List<QueryStrategyDTO> refreshStrategies = List.of(
            new QueryStrategyDTO("stars:>5000", "stars"),
            new QueryStrategyDTO("stars:2000..4999", "stars"),
            new QueryStrategyDTO("created:>=2024-01-01", "created"),
            new QueryStrategyDTO("forks:>500", "forks"),
            new QueryStrategyDTO("size:>10000", "stars"),
            new QueryStrategyDTO("topic:algorithm", "stars"),
            new QueryStrategyDTO("topic:web" , "stars"),
            new QueryStrategyDTO("topic:ai language:python", "stars"),
            new QueryStrategyDTO("pushed:>=2024-08-01", "updated")
    );
    private final AtomicInteger strategyIndex = new AtomicInteger(0);

    public QueryStrategyDTO getNextStrategy(){
        int index = strategyIndex.updateAndGet(i -> (i + 1) % refreshStrategies.size());
        return refreshStrategies.get(index);
    }
}