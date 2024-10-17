package org.minturtle.careersupport.codereview.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.minturtle.careersupport.codereview.dto.CodeReviewRequest;
import org.minturtle.careersupport.common.facade.GithubPullRequestFacade;
import org.minturtle.careersupport.common.utils.GithubUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GithubCodeReviewServiceTest {


    private final AiCodeReviewClient mockCodeReviewClient = mock(AiCodeReviewClient.class);

    private final GithubUtils mockGithubUtils = mock(GithubUtils.class);

    private final List<String> whiteList = List.of("java", "js");
    private final GithubCodeReviewService githubCodeReviewService = new GithubCodeReviewService(mockCodeReviewClient, mockGithubUtils, whiteList);


    @Test
    @DisplayName("Whitelist에 속한 확장자의 파일만 코드리뷰를 요청할 수 있다.")
    public void testFilterWhiteList() throws Exception{
        //given
        List<GHPullRequestFileDetail> fakeGithubReqFileDetail = List.of(
                mock(GHPullRequestFileDetail.class),
                mock(GHPullRequestFileDetail.class),
                mock(GHPullRequestFileDetail.class)
        );
        CodeReviewRequest codeReviewRequest = new CodeReviewRequest("ghtk", "rpnm", 1);

        GithubPullRequestFacade mockPrFacade = mock(GithubPullRequestFacade.class);

        given(mockGithubUtils.generatePullRequest(
                codeReviewRequest.getGithubToken(),
                codeReviewRequest.getRepositoryName(),
                codeReviewRequest.getPrNumber())
        )
                .willReturn(mockPrFacade);

        given(mockPrFacade.getChangedFiles()).willReturn(fakeGithubReqFileDetail);
        willDoNothing().given(mockPrFacade).comment(any());

        given(fakeGithubReqFileDetail.get(0).getFilename()).willReturn("test.java");
        given(fakeGithubReqFileDetail.get(0).getStatus()).willReturn("modified");
        given(fakeGithubReqFileDetail.get(0).getPatch()).willReturn("122");

        given(fakeGithubReqFileDetail.get(1).getFilename()).willReturn("test.gradle");

        given(fakeGithubReqFileDetail.get(2).getFilename()).willReturn("test.js");
        given(fakeGithubReqFileDetail.get(2).getStatus()).willReturn("modified");
        given(fakeGithubReqFileDetail.get(2).getPatch()).willReturn("124");

        given(mockCodeReviewClient.getAiCodeReview(any())).willReturn(
                Flux.just(
                        new AiCodeReviewClient.ReviewResponse("test.java", "댓글 1"),
                        new AiCodeReviewClient.ReviewResponse("test.js", "댓글 2")
                )
        );


        //when
        StepVerifier.create(githubCodeReviewService.doCodeReview(codeReviewRequest))
                .verifyComplete();

        //then
        // gradle은 필터링되어서 java와 js만 실행되어야 한다.
        verify(mockPrFacade, times(2))
                .comment(any());



        // AI 코드리뷰는 java와 js, 두개에 대해 실행되어야 한다.
        ArgumentCaptor<Flux<AiCodeReviewClient.ReviewRequest>> captor = forClass(Flux.class);

        verify(mockCodeReviewClient, times(1))
                .getAiCodeReview(captor.capture());
        Flux<AiCodeReviewClient.ReviewRequest> capturedFlux = captor.getValue();
        StepVerifier.create(capturedFlux)
                .expectNextCount(2)
                .verifyComplete();
    }

}