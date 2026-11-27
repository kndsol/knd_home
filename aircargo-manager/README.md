# Air Cargo Manager (Android)

항공화물 통합 관리 앱 — 운항 스케쥴 / 화물예약 / 화물추적 / 실시간 운항현황 / 메일 알림

## 1. 개발 환경

| 항목 | 버전 |
|---|---|
| IDE | Android Studio Ladybug(2024.2) 이상 |
| JDK | 17 (Android Studio 내장 JBR 사용 가능) |
| Gradle / AGP | 8.11.1 / 8.7.3 |
| Kotlin | 2.0.21 (Compose Compiler Plugin) |
| UI | Jetpack Compose (BOM 2024.12.01), Material3 |
| SDK | minSdk 26 (Android 8.0) / target·compile 35 |
| 주요 라이브러리 | Navigation Compose, JavaMail(android-mail 1.6.7) |

## 2. 빌드 / 실행

1. Android Studio → `Open` → 본 폴더 선택
2. Gradle Sync 완료 대기 (최초 1회 의존성 다운로드)
3. 에뮬레이터 또는 실기기 선택 → `Run ▶`
4. APK 생성: `Build > Build App Bundle(s) / APK(s) > Build APK(s)`
   - CLI: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## 3. 로그인

- 아이디 / 비밀번호 : `admin` / `admin` (고정, `AppViewModel.LOGIN_ID / LOGIN_PW`)

## 4. 기능 구성 (요구사항 매핑)

| 구분 | 요구사항 | 구현 |
|---|---|---|
| A | 운항스케쥴 — 인천공항 연동 또는 화면 | data.go.kr 인천공항 API 연동 (항공기 운항현황 상세 — 여객편/화물기), 출발·도착 탭, 항공사·편명 필터 / 키 미설정 시 예시 데이터 + 인천공항 웹 화면 |
| B | 화물예약 — 고객 입력 | ① 항공사(CI·UL·WE·M7) ② 편명·날짜·Pcs·Weight·Item·Destination·Remark(온도유지·Dry Ice 체크) ③ 예약자 회사·이름·전화번호 / 입력 검증, 확인 후 등록, 예약 내역·상태(요청/확정/취소)·공유 |
| C | 화물추적 — 각 항공사 WEB 연결 | AWB 입력(Prefix 자동, 체크디지트 검증) → 클립보드 복사 후 항공사 화물 WEB(WebView) 연결 |
| D | 실시간 운항현황 — Flightradar24 | ICN 공항 화면, 편명 검색, 스케쥴 편명 빠른 선택, 외부 앱/브라우저 열기 |
| 추가 | 알림 설정 — Gmail SMTP | 앱 비밀번호 발급 안내, 연결 점검·오류 원인 안내, 항공사별 수신자, 예약자 접수 확인 메일, 메일 양식 편집(변수), 발송 이력·재발송, 테스트 발송 |

## 5. 운영 전 설정 항목

### 5.1 인천공항 API 서비스키
- 공공데이터포털(data.go.kr) 활용신청: `인천국제공항공사_항공기 운항 현황 상세 조회` (statusOfAllFltDeOdp)
  - 출발: `getFltDeparturesDeOdp` / 도착: `getFltArrivalsDeOdp`
  - 여객기(P)·화물기(C) 구분, 조회일 기준 D-3 ~ D+6, 개발계정 일 500건
- 앱 `설정 > 인천공항 API 서비스키` 입력 (Encoding/Decoding 키 모두 가능)
- 응답 필드명 상이 대비 → 다중 후보 키로 파싱 (`ScheduleRepository.toFlight`)

### 5.2 Gmail 알림
- 발신 계정: Google 계정 **2단계 인증 활성화 → 보안 > 앱 비밀번호 생성(16자리)**
  - 일반 Gmail 비밀번호로는 SMTP 인증 불가 (535 오류)
- SMTP: `smtp.gmail.com:587` STARTTLS (`data/Notification.kt`)
- 앱 `알림 설정`에서 계정·수신자 등록 → `저장 후 테스트 메일 발송`으로 확인
- Gmail 일일 발송 한도 존재 (개인 계정 약 500통/일)

### 5.3 항공사 정보 (`data/Airlines.kt`)
| 코드 | 항공사 | AWB Prefix | 화물 WEB |
|---|---|---|---|
| CI | 중화항공 | 297 | cargo.china-airlines.com |
| UL | 스리랑칸항공 | 603 | srilankancargo.com |
| WE | 파라타항공 | (미확인) | parataair.com |
| M7 | MAS AIR (MASkargo) | 232 | maskargo.com 추적 페이지 |

- WE Prefix, 각 항공사 추적 상세 URL → 항공사 GSA 확인 후 수정

## 6. 제약 사항 (Prototype)

- 예약·알림 설정: 단말 내부 저장(SharedPreferences) — 서버 미연동
  - 서버 연동 시 `BookingRepository` 교체
- Gmail 앱 비밀번호: 단말 앱 전용 저장소 평문 저장 → 운영 시 EncryptedSharedPreferences 또는 서버 발송 방식 전환 권장
- 화물추적·FR24: 외부 사이트 화면 연결 방식 (사이트 구조 변경 시 영향)
- 로그인: 고정 계정 (인증 서버 미연동)

## 7. 소스 구조

```
app/src/main/java/com/aircargo/manager/
├─ MainActivity.kt
├─ AppViewModel.kt            # 로그인·예약·스케쥴·알림 상태
├─ data/
│  ├─ Airlines.kt             # 항공사·외부 URL
│  ├─ Booking.kt              # 예약 모델·저장소
│  ├─ ScheduleRepository.kt   # 인천공항 API
│  └─ Notification.kt         # 알림 설정·Gmail SMTP 발송
└─ ui/
   ├─ AppNavHost.kt           # 화면 라우팅
   ├─ components/Common.kt
   ├─ theme/Theme.kt
   └─ screens/                # Login, Home, Schedule, BookingForm, BookingList,
                              # Tracking, LiveStatus, Web, Settings, NotifySettings
```
