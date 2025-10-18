package io.github.repoboard.controller;

import io.github.repoboard.dto.view.SavedRepoView;
import io.github.repoboard.dto.view.UserView;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.SavedRepoDBService;
import io.github.repoboard.service.SavedRepoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 사용자가 저장한 GitHub 레포지토리를 관리하는 컨트롤러입니다.
 *
 * <p>
 * 저장, 삭제, 노트 추가, 핀 고정 등 개인화된 레포지토리 기능을 제공합니다.
 * </p>
 */
@Controller
@RequestMapping("/users/saved/repos")
@RequiredArgsConstructor
public class SavedRepoController {

    private final SavedRepoService savedRepoService;
    private final SavedRepoDBService savedRepoDBService;
    private final GitHubApiService gitHubApiService;

    /**
     * 저장한 레포지토리 페이지를 렌더링합니다.
     *
     * @param principal 현재 로그인한 사용자 정보
     * @param language 언어 필터 값 (예: Java, Python)
     * @param sort 정렬 기준 (예: popular, recent)
     * @param pinnedPage 핀 고정된 레포의 페이지 번호
     * @param unpinnedPage 고정되지 않은 레포의 페이지 번호
     * @param model Thymeleaf 렌더링 모델
     * @return 저장한 레포지토리 뷰
     */
    @GetMapping
    public String showSavedRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                                @RequestParam(required = false) String language,
                                @RequestParam(required = false, defaultValue = "popular") String sort,
                                @RequestParam(defaultValue = "0") int pinnedPage,
                                @RequestParam(defaultValue = "0") int unpinnedPage,
                                RedirectAttributes ra,
                                Model model){
        model.addAttribute("user", UserView.from(principal.getUser()));
        try {
            SavedRepoView view = savedRepoService.loadSavedRepos(principal.getUser().getId(),
                    language, sort, pinnedPage, unpinnedPage);

            model.addAttribute("pinnedRepos", view.getPinnedRepos());
            model.addAttribute("unpinnedRepos", view.getUnpinnedRepos());
            model.addAttribute("languageOptions", view.getLanguageOptions());
            model.addAttribute("selectedLanguage", language);
            model.addAttribute("sort", sort);

            return "repository/saved";
        } catch (EntityNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/users/saved/repos";
        } catch (Exception e){
            ra.addFlashAttribute("error", "저장소 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/users/saved/repos";
        }
    }

    /**
     * 저장한 레포의 README 마크다운 내용을 불러옵니다.
     *
     * @param repoId GitHub 레포지토리 ID
     * @return README 원문 내용
     */
    @GetMapping("/{repoId}/readme")
    public ResponseEntity<String> getReadmeByRepoId(@PathVariable Long repoId){
        try{
            String readme = gitHubApiService.getReadmeById(repoId);
            if(readme == null || readme.isBlank()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("README를 찾을 수 없습니다.");
            }
            return ResponseEntity.ok(readme);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest()
                    .body("잘못된 요청입니다: " + e.getMessage());
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("README를 불러오는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자가 특정 GitHub 레포를 저장합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param id GitHub 레포지토리 ID
     * @param ra 플래시 메시지 전달용 RedirectAttributes
     * @return 저장한 레포 페이지로 리다이렉트
     */
    @PostMapping
    public String saveRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                           @RequestParam("id") Long id,
                           RedirectAttributes ra){
        try{
            savedRepoService.savedRepoById(id, principal.getUser().getId());
        } catch (IllegalArgumentException | EntityNotFoundException e){
            ra.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e){
            ra.addFlashAttribute("error", "저장 중 서버 오류가 발생했습니다.");
        } catch (Exception e){
            ra.addFlashAttribute("error", "알 수 없는 오류가 발생했습니다.");
        }

        return "redirect:/users/saved/repos";
    }

    /**
     * 저장된 레포를 삭제합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param repoGithubId GitHub 레포지토리 ID
     * @return 저장한 레포 페이지로 리다이렉트
     */
    @PostMapping("/delete/{repoGithubId}")
    public String deleteRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                             RedirectAttributes ra,
                             @PathVariable Long repoGithubId){
        try{
            savedRepoDBService.delete(repoGithubId, principal.getUser().getId());
        } catch (EntityNotFoundException e){
            ra.addFlashAttribute("error", e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error","레포지토리 삭제 중 오류가 발생했습니다.");
        }

        return "redirect:/users/saved/repos";
    }

    /**
     * 저장된 레포에 메모(노트)를 추가 또는 수정합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param repoGithubId GitHub 레포지토리 ID
     * @param note 메모 내용
     * @return 저장한 레포 페이지로 리다이렉트
     */
    @PostMapping("/note/{repoGithubId}")
    public String updateNote(@AuthenticationPrincipal CustomUserPrincipal principal,
                             RedirectAttributes ra,
                             @PathVariable Long repoGithubId,
                             @RequestParam("note") String note){
        try {
            savedRepoDBService.updateNote(repoGithubId, principal.getUser().getId(), note);
        }catch (EntityNotFoundException e){
            ra.addFlashAttribute("error", e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error", "메모 업데이트 중 오류가 발생했습니다.");
        }

        return "redirect:/users/saved/repos";
    }

    /**
     * 저장된 레포의 핀 고정 여부를 업데이트합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param repoGithubId GitHub 레포지토리 ID
     * @param isPinned true면 고정, false면 해제
     * @return 저장한 레포 페이지로 리다이렉트
     */
    @PostMapping("/pin/{repoGithubId}")
    public String updatePinned(@AuthenticationPrincipal CustomUserPrincipal principal,
                               @PathVariable Long repoGithubId,
                               RedirectAttributes ra,
                               @RequestParam boolean isPinned){
        try {
            savedRepoDBService.updatePin(repoGithubId, principal.getUser().getId(), isPinned);
        }catch (EntityNotFoundException e){
            ra.addFlashAttribute("error", e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error", "핀 업데이트 중 오류가 발생했습니다.");
        }

        return "redirect:/users/saved/repos";
    }
}