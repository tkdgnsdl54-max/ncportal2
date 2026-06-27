# NC Portal

파일 탐색기 느낌의 UI를 가진 안드로이드 앱. **Kotlin + Jetpack Compose (Material 3)** 로 작성합니다.

> 현재는 "큰 틀"(앱 골격)만 잡혀 있습니다. 탐색기형 UI 쉘 + 추상화된 데이터 모델까지 동작하며,
> 실제 데이터 소스(SSH/SFTP·클라우드·로컬 등)는 `ExplorerRepository` 구현만 갈아끼우면 됩니다.

## 화면 구성 (구현된 것)

하단 탭 4개로 구성된 포털 셸(v0.2.0):

- **홈** — 저장 공간 요약(샘플), 빠른 작업(업로드/새 폴더/검색), 최근 파일 목록
- **파일** — 탐색기: 리스트/그리드 전환, 브레드크럼 경로 점프, 폴더 드릴다운, 시스템 back으로 상위 이동, 폴더 우선·이름순 정렬, 확장자별 아이콘, 빈 폴더 상태
- **전송** — 업로드/다운로드 큐 UI(현재 샘플 디자인)
- **설정** — 테마(시스템/라이트/다크), 다이내믹 컬러 토글(Android 12+), 앱 버전, 업데이트 확인(자리)

> 탭 전환은 navigation-compose 없이 `rememberSaveable` 상태로 처리(의존성 최소화).
> 데이터는 아직 `SampleExplorerRepository`(인메모리 샘플) — 실제 소스는 `ExplorerRepository` 구현만 교체하면 됨.

## 구조

```
app/src/main/java/com/ncportal/app/
├─ MainActivity.kt              # 진입점, edge-to-edge → NCPortalApp()
├─ model/Entry.kt               # 트리 노드 모델 (FOLDER/FILE)
├─ data/
│  ├─ ExplorerRepository.kt     # 데이터 소스 추상 인터페이스 ← 여기를 교체
│  └─ SampleExplorerRepository.kt  # 임시 인메모리 샘플 트리
└─ ui/
   ├─ NCPortalApp.kt            # 앱 루트: 테마 + 하단 탭 셸(홈/파일/전송/설정)
   ├─ FileFormat.kt             # 공용 유틸: 크기/날짜 포맷, 파일 아이콘
   ├─ components/SectionTopBar.kt  # 공용 섹션 상단바
   ├─ home/                     # HomeScreen + HomeViewModel (대시보드)
   ├─ transfers/TransfersScreen.kt # 전송 목록 (샘플 디자인)
   ├─ settings/SettingsScreen.kt   # 설정 (테마/다이내믹컬러/버전)
   ├─ theme/                    # Color / Type / Theme (Material 3)
   └─ explorer/                 # 파일 탭 (기존 탐색기)
      ├─ ExplorerUiState.kt     # 화면 상태 + ViewMode
      ├─ ExplorerViewModel.kt   # 폴더 스택 기반 내비게이션 상태
      └─ ExplorerScreen.kt      # 탐색기 Compose UI
```

## 버전 (gradle/libs.versions.toml)

| 항목 | 버전 |
|---|---|
| Gradle | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.10.01 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| JDK | 17 |

## 원격 SSH 빌드

로컬(Windows)에는 빌드 도구가 없으므로, 이 폴더를 원격 서버로 **동기화**한 뒤 거기서 빌드합니다.

### 1. 원격 서버 준비 (최초 1회)

- **JDK 17** 설치 (`java -version` 으로 17 확인)
- **Android SDK** 설치 후 둘 중 하나로 위치 지정:
  - 환경변수: `export ANDROID_HOME=/path/to/android-sdk`
  - 또는 프로젝트 루트에 `local.properties` 생성:
    ```properties
    sdk.dir=/path/to/android-sdk
    ```
- 필요한 SDK 패키지: `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`

### 2. 동기화 (Windows → 원격)

예시 (rsync, Git Bash 기준). `.gitignore` 대상(`build/`, `.gradle/` 등)은 제외:

```bash
rsync -avz --delete \
  --exclude '.gradle/' --exclude 'build/' --exclude 'local.properties' \
  ./ user@remote:/home/user/ncportal2/
```

### 3. 빌드 (원격에서)

```bash
cd ~/ncportal2
chmod +x gradlew          # 최초 1회 (Windows에서 동기화 시 실행권한이 빠질 수 있음)
./gradlew assembleDebug   # 결과물: app/build/outputs/apk/debug/app-debug.apk
```

### 4. 기기 설치

APK를 받아와 설치하거나, 원격에 기기/에뮬레이터가 연결돼 있다면:

```bash
./gradlew installDebug
```

## 다음 단계 (제안)

1. 실제 데이터 소스 결정 후 `ExplorerRepository` 구현체 작성 (예: SFTP 클라이언트)
2. 로딩/에러 상태를 `ExplorerUiState`에 추가 (네트워크 비동기 대응)
3. 파일 탭 동작(미리보기/다운로드/열기) 연결 — 현재는 폴더만 진입
4. 정렬·검색 옵션, 다운로드 진행률 등 UI 확장
