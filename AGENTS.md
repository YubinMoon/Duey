# AGENTS.md

이 문서는 Duey 저장소에서 에이전트가 안전하게 작업하기 위한 실행 지침이다. README처럼 프로젝트를 소개하기보다, 변경 전 확인할 규칙과 검증 기준을 우선한다.

## 1. Working Principles

- 모호한 요구사항은 구현 전에 질문한다. 여러 해석이 가능하면 선택지를 드러낸다.
- 필요한 파일만 수정한다. 인접 코드 정리, 스타일 변경, 리팩터링은 요청이 없으면 하지 않는다.
- 기존 구조와 패턴을 우선한다. 새 전역 상태, 새 계층, 새 추상화는 실제 필요가 있을 때만 추가한다.
- 사용자가 만든 변경사항을 되돌리지 않는다. 충돌하거나 작업을 막는 경우에만 사용자에게 확인한다.
- 모든 변경은 검증 가능한 목표로 끝낸다. 테스트나 빌드를 생략하면 이유를 최종 보고에 적는다.

## 2. Project Summary

Duey는 Android Jetpack Compose와 Material 3 기반의 할 일/일정 관리 앱이다. 하단 탭으로 숙제 목록, 달력 일정, 새 일정 추가, 더보기/관리 화면을 전환한다.

현재 앱은 Room DB를 중심으로 일정, 카테고리, 반복 일정 템플릿을 관리한다. 완료 상태, JSON 백업/복원, Glance 홈 화면 위젯, stage 업데이트 확인, Google 인증, 서버 동기화, AI 일정 파싱 흐름이 포함되어 있다.

저장소에는 Android 앱 모듈 `:app`과 Spring Boot 서버 모듈 `:server`가 함께 있다. 서버 모듈은 장기적으로 앱과 같은 저장소에서 함께 배포하는 대상이다. `dev`에서 개발 중일 때는 두 모듈이 항상 동기화될 필요는 없지만, 배포 단계에서는 앱과 서버 상태가 동기화되어야 한다.

## 3. Tech Stack

- Android Gradle Plugin: `9.1.1`
- Kotlin: `2.3.21`
- KSP: `2.3.6`
- Java toolchain: `17`
- Compile SDK: `37`
- Min SDK: `26`
- Target SDK: `35`
- Compose BOM: `2026.04.01`
- Room: `2.8.4`
- Glance AppWidget: `1.1.1`
- Spring Boot: `4.0.6`
- Formatting: Spotless `8.4.0`, ktlint `1.5.0`, google-java-format `1.24.0`

## 4. Build Variants and Versioning

- `debug`: `com.terry.duey.debug`, app label `Duey Debug`
- `stage`: `com.terry.duey.stage`, app label `Duey Stage`, debuggable, update check enabled
- `prod`: `com.terry.duey`, app label `Duey`

앱 버전은 [app/build.gradle.kts](app/build.gradle.kts)의 `appVersionName`을 SemVer 형식(`MAJOR.MINOR.PATCH`)으로 수정한다. `versionCode`는 `appVersionName`에서 계산된다.

## 5. Source Layout

### Android App

- [app/src/main/java/com/terry/duey/MainActivity.kt](app/src/main/java/com/terry/duey/MainActivity.kt): `enableEdgeToEdge()`, `MyTodoTheme`, `Scaffold`, 하단 탭 전환.
- [app/src/main/java/com/terry/duey/model/TodoItem.kt](app/src/main/java/com/terry/duey/model/TodoItem.kt): `AppDate`, `TodoItem`, `Category`.
- [app/src/main/java/com/terry/duey/model/RecurringTemplate.kt](app/src/main/java/com/terry/duey/model/RecurringTemplate.kt): 반복 일정 템플릿과 반복 타입.
- [app/src/main/java/com/terry/duey/data/AppDatabase.kt](app/src/main/java/com/terry/duey/data/AppDatabase.kt): Room DB. 현재 DB 이름은 `duey_v1_database`, version은 `1`, `exportSchema = false`.
- [app/src/main/java/com/terry/duey/data/TodoDao.kt](app/src/main/java/com/terry/duey/data/TodoDao.kt): 일정 조회, 추가, 수정, 삭제, 완료 상태 변경.
- [app/src/main/java/com/terry/duey/data/CategoryDao.kt](app/src/main/java/com/terry/duey/data/CategoryDao.kt): 카테고리 조회, 추가, 삭제, 이름 변경.
- [app/src/main/java/com/terry/duey/data/RecurringTemplateDao.kt](app/src/main/java/com/terry/duey/data/RecurringTemplateDao.kt): 반복 일정 템플릿 조회와 변경.
- [app/src/main/java/com/terry/duey/viewmodel/TodoViewModel.kt](app/src/main/java/com/terry/duey/viewmodel/TodoViewModel.kt): Room DAO Flow를 `StateFlow`로 노출하고 일정/카테고리/반복 일정/백업 기능을 조정한다.
- [app/src/main/java/com/terry/duey/ui](app/src/main/java/com/terry/duey/ui): Compose 화면.
- [app/src/main/java/com/terry/duey/widget/TodoWidget.kt](app/src/main/java/com/terry/duey/widget/TodoWidget.kt): Glance AppWidget.
- [app/src/main/java/com/terry/duey/auth](app/src/main/java/com/terry/duey/auth): Google 인증과 앱 세션.
- [app/src/main/java/com/terry/duey/sync](app/src/main/java/com/terry/duey/sync): 서버 동기화 클라이언트.
- [app/src/main/java/com/terry/duey/update](app/src/main/java/com/terry/duey/update): stage 업데이트 확인, 다운로드, 설치.
- [app/src/main/java/com/terry/duey/ai](app/src/main/java/com/terry/duey/ai): 일정 음성/텍스트 파싱 클라이언트.

### Server

- [server/src/main/java/com/terry/duey](server/src/main/java/com/terry/duey): Spring Boot 서버.
- [server/src/main/java/com/terry/duey/auth](server/src/main/java/com/terry/duey/auth): Google token 검증, JWT 발급, Spring Security.
- [server/src/main/java/com/terry/duey/sync](server/src/main/java/com/terry/duey/sync): 일정 동기화 API와 SQLite repository.
- [server/src/main/java/com/terry/duey/ai](server/src/main/java/com/terry/duey/ai): Gemini 기반 일정 파싱 API와 debug provider.
- [server/src/main/resources](server/src/main/resources): 환경별 Spring 설정과 `schema.sql`.

### Tests

- [app/src/test/java](app/src/test/java): JVM 단위 테스트.
- [app/src/androidTest/java](app/src/androidTest/java): Android/Compose 계측 테스트.
- [server/src/test/java](server/src/test/java): Spring Boot 서버 테스트.

## 6. Common Commands

Windows PowerShell에서는 `./gradlew`보다 `.\gradlew.bat`을 우선 사용한다.

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat format
.\gradlew.bat test
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleStage
.\gradlew.bat :app:assembleProd
.\gradlew.bat :server:lint
.\gradlew.bat :server:test
.\gradlew.bat :server:bootRunDebug
.\gradlew.bat connectedStageAndroidTest
```

## 7. Android Development Rules

- UI 상태는 가능하면 `TodoViewModel`의 `StateFlow`와 Room DAO 흐름을 확장해서 다룬다.
- Compose 화면에서는 `collectAsStateWithLifecycle()` 패턴을 우선한다.
- 날짜 비교와 범위 계산은 `AppDate`의 `Comparable`, `addDays`, `daysUntil`을 우선 사용한다.
- 시작일/종료일 선택은 기존 `RangeDatePickerDialog`를 재사용한다.
- 색상은 `MaterialTheme.colorScheme` 또는 [app/src/main/java/com/terry/duey/ui/theme/Color.kt](app/src/main/java/com/terry/duey/ui/theme/Color.kt)의 의미 있는 토큰을 사용한다. 화면별 임의 `Color(0xFF...)` 추가는 피한다.
- `MainActivity`는 `enableEdgeToEdge()`와 `consumeWindowInsets(innerPadding)`을 사용한다. 폼 화면 수정 시 `imePadding`, `navigationBarsPadding`, `consumeWindowInsets` 상호작용을 확인한다.
- 하단 탭 테스트 태그 `tab_homework`, `tab_schedule`, `tab_new`, `tab_more`는 UI 테스트가 의존하므로 변경 시 테스트도 함께 수정한다.

## 8. Data Rules

- 현재는 개발 초기 단계이므로 Room schema 변경 시 destructive migration을 허용한다.
- Room entity, converter, DAO schema를 바꾸면 DB version 증가 여부와 destructive migration 적용 범위를 함께 판단한다.
- 정식 migration 정책과 schema export는 추후 도입 예정이다. 관련 작업을 시작하면 정책을 먼저 확정한다.
- 기본 카테고리는 삭제/이름 변경 제한 대상이다. 카테고리 삭제 시 해당 일정의 fallback 처리도 유지한다.
- `TodoItem.isCompleted`는 목록, 달력, 관리 화면에 모두 영향을 준다.
- 반복 일정은 `RecurringTemplate`과 생성된 `TodoItem.recurringTemplateId`, `recurringOccurrenceDate`의 관계를 깨지 않게 변경한다.
- 백업/복원 JSON 필드명은 사용자 파일과 호환되어야 한다. 필드 변경 시 이전 백업 import 경로를 고려한다.
- 일정 데이터 모델이나 DAO 반환 타입을 바꾸면 `TodoWidget`, sync payload, 백업/복원을 함께 확인한다.

## 9. Server Rules

- 서버는 Spring Boot Java 17 코드다. Kotlin 앱 코드 스타일을 서버 Java 파일에 적용하지 않는다.
- 서버 Java 변경 후 `.\gradlew.bat :server:lint`와 `.\gradlew.bat :server:test`를 우선 실행한다.
- 인증 변경은 `SecurityConfig`, `JwtService`, `AuthService`, Google token verifier 테스트를 함께 본다.
- sync API 변경은 Android `SyncApiClient`와 서버 `SyncPayload` 호환성을 함께 확인한다.
- AI 파싱 변경은 debug provider와 Gemini provider의 동작 차이를 고려한다.
- AI provider는 교체 가능성을 유지한다. Gemini에만 강하게 결합된 앱/서버 계약을 추가하지 않는다.
- 환경값은 `application-*.yml`과 Gradle property 사용 흐름을 유지한다. secret 값을 저장소에 커밋하지 않는다.

## 10. Git Workflow

- 기본 개발 통합 브랜치는 `dev`이다.
- 새 기능이나 수정 작업은 `dev`에서 `codex/<task-name>` 형식의 작업 브랜치를 만들어 시작한다.
- 작업을 진행할 때는 완료 가능한 작은 단계로 나누고, 각 단계가 끝날 때마다 관련 검증을 실행한 뒤 커밋한다.
- 한 커밋에는 하나의 목적만 담는다. 큰 작업은 구현, 테스트, 문서처럼 검토 가능한 단위로 나누어 단계적으로 커밋한다.
- 커밋 전에는 반드시 `.\gradlew.bat format`을 실행해 코드 포맷을 유지한다. 문서만 수정한 경우에도 실행 여부를 판단하고, 생략하면 최종 보고에 이유를 적는다.
- 커밋 전에는 `git status --short`와 diff를 확인해 의도하지 않은 파일이 포함되지 않았는지 확인한다.
- `stage`와 `main` 브랜치는 직접 수정하거나 작업 대상으로 checkout하지 않는다.
- `stage`는 테스트 기준점 브랜치이고 `main`은 배포 브랜치다. 두 브랜치로의 merge, rebase, reset, push는 사용자가 명시적으로 요청한 경우에만 수행한다.
- 사용자가 완료를 확인하기 전에는 작업 브랜치를 `dev`로 merge하지 않는다.

## 11. Verification Policy

코드 수정 후 기본 검증 순서:

1. `.\gradlew.bat format`
2. `.\gradlew.bat test`
3. `.\gradlew.bat :app:assembleDebug`
4. `.\gradlew.bat :app:assembleStage`
5. `.\gradlew.bat :app:assembleProd`
6. 서버 변경이 있으면 `.\gradlew.bat :server:lint`와 `.\gradlew.bat :server:test`
7. UI/Android 변경이 있고 기기가 준비되어 있으면 `.\gradlew.bat connectedStageAndroidTest`

문서만 수정한 경우 Gradle 검증은 생략할 수 있다. 생략한 이유는 최종 보고에 적는다.

## 12. Known Risks and Cautions

- 일부 Kotlin 파일과 기존 문서에 한글 문자열 인코딩이 깨진 흔적이 있다.
- 깨진 한글 문자열은 별도 일괄 정리 작업으로 확장하지 않는다. 기능 수정 중 발견한 파일만 고친다.
- [app/src/main/java/com/terry/duey/model/TodoItem.kt](app/src/main/java/com/terry/duey/model/TodoItem.kt)의 `dayOfWeekLabel()` 한글 요일 문자열은 현재 깨져 보인다.
- ViewModel 테스트는 `AndroidViewModel(Application)` 구조와 맞지 않을 수 있다. 관련 테스트를 수정할 때 in-memory Room DB 또는 repository 계층 도입 여부를 먼저 판단한다.
- Compose UI 테스트는 문자열 의존도가 높을 수 있다. UI 문자열을 바꾸면 semantic/test tag 보강을 검토한다.
- `prod` signing config는 Gradle property에 의존한다. 로컬에서 `:app:assembleProd`가 서명 정보 때문에 실패할 수 있으므로 실패 원인을 구분해 보고한다.

## 13. Clarifications Needed

아래 항목은 아직 정책 선택이 필요하다. 관련 작업을 시작하기 전에 사용자에게 확인한다.

- `stage` 업데이트 배포 기준:
  - 옵션 A: GitHub Release의 prerelease만 stage 앱 업데이트 대상으로 사용한다.
  - 옵션 B: `stage-*` 또는 `v*-stage.*` 같은 stage 전용 tag 패턴만 업데이트 대상으로 사용한다.
  - 옵션 C: GitHub Release draft/prerelease 여부와 관계없이 최신 release asset을 사용하되, stage 앱에서만 업데이트 확인을 켠다.

## 14. Future Work Candidates

현재 비워둔다. 실제 후보가 생기면 사용자 확인 후 추가한다.
