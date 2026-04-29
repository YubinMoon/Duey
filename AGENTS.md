# Project: Duey (Android / Jetpack Compose)

이 문서는 에이전트가 `Duey` 프로젝트의 현재 상태를 빠르게 파악하고, 기존 구현 방식과 충돌하지 않게 작업하기 위한 기준입니다.

## 0. 주요 참고 문서

* **[DESIGN.md](./DESIGN.md):** UI 수정 시 우선 확인해야 하는 디자인 시스템 문서입니다. 다만 웹/Apple 스타일 기준이 섞여 있으므로 Android Compose 화면에서는 Material 3, 기존 초록색 테마, 현재 화면 밀도와 함께 해석하세요.
* **[R8_Configuration_Analysis.md](./R8_Configuration_Analysis.md):** R8/ProGuard 설정 분석 기록입니다. 릴리스 최적화나 keep rule을 수정할 때 참고하세요.

## 1. 프로젝트 개요

`Duey`는 Jetpack Compose와 Material 3 기반의 할 일/일정 관리 Android 앱입니다. 하단 탭으로 숙제 목록, 달력 일정, 새 일정 추가, 더보기/관리 화면을 전환합니다. 현재 데이터는 Room DB에 저장되며, 카테고리 관리, 완료 상태, JSON 백업/복원, 홈 화면 Glance 위젯이 포함되어 있습니다.

## 2. 기술 스택 및 빌드 환경

* Android Gradle Plugin: `9.1.1`
* Kotlin: `2.3.21`
* Compile SDK: `37`
* Min SDK: `26`
* Target SDK: `35`
* Compose BOM: `2026.04.01`
* Room: `2.8.4` (`room-runtime`, `room-ktx`, KSP compiler)
* Glance AppWidget: `1.1.1`
* Formatting: Spotless `8.4.0`, ktlint `1.5.0`
* Gradle namespace: `com.terry.duey`
* Product flavors: `stage` (`com.terry.duey.stage`, app label `Duey Stage`), `prod` (`com.terry.duey`, app label `Duey`)

주요 명령:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat test
.\gradlew.bat assembleStageDebug
.\gradlew.bat assembleProdDebug
.\gradlew.bat assembleProdRelease
.\gradlew.bat connectedStageDebugAndroidTest
```

Windows PowerShell 환경에서는 `./gradlew`보다 `.\gradlew.bat` 사용을 우선하세요.

## 3. 현재 주요 기능

* **숙제 탭:** 날짜별 할 일 그룹을 보여주며, 리스트 끝 근처에서 추가 날짜 범위를 로드하는 prefetch 방식이 적용되어 있습니다. 오늘 날짜로 이동하는 버튼과 완료 체크박스가 있습니다.
* **일정 탭:** 월간 달력, 월 이동 제스처, 날짜별 일정 개수, 선택 날짜의 일정 목록, 상세/수정 다이얼로그가 있습니다.
* **새 일정 추가:** 제목, 설명, 카테고리, 시작일/종료일을 입력합니다. 기간 선택은 `RangeDatePickerDialog`를 사용합니다.
* **더보기 탭:** 전체 일정 관리, 카테고리 관리, JSON 백업/복원 화면으로 이동합니다.
* **지연 삭제:** 전체 일정 관리 화면에서는 삭제 대상을 먼저 선택하고, 뒤로 나가거나 화면을 종료할 때 일괄 삭제합니다.
* **카테고리 관리:** 카테고리 추가/수정/삭제가 가능하며, 삭제된 카테고리의 일정은 기본 카테고리로 되돌리는 로직이 있습니다.
* **완료 상태:** `TodoItem.isCompleted`로 관리하며, 목록/달력/관리 화면에서 완료 상태를 시각적으로 반영합니다.
* **홈 화면 위젯:** `TodoWidget`이 오늘 날짜에 포함되는 일정을 Glance 기반 위젯으로 표시합니다.

## 4. 주요 파일 구조

### Core & Data

* `app/src/main/java/com/example/mytodo/model/TodoItem.kt`
  * `AppDate`, `TodoItem`, `Category` 정의.
  * `TodoItem`과 `Category`는 Room entity입니다.
* `app/src/main/java/com/example/mytodo/data/AppDatabase.kt`
  * Room singleton database. 현재 DB 이름은 `todo_database`, version은 `1`, `exportSchema = false`.
* `app/src/main/java/com/example/mytodo/data/TodoDao.kt`
  * 전체 일정 Flow 조회, 추가/수정/삭제, 다중 삭제, 완료 토글 쿼리.
* `app/src/main/java/com/example/mytodo/data/CategoryDao.kt`
  * 카테고리 Flow 조회, 추가, 삭제, 이름 변경.
* `app/src/main/java/com/example/mytodo/data/DateConverters.kt`
  * `AppDate` Room 저장을 위한 type converter.
* `app/src/main/java/com/example/mytodo/viewmodel/TodoViewModel.kt`
  * `AndroidViewModel`.
  * Room DAO Flow를 `StateFlow`로 노출합니다.
  * 디버그 빌드에서 DB가 비어 있으면 기본 카테고리와 더미 일정을 삽입합니다.
  * 일정 CRUD, 완료 토글, 카테고리 관리, JSON export/import를 담당합니다.

### UI

* `app/src/main/java/com/example/mytodo/MainActivity.kt`
  * `enableEdgeToEdge()`, `MyTodoTheme`, `Scaffold`, `NavigationBar`, 4개 탭 전환.
  * UI 테스트용 `testTag`: `tab_homework`, `tab_schedule`, `tab_new`, `tab_more`.
* `app/src/main/java/com/example/mytodo/ui/HomeworkScreen.kt`
  * 숙제/할 일 목록 화면.
  * `collectAsStateWithLifecycle()`로 ViewModel StateFlow를 구독합니다.
  * 날짜 그룹, prefetch, 오늘 이동, 완료 토글 UI 포함.
* `app/src/main/java/com/example/mytodo/ui/ScheduleScreen.kt`
  * 달력 기반 일정 화면.
  * 날짜 선택, 월 이동, 일정 상세/수정, 완료 토글, 카테고리 선택 기능 포함.
* `app/src/main/java/com/example/mytodo/ui/NewScheduleScreen.kt`
  * 새 일정 추가 화면.
  * `RangeDatePickerDialog`와 카테고리 선택 다이얼로그를 제공합니다.
* `app/src/main/java/com/example/mytodo/ui/MoreScreen.kt`
  * 더보기 메인 메뉴, 전체 일정 관리, 카테고리 관리, 백업/복원 화면.
  * `ActivityResultContracts.CreateDocument`와 `OpenDocument`로 JSON 파일을 내보내고 가져옵니다.
* `app/src/main/java/com/example/mytodo/widget/TodoWidget.kt`
  * Glance AppWidget 및 `TodoWidgetReceiver`.
  * `AndroidManifest.xml`와 `res/xml/todo_widget_info.xml`에 등록되어 있습니다.

### Theme

* `app/src/main/java/com/example/mytodo/ui/theme/Color.kt`
  * 앱 색상, 요일 색상 등 공통 색상 정의.
* `app/src/main/java/com/example/mytodo/ui/theme/Theme.kt`
  * Material 3 color scheme. Dynamic color는 현재 비활성화되어 있습니다.
* `app/src/main/java/com/example/mytodo/ui/theme/Type.kt`
  * Typography 정의.

### Tests

* `app/src/test/java/com/example/mytodo/viewmodel/TodoViewModelTest.kt`
  * ViewModel 단위 테스트 의도로 작성되어 있으나, 현재 `TodoViewModel`이 `AndroidViewModel(Application)` 기반으로 바뀐 상태와 맞지 않을 수 있습니다.
* `app/src/androidTest/java/com/example/mytodo/ui/TodoUiTest.kt`
  * Compose UI 테스트. 탭 전환, 일정 추가, 완료 토글 흐름을 검증하려는 테스트입니다.

## 5. 개발 지침

* **기존 구조 우선:** 새 기능은 가능하면 `TodoViewModel`과 Room DAO 흐름을 확장하세요. 별도 전역 상태나 화면 내부 임시 저장소를 만들기 전에 기존 `StateFlow`/Room 모델과 맞는지 확인하세요.
* **Room 변경 시 migration 고려:** entity, converter, DAO schema를 바꾸면 DB version 증가와 migration 또는 destructive migration 정책을 함께 결정해야 합니다.
* **날짜 로직:** 날짜 비교/범위 판단은 `AppDate`의 `Comparable`, `addDays`, `daysUntil`을 우선 사용하세요.
* **카테고리:** 기본 카테고리는 삭제/이름 변경 제한 대상입니다. 카테고리 삭제 시 해당 일정의 fallback 처리도 함께 유지하세요.
* **기간 선택:** 시작일/종료일 선택 UI는 `NewScheduleScreen.kt`의 `RangeDatePickerDialog`를 재사용하세요.
* **색상:** 화면별 임의 `Color(0xFF...)` 추가를 피하고, `MaterialTheme.colorScheme` 또는 `ui/theme/Color.kt`의 의미 있는 색상 토큰을 사용하세요.
* **Edge-to-edge/IME:** `MainActivity`가 `enableEdgeToEdge()`와 `consumeWindowInsets(innerPadding)`을 사용합니다. 폼 화면 수정 시 `imePadding`, `navigationBarsPadding`, `consumeWindowInsets` 상호작용을 확인하세요.
* **테스트 태그:** 탭 테스트가 `testTag`에 의존하므로 탭 tag 이름을 바꿀 때는 UI 테스트도 같이 수정하세요.
* **위젯 동기화:** 일정 데이터 모델이나 DAO 반환 타입을 바꾸면 `TodoWidget`도 같이 확인하세요.
* **백업 포맷:** `exportToJson()` / `importFromJson()`의 JSON 필드명은 사용자 백업 파일과 호환됩니다. 필드 변경 시 이전 백업 import를 고려하세요.
* **한글 문자열/인코딩:** 현재 일부 Kotlin 파일과 기존 문서에서 한글 문자열이 깨져 보이는 흔적이 있습니다. 문자열을 수정할 때는 UTF-8 저장 상태를 확인하고, 깨진 문자열을 그대로 확산시키지 마세요.
* **Duey 패키징:** 앱 표시명은 `Duey`를 기준으로 하며, `stage` flavor는 설치 구분을 위해 `Duey Stage`를 사용합니다. 실제 설치 식별자인 `applicationId`는 `prod`가 `com.terry.duey`, `stage`가 `com.terry.duey.stage`입니다.
* **Kotlin package 유지:** 1차 전환에서는 Kotlin 소스 package와 경로(`com.example.mytodo`)를 유지합니다. `namespace`는 `com.terry.duey`이므로 generated `BuildConfig`/`R` 참조를 추가할 때는 새 namespace와 기존 소스 package의 차이를 확인하세요.
* **브랜치 운영:** 기본 개발 통합 브랜치는 `dev`입니다. 새 기능 요청이 오면 항상 `dev`에서 기능별 작업 브랜치를 새로 만들어 작업을 시작하고, 사용자가 완료를 확인한 뒤에만 해당 작업 브랜치를 `dev`로 merge 하세요.
* **stage 브랜치 용도:** `stage`는 테스트용 브랜치입니다. 사용자가 요청할 때만 현재 `dev` 내용을 `stage`로 옮겨 테스트 기준점을 만들고, 일상적인 기능 작업의 직접 대상 브랜치로 사용하지 마세요.
* **main 브랜치 용도:** `main`은 배포용 브랜치입니다. 여러 작업이 정리되고 검증이 끝난 뒤, 사용자가 요청하는 시점에 `stage` 내용을 `main`으로 merge 하는 흐름을 유지하세요.

## 6. 검증 원칙

코드 수정 후 기본 순서:

1. `.\gradlew.bat spotlessApply`
2. `.\gradlew.bat test`
3. `.\gradlew.bat assembleStageDebug`
4. `.\gradlew.bat assembleProdDebug`
5. UI/Android 변경이 있고 기기가 준비되어 있으면 `.\gradlew.bat connectedStageDebugAndroidTest`
6. 배포 후보 확인이 필요하면 `.\gradlew.bat assembleProdRelease`

빌드나 테스트가 실패하면 실패 로그의 핵심 원인을 고친 뒤 다시 실행하세요. 단, 문서만 수정한 경우에는 Gradle 검증을 생략할 수 있으며, 생략 사유를 최종 보고에 명시하세요.

## 8. 향후 작업 후보

* 깨진 한글 문자열과 문서 인코딩 정리.
* Room migration 정책 및 schema export 도입.
* ViewModel 단위 테스트를 Room in-memory DB 또는 repository 계층 기반으로 재작성.
* Compose UI 테스트의 문자열 의존도를 줄이고 semantic/test tag를 보강.
* 위젯 갱신 주기와 앱 내부 변경 후 위젯 업데이트 트리거 개선.
* 일정 필터/검색, 완료 일정 숨김, 카테고리별 통계 기능 검토.
