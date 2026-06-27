package com.ncportal.app.data

import com.ncportal.app.model.Entry
import com.ncportal.app.model.EntryType

/**
 * In-memory placeholder board data so the UI shell is runnable before a real
 * backend exists. Boards = folders; posts = .md files carrying markdown [content].
 */
class SampleExplorerRepository : ExplorerRepository {

    override fun root(): Entry = ROOT

    private companion object {
        private fun board(id: String, name: String, posts: List<Entry>) =
            Entry(id = id, name = name, type = EntryType.FOLDER, children = posts)

        private fun post(id: String, fileName: String, modifiedAt: Long, content: String) =
            Entry(id = id, name = fileName, type = EntryType.FILE, modifiedAt = modifiedAt, content = content)

        val ROOT = board("root", "NC Portal", listOf(

            board("notice", "공지사항", listOf(
                post("n1", "portal-maintenance.md", 1_782_300_000_000, """
                    # 2026년 상반기 포털 점검 안내
                    6월 30일 02:00~05:00 사이 포털 접속이 일시 중단됩니다.

                    점검 대상은 다음과 같습니다.
                    - 파일 전송 모듈
                    - 게시판 검색 인덱스

                    > 점검 중에는 **로그인** 및 *파일 다운로드*가 제한됩니다.

                    자세한 내용은 [운영 정책](https://portal.example.com/policy)을 확인하세요.
                """.trimIndent()),
                post("n2", "reader-update.md", 1_782_000_000_000, """
                    # 신규 기능: 게시판 리더 출시
                    이제 `.md` 게시글을 앱에서 바로 읽을 수 있습니다.

                    ## 주요 변경점
                    1. 게시판 목록에 게시글 수 표시
                    2. 게시글 본문 마크다운 렌더링
                    3. 제목 자동 추출 (`# 제목` 기준)
                """.trimIndent()),
            )),

            board("free", "자유게시판", listOf(
                post("f1", "lunch-place.md", 1_781_900_000_000, """
                    # 회사 앞 점심 맛집 공유합니다
                    오늘 다녀온 **김밥천국 신관점** 후기예요.

                    - 가성비 최고, 김치찌개 추천
                    - 12시엔 사람 많으니 *11시 50분*쯤 가세요

                    ---
                    다음엔 분식 말고 한식 어떠세요?
                """.trimIndent()),
                post("f2", "weekend-hike.md", 1_781_500_000_000, """
                    # 주말 북한산 등산 모임 후기
                    날씨가 좋아 정상까지 다녀왔습니다.

                    > 다음 모임은 다음 달 둘째 주 토요일 예정입니다.

                    참여 신청은 댓글로 남겨주세요.
                """.trimIndent()),
            )),

            board("dev", "개발노트", listOf(
                post("d1", "compose-list-perf.md", 1_782_200_000_000, """
                    # Compose 리스트 성능 메모
                    `LazyColumn` 렌더링을 정리한 노트입니다.

                    ## 핵심 원칙
                    항목마다 **안정적인 key**를 부여하면 불필요한 recomposition을 줄일 수 있습니다.

                    ### 권장 패턴
                    1. `items(list, key = { it.id })` 사용
                    2. 람다 안에서 *무거운 연산* 피하기
                    3. 이미지엔 placeholder 지정

                    예시 코드:
                    ```kotlin
                    LazyColumn {
                        items(posts, key = { it.id }) { post ->
                            PostRow(post)
                        }
                    }
                    ```

                    > 측정 없이 최적화하지 마세요. `Layout Inspector`로 먼저 확인합니다.

                    ---
                    참고: [Compose 성능 가이드](https://developer.android.com/jetpack/compose/performance)
                """.trimIndent()),
                post("d2", "ssh-build.md", 1_781_000_000_000, """
                    # 원격 SSH 빌드 파이프라인 정리
                    로컬에서 컴파일하지 않고 원격 PC에서 빌드합니다.

                    - 소스 동기화 후 `./gradlew assembleDebug` 실행
                    - 산출물 APK만 회수

                    자세한 절차는 사내 위키를 참고하세요.
                """.trimIndent()),
            )),

            board("qna", "Q&A", listOf(
                post("q1", "reset-password.md", 1_782_100_000_000, """
                    # 비밀번호 초기화는 어떻게 하나요?
                    설정 화면에서 직접 변경할 수 있습니다.

                    1. **설정** 탭으로 이동
                    2. *계정* 항목 선택
                    3. `비밀번호 재설정` 버튼 클릭
                """.trimIndent()),
                post("q2", "dark-mode-issue.md", 1_781_800_000_000, """
                    # 다크모드가 적용되지 않아요
                    시스템 테마 설정을 확인해 주세요.

                    > 설정 > 테마에서 **시스템 설정 따르기**가 켜져 있으면
                    > 기기 다크모드를 따라갑니다.
                """.trimIndent()),
            )),

            board("tasks", "업무관리", listOf(
                post("t1", "업무보고서-양식.md", 1_782_500_000_000, """
                    # 업무 보고서

                    ## 기본 정보
                    - **작성자**: 홍길동
                    - **보고일자**: 2026년 6월 27일
                    - **소속**: 개발팀

                    ---

                    ## 금일 업무 내용

                    ### 완료 업무
                    1. 주간 코드 리뷰 참여
                    2. API 명세서 작성 완료
                    3. 버그 수정 3건 처리

                    ### 진행 중 업무
                    1. 신규 기능 개발 (약 70% 완료)
                    2. 단위 테스트 작성 중

                    ### 미완료 업무
                    - 디자인팀 피드백 반영 예정
                    - 배포 파이프라인 점검 필요

                    ---

                    ## 이슈 및 특이사항
                    > API 응답 지연 이슈가 발생하였으나 원인 파악 중입니다.
                    > 내일 중으로 해결 방안을 공유할 예정입니다.

                    ## 내일 업무 계획
                    1. API 지연 이슈 원인 분석 및 수정
                    2. 신규 기능 개발 완료 목표
                    3. 코드 리뷰 요청
                """.trimIndent()),
                post("t2", "업무요청서-양식.md", 1_782_480_000_000, """
                    # 업무 요청서

                    ## 요청 정보
                    - **요청자**: 홍길동
                    - **요청일**: 2026년 6월 27일
                    - **처리 기한**: 2026년 7월 4일
                    - **우선순위**: *높음*

                    ---

                    ## 요청 내용

                    ### 요청 제목
                    신규 사용자 인증 모듈 개발

                    ### 요청 배경
                    현재 인증 방식이 *보안 취약점*을 가지고 있어 **즉시 개선**이 필요합니다.

                    ### 요청 사항
                    1. OAuth 2.0 기반 로그인 구현
                    2. 세션 만료 시간 설정 (`기본 30분`)
                    3. 로그인 시도 횟수 제한 기능

                    ---

                    ## 처리 결과
                    > 담당자가 처리 후 아래 항목을 기입합니다.

                    - **처리자**:
                    - **처리일**:
                    - **처리 내용**:
                """.trimIndent()),
                post("t3", "주간계획서-양식.md", 1_782_460_000_000, """
                    # 주간 업무 계획서

                    ## 기간
                    **2026년 6월 30일 ~ 7월 4일** (25주차)

                    ---

                    ## 주간 목표
                    1. v0.3.0 배포 완료 및 안정화
                    2. 업무관리 시스템 설계 완료
                    3. 팀 회의록 3건 정리

                    ---

                    ## 요일별 계획

                    ### 월요일 (6/30)
                    - 주간 업무 공유 회의
                    - 코드 리뷰 완료

                    ### 화요일 (7/1)
                    - *신규 기능* 개발 착수
                    - 단위 테스트 작성

                    ### 수요일 (7/2)
                    - **중간 점검** 보고서 제출
                    - 팀 세미나 준비

                    ### 목요일 (7/3)
                    - 버그 수정 및 QA 대응
                    - 문서 업데이트

                    ### 금요일 (7/4)
                    - 주간 결과 보고
                    - 다음 주 계획 수립

                    ---

                    ## 비고
                    > 일정 변동 시 팀장에게 사전 보고 후 수정합니다.
                """.trimIndent()),
            )),
        ))
    }
}
