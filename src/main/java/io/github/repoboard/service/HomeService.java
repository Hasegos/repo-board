package io.github.repoboard.service;

import io.github.repoboard.common.util.QueryStrategyHolder;
import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.dto.strategy.QueryStrategyDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 홈 화면에서 GitHub 레포지토리를 조회하는 서비스.
 *
 * <p>언어, 정렬, 페이지, 새 전략 여부에 따라 GitHub API를 호출하며,
 * 세션을 통해 쿼리 전략을 관리한다.</p>
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final QueryStrategyHolder queryStrategyHolder;
    private final GitHubApiService gitHubApiService;

    /**
     * 홈 화면에서 보여줄 레포지토리를 조회한다.
     *
     * @param language 조회할 언어 (예: Java, Python)
     * @param sort 정렬 기준 (예: popular, recent)
     * @param refresh 새 전략 강제 적용 여부
     * @param page 요청 페이지 번호 (0부터 시작)
     * @param session 세션에 전략 정보를 저장/조회
     * @return GitHub 레포지토리 페이지 결과
     */
    public Page<GithubRepoDTO> getRepos(String language,
                                        String sort,
                                        boolean refresh,
                                        int page,
                                        HttpSession session){
        QueryStrategyDTO strategy = resolveStrategy(refresh,session);
        Pageable pageable = PageRequest.of(page,50);
        return gitHubApiService.fetchRepos(language,pageable, strategy, sort);
    }

    /**
     * 무한 스크롤 등 추가 레포지토리 데이터를 조회한다.
     *
     * @param language 조회할 언어
     * @param sort 정렬 기준
     * @param page 요청 페이지 번호
     * @param session 세션에 저장된 전략을 사용
     * @return GitHub 레포지토리 페이지 결과
     * @throws IllegalArgumentException 전략 정보가 세션에 없을 경우
     */
    public Page<GithubRepoDTO> getMoreRepos(String language,
                                            String sort,
                                            int page,
                                            HttpSession session){
        QueryStrategyDTO strategy = (QueryStrategyDTO) session.getAttribute("refreshStrategy");
        Pageable pageable = PageRequest.of(page, 50);
        return gitHubApiService.fetchRepos(language,pageable, strategy,sort);
    }

    /**
     * 새로고침 여부에 따라 전략을 갱신하거나 기존 전략을 반환한다.
     *
     * @param refresh true면 새로운 전략을 생성해 세션에 저장
     * @param session 세션에 전략을 저장/조회
     * @return 적용할 쿼리 전략
     * @throws IllegalArgumentException 세션에 전략이 없고 refresh=false일 경우
     */
    private QueryStrategyDTO resolveStrategy(boolean refresh, HttpSession session){
        if(refresh){
            QueryStrategyDTO strategy = queryStrategyHolder.getNextStrategy();
            session.setAttribute("refreshStrategy", strategy);
            return strategy;
        }else{
            return (QueryStrategyDTO) session.getAttribute("refreshStrategy");
        }
    }
}